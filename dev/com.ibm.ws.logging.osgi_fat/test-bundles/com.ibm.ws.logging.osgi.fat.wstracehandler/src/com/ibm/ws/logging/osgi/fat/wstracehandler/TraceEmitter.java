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

import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Emits trace records from inside the test bundle. The records are only created when the
 * server's trace specification enables this package (for example
 * {@code com.ibm.ws.logging.osgi.fat.wstracehandler.*=all}).
 */
public class TraceEmitter {

    private static final TraceComponent tc = Tr.register(TraceEmitter.class);

    private TraceEmitter() {
    }

    /**
     * Emits each value as its own {@code Tr.debug} trace record.
     *
     * @param values the text to emit; null entries are skipped
     */
    public static void emitTraces(List<String> values) {
        for (String value : values) {
            if (value != null) {
                Tr.debug(tc, value);
            }
        }
    }
}
