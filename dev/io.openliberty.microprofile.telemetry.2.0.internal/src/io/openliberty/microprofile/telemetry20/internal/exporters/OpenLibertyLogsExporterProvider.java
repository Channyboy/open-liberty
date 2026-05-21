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

import static io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil.DATA_TYPE_LOGS;
import static io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil.PROTOCOL_GRPC;
import static io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil.PROTOCOL_HTTP_PROTOBUF;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

public class OpenLibertyLogsExporterProvider extends AbstractOpenLibertySignalExporterProvider implements ConfigurableLogRecordExporterProvider, AutoConfigureListener {

    OpenLibertyLogRecordExporterWrapper wrapper;

    /**
     * @param signal
     */
    public OpenLibertyLogsExporterProvider(String signal) {
        super("logs");
        System.out.println("instantiate OpenLibertyLogsExporterProvider!");
    }

    OtlpHttpLogRecordExporterBuilder httpBuilder() {
        return OtlpHttpLogRecordExporter.builder();
    }

    OtlpGrpcLogRecordExporterBuilder grpcBuilder() {
        return OtlpGrpcLogRecordExporter.builder();

    }

    private final AtomicReference<MeterProvider> meterProviderRef = new AtomicReference<>(MeterProvider.noop());

    @Override
    public void afterAutoConfigure(OpenTelemetrySdk sdk) {
        meterProviderRef.set(sdk.getMeterProvider());
    }

    public LogRecordExporter defaultCreateExporterWrapper() {
        String protocol = OtlpConfigUtil.getOtlpProtocol(DATA_TYPE_LOGS, openTelemetryConfigProperties);

        if (protocol.equals(PROTOCOL_HTTP_PROTOBUF)) {
            OtlpHttpLogRecordExporterBuilder builder = httpBuilder();

            OtlpConfigUtil.configureOtlpExporterBuilder(
                                                        DATA_TYPE_LOGS,
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
            builder.setMeterProvider(meterProviderRef::get);

            return builder.build();
        } else if (protocol.equals(PROTOCOL_GRPC)) {
            OtlpGrpcLogRecordExporterBuilder builder = grpcBuilder();

            OtlpConfigUtil.configureOtlpExporterBuilder(
                                                        DATA_TYPE_LOGS,
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
            builder.setMeterProvider(meterProviderRef::get);

            return builder.build();
        }
        throw new ConfigurationException("Unsupported OTLP logs protocol: " + protocol);
    }

    @Override
    public LogRecordExporter createExporter(ConfigProperties config) {
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

        System.out.println("<<<<<<<<  OpenLibertyLogsExporterProvider  >>>>>");

        LogRecordExporter logRecordExporter = createOTLPExporter();
        wrapper = new OpenLibertyLogRecordExporterWrapper(logRecordExporter);
        return wrapper;
    }

    LogRecordExporter createOTLPExporter() {

        LogRecordExporter logRecordExporter = defaultCreateExporterWrapper();

        //TODO: FILL IN SSLCONFIGCHANGELISTENER PARAMATER WHEN SECURITY TEAM SUPPORTS IT.
        Map.Entry<SSLContext, X509TrustManager> pair = retriveSSLContextAndTrustManager(null);

        if (OtlpGrpcLogRecordExporter.class.isInstance(logRecordExporter)) {
            logRecordExporter = ((OtlpGrpcLogRecordExporter) logRecordExporter).toBuilder().setSslContext(pair.getKey(), pair.getValue()).build();
        } else if (OtlpGrpcLogRecordExporter.class.isInstance(logRecordExporter)) {
            logRecordExporter = ((OtlpHttpLogRecordExporter) logRecordExporter).toBuilder().setSslContext(pair.getKey(), pair.getValue()).build();

        }
        return logRecordExporter;

    }

    //@Override
    public LogRecordExporter createExporterOld(ConfigProperties config) {
        return null;
//
//        String logsExporterProperty = config.getString(OpenTelemetryConstants.CONFIG_LOGS_EXPORTER_PROPERTY);
//        String otlpEndpointProperty = config.getString("otel.exporter.otlp.endpoint");
//
//        System.out.println("LogExporter set to: " + logsExporterProperty);
//
//        /*
//         * Dissect
//         */
//        Pattern pattern = Pattern.compile("(https?)://([a-zA-Z0-9.]+):(\\d+)");
//        Matcher matcher = pattern.matcher(otlpEndpointProperty);
//
//        boolean isHttps = false;
//        String host = null;
//        String port = null;
//
//        if (matcher.matches()) {
//            isHttps = (matcher.group(1).toLowerCase().endsWith("s")) ? true : false;
//            if (!isHttps) {
//                //throw warning about having to use HTTPS when using this exporter.
//            }
//            host = matcher.group(2);
//            port = matcher.group(3);
//        } else {
//            // malformed! do something.
//            //TODO warning about malform
//
//            System.out.println("endpoint property not set correctly: " + otlpEndpointProperty);
//        }
//
//        System.out.println("<<<<<<<<  OpenLibertyLogsExporterProvider  >>>>>");
//
//        LogRecordExporter logRecordExporter = defaultCreateExporterWrapper(config);
//        if (logRecordExporter == null) {
//            //uh oh
//            //another warning? - the throw probably goes past this.
//            return null;
//        }
//
//        if (host != null && !host.isEmpty() && port != null && !port.isEmpty()) {
//
//            Map.Entry<SSLContext, X509TrustManager> pair = retriveSSLContextAndTrustManager(host, port, config);
//
//            if (OtlpGrpcMetricExporter.class.isInstance(logRecordExporter)) {
//                logRecordExporter = ((OtlpGrpcLogRecordExporter) logRecordExporter).toBuilder().setSslContext(pair.getKey(), pair.getValue()).build();
//            } else if (OtlpHttpMetricExporter.class.isInstance(logRecordExporter)) {
//                logRecordExporter = ((OtlpHttpLogRecordExporter) logRecordExporter).toBuilder().setSslContext(pair.getKey(), pair.getValue()).build();
//
//            }
//            //exporter = exporter.toBuilder().setSslContext(pair.getKey(), pair.getValue()).build();
//            OpenLibertyLogRecordExporterWrapper wrapper = new OpenLibertyLogRecordExporterWrapper(logRecordExporter);
//
//            //TODO
//            //OpenTelemetryInfoFactoryImpl.setLogRecordExporterWrapper(wrapper);
//            return wrapper;
//        } else {
//            //WARNING about how endpoint is not configured.
//
//            //TODO fill
//            return null;
//        }
    }

    @Override
    public String getName() {
        return "libertyotlp";
    }

    public Map.Entry<SSLContext, X509TrustManager> retriveSSLContextAndTrustManager(String host, String port, ConfigProperties config) {
        return null;
//        JSSEHelper jsse = JSSEHelper.getInstance();
//
//        /*
//         * ConnectionInfo consists of at least three things
//         *
//         * public static final String CONNECTION_INFO_DIRECTION = "com.ibm.ssl.direction";
//         * we want OUTBOUND
//         * public static final String CONNECTION_INFO_REMOTE_HOST = "com.ibm.ssl.remoteHost";
//         * this is mandatory
//         * public static final String CONNECTION_INFO_REMOTE_PORT = "com.ibm.ssl.remotePort";
//         * this is optional... but we NEED this.
//         *
//         */
//
//        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
//        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
//        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, host);
//        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, port);
//
//        SSLContext sslContext = null;
//        X509TrustManager trustManager = null;
//        SSLConfig sslConfig;
//        String alias = null;
//
//        try {
//
//            Object[] retPair = jsse.getSSLContext2(null, connectionInfo, mynotifier, true);
//            sslContext = (SSLContext) retPair[0];
//            trustManager = (X509TrustManager) retPair[1];
//
//            //debug to see what SSL Config we would get with just the connectionInfo we would
//            sslConfig = (SSLConfig) jsse.getProperties(null, connectionInfo, myOtherNotifier, true);
//            alias = sslConfig.getProperty(Constants.SSLPROP_ALIAS);
//            System.out.println("@@ DDebug: The alias the SSL component would return is " + alias);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return new AbstractMap.SimpleEntry<SSLContext, X509TrustManager>(sslContext, trustManager);

    }

}
