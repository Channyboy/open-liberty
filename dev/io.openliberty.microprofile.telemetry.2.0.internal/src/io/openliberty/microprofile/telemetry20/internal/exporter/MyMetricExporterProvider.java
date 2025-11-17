package io.openliberty.microprofile.telemetry20.internal.exporter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.ws.ssl.LibertySSLContext;

import io.openliberty.microprofile.telemetry20.internal.info.OpenTelemetryInfoFactoryImpl;
import io.openliberty.microprofile.telemetry20.internal.ssl.MySSLContext;
import io.openliberty.microprofile.telemetry20.internal.ssl.MySSLContextProvider;
import io.openliberty.microprofile.telemetry20.internal.ssl.MyX509TrustManager;
import io.opentelemetry.exporter.otlp.internal.OtlpMetricExporterProvider;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

public class MyMetricExporterProvider implements ConfigurableMetricExporterProvider {

    @Override
    public MetricExporter createExporter(ConfigProperties config) {

        System.out.println("!!!!!!!!!!!!!!!!!! THE BEST EXPORTER IN TOWN 123" + config);

        //gh.toBuilder().setSslContext(, null)

        // Do we need to delay until we retrieve a config.

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
        JSSEHelper jsse = JSSEHelper.getInstance();

        System.out.println("does defaultSSLConfig exist " + jsse.doesSSLConfigExist("defaultSSLConfig"));
        System.out.println("does outboundSSLSettings exist " + jsse.doesSSLConfigExist("outboundSSLSettings"));

        //SSL1
        ///////////////////////////////////////////////////////////////////////////////////////
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, "asdljf");
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, "5317");

        SSLContext sslContext = null;
        X509TrustManager tm = null;
        SSLConfig sslConfig;
        String alias = null;
        MySSLContext mySSLContextWrapper = null;
        MyX509TrustManager myX509TrustManagerWrapper = null;
        MySSLContextProvider mySSLContextSpiwrapper = null;
        try {
            //sslContext = jsse.getSSLContext(null, connectionInfo, null);
            Object[] stuff = jsse.getSSLContext2(null, connectionInfo, null, true);
            sslContext = (SSLContext) stuff[0];
            tm = (X509TrustManager) stuff[1];

            sslConfig = (SSLConfig) jsse.getProperties(null, connectionInfo, null, true);
            alias = sslConfig.getProperty(Constants.SSLPROP_ALIAS);

            if (sslContext instanceof LibertySSLContext) {
                System.out.println(" ! $ % * (party time");
                mySSLContextSpiwrapper = new MySSLContextProvider(sslContext, alias);
                mySSLContextWrapper = new MySSLContext(mySSLContextSpiwrapper, sslContext.getProvider(), sslContext.getProtocol());
            }

            myX509TrustManagerWrapper = new MyX509TrustManager(tm);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Setting up to be changed later
        OpenTelemetryInfoFactoryImpl.setSSLContextProvider(mySSLContextSpiwrapper);
        OpenTelemetryInfoFactoryImpl.setX509TrustManager(myX509TrustManagerWrapper);

        //Lets create the correct sslcontext and x509 manager
        ///////////////////
        final Map<String, Object> connectionInfo2 = new HashMap<String, Object>();
        connectionInfo2.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
        connectionInfo2.put(Constants.CONNECTION_INFO_REMOTE_HOST, "localhost");
        connectionInfo2.put(Constants.CONNECTION_INFO_REMOTE_PORT, "4317");

        SSLContext newSSLContext = null;
        X509TrustManager newTrustManager = null;
        SSLConfig sslConfig2;
        String alias2 = null;

        try {
            //sslContext = jsse.getSSLContext(null, connectionInfo, null);
            Object[] stuff = jsse.getSSLContext2(null, connectionInfo2, null, true);
            newSSLContext = (SSLContext) stuff[0];
            newTrustManager = (X509TrustManager) stuff[1];

            sslConfig2 = (SSLConfig) jsse.getProperties(null, connectionInfo2, null, true);
            alias2 = sslConfig2.getProperty(Constants.SSLPROP_ALIAS);

        } catch (Exception e) {
            e.printStackTrace();
        }

        //Set up for instances to update.
        OpenTelemetryInfoFactoryImpl.setNewSSLCOntextSwapWith(newSSLContext);
        OpenTelemetryInfoFactoryImpl.setNewAlias(alias2);
        OpenTelemetryInfoFactoryImpl.setNewTrustmanagerSwapWith(newTrustManager);

        ////////////////////////////////////////////////////////////////////////////////////////
        OtlpGrpcMetricExporter gh = (OtlpGrpcMetricExporter) new OtlpMetricExporterProvider().createExporter(config);
        //OtlpGrpcMetricExporter gh2 = (OtlpGrpcMetricExporter) new OtlpMetricExporterProvider().createExporter(config);

        gh = gh.toBuilder().setSslContext(mySSLContextWrapper, myX509TrustManagerWrapper).build();

        return gh;
    }

    /*
     * Hot swapping the SSLConfig with relfleciion.
     */
    public void foo(ConfigProperties config) {
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
        JSSEHelper jsse = JSSEHelper.getInstance();

        System.out.println("does defaultSSLConfig exist " + jsse.doesSSLConfigExist("defaultSSLConfig"));
        System.out.println("does outboundSSLSettings exist " + jsse.doesSSLConfigExist("outboundSSLSettings"));

        //SSL1
        ///////////////////////////////////////////////////////////////////////////////////////
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, "jokerhill");
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, "4315");

        SSLContext sslContext = null;
        X509TrustManager tm = null;

        try {
            //sslContext = jsse.getSSLContext(null, connectionInfo, null);
            Object[] stuff = jsse.getSSLContext2(null, connectionInfo, null, true);
            sslContext = (SSLContext) stuff[0];
            tm = (X509TrustManager) stuff[1];
        } catch (Exception e) {
            e.printStackTrace();
        }

        //SSL2
        ///////////////////////////////////////////////////////////////////////////////////////
        final Map<String, Object> connectionInfo2 = new HashMap<String, Object>();
        connectionInfo2.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
        connectionInfo2.put(Constants.CONNECTION_INFO_REMOTE_HOST, "localhost");
        connectionInfo2.put(Constants.CONNECTION_INFO_REMOTE_PORT, "4317");
        SSLContext sslContext2 = null;;
        X509TrustManager tm2 = null;

        try {
            //sslContext = jsse.getSSLContext(null, connectionInfo, null);
            Object[] stuff = jsse.getSSLContext2(null, connectionInfo2, null, true);
            sslContext2 = (SSLContext) stuff[0];
            tm2 = (X509TrustManager) stuff[1];
        } catch (Exception e) {
            e.printStackTrace();
        }

        ////////////////////////////////////////////////////////////////////////////////////////
        OtlpGrpcMetricExporter gh = (OtlpGrpcMetricExporter) new OtlpMetricExporterProvider().createExporter(config);
        OtlpGrpcMetricExporter gh2 = (OtlpGrpcMetricExporter) new OtlpMetricExporterProvider().createExporter(config);

        //Commented out, lets do sneaky stuff.
        gh = gh.toBuilder().setSslContext(sslContext, tm).build();
        gh2 = gh2.toBuilder().setSslContext(sslContext2, tm2).build();

        OpenTelemetryInfoFactoryImpl.setExporter(gh);
        OpenTelemetryInfoFactoryImpl.setUpdatedExporter(gh2);
    }

    public void reflectionFoo(JSSEHelper jsse, Map<String, Object> connectionInfo, OtlpGrpcMetricExporter gh, ConfigProperties ogConfig) {

        OtlpGrpcMetricExporter updatedGH = (OtlpGrpcMetricExporter) new OtlpMetricExporterProvider().createExporter(ogConfig);

        for (Field f : gh.getClass().getDeclaredFields()) {
            //System.out.println("f name " + f.getName());
            if (f.getName().equalsIgnoreCase("delegate")) {
                System.out.println("here?");
                f.setAccessible(true);
                try {
                    f.set(gh, null);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public String getName() {
        return "poop";
    }

}
