/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsLogHandler;

/**
 * A test WsLogHandler that captures routed messages into CapturedMessageStore
 * (and optionally to a file if configured), deliberately avoiding System.out / logging
 * frameworks to prevent a routing loop back through BaseTraceService.
 */
public class TestWsLogHandler implements WsLogHandler {

    public static final String HANDLER_ID = "TEST_HANDLER";

    private static final String OUTPUT_FILE = "C:\\devdir\\TestOutput\\output.txt";

    /** {@inheritDoc} */
    @Override
    public void publish(RoutedMessage routedMessage, boolean messageHidden) {
        if (routedMessage != null) {
            String formattedMsg = routedMessage.getFormattedMsg();
            CapturedMessageStore.addMessage(formattedMsg, messageHidden);
        }

        String s_prop = System.getProperty("writeToOutput");
        boolean prop = (s_prop == null) ? false : Boolean.valueOf(s_prop.trim());

        if (s_prop != null && prop && routedMessage != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_FILE, true))) {
                writer.println("[TestWsLogHandler] hidden=" + messageHidden + " | " + routedMessage.getFormattedMsg());
            } catch (IOException e) {
                // Intentionally swallowed — cannot use logging here without causing a routing loop.
            }
        }
    }

}
