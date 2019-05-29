/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.EARApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.metrics.impl.MetricRegistryImpl;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

@Component(service = { ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ApplicationListener implements ApplicationStateListener {

    private SharedMetricRegistries sharedMetricRegistry;

    public static Map<String, String> contextRoot_Map = new HashMap<String, String>();

    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        Container appContainer = appInfo.getContainer();

        /*
         * The following try block is to ensure that the application that is starting is actually
         * a deployed application.
         * Will end this method if not.
         */
        try {
            NonPersistentCache cache = appContainer.adapt(NonPersistentCache.class);
            ApplicationClassesContainerInfo applicationClassesContainerInfo = (ApplicationClassesContainerInfo) cache.getFromCache(ApplicationClassesContainerInfo.class);
            if (applicationClassesContainerInfo == null) {
                return;
            }
        } catch (UnableToAdaptException e) {
            return;
        }

        String appName = appInfo.getDeploymentName();
        String contextRoot = null;

        //Goes through each container "entry" in the EAR file.
        if (appInfo instanceof EARApplicationInfo) {
            System.out.println("applicationStarting: EAR");
            for (Entry entry : appContainer) {
                try {
                    Container subContainer = entry.adapt(Container.class);
                    if (subContainer != null) {
                        resolveContextRootFromContainer(subContainer, true, appName);
                    }
                } catch (UnableToAdaptException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("applicationStarting: NOT EAR - WAR?");
            contextRoot = resolveContextRootFromContainer(appContainer, false, appName);
            if (contextRoot != null && appName != null) {
                contextRoot_Map.put(appName, contextRoot);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {}

    /** {@inheritDoc} */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {}

    /** {@inheritDoc} */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        MetricRegistry registry = sharedMetricRegistry.getOrCreate(MetricRegistry.Type.APPLICATION.getName());

        if (MetricRegistryImpl.class.isInstance(registry)) {
            MetricRegistryImpl impl = (MetricRegistryImpl) registry;
            impl.unRegisterApplicationMetrics(appInfo.getDeploymentName());
        }
    }

    @Reference
    public void getSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
        this.sharedMetricRegistry = sharedMetricRegistry;
    }

    /**
     *
     * Adapts a container into a NonPersistentCache to retrieve the WebModuleInfo
     * which will subsequently provide us with the ContextRoot and ApplicationName
     * to be set in the contextRoot_Map. For use by the MetricAppNameConfigSource to configure
     * the config value with the application appropriate contextRoot
     *
     * @param container
     * @return applicationName The application Name
     */
    @FFDCIgnore(UnableToAdaptException.class)
    private String resolveContextRootFromContainer(Container container, boolean isEAR, String applicationName) {

        String contextRoot = null;
        String moduleName = null;
        String appNameToRegister = null;
        NonPersistentCache overlayCache = null;

        try {
            overlayCache = container.adapt(NonPersistentCache.class);
        } catch (UnableToAdaptException e) {
            e.printStackTrace();
        }

        if (overlayCache != null) {
            WebModuleInfo webModuleInfo = (WebModuleInfo) overlayCache.getFromCache(WebModuleInfo.class);
            /*
             * We don't know if the current Container/NonPersistentcache can get us a WebModuleInfo
             * Hence the null check!
             */

            if (webModuleInfo != null) {

                contextRoot = webModuleInfo.getContextRoot();
                moduleName = webModuleInfo.getName();
                appNameToRegister = moduleName;
                if (isEAR && moduleName != null && contextRoot != null) {
                    System.out.println("Adding ear name " + applicationName + "#" + moduleName);
                    appNameToRegister = applicationName + "#" + moduleName;
                    //contextRoot_Map.put(applicationName + "#" + moduleName, contextRoot);
                }
                System.out.println("ApplicatoinListener.resolveContextRoot register: " + appNameToRegister + " with " + contextRoot);
                contextRoot_Map.put(appNameToRegister, contextRoot);
            }
            /*
             * For instances where this is a Jar inside a EAR file (i.e ejb jar)?
             */
            else {
                ModuleInfo moduleInfo = (ModuleInfo) overlayCache.getFromCache(ModuleInfo.class);
                if (moduleInfo != null) {
                    moduleName = moduleInfo.getName();
                    appNameToRegister = moduleName;
                    System.out.println("ApplicatoinListener.resolveContextRoot normalodule: " + moduleInfo.getName());
                    if (isEAR && moduleName != null) {
                        System.out.println("Adding ear name " + applicationName + "#" + moduleName);
                        appNameToRegister = applicationName + "#" + moduleName;
                        //contextRoot_Map.put(applicationName + "#" + moduleName, contextRoot);
                    }
                    contextRoot_Map.put(appNameToRegister, contextRoot);
                }
            }
        }
        return contextRoot;
    }
}
