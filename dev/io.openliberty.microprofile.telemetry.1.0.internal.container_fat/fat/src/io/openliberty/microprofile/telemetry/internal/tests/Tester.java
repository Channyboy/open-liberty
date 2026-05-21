/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.tests;

import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.telemetry.internal.utils.KeyPairs;

/**
 *
 */
@RunWith(FATRunner.class)
public class Tester {

    @Server("TestTestServer")
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server.startServer();
        RemoteFile rf = server.getFFDCLogFile("sdf");

    }

    @Test
    public void sslthing() throws Exception {
        Log.info(this.getClass(), "sslthing", "start");
        KeyPairs kp = new KeyPairs();

        TimeUnit.SECONDS.sleep(5);

        Log.info(this.getClass(), "sslthing", "done");
    }

}
