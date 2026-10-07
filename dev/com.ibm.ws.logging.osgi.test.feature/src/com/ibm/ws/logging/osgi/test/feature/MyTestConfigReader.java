/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.osgi.test.feature;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.logging.WsLogHandler;
import com.ibm.ws.logging.osgi.MessageRouterConfigListener;

@Component(service = { MyTestConfigReader.class },
           configurationPid = "com.ibm.ws.logging.osgi.test.feature",
           configurationPolicy = ConfigurationPolicy.OPTIONAL,
           immediate = true,
           property = { "service.vendor=IBM" })
public class MyTestConfigReader {

    private volatile MessageRouterConfigListener msgRouterConfigListener;

    private ServiceRegistration<WsLogHandler> handlerRegistration;

    @Reference(policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL)
    protected void setMessageRouterConfigListener(MessageRouterConfigListener listener) {
        this.msgRouterConfigListener = listener;
    }

    protected void unsetMessageRouterConfigListener(MessageRouterConfigListener listener) {
        this.msgRouterConfigListener = null;
    }

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        String routerThing = (String) properties.get("routerThing");
        System.out.println("[loggingTest] activate - porps: " + properties);

        // 1. Subscribe the handler ID to the desired message ID patterns.
        //    The routing table entry is established before the service is registered
        //    so the handler is wired correctly the moment it appears in the registry.
        updateRouter(routerThing);

        // 2. Register TestWsLogHandler as a WsLogHandler OSGi service.
        //    MessageRouterConfigurator's wsLogHandlerListener picks this up and calls
        //    WsMessageRouterImpl.setWsLogHandler(HANDLER_ID, handler).
        Dictionary<String, Object> handlerProps = new Hashtable<String, Object>();
        handlerProps.put("id", TestWsLogHandler.HANDLER_ID);
        handlerProps.put("service.vendor", "IBM");
        handlerRegistration = context.getBundleContext().registerService(
                WsLogHandler.class, new TestWsLogHandler(), handlerProps);
    }

    @Modified
    protected void modified(ComponentContext context, Map<String, Object> properties) {
        String routerThing = (String) properties.get("routerThing");
        System.out.println("[loggingTest] modified - routerThing: " + routerThing);

        // Handler is already registered — only the routing subscription needs updating.
        updateRouter(routerThing);
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
        System.out.println("[loggingTest] deactivate");
        if (handlerRegistration != null) {
            handlerRegistration.unregister();
            handlerRegistration = null;
        }
    }

    private void updateRouter(String routerThing) {
        MessageRouterConfigListener listener = msgRouterConfigListener;
        if (listener != null && routerThing != null && !routerThing.trim().isEmpty()) {
            listener.updateMessageListForHandler(routerThing, TestWsLogHandler.HANDLER_ID);
        }
    }

}
