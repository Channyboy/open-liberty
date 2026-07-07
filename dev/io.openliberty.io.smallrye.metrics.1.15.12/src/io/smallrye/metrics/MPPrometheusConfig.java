/*
 *   Copyright 2020, 2024 Red Hat, Inc, and individual contributors
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
/*
 * Liberty changes are enclosed by LIBERTY CHANGE START and LIBERTY CHANGE END
 */
package io.smallrye.metrics;

import org.eclipse.microprofile.config.ConfigProvider;

import io.micrometer.prometheusmetrics.PrometheusConfig;

/**
 *
 * MPPrometheusConfig is an implementation of the {@link PrometheusConfig} which will accept Prometheus
 * related configuration values prepended with "mp.metrics.". This Config is used for the
 * {@link io.micrometer.prometheus.PrometheusMeterRegistry}
 * that is created for the MicroProfile Metric Registries in {@link SharedMetricRegistries}. This Config is not created within
 * the
 * {@link SharedMetricRegistries} due to some vendors having to load the SmallRye classes with reflection and the possibility of
 * the
 * Micrometer Prometheus library not being on the class path during runtime.
 *
 */
public class MPPrometheusConfig implements PrometheusConfig {

    @Override
    public String get(final String propertyName) {
        return ConfigProvider.getConfig().getOptionalValue("mp.metrics." + propertyName, String.class).orElse(null);
    }
}
