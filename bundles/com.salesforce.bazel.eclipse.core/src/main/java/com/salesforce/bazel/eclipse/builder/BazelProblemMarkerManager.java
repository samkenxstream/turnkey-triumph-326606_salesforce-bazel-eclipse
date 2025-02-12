/**
 * Copyright (c) 2020, Salesforce.com, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */
package com.salesforce.bazel.eclipse.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import com.salesforce.bazel.eclipse.config.BazelEclipseProjectSupport;
import com.salesforce.bazel.sdk.logging.LogHelper;
import com.salesforce.bazel.sdk.model.BazelProblem;

/**
 * Publishes {@link BazelProblem} instances as {@link IMarker} markers to the Problems View. Keeps track of previously
 * published markers so they can be removed again.
 * <p>
 * TODO Review the need for running within a WorkspaceModifyOperation, and whether it is done correctly.
 */
public class BazelProblemMarkerManager {

    private static final String BAZEL_MARKER_TYPE = "com.salesforce.bazel.eclipse.bazelmarker";

    private static final LogHelper LOG = LogHelper.log(BazelProblemMarkerManager.class);

    // owner of marker (combination of project and ownerId) to List of IMarker instances
    private static ConcurrentHashMap<String, List<IMarker>> ownerToMarkers = new ConcurrentHashMap<>();

    private final String ownerId;

    /**
     * Create a new BazelProblemMarkerManager instance.
     *
     * Each BazelProblemMarkerManager references a shared marker store.
     *
     * @param ownerId
     *            an opaque String to associate published markers with, so that they can be found again later
     */
    public BazelProblemMarkerManager(String ownerId) {
        this.ownerId = Objects.requireNonNull(ownerId);
    }

    /**
     * Publishes specified BazelProblem instances as markers to the Problems View.
     *
     * @param problems
     *            the problems to publish
     * @param project
     *            the project to associate the published markers with
     * @param monitor
     *            the progress monitor to use during publishing
     */
    public void publish(List<BazelProblem> problems, IProject project, IProgressMonitor monitor) {
        publish(problems, project, monitor, false);
    }

    /**
     * Publishes specified BazelProblem instances as markers to the Problems View. Previous markers that were published
     * and associated with the specified project and this insgance's ownerId are first removed, before publishing.
     *
     * @param problems
     *            the problems to publish
     * @param project
     *            the project to associate the published markers with
     * @param monitor
     *            the progress monitor to use during publishing
     */
    public void clearAndPublish(List<BazelProblem> problems, IProject project, IProgressMonitor monitor) {
        publish(problems, project, monitor, true);
    }

    /**
     * Clears the markers associated with the specified project and this instance's ownerId.
     *
     * @param project
     *            the project to clear the markers for
     * @param monitor
     *            the progress monitor to use during marker clearing
     */
    public void clear(IProject project, IProgressMonitor monitor) {
        clear(Collections.singleton(project), monitor);
    }

    /**
     * Clears the markers associated with the specified projects and this instance's ownerId.
     *
     * @param project
     *            the project to clear the markers for
     * @param monitor
     *            the progress monitor to use during marker clearing
     */
    public void clear(Collection<IProject> projects, IProgressMonitor monitor) {
        BazelEclipseProjectSupport.runWithProgress(monitor, new WorkspaceModifyOperation() {
            @Override
            protected void execute(IProgressMonitor monitor) {
                runClear(projects);
            }
        });
    }

    private void runClear(Collection<IProject> projects) {
        for (IProject project : projects) {
            List<IMarker> markers = getAndRemoveMarkers(project);
            deleteMarkers(markers);
        }
    }

    private static void deleteMarkers(List<IMarker> markers) {
        for (IMarker marker : markers) {
            try {
                marker.delete();
            } catch (Exception ex) {
                LOG.error("Failed to delete marker " + ex, ex);
            }

        }
    }

    private void publish(List<BazelProblem> problems, IProject project, IProgressMonitor monitor,
            boolean clearBeforePublish) {
        BazelEclipseProjectSupport.runWithProgress(monitor, new WorkspaceModifyOperation() {
            @Override
            protected void execute(IProgressMonitor monitor) {
                if (clearBeforePublish) {
                    runClear(Collections.singleton(project));
                }
                runPublish(problems, project);
            }
        });
    }

    private void runPublish(List<BazelProblem> problems, IProject project) {
        List<IMarker> markers = new ArrayList<>();
        for (BazelProblem problem : problems) {
            String resourcePath = problem.getResourcePath();
            IResource resource = project.findMember(resourcePath);
            try {
                IMarker marker = resource.createMarker(BAZEL_MARKER_TYPE);
                populateMarkerFromBazelProblem(marker, problem);
                markers.add(marker);
            } catch (CoreException ex) {
                LOG.error("Error creating marker for problem: " + problem, ex);
            }
        }
        registerMarkers(project, markers);
    }

    private void registerMarkers(IProject project, List<IMarker> markers) {
        String key = buildOwnerKey(project);
        ownerToMarkers.merge(key, markers, (oldValue, value) -> {
            oldValue.addAll(markers);
            return oldValue;
        });
    }

    private List<IMarker> getAndRemoveMarkers(IProject project) {
        String key = buildOwnerKey(project);
        List<IMarker> markers = ownerToMarkers.remove(key);
        return markers == null ? Collections.emptyList() : markers;
    }

    private String buildOwnerKey(IProject project) {
        return project.toString() + "__" + ownerId;
    }

    private static void populateMarkerFromBazelProblem(IMarker marker, BazelProblem problem) throws CoreException {
        marker.setAttribute(IMarker.MESSAGE, problem.getDescription());
        marker.setAttribute(IMarker.LOCATION, getLocationValue(problem));
        marker.setAttribute(IMarker.LINE_NUMBER, problem.getLineNumber());
        marker.setAttribute(IMarker.SEVERITY, problem.isError() ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING);
    }

    private static String getLocationValue(BazelProblem problem) {
        return "line " + problem.getLineNumber();
    }
}
