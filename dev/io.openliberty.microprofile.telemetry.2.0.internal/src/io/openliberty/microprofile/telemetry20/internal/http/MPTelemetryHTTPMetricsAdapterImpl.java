/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.internal.http;

import java.time.Duration;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.http.monitor.HttpStatAttributes;
import io.openliberty.http.monitor.metrics.HTTPMetricAdapter;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryAccessor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.context.Context;

/**
 *
 */
@Component(service = { HTTPMetricAdapter.class }, configurationPolicy = ConfigurationPolicy.IGNORE)
public class MPTelemetryHTTPMetricsAdapterImpl implements HTTPMetricAdapter {

    static final String INSTR_SCOPE = "io.openliberty.microprofile.telemetry20.internal.http";

    @Activate
    public void activate() {
        System.out.println("Activating Telemetry HTTPMetricAdapter");
    }

    @Override
    public void updateHttpMetrics(HttpStatAttributes httpStatAttributes, Duration duration, String appName) {

        System.out.println("DO SOMETHING - appName " + appName);

        OpenTelemetry otelInstance = OpenTelemetryAccessor.getOpenTelemetryInfo((appName == null) ? "SERVER" : appName).getOpenTelemetry();

        if (otelInstance == null) {
            otelInstance = OpenTelemetryAccessor.getOpenTelemetryInfo("SERVER").getOpenTelemetry();
        }

        //use default boundaries
        DoubleHistogram dHistogram = otelInstance.getMeterProvider().get(INSTR_SCOPE).histogramBuilder("http.server.request.duration").setUnit("seconds")
                        .setDescription("test description")
                        .setExplicitBucketBoundariesAdvice(List.of(0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0)).build();

        Context ctx = Context.current();

        dHistogram.record(duration.toSeconds(), retrieveAttributes(httpStatAttributes), ctx);

    }

    private Attributes retrieveAttributes(HttpStatAttributes httpStatAttributes) {

        AttributesBuilder attributesBuilder = Attributes.builder();

        attributesBuilder.put("request_method", httpStatAttributes.getRequestMethod());
        attributesBuilder.put("http_scheme", httpStatAttributes.getScheme());

        Integer status = httpStatAttributes.getResponseStatus().orElse(-1);
        attributesBuilder.put("response_status", status == -1 ? "" : status.toString().trim());

        attributesBuilder.put("http_route", httpStatAttributes.getHttpRoute().orElse(""));

        attributesBuilder.put("network_name", httpStatAttributes.getNetworkProtocolName());
        attributesBuilder.put("network_version", httpStatAttributes.getNetworkProtocolVersion());

        attributesBuilder.put("server_name", httpStatAttributes.getServerName());
        attributesBuilder.put("server_port", String.valueOf(httpStatAttributes.getServerPort()));

        if (httpStatAttributes.getErrorType().isPresent()) {
            attributesBuilder.put("error_type", httpStatAttributes.getErrorType().get());
        }

        return attributesBuilder.build();
    }

}
