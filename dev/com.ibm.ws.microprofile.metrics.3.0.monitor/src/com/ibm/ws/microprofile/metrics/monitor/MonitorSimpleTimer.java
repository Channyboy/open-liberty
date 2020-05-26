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
package com.ibm.ws.microprofile.metrics.monitor;

import java.time.Duration;
import java.util.HashSet;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.microprofile.metrics23.impl.SimpleTimerImpl;

public class MonitorSimpleTimer extends SimpleTimerImpl {
	
	private static final TraceComponent tc = Tr.register(MonitorSimpleTimer.class);
	
	MBeanServer mbs;
    String objectName, counterAttribute, counterSubAttribute, gaugeAttribute, gaugeSubAttribute;
    long time;
    boolean isComposite = false;
    
    HashSet<Long> usedTimes = new HashSet<Long>();

    private long cachedMBPreviousMinute_max = 0L;
    private long cachedMBPreviousMinute_min = 0L;
    private long cachedMBCurrentMinute_max = 0L;
    private long cachedMBCurrentMinute_min = 0L;
    
    private long displayMaxCurrent_forThisMinute = 0L;
    private long displayMinCurrent_forThisMinute = 0L;
    private long displayMaxCurrent_forThisMinute_val = 0L;
    private long displayMinCurrent_forThisMinute_val = 0L;
    
    
    private long displayMaxPrev_forThisMinute = 0L;
    private long displayMinPrev_forThisMinute = 0L;
    
    private long mbean_current_min;
    private long mbean_current_max;
    private long mbean_currentMinute;
    
    private long mbean_previous_min;
    private long mbean_prev_max;
    private long mbean_prevMinute;
    
    private long rollingBaseMinute = 0L;
    
    public MonitorSimpleTimer(MBeanServer mbs, String objectName, String counterAttribute,
    		String counterSubAttribute, String gaugeAttribute, String gaugeSubAttribute) {
    	this.mbs = mbs;
        this.objectName = objectName;
        this.counterAttribute = counterAttribute;
        this.counterSubAttribute = counterSubAttribute;
        this.gaugeAttribute = gaugeAttribute;
        this.gaugeSubAttribute = gaugeSubAttribute;
       
    }

    @Override
    public long getCount() {
        try {
        	if (counterSubAttribute != null) {
                CompositeData value = (CompositeData) mbs.getAttribute(new ObjectName(objectName), counterAttribute);
                Number numValue = (Number) value.get(counterSubAttribute);       
                return numValue.longValue();
        	} else {
                Number value = (Number) mbs.getAttribute(new ObjectName(objectName), counterAttribute);
                return value.longValue();        		
        	}
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getCount exception message: ", e.getMessage());
                FFDCFilter.processException(e, getClass().getSimpleName(), "getCount:Exception");
            }
        }
        return 0;
    }
    
    @Override
    public Duration getElapsedTime() {
        try {   
        	if (gaugeSubAttribute != null) {
                CompositeData value = (CompositeData) mbs.getAttribute(new ObjectName(objectName), gaugeAttribute);
                Number numValue = (Number) value.get(gaugeSubAttribute);
                return Duration.ofNanos(numValue.longValue());
        	} else {
        		 Number numValue = (Number) mbs.getAttribute(new ObjectName(objectName), gaugeAttribute);
                 return Duration.ofNanos(numValue.longValue());
        	}

        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getElapsedTime exception message: ", e.getMessage());
                FFDCFilter.processException(e, getClass().getSimpleName(), "getElapsedTime:Exception");
            }
        }
        return Duration.ZERO;
    }
    

    @Override
    public synchronized Duration getMaxTimeDuration() {
    	    	
    	getMinMaxValues();
    	long currentMinute = getCurrentMinuteFromSystem();
    	
    	//If there exists no 'Previous' data AND the current minute DOES NOT match the latest minute AND if haven't already used this value
    	if ((cachedMBPreviousMinute_max != mbean_prevMinute || displayMaxPrev_forThisMinute == currentMinute) //NOT stale unless still displaying
    			&& currentMinute != mbean_prevMinute
    			&& mbean_prevMinute != 0
    			&& (mbean_prevMinute > rollingBaseMinute || (currentMinute == displayMaxCurrent_forThisMinute && mbean_prev_max == displayMaxCurrent_forThisMinute_val))) //Because we may have used a "current" value before... and that value may now be an "old" value.
    	{
    		cachedMBPreviousMinute_max = mbean_prevMinute;
    		displayMaxPrev_forThisMinute = currentMinute;
    		return Duration.ofNanos(mbean_prev_max);
    	}
    	
		/*
		 * This logic is to display the current mbean value only under a special
		 * circumstance. That is if the mbean has not been updated in awhile (i.e
		 * current minutes don't match anymore). Due to the nature of interaction with
		 * the Mbean (in which the Mbean is only updated if a REST request occurs) we do
		 * not know if the Mbean will ever be updated again. Since we want to have as up
		 * to date information as possible, we will pro-actively retrieve the current
		 * value and interpret it as the "previous" value and display it. Note that at
		 * this point in time the mbean's ACTUAL previous value would have already been
		 * displayed/retrieved before by the monitoring tool which would run at constant
		 * intervals.
		 * 
		 * First check to ensure we're not displaying the mbean's current max value
		 * again UNLESS of course we're still within the current/on-going complete
		 * minute (i.e The value will be displayed for a full minute 12:00:00 to
		 * 12:00:59)
		 * 
		 * Second check to ensure that the actual current minute is not a match with the
		 * mbean's current minute. We are suppose to display the mbean's current value
		 * ONLY if the value is stale (i.e the mbean's current minute is x-minutes ago)
		 * 
		 */
    	if ((cachedMBCurrentMinute_max != mbean_currentMinute || displayMaxCurrent_forThisMinute == currentMinute) //NOT stale unless still displaying
    			&& currentMinute != mbean_currentMinute)
    	{
    		//This value is used for FIRST check in the IF statement - to determine if we're redisplaying current values
    		cachedMBCurrentMinute_max = mbean_currentMinute;
    		
    		/*
    		 * This value is for FIRST check in the IF statement - to determine if we're still ongoingly displaying current value
    		 * 
    		 * This value is also used to see if we want to display a mbean's previous value.
    		 * 
    		 * It could be the case that after displaying the stale current minute, the MBean is updated
    		 * again. We do not want to display this  value again.
    		 */
    		displayMaxCurrent_forThisMinute = currentMinute;
    		
    		/*
    		 * This value is used in a check to see if we want to display a mbean's previous value.
    		 * 
    		 * It could be the case that after displaying the stale current minute, the MBean is updated
    		 * again. We do not want to display this  value again.
    		 */
    		displayMaxCurrent_forThisMinute_val = mbean_current_max;
    		
			/*
			 * Need to update a rolling window
			 * 
			 * Otherwise, depending on the scenario can erroneously display an mbeans
			 * "PREVIOUS" value again after displaying a stale mbean's "CURRENT" value
			 */
    		rollingBaseMinute = currentMinute;
    		return Duration.ofNanos(mbean_current_max);
    	}
    	
		return null;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Duration getMinTimeDuration() {
    	 	
    	
    	getMinMaxValues();
    	long currentMinute = getCurrentMinuteFromSystem();
    	
    	
    	/*
    	 * For every invocation of this block:
    	 * cachedMinuteOld is set to the current minute
    	 * displayMinOld_thisMinute is set to the current minute - indicates that for this current minute, the OLD value from the mbean is too be displayed
    	 * 
    	 * If cahed value and old minute value is not the same (i.e stale) AND we're not still displaying
    	 * AND currentMinute doesn't equal the recorded OLD minute
    	 * AND there is a vlue (minuteOld != 0)
    	 * AND the previous minute is greater than our rolling window UNLESS we were displaying a CURRENT/NEW value when it was bumped into the PREVIOUS mbean value) 
    	 */
    	
    	if ((cachedMBPreviousMinute_min != mbean_prevMinute || displayMinPrev_forThisMinute == currentMinute) //NOT stale unless still displaying
    			&& currentMinute != mbean_prevMinute
    			&& mbean_prevMinute != 0
    			&& ( mbean_prevMinute > rollingBaseMinute || (currentMinute == displayMinCurrent_forThisMinute && displayMinCurrent_forThisMinute_val == mbean_previous_min))) //Because we may have used a "current" value before... and that value may now be an "old" value.
    	{
    		cachedMBPreviousMinute_min = mbean_prevMinute;
    		displayMinPrev_forThisMinute = currentMinute;
    		return Duration.ofNanos(mbean_previous_min);
    	}
    	
		/*
		 * This logic is to display the current mbean value only under a special
		 * circumstance. That is if the mbean has not been updated in awhile (i.e
		 * current minutes don't match anymore). Due to the nature of interaction with
		 * the Mbean (in which the Mbean is only updated if a REST request occurs) we do
		 * not know if the Mbean will ever be updated again. Since we want to have as up
		 * to date information as possible, we will pro-actively retrieve the current
		 * value and interpret it as the "previous" value and display it. Note that at
		 * this point in time the mbean's ACTUAL previous value would have already been
		 * displayed/retrieved before by the monitoring tool which would run at constant
		 * intervals.
		 * 
		 * First check to ensure we're not displaying the mbean's current max value
		 * again UNLESS of course we're still within the current/on-going complete
		 * minute (i.e The value will be displayed for a full minute 12:00:00 to
		 * 12:00:59)
		 * 
		 * Second check to ensure that the actual current minute is not a match with the
		 * mbean's current minute. We are suppose to display the mbean's current value
		 * ONLY if the value is stale (i.e the mbean's current minute is x-minutes ago)
		 * 
		 */
    	if ((cachedMBCurrentMinute_min != mbean_currentMinute || displayMinCurrent_forThisMinute == currentMinute) //NOT stale unless still displaying
    			&& currentMinute != mbean_currentMinute)
    	{
    		cachedMBCurrentMinute_min = mbean_currentMinute;
    		displayMinCurrent_forThisMinute = currentMinute;
    		displayMinCurrent_forThisMinute_val = mbean_current_min;
    		rollingBaseMinute = currentMinute;
    		return Duration.ofNanos(mbean_current_min);
    	}
    	
		return null;
    }

	private void getMinMaxValues() {
		try {
			synchronized(this) {
				
				
				CompositeData value= (CompositeData) mbs.getAttribute(new ObjectName(objectName),"MinuteLatestMinimumDuration");
				mbean_current_min = ((Number) value.get("currentValue")).longValue();
				 
				value = (CompositeData) mbs.getAttribute(new ObjectName(objectName), "MinuteLatestMaximumDuration");
				 mbean_current_max = ((Number) value.get("currentValue")).longValue();
				 
				 value = (CompositeData) mbs.getAttribute(new ObjectName(objectName),"MinuteLatest");
				 mbean_currentMinute = ((Number) value.get("currentValue")).longValue();
				 
				 value = (CompositeData) mbs.getAttribute(new ObjectName(objectName),"MinutePreviousMinimumDuration");
				 mbean_previous_min = ((Number) value.get("currentValue")).longValue();
				 value = (CompositeData) mbs.getAttribute(new ObjectName(objectName),"MinutePreviousMaximumDuration");
				 mbean_prev_max = ((Number) value.get("currentValue")).longValue();
				 
				 value = (CompositeData) mbs.getAttribute(new ObjectName(objectName),"MinutePrevious");
				 mbean_prevMinute  = ((Number) value.get("currentValue")).longValue();
			}

		} catch (Exception e) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "getMinMaxValues exception message: ", e.getMessage());
				FFDCFilter.processException(e, getClass().getSimpleName(), "getMinMaxValues:Exception");
			}
		}
	}
    
    // Get the current system time in minutes, truncating. This number will increase by 1 every complete minute.
    private long getCurrentMinuteFromSystem() {
        return System.currentTimeMillis() / 60000;
    }
}
