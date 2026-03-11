/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.internal.exporters;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 *
 */
public class OpenLibertySpanExporterWrapper implements SpanExporter {

    SpanExporter delegate = null;

    final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    public OpenLibertySpanExporterWrapper(SpanExporter spanExporter) {
        delegate = spanExporter;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> arg0) {
        rwl.readLock().lock();
        try {
            CompletableResultCode crc = delegate.export(arg0);
            return crc;
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public CompletableResultCode flush() {
        rwl.readLock().lock();
        try {
            return delegate.flush();
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }

    public void updateDelegate(SpanExporter newDelegate) {
        rwl.writeLock().lock();
        try {
            delegate.flush();

            delegate.shutdown();

            this.delegate = newDelegate;
        } finally {
            rwl.writeLock().unlock();
        }

    }

}
