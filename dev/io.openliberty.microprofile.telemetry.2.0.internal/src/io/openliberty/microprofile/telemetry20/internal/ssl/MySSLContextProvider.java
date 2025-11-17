/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.internal.ssl;

import java.security.KeyManagementException;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.ssl.LibertySSLContextSpi;
import com.ibm.ws.ssl.LibertySSLSocketFactoryWrapper;

/**
 *
 */
public class MySSLContextProvider extends SSLContextSpi {

    private static final TraceComponent tc = Tr.register(LibertySSLContextSpi.class, "SSL", "com.ibm.ws.ssl");

    /*
     * Swappable
     */
    private SSLContext delegate;

    private String alias;

    public MySSLContextProvider(SSLContext delegate, String alias) {
        this.delegate = delegate;
        this.alias = alias;
    }

    @Override
    protected void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom sr) throws KeyManagementException {
        delegate.init(km, tm, sr);
    }

    @Override
    protected SSLSocketFactory engineGetSocketFactory() {
        return new LibertySSLSocketFactoryWrapper(delegate.getSocketFactory(), this.alias);
    }

    @Override
    protected SSLServerSocketFactory engineGetServerSocketFactory() {
        return delegate.getServerSocketFactory();
    }

    @Override
    protected SSLEngine engineCreateSSLEngine() {
        return delegate.createSSLEngine();
    }

    @Override
    protected SSLEngine engineCreateSSLEngine(String host, int port) {
        return delegate.createSSLEngine(host, port);
    }

    @Override
    protected SSLSessionContext engineGetServerSessionContext() {
        return delegate.getServerSessionContext();
    }

    @Override
    protected SSLSessionContext engineGetClientSessionContext() {
        return delegate.getClientSessionContext();
    }

    public void swapDelegate(SSLContext newDelegate) {
        this.delegate = newDelegate;
    }

    public void swapAlias(String newAlias) {
        this.alias = newAlias;
    }

}
