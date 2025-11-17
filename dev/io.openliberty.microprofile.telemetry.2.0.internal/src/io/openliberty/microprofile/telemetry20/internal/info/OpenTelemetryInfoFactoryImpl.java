/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.internal.info;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.function.BiFunction;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.common.info.AbstractOpenTelemetryInfoFactory;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryInfoFactory;
import io.openliberty.microprofile.telemetry20.internal.ssl.MySSLContextProvider;
import io.openliberty.microprofile.telemetry20.internal.ssl.MyX509TrustManager;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.instrumentation.resources.HostResource;
import io.opentelemetry.instrumentation.resources.OsResource;
import io.opentelemetry.instrumentation.resources.ProcessResource;
import io.opentelemetry.instrumentation.resources.ProcessRuntimeResource;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Classes;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Cpu;
import io.opentelemetry.instrumentation.runtimemetrics.java8.GarbageCollector;
import io.opentelemetry.instrumentation.runtimemetrics.java8.MemoryPools;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Threads;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

/**
 * This class contains version specific configuration for OpenTelemetryInfoFactory
 */
//We want this to start before CDI so the meta data slot is ready before anyone triggers the CDI producer.
@Component(service = { OpenTelemetryInfoFactory.class }, property = { "service.vendor=IBM", "service.ranking:Integer=1500" })
public class OpenTelemetryInfoFactoryImpl extends AbstractOpenTelemetryInfoFactory {

    static long startMilli = System.currentTimeMillis();

    static Timer myTimer = new Timer();
    static OtlpGrpcMetricExporter myexporter = null;
    static OtlpGrpcMetricExporter updatedExporter = null;

    static Object updatedExporterDelegate = null;

    public static void setExporter(OtlpGrpcMetricExporter exp) {
        myexporter = exp;
    }

    public static void setUpdatedExporter(OtlpGrpcMetricExporter exp) {
        updatedExporter = exp;
        try {
            updatedExporterDelegate = getDelegate(updatedExporter);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    /***********
     *
     * Thew SSLCOntext wrapper stuff
     */

    static MySSLContextProvider daSSLContextProvider = null;
    static MyX509TrustManager daTrustManager = null;

    static SSLContext newSSLContextToSwapWith = null;
    static X509TrustManager newtrustManagerSwapWith = null;

    static String alias = null;

    public static void setX509TrustManager(MyX509TrustManager inc) {
        daTrustManager = inc;
    }

    public static void setNewTrustmanagerSwapWith(X509TrustManager inc) {
        newtrustManagerSwapWith = inc;
    }

    public static void setNewAlias(String inc) {
        alias = inc;
    }

    public static void setSSLContextProvider(MySSLContextProvider inc) {
        daSSLContextProvider = inc;
    }

    public static void setNewSSLCOntextSwapWith(SSLContext inc) {
        newSSLContextToSwapWith = inc;
    }
    ////////////////////////////////////////////////

    static Object getDelegate(OtlpGrpcMetricExporter g) throws IllegalArgumentException, IllegalAccessException {
        Object obj = null;
        for (Field f : g.getClass().getDeclaredFields()) {
            //System.out.println("f name " + f.getName());
            if (f.getName().equalsIgnoreCase("delegate")) {
                System.out.println("getting delegate ?");
                f.setAccessible(true);
                obj = f.get(g);
            }
        }
        return obj;
    }

    static void updateDelegate(OtlpGrpcMetricExporter g, Object delegate) {
        for (Field f : g.getClass().getDeclaredFields()) {
            //System.out.println("f name " + f.getName());
            if (f.getName().equalsIgnoreCase("delegate")) {
                System.out.println("updateing delegate ?");
                f.setAccessible(true);
                try {
                    f.set(g, delegate);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static final TraceComponent tc = Tr.register(OpenTelemetryInfoFactoryImpl.class);

    private static final String DISABLED_RESOURCE_PROVIDERS = "otel.java.disabled.resource.providers";
    private static final String RESOURCES_PACKAGE = "io.opentelemetry.instrumentation.resources.";
    private static final String OS_RESOURCE_PROVIDER = RESOURCES_PACKAGE + "OsResourceProvider";
    private static final String HOST_RESOURCE_PROVIDER = RESOURCES_PACKAGE + "HostResourceProvider";
    private static final String PROCESS_RESOURCE_PROVIDER = RESOURCES_PACKAGE + "ProcessResourceProvider";
    private static final String PROCESS_RUNTIME_RESOURCE_PROVIDER = RESOURCES_PACKAGE + "ProcessRuntimeResourceProvider";

    // Version specific API calls to AutoConfiguredOpenTelemetrySdk.builder()
    @Override
    public OpenTelemetry buildOpenTelemetry(Map<String, String> openTelemetryProperties,
                                            BiFunction<? super Resource, ConfigProperties, ? extends Resource> resourceCustomiser, ClassLoader classLoader) {

        // System.out.println("\n hello ---------");
        // System.out.println(openTelemetryProperties);

//Old code for hotswapping the delegate in the exporter via reflection
//        myTimer.schedule(new TimerTask() {
//
//            @Override
//            public void run() {
//                System.out.println("updating SSL");
//                updateDelegate(myexporter, updatedExporterDelegate);
//            }
//        }, 10000);

        ///Code for swapping delegates via sslcontext(Provider) wrapper and x509 trustmanager wrapper
        myTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                System.out.println("updating wrappers");
                daSSLContextProvider.swapAlias(alias);
                daSSLContextProvider.swapDelegate(newSSLContextToSwapWith);
                daTrustManager.swapDelegate(newtrustManagerSwapWith);
            }
        }, 10000);

        OpenTelemetrySdk openTelemetry = AutoConfiguredOpenTelemetrySdk.builder()
                        .addPropertiesCustomizer(x -> openTelemetryProperties) //Overrides OpenTelemetry's property order
                        .addResourceCustomizer(resourceCustomiser)
                        .setServiceClassLoader(classLoader)
                        .disableShutdownHook()
                        .build()
                        .getOpenTelemetrySdk();

        return openTelemetry;
    }

    @Override
    protected ResourceBuilder customizeResource(Resource resource, ConfigProperties c, boolean isEnabled) {
        ResourceBuilder builder = super.customizeResource(resource, c, isEnabled);
        builder.put(OpenTelemetryConstants.KEY_SERVICE_INSTANCE_ID, UUID.randomUUID().toString());
        //Resource providers can be disabled with otel.java.disabled.resource.providers
        Set<String> disabledProviders = new HashSet<>(c.getList(DISABLED_RESOURCE_PROVIDERS));

        if (!disabledProviders.contains(OS_RESOURCE_PROVIDER)) {
            builder.putAll(OsResource.get());
        }
        if (!disabledProviders.contains(HOST_RESOURCE_PROVIDER)) {
            builder.putAll(HostResource.get());
        }
        if (!disabledProviders.contains(PROCESS_RESOURCE_PROVIDER)) {
            builder.putAll(ProcessResource.get());
        }
        if (!disabledProviders.contains(PROCESS_RUNTIME_RESOURCE_PROVIDER)) {
            builder.putAll(ProcessRuntimeResource.get());
        }
        return builder;
    }

    /** {@inheritDoc} */
    @Override
    protected void addDefaultVersionedProperties(Map<String, String> telemetryProperties) {
        //no op;
    }

    /** {@inheritDoc} */
    @Override
    protected void mergeInJVMMetrics(OpenTelemetry openTelemetry, boolean runtimeEnabled) {
        //JVM metrics are, naturally, created by the JVM and so are shared across apps.
        //Thus we only output them in runtime mode where they will not be misleading.
        if (openTelemetry != null && runtimeEnabled && runningOnJ9OrHotspot()) {
            // Register observers for runtime metrics
            Classes.registerObservers(openTelemetry);
            Cpu.registerObservers(openTelemetry);
            MemoryPools.registerObservers(openTelemetry);
            Threads.registerObservers(openTelemetry);
            GarbageCollector.registerObservers(openTelemetry);
        }
    }
}
