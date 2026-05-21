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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.JSSEProvider;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.websphere.ssl.SSLConfigChangeListener;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.ssl.JSSEProviderFactory;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

/**
 *
 */
public class AbstractOpenLibertySignalExporterProvider {

    final String TELEMETRY_SIGNAL;

    public AbstractOpenLibertySignalExporterProvider(String signal) {
        TELEMETRY_SIGNAL = signal;
    }

    private static final TraceComponent tc = Tr.register(AbstractOpenLibertySignalExporterProvider.class);

    protected ConfigProperties openTelemetryConfigProperties = null;
    protected int collectorPort = 0;
    protected String collectorHost = null;

    /**
     * To be called in the createExporter(...) method in the provider class.
     * The config is immutable. We'll set it globally instead of passing it around
     * through the methods for ease of use.
     *
     * @param config
     */
    protected void init(ConfigProperties config) {
        openTelemetryConfigProperties = config;
    }

    @FFDCIgnore({ MalformedURLException.class, URISyntaxException.class })
    void validateOTLPEndpointProperty(String signal) {

        /*
         * If both properties are null. They did not set it.
         * Will be using default `http`.
         */
        String otlpEndPointPropertyKey = "otel.exporter.otlp." + signal + ".endpoint";
        String otlpEndpointPropertyValue = openTelemetryConfigProperties.getString(otlpEndPointPropertyKey);

        if (otlpEndpointPropertyValue == null) {
            otlpEndPointPropertyKey = "otel.exporter.otlp.endpoint";
            otlpEndpointPropertyValue = openTelemetryConfigProperties.getString(otlpEndPointPropertyKey);
        }

        /*
         * If null ; nothing was configured
         * otherwise let's break it down.
         */
        if (otlpEndpointPropertyValue != null) {
            URL url;

            try {
                url = new URI(otlpEndpointPropertyValue).toURL();
            } catch (URISyntaxException e) {
                throw new LibertyOTLPExporterConfigurationExcepton(String.format("The endpoint provided has caused an URISyntaxException. The endpoint configured is: [%s]",
                                                                                 otlpEndpointPropertyValue), e);
            } catch (MalformedURLException e) {
                throw new LibertyOTLPExporterConfigurationExcepton(String.format("The endpoint provided has caused a MalformedURLException. The endpoint configured is: [%s]",
                                                                                 otlpEndpointPropertyValue), e);
            }

            System.out.println("what is this " + url.getProtocol());

            if (!url.getProtocol().equalsIgnoreCase("https")) {
                //TODO
                //The libertyotlp exporter requires that the otel.exporter.otlp.endpoint (or the superseding {0}) property to be  be configured with `https`. The property read in is [{1}] and the value is [{2}].
                //Tr.error(tc, "msgkey", null);

                throw new LibertyOTLPExporterConfigurationExcepton(String
                                .format("The libertyotlp exporter requires an endpoint to be configured with https. The endpoint configured is: [%s]", otlpEndpointPropertyValue));
            }

            collectorHost = url.getHost();
            collectorPort = (url.getPort() == -1) ? url.getDefaultPort() : url.getPort();

            if (collectorHost == null || collectorHost.isEmpty() || collectorPort <= 0) {
                throw new LibertyOTLPExporterConfigurationExcepton(String.format("Could not resolve the hostname or port name from the endpoint. The endpoint configured is: [%s]",
                                                                                 otlpEndpointPropertyValue));
            }

        } else { //Defaults are being used. Error since we would require HTTPS
            System.out.println("RUH ROH using default");
            //TODO
            //The libertyotlp exporter requires that the otel.exporter.otlp.endpoint (or the superseding {0}) property to be  be configured with `https`. The mpTelemetry runtime has detected that neither of these properties were configured. The default value uses `http`.
            //Tr.error(tc, "msgkey", null);
            String msg = String
                            .format("The otel.exporter.otlp.endpoint and otel.exporter.otlp.%s.endpoint has not been configured. "
                                    + "The default value will use `http` and the libertyotlp exporter requires a endpoint configured with `https`",
                                    TELEMETRY_SIGNAL);
            throw new LibertyOTLPExporterConfigurationExcepton(msg);
        }

    }

    Map.Entry<SSLContext, X509TrustManager> retriveSSLContextAndTrustManager() {
        return retriveSSLContextAndTrustManager(null);
    }

    Map.Entry<SSLContext, X509TrustManager> retriveSSLContextAndTrustManager(SSLConfigChangeListener changeListener) {

        //Redundant check. But just in case...
        if (collectorHost == null || collectorHost.isEmpty() || collectorPort <= 0) {
            throw new LibertyOTLPExporterConfigurationExcepton(String.format("The target OTLP endpoint's hostname and/or portname were never resolved. host = [%s] port = [%d]",
                                                                             collectorHost, collectorPort));
        }

        JSSEHelper jsse = JSSEHelper.getInstance();

        if (jsse == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Retrieved a null instance for the JSSEHelper");
            }
            throw new LibertyOTLPExporterConfigurationExcepton("Encountered an internal error with the SSL component while attempting to configure a TLS connection");
            //Encountered an internal error with the SSL component while attempting to configure a TLS connection

            //Tr.ERROR
        }

        /*
         * ConnectionInfo consists of at least three things
         *
         * public static final String CONNECTION_INFO_DIRECTION = "com.ibm.ssl.direction";
         * we want OUTBOUND!
         * public static final String CONNECTION_INFO_REMOTE_HOST = "com.ibm.ssl.remoteHost";
         * this is mandatory: Substitute with otel collector host
         * public static final String CONNECTION_INFO_REMOTE_PORT = "com.ibm.ssl.remotePort";
         * this is optional... but we NEED this: Substitute with otel collector port.
         *
         */

        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, collectorHost);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, String.valueOf(collectorPort));

        SSLContext sslContext = null;
        X509TrustManager trustManager = null;
        SSLConfig sslConfig;
        //String alias = null;

        try {

            Object[] retPair = jsse.getSSLContext2(null, connectionInfo, changeListener, true);

            //TODO: CURRENTLY using the modified version that returns BOTH SSL context and TRUST
            // NEED TO: Resolve how to figure out  how to get Trust manager?
            sslContext = (SSLContext) retPair[0];
            trustManager = (X509TrustManager) retPair[1];

            // Debug to see what SSL Config we would get with just the connectionInfo we would
            sslConfig = (SSLConfig) jsse.getProperties(null, connectionInfo, null, true);

            //String ctxtProvider = getSSLContextProperty(, sslConfig);
            String ctxtProvider = sslConfig.getProperty(Constants.SSLPROP_CONTEXT_PROVIDER);
            String trustMgr = sslConfig.getProperty(Constants.SSLPROP_TRUST_MANAGER);

            TrustManagerFactory factory = TrustManagerFactory.getInstance(trustMgr, ctxtProvider);
            factory.init(null);
            //System.out.println("DDebug: AbstractJSSEProvider: getWSTrustManager: Retrieving the trustmanager from factory " + Arrays.toString(defaultTMArray));

            /*
             * SSL Context - normally
             */

            JSSEProvider jsseProv = JSSEProviderFactory.getInstance(ctxtProvider);
            SSLContext context = jsseProv.getSSLContext(connectionInfo, sslConfig);

            //Modified getWSTrustManger to be public - obtains List of  Trustmanger. Should only be one item... or the first time.
//            List<TrustManager> trustMgrs = new ArrayList<TrustManager>();
//            ((AbstractJSSEProvider) jsseProv).getWSTrustmanager(trustMgrs, connectionInfo, sslConfig);

            /*
             * End Experiment
             */

            // alias = sslConfig.getProperty(Constants.SSLPROP_ALIAS);
            //System.out.println("@@ DDebug: The alias the SSL component would return is " + alias);

            //Some experimentation stuff: forgot about what... but not really relevant.
            KeyStore ks = TesterTester.getInstance().getKeyStore("genKeyStore");
            System.out.println("hello world " + ks.getType());

        } catch (Exception e) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Encounted an exception when retrieving SSLContext and X509TrustManager: " + e);
            }
            throw new LibertyOTLPExporterConfigurationExcepton("Encountered an internal error with the SSL component while attempting to configure the TLS connection: " + e);
            //Encountered an internal error with the SSL component while attempting to configure a TLS connection
        }
        return new AbstractMap.SimpleEntry<SSLContext, X509TrustManager>(sslContext, trustManager);

    }

    String decodePassword() {
        return "sdf";
    }

}
