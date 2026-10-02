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
package com.ibm.ws.logging.osgi.test.feature;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

@Component(
    service = { RESTHandler.class },
    configurationPolicy = ConfigurationPolicy.IGNORE,
    immediate = true,
    property = {
        "service.vendor=IBM",
        RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "=/loggingTest",
        RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/"
    }
)
public class LoggingTestEndpoint implements RESTHandler {

    private static final TraceComponent tc = Tr.register(LoggingTestEndpoint.class);
    private static final Random RANDOM = new Random();

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = new PrintWriter(response.getWriter());

        String sMsgCount = request.getParameter("msgCount");
        String rawMessages = request.getParameter("messages");
        String resultBanner = "";

        if (sMsgCount != null && rawMessages != null && !rawMessages.trim().isEmpty()) {
            int msgCount = 1;
            try {
                msgCount = Integer.parseInt(sMsgCount.trim());
            } catch (NumberFormatException e) {
                msgCount = 1;
            }

            String[] messages = rawMessages.split("[,\\r\\n]+");

            long start = System.currentTimeMillis();
            for (int i = 0; i < msgCount; i++) {
                String chosenMsg = messages[RANDOM.nextInt(messages.length)].trim();
                Tr.info(tc, chosenMsg);
            }
            long elapsed = System.currentTimeMillis() - start;

            resultBanner = "<div style='color:green; padding:10px; border:1px solid green; margin-bottom:15px;'>"
                    + "<b>Sent " + msgCount + " messages in " + elapsed + " ms!</b>"
                    + "</div>";
        }

        out.println("<!DOCTYPE html><html><head><title>Logging Test Generator</title></head><body>");
        out.println("<h2>Message Router Test Generator</h2>");
        out.println(resultBanner);
        out.println("<form method='POST'>");
        out.println("  <div>");
        out.println("    <label><b>Messages (comma or newline separated):</b></label><br/>");
        out.println("    <textarea name='messages' rows='6' cols='60'>"
                    + (rawMessages != null ? rawMessages : "ABCD1231I Hello World, ABCDEE045I Sample Message, BCDAEE410W Warning msg")
                    + "</textarea>");
        out.println("  </div><br/>");
        out.println("  <div>");
        out.println("    <label><b>Message Count:</b></label><br/>");
        out.println("    <input type='number' name='msgCount' value='" + (sMsgCount != null ? sMsgCount : "100") + "' min='1' />");
        out.println("  </div><br/>");
        out.println("  <input type='submit' value='Send Messages' style='padding:6px 16px; font-weight:bold;' />");
        out.println("</form>");
        out.println("</body></html>");
    }
}
