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
package com.ibm.ws.logging.osgi.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * FAT tests for the WsTraceRouter / WsTraceHandler flow.
 *
 * <ul>
 * <li>{@code WsTraceHandlerServer}: the wstracehandler test bundle emits posted values with
 * Tr.debug; its TestWsTraceHandler captures them into an in-memory store, and the REST endpoint
 * reports whether every value was captured (HTTP 200) or not (417).</li>
 * <li>{@code TraceEmitterServer}: starts with only the traceEmitter-1.0 feature, whose 10 activation
 * traces are buffered in {@code earlierTraces}. Adding wsTraceHandler-1.0 with a dynamic
 * server.xml update must replay the buffered traces to the new handler.</li>
 * </ul>
 *
 * <p>Each test starts the server it needs and {@link #stopServer()} stops it afterwards, because
 * the bundles can only be uninstalled while no server is running.
 *
 * <p>This flow is independent of the message router and WsLogHandler tests.
 */
@RunWith(FATRunner.class)
public class WsTraceHandlerTest {

    private static final Class<?> c = WsTraceHandlerTest.class;

    /** Bundle symbolic name - no .jar suffix. */
    private static final String BUNDLE = "com.ibm.ws.logging.osgi.fat.wstracehandler";
    /** Feature short name - no .mf suffix. */
    private static final String FEATURE = "wsTraceHandler-1.0";

    /** Bundle that emits traces at activation (earlierTraces testing). */
    private static final String EMITTER_BUNDLE = "com.ibm.ws.logging.osgi.fat.traceemitter";
    private static final String EMITTER_FEATURE = "traceEmitter-1.0";

    /** Prefix and count of the traces emitted by TraceEmitterService (see that class). */
    private static final String EMITTER_TRACE_PREFIX = "TRACE_EMITTER_EARLY_TRACE_";
    private static final int EMITTER_TRACE_COUNT = 10;

    /** Replacement server.xml, relative to the autoFVT test files directory (publish/files). */
    private static final String ADD_TRACE_HANDLER_SERVER_XML = "wsTraceHandler/server-addTraceHandlerUpdate.xml";

    /** How long to wait after the server is ready before adding the WsTraceHandler feature. */
    private static final long WAIT_BEFORE_UPDATE_MS = 5000;

    @Server("WsTraceHandlerServer")
    public static LibertyServer traceHandlerServer;

    @Server("TraceEmitterServer")
    public static LibertyServer emitterServer;

    /** The server started by the currently running test; used by the HTTP helpers. */
    private static LibertyServer activeServer;

    /** Set when a test replaced the server.xml of {@link #emitterServer} and it must be restored. */
    private static boolean emitterConfigSaved;

    /**
     * Installs both test bundles and features once. No server is started here: each test
     * starts the server it needs.
     */
    @BeforeClass
    public static void installBundlesAndFeatures() throws Exception {
        traceHandlerServer.installSystemBundle(BUNDLE);
        traceHandlerServer.installSystemFeature(FEATURE);
        traceHandlerServer.installSystemBundle(EMITTER_BUNDLE);
        traceHandlerServer.installSystemFeature(EMITTER_FEATURE);
    }

    @AfterClass
    public static void uninstallBundlesAndFeatures() throws Exception {
        if (traceHandlerServer == null) {
            return;
        }
        traceHandlerServer.uninstallSystemBundle(BUNDLE);
        traceHandlerServer.uninstallSystemFeature(FEATURE);
        traceHandlerServer.uninstallSystemBundle(EMITTER_BUNDLE);
        traceHandlerServer.uninstallSystemFeature(EMITTER_FEATURE);
    }

    /**
     * Stops whichever server the test started. The uninstall in {@link #uninstallBundlesAndFeatures()}
     * requires that no server is running.
     */
    @After
    public void stopServer() throws Exception {
        if (activeServer != null && activeServer.isStarted()) {
            activeServer.stopServer();
        }
        activeServer = null;

        if (emitterConfigSaved) {
            emitterServer.restoreServerConfiguration();
            emitterConfigSaved = false;
        }
    }

    /** Starts the given server, waits for it to be ready and makes it the active server. */
    private static void startServer(LibertyServer s) throws Exception {
        activeServer = s;
        s.startServer();
        s.waitForStringInLog("CWWKF0011I");
    }

    /** The wsTraceHandler-1.0 feature started and the REST endpoint is reachable. */
    @Test
    public void testEndpointReachable() throws Exception {
        Log.info(c, "testEndpointReachable", "Checking /wsTraceHandler/ is reachable");
        startServer(traceHandlerServer);
        assertEquals("Expected HTTP 200 from /wsTraceHandler/", 200, get("/wsTraceHandler/"));
    }

    /**
     * Posts values to the endpoint. The bundle emits each one with Tr.debug and then checks that
     * the WsTraceHandler captured it; 200 means every value was found in the in-memory store.
     */
    @Test
    public void testEmittedTracesAreCaptured() throws Exception {
        Log.info(c, "testEmittedTracesAreCaptured", "Emitting traces and verifying capture");
        startServer(traceHandlerServer);
        assertEquals("Clear should return 200", 200, get("/wsTraceHandler/clear"));

        int status = postForm("/wsTraceHandler/emit",
                              "values=WSTRACE_FAT_VALUE_ONE alpha,WSTRACE_FAT_VALUE_TWO beta,WSTRACE_FAT_VALUE_THREE gamma");
        assertEquals("Every emitted value should have been captured by the WsTraceHandler (200 = PASSED, 417 = FAILED)",
                     200, status);

        //for debug
        logCapturedTraces();
    }

    /**
     * Tests the earlierTraces buffer. The server starts with only traceEmitter-1.0, so its 10
     * activation traces are produced while no WsTraceHandler exists and are buffered. After the
     * server is ready (plus a short wait) wsTraceHandler-1.0 is added by a dynamic server.xml
     * update; the router must then replay all 10 buffered traces to the new handler.
     */
    @Test
    public void testEarlierTracesReplayedToNewTraceHandler() throws Exception {
        Log.info(c, "testEarlierTracesReplayedToNewTraceHandler", "Starting emitter-only server");
        startServer(emitterServer);

        Log.info(c, "testEarlierTracesReplayedToNewTraceHandler",
                 "Waiting " + WAIT_BEFORE_UPDATE_MS + "ms before adding " + FEATURE);
        Thread.sleep(WAIT_BEFORE_UPDATE_MS);

        emitterServer.saveServerConfiguration();
        emitterConfigSaved = true;
        emitterServer.setMarkToEndOfLog();
        emitterServer.setServerConfigurationFile(ADD_TRACE_HANDLER_SERVER_XML);
        // Also wait for the REST web application, which is what serves /wsTraceHandler/traces.
        emitterServer.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet(),
                                                        "CWWKT0016I.*/wsTraceHandler");

        String traces = logCapturedTraces();
        List<String> lines = nonEmptyLines(traces);

        for (int i = 1; i <= EMITTER_TRACE_COUNT; i++) {
            String value = EMITTER_TRACE_PREFIX + i;
            // Match the whole token: ..._TRACE_1 is a prefix of ..._TRACE_10.
            Pattern whole = Pattern.compile(Pattern.quote(value) + "(\\s|$)");
            boolean found = false;
            for (String line : lines) {
                if (whole.matcher(line).find()) {
                    found = true;
                    break;
                }
            }
            assertTrue("Early trace " + value + " was not replayed to the WsTraceHandler. Captured:\n" + traces, found);
        }
        assertEquals("Expected exactly the " + EMITTER_TRACE_COUNT + " buffered traces. Captured:\n" + traces,
                     EMITTER_TRACE_COUNT, lines.size());
    }

    private static List<String> nonEmptyLines(String text) {
        List<String> lines = new ArrayList<String>();
        for (String line : text.split("\\r?\\n")) {
            if (!line.trim().isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * Calls GET /wsTraceHandler/traces and logs the returned string (one captured trace per line)
     * with {@link Log#info}. Returns the string so a caller can also inspect it.
     */
    static String logCapturedTraces() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl() + "/wsTraceHandler/traces").openConnection();
        conn.setRequestMethod("GET");
        try {
            int status = conn.getResponseCode();
            if (status != 200) {
                Log.info(c, "logCapturedTraces", "GET /wsTraceHandler/traces returned " + status
                                                 + "; body: " + readFully(conn.getErrorStream()));
                return "";
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n"); // keep the one-entry-per-line layout of the response
                }
            }
            String traces = sb.toString();
            Log.info(c, "logCapturedTraces", "Captured traces:\n" + traces);
            return traces;
        } finally {
            conn.disconnect();
        }
    }

    private static String baseUrl() {
        return "http://" + activeServer.getHostname() + ":" + activeServer.getHttpDefaultPort();
    }

    /** Issues a GET and returns the HTTP status; logs the body when it is not 200. */
    private static int get(String path) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl() + path).openConnection();
        conn.setRequestMethod("GET");
        try {
            int status = conn.getResponseCode();
            if (status != 200) {
                Log.info(c, "get", "GET " + path + " returned " + status + "; body: " + readFully(conn.getErrorStream()));
            }
            return status;
        } finally {
            conn.disconnect();
        }
    }

    /** Posts a single form parameter {@code name=value}; the value is URL-encoded here. Returns the HTTP status. */
    private static int postForm(String path, String nameAndValue) throws Exception {
        int eq = nameAndValue.indexOf('=');
        String encoded = URLEncoder.encode(nameAndValue.substring(0, eq), "UTF-8") + "="
                         + URLEncoder.encode(nameAndValue.substring(eq + 1), "UTF-8");

        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl() + path).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(encoded.getBytes("UTF-8"));
        }
        try {
            int status = conn.getResponseCode();
            if (status != 200) {
                Log.info(c, "postForm", "POST " + path + " returned " + status + "; body: " + readFully(conn.getErrorStream()));
            }
            return status;
        } finally {
            conn.disconnect();
        }
    }

    private static String readFully(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
