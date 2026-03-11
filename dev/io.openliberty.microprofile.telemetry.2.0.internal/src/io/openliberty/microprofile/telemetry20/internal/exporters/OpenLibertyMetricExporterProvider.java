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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.websphere.ssl.SSLConfigChangeEvent;
import com.ibm.websphere.ssl.SSLConfigChangeListener;

import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry20.internal.info.OpenTelemetryInfoFactoryImpl;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

public class OpenLibertyMetricExporterProvider implements ConfigurableMetricExporterProvider {

    OtlpHttpMetricExporterBuilder httpBuilder() {
        return OtlpHttpMetricExporter.builder();
    }

    OtlpGrpcMetricExporterBuilder grpcBuilder() {
        return OtlpGrpcMetricExporter.builder();
    }

    public MetricExporter defaultCreateExporterWrapper(ConfigProperties config) {
        String protocol = OtlpConfigUtil.getOtlpProtocol(DATA_TYPE_METRICS, config);

        if (protocol.equals(PROTOCOL_HTTP_PROTOBUF)) {
            OtlpHttpMetricExporterBuilder builder = httpBuilder();

            OtlpConfigUtil.configureOtlpExporterBuilder(
                                                        DATA_TYPE_METRICS,
                                                        config,
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
                                                               config, builder::setAggregationTemporalitySelector);
            OtlpConfigUtil.configureOtlpHistogramDefaultAggregation(
                                                                    config, builder::setDefaultAggregationSelector);

            return builder.build();
        } else if (protocol.equals(PROTOCOL_GRPC)) {
            OtlpGrpcMetricExporterBuilder builder = grpcBuilder();

            OtlpConfigUtil.configureOtlpExporterBuilder(
                                                        DATA_TYPE_METRICS,
                                                        config,
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
                                                               config, builder::setAggregationTemporalitySelector);
            OtlpConfigUtil.configureOtlpHistogramDefaultAggregation(
                                                                    config, builder::setDefaultAggregationSelector);

            return builder.build();
        }
        throw new ConfigurationException("Unsupported OTLP metrics protocol: " + protocol);
    }

    @Override
    public MetricExporter createExporter(ConfigProperties config) {

        String metricExporterProperty = config.getString(OpenTelemetryConstants.CONFIG_METRICS_EXPORTER_PROPERTY);
        String otlpEndpointProperty = config.getString("otel.exporter.otlp.endpoint");

        /*
         * Dissect
         */
        Pattern pattern = Pattern.compile("(https?)://([a-zA-Z0-9.]+):(\\d+)");
        Matcher matcher = pattern.matcher(otlpEndpointProperty);

        boolean isHttps = false;
        String host = null;
        String port = null;

        if (matcher.matches()) {
            isHttps = (matcher.group(1).toLowerCase().endsWith("s")) ? true : false;
            if (!isHttps) {
                //throw warning about having to use HTTPS when using this exporter.
            }
            host = matcher.group(2);
            port = matcher.group(3);
        } else {
            // malformed! do something.
            //TODO warning about malform

            System.out.println("endpoint property not set correctly: " + otlpEndpointProperty);
        }

        System.out.println("<<<<<<<<  OpenLibertyMetricExporterProvider  >>>>>");

        MetricExporter metricExporter = defaultCreateExporterWrapper(config);
        if (metricExporter == null) {
            //uh oh
            //another warning? - the throw probably goes past this.
            return null;
        }

        if (host != null && !host.isEmpty() && port != null && !port.isEmpty()) {

            Map.Entry<SSLContext, X509TrustManager> pair = retriveSSLContextAndTrustManager(host, port, config);

            if (OtlpGrpcMetricExporter.class.isInstance(metricExporter)) {
                metricExporter = ((OtlpGrpcMetricExporter) metricExporter).toBuilder().setSslContext(pair.getKey(), pair.getValue()).build();
            } else if (OtlpHttpMetricExporter.class.isInstance(metricExporter)) {
                metricExporter = ((OtlpHttpMetricExporter) metricExporter).toBuilder().setSslContext(pair.getKey(), pair.getValue()).build();

            }
            //exporter = exporter.toBuilder().setSslContext(pair.getKey(), pair.getValue()).build();
            OpenLibertyMetricsExporterWrapper wrapper = new OpenLibertyMetricsExporterWrapper(metricExporter);

            OpenTelemetryInfoFactoryImpl.setMetricsExporterWrapper(wrapper);
            return wrapper;
        } else {
            //WARNING about how endpoint is not configured.

            //TODO fill
            return null;
        }
    }

    @Override
    public String getName() {
        return "libertyotlp";
    }

    public Map.Entry<SSLContext, X509TrustManager> retriveSSLContextAndTrustManager(String host, String port, ConfigProperties config) {
        JSSEHelper jsse = JSSEHelper.getInstance();

        /*
         * ConnectionInfo consists of at least three things
         *
         * public static final String CONNECTION_INFO_DIRECTION = "com.ibm.ssl.direction";
         * we want OUTBOUND
         * public static final String CONNECTION_INFO_REMOTE_HOST = "com.ibm.ssl.remoteHost";
         * this is mandatory
         * public static final String CONNECTION_INFO_REMOTE_PORT = "com.ibm.ssl.remotePort";
         * this is optional... but we NEED this.
         *
         */

        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, host);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, port);

        SSLContext sslContext = null;
        X509TrustManager trustManager = null;
        SSLConfig sslConfig;
        String alias = null;

        try {

            Object[] retPair = jsse.getSSLContext2(null, connectionInfo, mynotifier, true);
            sslContext = (SSLContext) retPair[0];
            trustManager = (X509TrustManager) retPair[1];

            //debug to see what SSL Config we would get with just the connectionInfo we would
            sslConfig = (SSLConfig) jsse.getProperties(null, connectionInfo, myOtherNotifier, true);
            alias = sslConfig.getProperty(Constants.SSLPROP_ALIAS);
            System.out.println("@@ DDebug: The alias the SSL component would return is " + alias);

            SSLConfig explitSSLConfig = (SSLConfig) jsse.getProperties("debugSSLConfig", connectionInfo, thirdNotifier, true);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new AbstractMap.SimpleEntry<SSLContext, X509TrustManager>(sslContext, trustManager);

    }

    static MyPrivateSSLConfigListener mynotifier = new MyPrivateSSLConfigListener("first");
    static MyPrivateSSLConfigListener myOtherNotifier = new MyPrivateSSLConfigListener("second");

    static MyPrivateSSLConfigListener thirdNotifier = new MyPrivateSSLConfigListener("third");

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
