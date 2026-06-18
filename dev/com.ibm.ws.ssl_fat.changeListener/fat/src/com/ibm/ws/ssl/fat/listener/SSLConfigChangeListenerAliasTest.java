/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.ssl.fat.listener;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.SSL;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test SSL configuration change notifications when registering
 * SSLConfigChangeListener with a direct alias.
 */
@RunWith(FATRunner.class)
public class SSLConfigChangeListenerAliasTest {

    private static final String APP_NAME = "sslChangeListener";
    private static final String SERVER_NAME = "SSLChangeListenerServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
    	
        WebArchive testWAR = ShrinkWrap
                .create(WebArchive.class, APP_NAME+".war")
                .addPackage(
                            "com.ibm.ws.ssl.fat.listener.app");

        ShrinkHelper.exportDropinAppToServer(server, testWAR,
                                     DeployOptions.SERVER_ONLY);

        // Start the server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {

        server.stopServer();
    }

    /**
     * Test that SSLConfigChangeListener receives notification when SSL config
     * is updated for a specific alias.
     * 
     * Steps:
     * 1. Register listener with alias "testSSLConfig"
     * 2. Update the SSL configuration for "testSSLConfig"
     * 3. Verify listener received notification in logs
     */
    @Test
    public void testSSLConfigChangeWithAlias() throws Exception {
        final String testAlias = "testSSLConfig";
        final String listenerId = "alias_" + testAlias;
        
        // Register the listener
        String response = HttpUtils.getHttpResponseAsString(server, 
            "/" + APP_NAME + "/sslChangeListener/registerWithAlias/" + testAlias);
        assertNotNull("Failed to register listener", response);
        
        
        // Mark the log for searching
        server.setMarkToEndOfLog();
        
        // Update the SSL configuration to trigger notification
        // Toggle the sslProtocol to trigger the change listener
        ServerConfiguration config = server.getServerConfiguration();
        SSL sslConfig = config.getSSLById(testAlias);
        assertNotNull("SSL config should exist: " + testAlias, sslConfig);
        
        // Toggle sslProtocol: if it's TLSv1.3, change to TLSv1.2, otherwise change to TLSv1.3
        String currentProtocol = sslConfig.getSslProtocol();
        String newProtocol = "TLSv1.3".equals(currentProtocol) ? "TLSv1.2" : "TLSv1.3";
        sslConfig.setSslProtocol(newProtocol);
        
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
        
        // Verify the listener was notified
        assertNotNull("SSLConfigChangeListener notification not found in logs",
            server.waitForStringInLogUsingMark("SSLConfigChangeListener notification received for alias: " + testAlias));
        
        // Cleanup: deregister the listener
        HttpUtils.getHttpResponseAsString(server, 
            "/" + APP_NAME + "/sslChangeListener/deregister/" + listenerId);
    }

    /**
     * Test that multiple listeners can be registered for different aliases
     * and each receives notifications independently.
     */
    @Test
    public void testMultipleAliasListeners() throws Exception {
        final String alias1 = "testSSLConfig";
        final String alias2 = "anotherSSLConfig";
        
        // Register listeners for both aliases
        HttpUtils.getHttpResponseAsString(server, 
            "/" + APP_NAME + "/sslChangeListener/registerWithAlias/" + alias1);
        HttpUtils.getHttpResponseAsString(server, 
            "/" + APP_NAME + "/sslChangeListener/registerWithAlias/" + alias2);
        
        // Mark the log
        server.setMarkToEndOfLog();
        
        // Update only the first SSL config
        // Toggle the sslProtocol to trigger the change listener
        ServerConfiguration config = server.getServerConfiguration();
        SSL sslConfig = config.getSSLById(alias1);
        assertNotNull("SSL config should exist: " + alias1, sslConfig);
        
        // Toggle sslProtocol: if it's TLSv1.3, change to TLSv1.2, otherwise change to TLSv1.3
        String currentProtocol = sslConfig.getSslProtocol();
        String newProtocol = "TLSv1.3".equals(currentProtocol) ? "TLSv1.2" : "TLSv1.3";
        sslConfig.setSslProtocol(newProtocol);
        
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
        
        // Verify only the first listener was notified
        assertNotNull("First listener should be notified",
            server.waitForStringInLogUsingMark("SSLConfigChangeListener notification received for alias: " + alias1));
        
        // Ensure that the second notification never does happen (wait 10 seconds)
        assertNull("Second listener should not be notified",
            server.waitForStringInLogUsingMark("SSLConfigChangeListener notification received for alias: " + alias2, 10000));
        
        //Maybe TODO: Perhaps use an endpoint that outputs the count of the individual notification

        // Cleanup
        HttpUtils.getHttpResponseAsString(server, 
            "/" + APP_NAME + "/sslChangeListener/deregister/alias_" + alias1);
        HttpUtils.getHttpResponseAsString(server, 
            "/" + APP_NAME + "/sslChangeListener/deregister/alias_" + alias2);
    }
}

// Made with Bob
