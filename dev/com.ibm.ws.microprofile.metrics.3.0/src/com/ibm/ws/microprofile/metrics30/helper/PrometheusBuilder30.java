/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics30.helper;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics23.helper.PrometheusBuilder23;

/**
 *
 */
public class PrometheusBuilder30 extends PrometheusBuilder23 {

    private static final TraceComponent tc = Tr.register(PrometheusBuilder30.class);

    public static void buildTimer(StringBuilder builder, String name, Timer timer, String description, String tags) {
        buildMetered(builder, name, timer, description, tags);
        double conversionFactor = Constants.NANOSECONDCONVERSION;

        String lineName = name + "_elapsedTime_" + MetricUnits.SECONDS.toString();
        getPromTypeLine(builder, lineName, "gauge");

        getPromValueLine(builder, lineName, timer.getElapsedTime().toNanos() * conversionFactor, tags);

        // Build Histogram
        buildSampling(builder, name, timer, description, conversionFactor, tags, Constants.APPENDEDSECONDS);
    }

    public static void buildSimpleTimer(StringBuilder builder, String name, SimpleTimer simpleTimer, String description, String tags) {
        double conversionFactor = Constants.NANOSECONDCONVERSION;

        buildCounting(builder, name, simpleTimer, description, tags);

        String lineName = name + "_elapsedTime_" + MetricUnits.SECONDS.toString();
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, simpleTimer.getElapsedTime().toNanos() * conversionFactor, tags);

        lineName = name + "_maxTimeDuration_" + MetricUnits.SECONDS.toString();
        getPromTypeLine(builder, lineName, "gauge");

        Number value = (simpleTimer.getMaxTimeDuration() != null) ? simpleTimer.getMaxTimeDuration().toNanos() * conversionFactor : Double.NaN;

        getPromValueLine(builder, lineName, value, tags);

        lineName = name + "_minTimeDuration_" + MetricUnits.SECONDS.toString();

//        getPromTypeLine(builder, lineName, "gauge");
//        System.out.println(simpleTimer.getMinTimeDuration().toNanos());
//        System.out.println(simpleTimer.getMinTimeDuration().toNanos() * conversionFactor);
//        System.out.println(notANumber(simpleTimer.getMinTimeDuration().toNanos() * conversionFactor));
        value = (simpleTimer.getMinTimeDuration() != null) ? simpleTimer.getMinTimeDuration().toNanos() * conversionFactor : Double.NaN;
        getPromValueLine(builder, lineName, value, tags);

    }

}
