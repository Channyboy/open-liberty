/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.metrics40.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

import io.smallrye.metrics.MetricsRequestHandler;

/**
 *
 */
public class MetricRESTHandler implements RESTHandler {
    private static final TraceComponent tc = Tr.register(MetricRESTHandler.class);
    protected SharedMetricRegistries sharedMetricRegistry;
    protected MetricsRequestHandler metricsRequestHandler;

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) throws IOException {
        Locale locale = null;
        String regName = "";
        String attName = "";
        String acceptHeader = null;
        String method = "";
        String requestPath = "";
        String accept = request.getHeader(Constants.ACCEPT_HEADER);
        Stream<String> acceptHeaders = null;
        try {
            locale = request.getLocale();
            regName = request.getPathVariable(Constants.SUB);
            attName = request.getPathVariable(Constants.ATTRIBUTE);
            acceptHeader = request.getHeader(Constants.ACCEPT_HEADER);
            method = request.getMethod();
            requestPath = request.getURI();
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Tr.formatMessage(tc, locale, "internal.error.CWMMC0006E", e));
        }
        if (acceptHeader != null) {
            ArrayList<String> tmp = new ArrayList<String>();
            tmp.add(acceptHeader);
            acceptHeaders = tmp.stream();
        }
        metricsRequestHandler.handleRequest(requestPath, method, acceptHeaders == null ? null : acceptHeaders, (status, message, headers) -> {
            headers.forEach(
                            (key, value) -> response.addResponseHeader(key, value));
            response.setStatus(status);
            response.getWriter().write(message);
        });
    }
}
