/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.fat.utils.HealthFileUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class MPHealthTestFileBased extends FATServletClient {

    @Server("checkpointMPHealthFileBased")
    public static LibertyServer server;

    private static final String APP_NAME = "mphealth";

    private static final String MESSAGE_LOG = "logs/messages.log";

    public TestMethod testMethod;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.MPHealthFileBasedRepeat("checkpointMPHealthFileBased");

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_NAME);
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        configureBeforeCheckpoint();
        server.setCheckpoint(getCheckpointPhase(), true,
                             server -> {
                                 configureBeforeRestore();
                             });
        server.setConsoleLogName(getTestMethod(TestMethod.class, testName) + ".log");
        server.startServer(true, false); // Do not validate apps since we have a delayed startup.
    }

    private CheckpointPhase getCheckpointPhase() {
        CheckpointPhase phase = CheckpointPhase.AFTER_APP_START;
        switch (testMethod) {
            default:
                break;
        }
        return phase;
    }

    private void configureBeforeCheckpoint() {
        try {
            server.saveServerConfiguration();
            Log.info(getClass(), testName.getMethodName(), "Configuring during checkpoint: " + testMethod);
            switch (testMethod) {
                default:
                    Log.info(getClass(), testName.getMethodName(), "No configuration required: " + testMethod);
                    break;
            }
        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }
    }

    @Test
    public void testDefaultFileBasedHealthChecks() throws Exception {
        String name = getTestMethodNameOnly(testName);

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        /*
         * Check that the startFileHealthCheckProcesses entry trace exists.
         * This indicates that the health files haven't been created before the checkpoint.
         * The files are created for the first time in this method and the timer process kicks off as well.
         * This is also the method that called as part of the CheckpointPhase.onRestore().
         * The restore immediately starts the already started application and by the time this test starts
         * the health files would have been created after resuming the process.
         *
         * Checking the trace that the method entry happens after restore is the only way to validate
         * that the health files were not created before restore.
         */
        Log.info(getClass(), "testDefaultFileBasedHealthChecks", "Testing that startFileHealthCheckProcesses entry trace exists in the trace after restore");
        List<String> myLines = server.findStringsInTrace("startFileHealthCheckProcesses Entry");
        assertEquals("StartFileHealthCheckProcesses has not been found", 1, myLines.size());

        /*
         * Ensure that the Application has started message exists (CWWKZ0001I) and then check that the files are there.
         */

        List<String> lines = server.findStringsInFileInLibertyServerRoot("CWWKZ0001I:", MESSAGE_LOG);
        assertEquals("The CWWKZ0001I Application started message did not appear in messages.log", 1, lines.size());

        /*
         * The started and live files should now have been created in the /health directory.
         * The ready file is not created because the health check returns DOWN.
         *
         * Expect:
         * [X] /health dir
         * [X] Started
         * [ ] Ready
         * [X] Live
         * Don't need to check health dir and live again. Already created from above.
         */
        assertTrue(HealthFileUtils.HEALTH_DIR_SHOULD_HAVE, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        assertTrue(HealthFileUtils.STARTED_SHOULD_HAVE, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        assertTrue(HealthFileUtils.LIVE_SHOULD_HAVE, HealthFileUtils.getLiveFile(serverRootDirFile).exists());
        assertFalse(HealthFileUtils.READY_SHOULD_NOT_HAVE, HealthFileUtils.getReadyFile(serverRootDirFile).exists());

    }

    private void configureBeforeRestore() {
        try {
            Log.info(getClass(), testName.getMethodName(), "Configuring during restore: " + testMethod);
            switch (testMethod) {
                default:
                    Log.info(getClass(), testName.getMethodName(), "No configuration required: " + testMethod);
                    break;
            }

        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer("CWMMH0052W");
        server.restoreServerConfiguration();
    }

    static enum TestMethod {
        testDefaultFileBasedHealthChecks,
        unknown
    }
}
