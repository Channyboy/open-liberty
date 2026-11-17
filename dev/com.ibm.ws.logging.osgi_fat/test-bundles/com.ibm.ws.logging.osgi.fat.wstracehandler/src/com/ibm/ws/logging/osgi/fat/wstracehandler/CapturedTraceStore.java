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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory store for trace records captured by {@link TestWsTraceHandler}.
 * Verification is a plain substring check: a value is "present" if any captured
 * trace text contains it.
 */
public class CapturedTraceStore {

    private static final List<String> TRACES = new CopyOnWriteArrayList<String>();

    private CapturedTraceStore() {
    }

    public static void add(String trace) {
        if (trace != null) {
            TRACES.add(trace);
        }
    }

    /** @return true if any captured trace contains the given text. */
    public static boolean contains(String value) {
        if (value == null) {
            return false;
        }
        for (String trace : TRACES) {
            if (trace.contains(value)) {
                return true;
            }
        }
        return false;
    }

    /** @return the values (in order) that are not contained in any captured trace. */
    public static List<String> findMissing(List<String> values) {
        List<String> missing = new ArrayList<String>();
        for (String value : values) {
            if (!contains(value)) {
                missing.add(value);
            }
        }
        return missing;
    }

    /** @return all captured traces in one string, each entry followed by a new line. */
    public static String getAllAsString() {
        StringBuilder sb = new StringBuilder();
        for (String trace : TRACES) {
            sb.append(trace).append("\n");
        }
        return sb.toString();
    }

    public static int getCount() {
        return TRACES.size();
    }

    public static void clear() {
        TRACES.clear();
    }
}
