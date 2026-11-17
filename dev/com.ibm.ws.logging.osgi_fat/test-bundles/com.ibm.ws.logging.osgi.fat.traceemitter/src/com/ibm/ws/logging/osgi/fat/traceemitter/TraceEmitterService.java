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
package com.ibm.ws.logging.osgi.fat.traceemitter;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * FAT test component that emits a fixed set of traces as soon as it is activated. It is used
 * to produce traces early in server startup, before a WsTraceHandler is registered, so the
 * WsTraceRouter earlierTraces buffer can be tested.
 *
 * <p>The traces are only created when the server's trace specification enables this package
 * (for example {@code com.ibm.ws.logging.osgi.fat.traceemitter.*=all}).
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class TraceEmitterService {

    private static final TraceComponent tc = Tr.register(TraceEmitterService.class);

    /** Number of traces emitted by {@link #emitTrace()}. */
    public static final int TRACE_COUNT = 10;

    /** Prefix of every emitted trace; the trace number (1 to {@value #TRACE_COUNT}) is appended. */
    public static final String TRACE_PREFIX = "TRACE_EMITTER_EARLY_TRACE_";

    @Activate
    protected void activate() {
        emitTrace();
    }

    /**
     * Emits {@value #TRACE_COUNT} traces with {@code Tr.debug}, named {@code TRACE_EMITTER_EARLY_TRACE_1}
     * to {@code TRACE_EMITTER_EARLY_TRACE_10}.
     */
    void emitTrace() {
        for (int i = 1; i <= TRACE_COUNT; i++) {
            Tr.debug(tc, TRACE_PREFIX + i);
        }
    }
}
