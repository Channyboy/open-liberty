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
package com.ibm.ws.microprofile.metrics30.impl;

import javax.enterprise.inject.Vetoed;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.ws.microprofile.metrics23.impl.MetricRegistry23Impl;

/**
 * A registry of metric instances.
 */
@Vetoed
public class MetricRegistry30Impl extends MetricRegistry23Impl {

    /**
     * @param configResolver
     */
    public MetricRegistry30Impl(ConfigProviderResolver configResolver) {
        super(configResolver);
    }

    @Override
    public Timer timer(Metadata metadata, Tag... tags) {
        return getOrAdd(metadata, MetricBuilder30.TIMERS, tags);
    }

    @Override
    public SimpleTimer simpleTimer(Metadata metadata, Tag... tags) {
        return getOrAdd(metadata, MetricBuilder30.SIMPLE_TIMER, tags);
    }

    protected interface MetricBuilder30<T extends Metric> extends MetricBuilder23<Metric> {

        MetricBuilder<Timer> TIMERS = new MetricBuilder<Timer>() {
            @Override
            public Timer newMetric() {
                return new Timer30Impl();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Timer.class.isInstance(metric);
            }
        };

        MetricBuilder<SimpleTimer> SIMPLE_TIMER = new MetricBuilder<SimpleTimer>() {
            @Override
            public SimpleTimer newMetric() {
                return new SimpleTimer30Impl();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return SimpleTimer.class.isInstance(metric);
            }
        };
    }
}
