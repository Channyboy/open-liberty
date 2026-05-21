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

import static io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil.DATA_TYPE_METRICS;
import static io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil.PROTOCOL_GRPC;
import static io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil.PROTOCOL_HTTP_PROTOBUF;

import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import com.ibm.websphere.ssl.SSLConfigChangeEvent;
import com.ibm.websphere.ssl.SSLConfigChangeListener;

import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

public class OpenLibertyMetricExporterProvider extends AbstractOpenLibertySignalExporterProvider implements ConfigurableMetricExporterProvider {

    static OpenLibertyMetricExporterProvider instance = null;

    OpenLibertyMetricsExporterWrapper wrapper = null;

    //below two currently not used.
    String hostGlobal = null;
    String portGlobal = null;

    public OpenLibertyMetricExporterProvider() {
        super("metrics");
        System.out.println("instantiate OpenLibertyMetricExporterProvider!");
        instance = this;
    }

    //Debug testing only: Used by the REST handler to invoke a swap.
    public static OpenLibertyMetricExporterProvider getInstance() {
        HashMap<String, String> sdf = new HashMap<String, String>();
        return instance;
    }

    OtlpHttpMetricExporterBuilder httpBuilder() {
        return OtlpHttpMetricExporter.builder();
    }

    OtlpGrpcMetricExporterBuilder grpcBuilder() {
        return OtlpGrpcMetricExporter.builder();
    }

    public MetricExporter defaultCreateExporterWrapper() {
        String protocol = OtlpConfigUtil.getOtlpProtocol(DATA_TYPE_METRICS, openTelemetryConfigProperties);

        if (protocol.equals(PROTOCOL_HTTP_PROTOBUF)) {
            OtlpHttpMetricExporterBuilder builder = httpBuilder();

            OtlpConfigUtil.configureOtlpExporterBuilder(
                                                        DATA_TYPE_METRICS,
                                                        openTelemetryConfigProperties,
                                                        builder::setEndpoint,
                                                        builder::addHeader,
                                                        builder::setCompression,
                                                        builder::setTimeout,
                                                        x -> {
                                                        },
                                                        (x, y) -> {
                                                        },
                                                        builder::setRetryPolicy,
                                                        builder::setMemoryMode);
            OtlpConfigUtil.configureOtlpAggregationTemporality(
                                                               openTelemetryConfigProperties, builder::setAggregationTemporalitySelector);
            OtlpConfigUtil.configureOtlpHistogramDefaultAggregation(
                                                                    openTelemetryConfigProperties, builder::setDefaultAggregationSelector);

            return builder.build();
        } else if (protocol.equals(PROTOCOL_GRPC)) {
            OtlpGrpcMetricExporterBuilder builder = grpcBuilder();

            OtlpConfigUtil.configureOtlpExporterBuilder(
                                                        DATA_TYPE_METRICS,
                                                        openTelemetryConfigProperties,
                                                        builder::setEndpoint,
                                                        builder::addHeader,
                                                        builder::setCompression,
                                                        builder::setTimeout,
                                                        x -> {
                                                        },
                                                        (x, y) -> {
                                                        },
                                                        builder::setRetryPolicy,
                                                        builder::setMemoryMode);
            OtlpConfigUtil.configureOtlpAggregationTemporality(
                                                               openTelemetryConfigProperties, builder::setAggregationTemporalitySelector);
            OtlpConfigUtil.configureOtlpHistogramDefaultAggregation(
                                                                    openTelemetryConfigProperties, builder::setDefaultAggregationSelector);

            return builder.build();
        }
        throw new ConfigurationException("Unsupported OTLP metrics protocol: " + protocol);
    }

    @Override
    public MetricExporter createExporter(ConfigProperties config) {

        init(config);

        /*
         * Note:
         * Failures in any of these subsequent methods will either a ConfigurationException or LibertyOTLPExporterConfigurationExcepton.
         * These exceptions will cause this provider to fail. This results in mpTelemetry throwing an error indicating an internal error has occurred.
         * This prevents this exporter (i.e., wrapper) from being "used".
         * We do not want to return null as that will result in an error indicating "libertyotlp" doesn't exist, which will be confusing for users.
         * We also don't want to create an empty wrapper (we don't want it to be used!).
         */
        validateOTLPEndpointProperty(TELEMETRY_SIGNAL);

        System.out.println("<<<<<<<<  OpenLibertyMetricExporterProvider  >>>>>");

        MetricExporter metricExporter = createOTLPExporter();
        wrapper = new OpenLibertyMetricsExporterWrapper(metricExporter);
        return wrapper;
    }

    MetricExporter createOTLPExporter() {

        MetricExporter metricExporter = defaultCreateExporterWrapper();

        //TODO: FILL IN SSLCONFIGCHANGELISTENER PARAMATER WHEN SECURITY TEAM SUPPORTS IT.
        Map.Entry<SSLContext, X509TrustManager> pair = retriveSSLContextAndTrustManager(null);

        if (OtlpGrpcMetricExporter.class.isInstance(metricExporter)) {
            metricExporter = ((OtlpGrpcMetricExporter) metricExporter).toBuilder().setSslContext(pair.getKey(), pair.getValue()).build();
        } else if (OtlpHttpMetricExporter.class.isInstance(metricExporter)) {
            metricExporter = ((OtlpHttpMetricExporter) metricExporter).toBuilder().setSslContext(pair.getKey(), pair.getValue()).build();

        }
        return metricExporter;

    }

    public void doSSlUpdate() {

        MetricExporter newExporter = createOTLPExporter();
        wrapper.updateDelegate(newExporter);
    }

    @Override
    public String getName() {
        return "libertyotlp";
    }

    static MyPrivateSSLConfigListener mynotifier = new MyPrivateSSLConfigListener("first");
    static MyPrivateSSLConfigListener myOtherNotifier = new MyPrivateSSLConfigListener("second");

    //POC: TEST
    static class MyPrivateSSLConfigListener implements SSLConfigChangeListener {

        String name;

        public MyPrivateSSLConfigListener(String name) {
            this.name = name;
        }

        @Override
        public void stateChanged(SSLConfigChangeEvent e) {
            System.out.println(name + " changed: " + e);

            //Do thing with - creating new exporter and swapping with the wrapper.
        }
    }

}
