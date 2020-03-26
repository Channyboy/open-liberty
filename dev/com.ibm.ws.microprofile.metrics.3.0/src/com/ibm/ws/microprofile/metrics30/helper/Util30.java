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
package com.ibm.ws.microprofile.metrics30.helper;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics23.helper.Util23;

/**
 *
 */
public class Util30 extends Util23 {
    private static final TraceComponent tc = Tr.register(Util30.class);

    public static Map<String, Number> getTimerNumbers(Timer timer, String tags, double conversionFactor) {
        Map<String, Number> results = new HashMap<String, Number>();
        results.put(Constants.COUNT + tags, timer.getCount());
        results.put("elapsedTime" + tags, timer.getElapsedTime().toNanos());
        results.put(Constants.MEAN_RATE + tags, timer.getMeanRate());
        results.put(Constants.ONE_MINUTE_RATE + tags, timer.getOneMinuteRate());
        results.put(Constants.FIVE_MINUTE_RATE + tags, timer.getFiveMinuteRate());
        results.put(Constants.FIFTEEN_MINUTE_RATE + tags, timer.getFifteenMinuteRate());

        results.put(Constants.MAX + tags, timer.getSnapshot().getMax() / conversionFactor);
        results.put(Constants.MEAN + tags, timer.getSnapshot().getMean() / conversionFactor);
        results.put(Constants.MIN + tags, timer.getSnapshot().getMin() / conversionFactor);

        results.put(Constants.STD_DEV + tags, timer.getSnapshot().getStdDev() / conversionFactor);

        results.put(Constants.MEDIAN + tags, timer.getSnapshot().getMedian() / conversionFactor);
        results.put(Constants.PERCENTILE_75TH + tags, timer.getSnapshot().get75thPercentile() / conversionFactor);
        results.put(Constants.PERCENTILE_95TH + tags, timer.getSnapshot().get95thPercentile() / conversionFactor);
        results.put(Constants.PERCENTILE_98TH + tags, timer.getSnapshot().get98thPercentile() / conversionFactor);
        results.put(Constants.PERCENTILE_99TH + tags, timer.getSnapshot().get99thPercentile() / conversionFactor);
        results.put(Constants.PERCENTILE_999TH + tags, timer.getSnapshot().get999thPercentile() / conversionFactor);

        return results;
    }

    public static Map<String, Object> getSimpleTimerNumbers2(SimpleTimer simpleTimer, String tags, double conversionFactor) {
        Map<String, Object> results = new HashMap<String, Object>();
        results.put(Constants.COUNT + tags, simpleTimer.getCount());
        results.put("elapsedTime" + tags, simpleTimer.getElapsedTime().toNanos());

        Number value = (simpleTimer.getMaxTimeDuration() != null) ? simpleTimer.getMaxTimeDuration().toNanos() * conversionFactor : null;
        results.put("maxTimeDuration" + tags, value);
        value = (simpleTimer.getMinTimeDuration() != null) ? simpleTimer.getMinTimeDuration().toNanos() * conversionFactor : null;
        results.put("minTimeDuration" + tags, value);
        return results;
    }

}
