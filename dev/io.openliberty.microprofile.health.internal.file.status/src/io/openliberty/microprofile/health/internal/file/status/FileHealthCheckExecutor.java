/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health.internal.file.status;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.health.internal.common.HealthCheckConstants;

/**
 *
 */
@Component(service = {
        FileHealthCheckExecutor.class}, configurationPid = "io.openliberty.microprofile.health", configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true, property = {
                "service.vendor=IBM"})
public class FileHealthCheckExecutor {

    private int prevUpdateIntervalValue = 0;

    private static final TraceComponent tc = Tr
            .register(FileHealthCheckExecutor.class);

    private Timer mainTimer = null;
    private TimerTask fileHealthStatusTimerTask = null;

    @Activate
    protected void activate(ComponentContext context,
            Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {

            Tr.debug(tc, "Activate MP Health COnfig", properties);
        }
        processConfig(context, properties);
    }

    @Modified
    protected void modified(ComponentContext context,
            Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.debug(tc, "Modified MP Health Config", properties);
        }
        processConfig(context, properties);
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.debug(tc, "Deactivate HalthConfig with reason=" + reason);
        }
        stopTimers();
    }

    private void stopTimers() {
        if (fileHealthStatusTimerTask != null) {
            fileHealthStatusTimerTask.cancel();
            fileHealthStatusTimerTask = null;
        }

        if (mainTimer != null) {
            mainTimer.cancel();
            mainTimer = null;
        }

    }

    private void processConfig(ComponentContext context,
            Map<String, Object> properties) {

        Boolean isEnableFile = false;
        Integer periodFileSeconds = 10; // default
        Boolean isPeriodFileSecondsConfigured = false;

        /*
         * Parsing logic
         */
        String configEnableFileObj = (String) properties
                .get(HealthCheckConstants.CONFIG_ATTRIBUTE_ENABLE_FILE);

        String configFilePeriodSecondsObj = (String) properties
                .get(HealthCheckConstants.CONFIG_ATTRIBUTE_FILE_PERIOD_SECONDS);

        if (configEnableFileObj != null) {
            isEnableFile = Boolean.parseBoolean(configEnableFileObj);
        }

        if (configFilePeriodSecondsObj != null) {
            isPeriodFileSecondsConfigured = true;
            try {
                periodFileSeconds = Integer.parseInt(configEnableFileObj);
            } catch (Exception e) {
                // will probably throw FFDC if exception caught (i.e.,
                // NumberFormatException)

                // TODO: warning? something?
            }

        } else {
            isPeriodFileSecondsConfigured = false;
            // default it to 10 seconds as there is no configured file
            periodFileSeconds = 10;
        }

        /*
         * Toggle logic
         */

        if (isEnableFile) {
            if (mainTimer == null) {
                mainTimer = new Timer(true);
            }

            if (prevUpdateIntervalValue != periodFileSeconds) {
                if (fileHealthStatusTimerTask != null) {
                    fileHealthStatusTimerTask.cancel();
                }
                prevUpdateIntervalValue = periodFileSeconds;
                fileHealthStatusTimerTask = new FileHealthStatusTimerTask();
            }

            mainTimer.schedule(fileHealthStatusTimerTask, 0, periodFileSeconds);

        } else if (!isEnableFile) {

            if (isPeriodFileSecondsConfigured) {
                // TODO: throw warning that periodFIleSEconds configured w/o
                // file
                // enablement
            }

            stopTimers();
        }

    }

    class FileHealthStatusTimerTask extends TimerTask {

        @Override
        public void run() {
            // TODO - stuff with executor.
        }

    }

}
