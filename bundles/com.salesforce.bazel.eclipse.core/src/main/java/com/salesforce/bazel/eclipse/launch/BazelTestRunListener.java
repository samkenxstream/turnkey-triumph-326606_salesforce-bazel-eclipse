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
package com.salesforce.bazel.eclipse.launch;

import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestRunSession;

import com.salesforce.bazel.sdk.logging.LogHelper;

/**
 * Logging hooks for test execution from BEF.
 */
public class BazelTestRunListener extends TestRunListener {
    static final LogHelper LOG = LogHelper.log(BazelTestRunListener.class);

    @Override
    public void sessionLaunched(ITestRunSession session) {
        LOG.info("test session launched for project {}", session.getLaunchedProject().getProject().getName());
    }

    @Override
    public void sessionStarted(ITestRunSession session) {
        LOG.info("test session started for project {} run {}", session.getLaunchedProject().getProject().getName(),
            session.getTestRunName());
    }

    @Override
    public void sessionFinished(ITestRunSession session) {
        LOG.info("test session finished for project {}", session.getLaunchedProject().getProject().getName());
    }

    @Override
    public void testCaseStarted(ITestCaseElement testCaseElement) {
        LOG.info("test case {}.{} started", testCaseElement.getTestClassName(), testCaseElement.getTestMethodName());
    }

    @Override
    public void testCaseFinished(ITestCaseElement testCaseElement) {
        LOG.info("test case {}.{} finished", testCaseElement.getTestClassName(), testCaseElement.getTestMethodName());
    }

}
