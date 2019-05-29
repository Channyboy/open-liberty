/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.microprofile.metrics.ApplicationListener;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

@Component(service = { ConfigSource.class }, configurationPid = "com.ibm.ws.microprofile.metrics.config", configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true, property = { "service.vendor=IBM" })
public class MetricAppNameConfigSource implements ConfigSource {

    private static final String METRICS_APPNAME_CONFIG_KEY = "mp.metrics.appName";

    private static final int CONFIG_ORDINAL = 80;

    private static Map<String, String> applicationContextRootMap = new HashMap<String, String>();

    @Override
    public int getOrdinal() {
        return CONFIG_ORDINAL;
    }

    @Override
    public String getName() {
        return "Metric Instrumented Application's Name";
    }

    @Override
    public Set<String> getPropertyNames() {
        return applicationContextRootMap.keySet();
    }

    @Override
    public Map<String, String> getProperties() {
        return applicationContextRootMap;
    }

    @Override
    public String getValue(String propertyName) {
        if (propertyName.equals(METRICS_APPNAME_CONFIG_KEY)) {

            String appName = null;
            String contextRoot = null;
            appName = resolveApplicationName();
            contextRoot = ApplicationListener.contextRoot_Map.get(appName);

            //perhaps its a WAR in a EAR?
            if (contextRoot == null) {
                String moduleName = resolveModuleName();
                contextRoot = ApplicationListener.contextRoot_Map.get(appName + "#" + moduleName);
            }
            if (contextRoot != null) {
                return contextRoot;
            }
        }
        return null;
    }

    private String resolveApplicationName() {
        String appName = null;
        try {
            ComponentMetaDataAccessorImpl cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
            appName = cmdai.getComponentMetaData().getModuleMetaData().getJ2EEName().getApplication();
        } catch (NullPointerException e) {
        } catch (Exception e) {
        }
        return appName;
    }

    private String resolveModuleName() {
        String moduleName = null;
        try {
            ComponentMetaDataAccessorImpl cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
            moduleName = cmdai.getComponentMetaData().getModuleMetaData().getName();
        } catch (NullPointerException e) {
        } catch (Exception e) {
        }
        return moduleName;
    }

    //prolly a webmodulemetadata
    //glen marcy - modulemetadata to webmoduleinfo
    //also get app name#modulemetadata combine appname + module name
}
