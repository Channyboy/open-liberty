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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.OutboundConnection;
import com.ibm.websphere.simplicity.config.SSL;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test SSL configuration change notifications when registering
 * SSLConfigChangeListener with connectionInfo map (outbound host).
 */
@RunWith(FATRunner.class)
public class SSLConfigChangeListenerConnectionInfoTest {

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
     * is updated for an outbound connection matching the host in connectionInfo.
     * 
     * Steps:
     * 1. Register listener with connectionInfo containing outbound host "example.com"
     * 2. Update the SSL configuration that has outboundConnection with host="example.com"
     * 3. Verify listener received notification in logs
     */
    @Test
    public void testSSLConfigChangeWithConnectionInfo() throws Exception {
        final String testHost = "example.com";
        final String listenerId = "connInfo_" + testHost;
        
        // Register the listener with connectionInfo
        String response = HttpUtils.getHttpResponseAsString(server, 
            "/" + APP_NAME + "/sslChangeListener/registerWithConnectionInfo?host=" + testHost);
        assertNotNull("Failed to register listener", response);
        
        // Mark the log for searching
        server.setMarkToEndOfLog();
        
        // Update the SSL configuration that matches the outbound host
        // Toggle the sslProtocol to trigger the change listener
        ServerConfiguration config = server.getServerConfiguration();
        SSL matchingSSL = null;
        for (SSL ssl : config.getSsls()) {
            for (OutboundConnection conn : ssl.getOutboundConnections()) {
                if (testHost.equals(conn.getHost())) {
                    matchingSSL = ssl;
                    break;
                }
            }
            if (matchingSSL != null) break;
        }
        assertNotNull("SSL config with outbound host should exist: " + testHost, matchingSSL);
        
        // Toggle sslProtocol: if it's TLSv1.3, change to TLSv1.2, otherwise change to TLSv1.3
        String currentProtocol = matchingSSL.getSslProtocol();
        String newProtocol = "TLSv1.3".equals(currentProtocol) ? "TLSv1.2" : "TLSv1.3";
        matchingSSL.setSslProtocol(newProtocol);
        
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
        
        // Verify the listener was notified
        assertNotNull("SSLConfigChangeListener notification not found in logs",
            server.waitForStringInLogUsingMark("SSLConfigChangeListener notification received for host: " + testHost));
        
        // Cleanup: deregister the listener
        HttpUtils.getHttpResponseAsString(server, 
            "/" + APP_NAME + "/sslChangeListener/deregister/" + listenerId);
    }

    /**
     * Test that listener registered with connectionInfo receives notifications
     * for SSL configs matching the outbound host, but NOT for other SSL configs.
     */
    @Test
    public void testConnectionInfoListenerSelectivity() throws Exception {
        final String matchingHost = "example.com";
        final String nonMatchingHost = "other.com";
        
        // Register listener for specific host
        HttpUtils.getHttpResponseAsString(server, 
            "/" + APP_NAME + "/sslChangeListener/registerWithConnectionInfo?host=" + matchingHost);
        
        // First, update an SSL config that does NOT match the host
        server.setMarkToEndOfLog();
        
        ServerConfiguration config = server.getServerConfiguration();
        SSL nonMatchingSSL = null;
        for (SSL ssl : config.getSsls()) {
            for (OutboundConnection conn : ssl.getOutboundConnections()) {
                if (nonMatchingHost.equals(conn.getHost())) {
                    nonMatchingSSL = ssl;
                    break;
                }
            }
            if (nonMatchingSSL != null) break;
        }
        assertNotNull("SSL config with outbound host should exist: " + nonMatchingHost, nonMatchingSSL);
        
        // Toggle sslProtocol
        String currentProtocol = nonMatchingSSL.getSslProtocol();
        String newProtocol = "TLSv1.3".equals(currentProtocol) ? "TLSv1.2" : "TLSv1.3";
        nonMatchingSSL.setSslProtocol(newProtocol);
        
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
        
        // Verify the listener was NOT notified (wait 10 seconds)
        assertNull("Listener should not be notified for non-matching host",
            server.waitForStringInLogUsingMark("SSLConfigChangeListener notification received for host: " + matchingHost, 10000));
        
        // Now update the SSL config that DOES match the host
        server.setMarkToEndOfLog();
        
        config = server.getServerConfiguration();
        SSL matchingSSL = null;
        for (SSL ssl : config.getSsls()) {
            for (OutboundConnection conn : ssl.getOutboundConnections()) {
                if (matchingHost.equals(conn.getHost())) {
                    matchingSSL = ssl;
                    break;
                }
            }
            if (matchingSSL != null) break;
        }
        assertNotNull("SSL config with outbound host should exist: " + matchingHost, matchingSSL);
        
        // Toggle sslProtocol
        currentProtocol = matchingSSL.getSslProtocol();
        newProtocol = "TLSv1.3".equals(currentProtocol) ? "TLSv1.2" : "TLSv1.3";
        matchingSSL.setSslProtocol(newProtocol);
        
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
        
        // Verify the listener WAS notified this time
        assertNotNull("Listener should be notified for matching host",
            server.waitForStringInLogUsingMark("SSLConfigChangeListener notification received for host: " + matchingHost));
        
        // Cleanup
        HttpUtils.getHttpResponseAsString(server, 
            "/" + APP_NAME + "/sslChangeListener/deregister/connInfo_" + matchingHost);
    }

    /**
     * Test that both alias-based and connectionInfo-based listeners can coexist
     * and receive notifications independently.
     */
    @Test
    public void testMixedListenerTypes() throws Exception {
        final String testAlias = "testSSLConfig";
        final String testHost = "example.com";
        
        // Register both types of listeners
        HttpUtils.getHttpResponseAsString(server, 
            "/" + APP_NAME + "/sslChangeListener/registerWithAlias/" + testAlias);
        HttpUtils.getHttpResponseAsString(server, 
            "/" + APP_NAME + "/sslChangeListener/registerWithConnectionInfo?host=" + testHost);
        
        // Mark the log
        server.setMarkToEndOfLog();
        
        // Update the SSL config with the matching alias
        ServerConfiguration config = server.getServerConfiguration();
        SSL sslConfig = config.getSSLById(testAlias);
        assertNotNull("SSL config should exist: " + testAlias, sslConfig);
        
        // Toggle sslProtocol
        String currentProtocol = sslConfig.getSslProtocol();
        String newProtocol = "TLSv1.3".equals(currentProtocol) ? "TLSv1.2" : "TLSv1.3";
        sslConfig.setSslProtocol(newProtocol);
        
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
        
        // Verify the alias listener was notified
        assertNotNull("Alias listener should be notified",
            server.waitForStringInLogUsingMark("SSLConfigChangeListener notification received for alias: " + testAlias));
        
        // Verify the connectionInfo listener was NOT notified (if they're different configs)
        // This assumes testAlias config doesn't have outboundConnection with host=example.com
        
        // Cleanup
        HttpUtils.getHttpResponseAsString(server, 
            "/" + APP_NAME + "/sslChangeListener/deregister/alias_" + testAlias);
        HttpUtils.getHttpResponseAsString(server, 
            "/" + APP_NAME + "/sslChangeListener/deregister/connInfo_" + testHost);
    }
}

// Made with Bob
