/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.internal.ssl;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.ws.ssl.internal.LibertyConstants;

@Component(configurationPid = "com.ibm.ws.ssl.repertoire", configurationPolicy = ConfigurationPolicy.REQUIRE, property = "service.vendor=IBM")
public class TelemetrySSLConfigRepertoireTracker {

    private String id = "unknownConfig";

    private String servicePid = "unknownConfig";

    @Activate
    protected void activate(Map<String, Object> properties) {
        id = (String) properties.get(LibertyConstants.KEY_ID);
        servicePid = (String) properties.get("service.pid");

        //System.out.println("My personal repertoire: CREATED = " + id + ":" + servicePid + " |  " + properties);
    }

    @Modified
    protected void modified(Map<String, Object> properties) {
        //System.out.println("My personal repertoire: MODIFIED = " + properties);
    }

    @Deactivate
    protected void deactivate(int reason) {
        // System.out.println("My personal repertoire: REMOVED = " + reason);
    }
}
