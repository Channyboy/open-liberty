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
package io.openliberty.microprofile.metrics40.internal.impl;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.osgi.service.component.annotations.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.mpmetrics.MpMetricRegistryAdapter;

/**
 * A map of shared, named metric registries.
 */
@Component(service = SharedMetricRegistries.class, immediate = true)
public class SharedMetricRegistries {

    private static PrometheusMeterRegistry prometheusMeterRegistry = null;

    public static void clear() {
        MetricRegistries.dropAll();

    }

    public static void remove(String key) {
        Type type = typeOf(key);
        MetricRegistries.drop(type);
    }

    public MetricRegistry add(String name, MetricRegistry registry) {
        Type type = typeOf(name);
        return MetricRegistries.get(type);
    }

    protected static Type typeOf(String name) {
        if (name.equals("base")) {
            return Type.BASE;
        } else if (name.equals("vendor")) {
            return Type.VENDOR;
        } else if (name.equals("application")) {
            return Type.APPLICATION;
        } else {
            throw new IllegalArgumentException("Name of registry must be base vendor or application");
        }

    }

    protected MetricRegistry createNewMetricRegsitry(Type type) {
        return new MpMetricRegistryAdapter(type, Metrics.globalRegistry);
    }

    public void associateMetricIDToApplication(MetricID metricID, String appName, MetricRegistry registry) {
        if (MpMetricRegistryAdapter.class.isInstance(registry)) {
            MpMetricRegistryAdapter metricRegistryImpl = (MpMetricRegistryAdapter) registry;

            //TODO: add multi apps
            //metricRegistryImpl.addNameToApplicationMap(metricID, appName);
        }

    }

    public MetricRegistry getOrCreate(String name) {
        //System.out.println("SMR : " + Metrics.globalRegistry.toString());
        //Need to create the PrometheusMetricRegistry.
        if (prometheusMeterRegistry == null) {
            prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            Metrics.addRegistry(prometheusMeterRegistry);

            for (MeterRegistry mr : Metrics.globalRegistry.getRegistries()) {
                System.out.println("found " + mr);
            }

        }

        //Calls The SmallRye `MetricRegistires` (i.e equivalent to this class)

        Type type = typeOf(name);
        final MetricRegistry existingMetricRegistry = MetricRegistries.get(type);

        if (existingMetricRegistry == null) {
            final MetricRegistry createdMetricRegistry = createNewMetricRegsitry(type);
            final MetricRegistry raced = add(name, createdMetricRegistry);
            if (raced == null) {
                return createdMetricRegistry;
            }
            return raced;
        }
        return existingMetricRegistry;
    }

}
