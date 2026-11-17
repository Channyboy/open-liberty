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
package com.ibm.ws.logging.osgi.fat.wstracehandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * REST endpoint at /wsTraceHandler used by the FAT test code.
 * <ul>
 * <li>POST /wsTraceHandler/emit - emits the posted values as traces, then checks that each one
 * reached the in-memory store. HTTP 200 if all were captured, 417 if any is missing.</li>
 * <li>GET/POST /wsTraceHandler/clear - clears the in-memory store.</li>
 * <li>GET /wsTraceHandler/traces - returns every captured trace as plain text, one entry per line.</li>
 * <li>GET /wsTraceHandler/ - status page.</li>
 * </ul>
 */
@Component(
    service = { RESTHandler.class },
    configurationPolicy = ConfigurationPolicy.IGNORE,
    immediate = true,
    property = {
        "service.vendor=IBM",
        RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "=/wsTraceHandler",
        RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/",
        RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/{sub}"
    }
)
public class WsTraceHandlerEndpoint implements RESTHandler {

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) throws IOException {
        String path = request.getPath();
        if (path == null) {
            path = "";
        }
        path = path.trim();

        if (path.equals("/emit")) {
            handleEmit(request, response);
        } else if (path.equals("/clear")) {
            handleClear(response);
        } else if (path.equals("/traces")) {
            handleTraces(response);
        } else if (path.isEmpty() || path.equals("/")) {
            handleStatus(response);
        } else {
            // Unknown path: fail loudly so a mistyped endpoint in a test is noticed.
            write(response, 404, "{\"error\": \"Unknown endpoint: " + escapeJson(path) + "\"}");
        }
    }

    /**
     * POST /wsTraceHandler/emit — parameter "values" (comma or newline separated), or the request body.
     */
    private void handleEmit(RESTRequest request, RESTResponse response) throws IOException {
        String raw = request.getParameter("values");
        if (raw == null || raw.trim().isEmpty()) {
            raw = readRequestBody(request);
        }
        List<String> values = parseValues(raw);
        if (values.isEmpty()) {
            write(response, 400, "{\"error\": \"No values provided. Use the 'values' parameter or the request body.\"}");
            return;
        }

        TraceEmitter.emitTraces(values);

        List<String> missing = CapturedTraceStore.findMissing(values);
        StringBuilder json = new StringBuilder();
        json.append("{\"status\": \"").append(missing.isEmpty() ? "PASSED" : "FAILED").append("\"");
        json.append(", \"emittedCount\": ").append(values.size());
        json.append(", \"totalCaptured\": ").append(CapturedTraceStore.getCount());
        json.append(", \"missing\": [");
        for (int i = 0; i < missing.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"").append(escapeJson(missing.get(i))).append("\"");
        }
        json.append("]}");
        write(response, missing.isEmpty() ? 200 : 417, json.toString());
    }

    /**
     * GET /wsTraceHandler/traces — all captured traces in one plain-text string, each entry followed by a new line.
     */
    private void handleTraces(RESTResponse response) throws IOException {
        response.setStatus(200);
        response.setContentType("text/plain");
        response.getWriter().write(CapturedTraceStore.getAllAsString());
    }

    private void handleClear(RESTResponse response) throws IOException {
        int previousCount = CapturedTraceStore.getCount();
        CapturedTraceStore.clear();
        write(response, 200, "{\"status\": \"SUCCESS\", \"clearedCount\": " + previousCount + "}");
    }

    private void handleStatus(RESTResponse response) throws IOException {
        write(response, 200, "{\"status\": \"OK\", \"capturedCount\": " + CapturedTraceStore.getCount()
                + ", \"endpoints\": [\"POST /wsTraceHandler/emit\",\"GET /wsTraceHandler/traces\",\"GET /wsTraceHandler/clear\"]}");
    }

    private void write(RESTResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(json);
    }

    private List<String> parseValues(String text) {
        List<String> list = new ArrayList<String>();
        if (text == null) {
            return list;
        }
        for (String token : text.split("[,\\r\\n]+")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    private String readRequestBody(RESTRequest request) throws IOException {
        Reader reader = request.getInput();
        if (reader == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
