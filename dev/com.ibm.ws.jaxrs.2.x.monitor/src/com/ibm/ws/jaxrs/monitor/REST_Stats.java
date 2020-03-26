/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.monitor;

import com.ibm.websphere.monitor.meters.Counter;
import com.ibm.websphere.monitor.meters.Gauge;
import com.ibm.websphere.monitor.meters.Meter;
import com.ibm.websphere.monitor.meters.StatisticsMeter;

/**
 * This is used to report RESTful Resource Method Related Statistics.
 * Each Restful resource method will have one instance of RestStatsMXBean.
 * Statistic reported :
 * 1) Application Name (Ear (when available)  and War)
 * 2) Resource Method Name
 * 3) Request Count for resource method
 * 4) Cumulative Response Time in nano seconds.
 * 
 */
public class REST_Stats extends Meter implements RestStatsMXBean {

    private String appName;
    private String methodName;

    //Following is the stats we are reporting for Resource Methods.
    private Counter requestCount;
    private final StatisticsMeter responseTime;
    private final Gauge minutePreviousMinimumDuration;
    private final Gauge minutePreviousMaximumDuration;
    
    private final Gauge minutePrevious;

    private final Gauge minuteLatestMinimumDuration;
    private final Gauge minuteLatestMaximumDuration;
    
    private final Gauge minuteLatest;
    /**
     * Constructor.
     * We will store AppName and Resource Method Name
     */
    public REST_Stats(String aName, String mName) {
        setAppName(aName);
        setMethodName(mName);
        requestCount = new Counter();
        requestCount.setDescription("This shows number of requests to a Restful resource method");
        
        responseTime = new StatisticsMeter();
        responseTime.setDescription("Cumulative Response Time (NanoSeconds) for a Restful resource method");
        responseTime.setUnit("ns");
        
        minutePreviousMinimumDuration = new Gauge();
        minutePreviousMinimumDuration.setDescription("Lowest timed duration in the past complete minute");
        minutePreviousMinimumDuration.setUnit("ns");
        minutePreviousMinimumDuration.setCurrentValue(0);
        
        
        minutePreviousMaximumDuration = new Gauge();
        minutePreviousMaximumDuration.setDescription("The number of minutes since Epoch for the values recorded with minutePreviousMinimumDuration and minutePreviousMaximumDuration");
        minutePreviousMaximumDuration.setUnit("ns");
        minutePreviousMaximumDuration.setCurrentValue(0);

        minutePrevious = new Gauge();
        minutePrevious.setDescription("Highest timed duration in the past complete minute");
        minutePrevious.setUnit("minute");
        minutePrevious.setCurrentValue(0);
        
        minuteLatestMinimumDuration = new Gauge();
        minuteLatestMinimumDuration.setDescription("Lowest timed duration in the past complete minute");
        minuteLatestMinimumDuration.setUnit("ns");
        minuteLatestMinimumDuration.setCurrentValue(0);
        
        
        minuteLatestMaximumDuration = new Gauge();
        minuteLatestMaximumDuration.setDescription("Highest timed duration in the past complete minute");
        minuteLatestMaximumDuration.setUnit("ns");
        minuteLatestMaximumDuration.setCurrentValue(0);
        
        minuteLatest = new Gauge();
        minuteLatest.setDescription("The number of minutes since Epoch for the values recorded with minuteLatestMinimumDuration and minuteLatestMaximumDuration");
        minuteLatest.setUnit("minute");
        minuteLatest.setCurrentValue(0);
    }

    /**
     * Getter for Application Name.
     * */
    @Override
    public String getAppName() {
        return this.appName;
    }

    /**
     * Getter for Description
     **/
    @Override
    public String getDescription() {
        return "Report Stats for specified Restful resource method.";
    }

    /**
     * Demonstrate Number of Requests, received for this resource method.
     * 
     **/
    @Override
    public long getRequestCount() {
        return this.requestCount.getCurrentValue();
    }

    /**
     * Reports Average Response Time.
     * We store Cumulative Time (total service time for specified resource method)
     * responseTime = CummulativeTime / number of requests.
     * 
     * */
    @Override
    public double getResponseTime() {
        return responseTime.getTotal();
    }

    /**
     * Getter for Resource Method Name
     * */
    @Override
    public String getMethodName() {
        return this.methodName;
    }

    /**
     * @param appName the appName to set
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * @param methodName the resource method name to set
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * @param requestCount the requestCount to set
     */
    public void setRequestCount(Counter requestCount) {
        this.requestCount = requestCount;
    }

    /**
     * This will increment request count by i.
     * Typically i would be 1.
     * 
     * @param i
     */
    public void incrementCountBy(int i) {
        this.requestCount.incrementBy(i);
    }

    /**
     * updateRT()
     * 
     * This is responsible for updating Response Time.
     * We already have valid serviceStartTime.
     * Now we should take (current time - serviceStartTime).
     * Add it to cumulative time.
     * 
     * @param elapsed
     * 
     * @param l
     */
    public void updateRT(long elapsed) {
        this.responseTime.addDataPoint(elapsed);
    }

    
    public void updateMinutePreviousMaximumDuration(long max) {
        this.minutePreviousMaximumDuration.setCurrentValue(max);
    }
    
    public void updateMinutePreviousMinimumDuration(long min) {
        this.minutePreviousMinimumDuration.setCurrentValue(min);
    }
    
    public void updateMinutePrevious(long min) {
        this.minutePrevious.setCurrentValue(min);
    }
    
    public void updateMinuteLatestMaximumDuration(long max) {
        this.minuteLatestMaximumDuration.setCurrentValue(max);
    }
    
    public void updateMinuteLatestMinimumDuration(long min) {
        this.minuteLatestMinimumDuration.setCurrentValue(min);
    }
    
    public void updateMinuteLatest(long min) {
        this.minuteLatest.setCurrentValue(min);
    }
    
    /**
     * Method getRequestCountDetails()
     * This is returning the details for requestCount.
     * Type = Counter.
     * Data: count, description, unit, Readings
     **/
    @Override
    public Counter getRequestCountDetails() {
        return this.requestCount;
    }

    /**
     * Method getResponseTimeDetails()
     * This is returning the details for responseTime.
     * Type = StatisticMeter.
     * Data: mean, min, max, description, unit
     **/

    @Override
    public StatisticsMeter getResponseTimeDetails() {
        return this.responseTime;
    }
    
    
    @Override
    public Gauge getMinutePreviousMinimumDuration() {
        return this.minutePreviousMinimumDuration;
    }
    
    @Override
    public Gauge getMinutePreviousMaximumDuration() {
        return this.minutePreviousMaximumDuration;
    }
    
    @Override
    public Gauge getMinuteLatestMinimumDuration() {
        return this.minuteLatestMinimumDuration;
    }
    
    @Override
    public Gauge getMinuteLatestMaximumDuration() {
        return this.minuteLatestMaximumDuration;
    }

	@Override
	public Gauge getMinuteLatest() {
		return this.minuteLatest;
	}

	@Override
	public Gauge getMinutePrevious() {
		return this.minutePrevious;
	}

}
