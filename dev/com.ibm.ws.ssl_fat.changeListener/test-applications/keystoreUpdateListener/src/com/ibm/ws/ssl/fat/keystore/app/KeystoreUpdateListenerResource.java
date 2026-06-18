/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.ssl.fat.keystore.app;

import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.enterprise.context.ApplicationScoped;

import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLConfigChangeEvent;
import com.ibm.websphere.ssl.SSLConfigChangeListener;

/**
 * REST resource for registering SSLConfigChangeListener to test keystore file updates.
 */
@ApplicationScoped
@Path("/keystoreUpdateListener")
public class KeystoreUpdateListenerResource {

    private static final Logger LOG = Logger.getLogger(KeystoreUpdateListenerResource.class.getName());
    private static SSLConfigChangeListener listener;
    private static String listenerId;

    /**
     * Register a listener for the specified SSL config alias.
     */
    @GET
    @Path("/register/{alias}")
    @Produces(MediaType.TEXT_PLAIN)
    public String registerListener(@PathParam("alias") String alias) {
        try {
            listenerId = "keystore_" + alias;
            
            listener = new SSLConfigChangeListener() {
                @Override
                public void stateChanged(SSLConfigChangeEvent event) {
                    LOG.info("KeystoreUpdateListener notification received for alias: " + event.getAlias() 
                           + ", state: " + event.getState());
                }
            };

            JSSEHelper jsseHelper = JSSEHelper.getInstance();
            jsseHelper.registerSSLConfigChangeListener(alias, null, listener);
            
            LOG.info("Registered KeystoreUpdateListener for alias: " + alias);
            return "Registered listener with ID: " + listenerId;
        } catch (Exception e) {
            LOG.severe("Failed to register listener: " + e.getMessage());
            e.printStackTrace();
            return "Failed: " + e.getMessage();
        }
    }

    /**
     * Deregister the listener.
     */
    @GET
    @Path("/deregister")
    @Produces(MediaType.TEXT_PLAIN)
    public String deregisterListener() {
        try {
            if (listener != null) {
                JSSEHelper jsseHelper = JSSEHelper.getInstance();
                jsseHelper.deregisterSSLConfigChangeListener(listener);
                LOG.info("Deregistered KeystoreUpdateListener with ID: " + listenerId);
                listener = null;
                listenerId = null;
                return "Deregistered listener";
            }
            return "No listener registered";
        } catch (Exception e) {
            LOG.severe("Failed to deregister listener: " + e.getMessage());
            e.printStackTrace();
            return "Failed: " + e.getMessage();
        }
    }
}

// Made with Bob
