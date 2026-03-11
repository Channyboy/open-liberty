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
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

/**
 *
 */
public class OpenLibertyLogRecordExporterWrapper implements LogRecordExporter {

    final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    LogRecordExporter delegate = null;

    public OpenLibertyLogRecordExporterWrapper(LogRecordExporter logRecordExporter) {
        this.delegate = logRecordExporter;
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> arg0) {
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

    public void updateDelegate(LogRecordExporter newDelegate) {
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
