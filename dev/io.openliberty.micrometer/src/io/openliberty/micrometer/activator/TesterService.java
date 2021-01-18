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
package io.openliberty.micrometer.activator;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(service = { TesterService.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class TesterService {

    @Activate
    public void activate(ComponentContext context, Map<String, Object> properties) {
//        System.out.println("activating activator");
//
//        System.out.println(io.micrometer.core.instrument.MeterRegistry.class); //works
//        System.out.println(io.prometheus.client.Counter.class); //works
//        System.out.println(org.HdrHistogram.AtomicHistogram.class); //works
//        System.out.println(org.LatencyUtils.LatencyStats.class); //works
//
//        //FAIL
//        System.out.println(io.micrometer.prometheus.PrometheusMeterRegistry.class);
    }

}
