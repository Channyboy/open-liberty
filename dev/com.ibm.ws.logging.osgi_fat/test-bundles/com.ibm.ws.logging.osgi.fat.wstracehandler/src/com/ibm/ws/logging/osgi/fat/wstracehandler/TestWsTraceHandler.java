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

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsTraceHandler;

/**
 * A FAT test WsTraceHandler that captures routed trace records into CapturedTraceStore.
 * It is registered as a WsTraceHandler OSGi service with a non-null "id" property, which the
 * trace router requires. It deliberately does not emit any trace or log message itself, to
 * avoid feeding the trace router from inside a handler.
 */
@Component(service = { WsTraceHandler.class },
           immediate = true,
           property = { "id=" + TestWsTraceHandler.HANDLER_ID,
                        "service.vendor=IBM" })
public class TestWsTraceHandler implements WsTraceHandler {

    public static final String HANDLER_ID = "WS_TRACE_HANDLER";

    @Override
    public void publish(RoutedMessage routedMessage) {
        if (routedMessage != null) {
            // For trace records getFormattedMsg() is null; the text is in the verbose message.
            CapturedTraceStore.add(routedMessage.getFormattedVerboseMsg());
        }
    }
}
