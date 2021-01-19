/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package io.smallrye.metrics.setup;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

import io.smallrye.metrics.MetricProducer;
import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.MetricsRequestHandler;
import io.smallrye.metrics.interceptors.MetricNameFactory;

@Component(service = WebSphereCDIExtension.class, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class MetricCdiInjectionExtension implements Extension, WebSphereCDIExtension {

    @Activate
    public void activate(ComponentContext context, Map<String, Object> properties) {
        System.out.println("MetricCdiInjectionExtension: Activating2");
    }

    void logVersion(@Observes BeforeBeanDiscovery bbd) {
        System.out.println("test");
        if (getImplementationVersion().isPresent()) {
            System.out.println("MetricCdiInjectionExtension: implvers -" + getImplementationVersion().get());
        } else {
            System.out.println("MetricCdiInjectionExtension: implvers - Unkown");
        }
        //temp
        //SmallRyeMetricsLogging.log.logSmallRyeMetricsVersion(getImplementationVersion().orElse("unknown"));
    }

    void registerAnnotatedTypes(@Observes BeforeBeanDiscovery bbd, BeanManager manager) {
        String extensionName = MetricCdiInjectionExtension.class.getName();
        System.out.println("something2");
        for (Class clazz : new Class[] {
                                         MetricProducer.class,
                                         MetricNameFactory.class,
                                         MetricRegistries.class,
                                         MetricsRequestHandler.class
        }) {
            bbd.addAnnotatedType(manager.createAnnotatedType(clazz), extensionName + "_" + clazz.getName());
        }
    }

    protected void defaultMetricRegistry(@Observes AfterBeanDiscovery abd, BeanManager manager) {
        System.out.println("Test afterbeandiscovery");
    }

    private Optional<String> getImplementationVersion() {
        return AccessController.doPrivileged(new PrivilegedAction<Optional<String>>() {
            @Override
            public Optional<String> run() {
                Properties properties = new Properties();
                try {
                    final InputStream resource = this.getClass().getClassLoader().getResourceAsStream("project.properties");
                    if (resource != null) {
                        properties.load(resource);
                        return Optional.ofNullable(properties.getProperty("smallrye.metrics.version"));
                    }
                } catch (IOException e) {
                    System.out.println("MetricCdiInjectionExtension: Unable to detect version of SmallRye Metrics");
                    //temp
                    //SmallRyeMetricsLogging.log.unableToDetectVersion();
                }
                return Optional.empty();
            }
        });
    }
}
