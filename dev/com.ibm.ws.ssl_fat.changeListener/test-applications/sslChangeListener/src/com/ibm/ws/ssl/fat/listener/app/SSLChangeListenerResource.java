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
package com.ibm.ws.ssl.fat.listener.app;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.enterprise.context.ApplicationScoped;

import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLConfigChangeEvent;
import com.ibm.websphere.ssl.SSLConfigChangeListener;
import com.ibm.websphere.ssl.SSLException;

/**
 * REST resource for testing SSL configuration change notifications.
 * This resource allows registration of SSLConfigChangeListeners that log
 * when SSL configuration changes occur.
 */
@ApplicationScoped
@Path("/sslChangeListener")
public class SSLChangeListenerResource {

    private static final Logger LOG = Logger.getLogger(SSLChangeListenerResource.class.getName());
    
    // Track registered listeners for cleanup
    private static final Map<String, SSLConfigChangeListener> listeners = new HashMap<>();

    /**
     * Register an SSLConfigChangeListener with a direct alias.
     * 
     * @param alias The SSL configuration alias to monitor
     * @return Response indicating success or failure
     */
    @GET
    @Path("/registerWithAlias/{alias}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response registerWithAlias(@PathParam("alias") String alias) {
        LOG.info("Registering SSLConfigChangeListener with alias: " + alias);
        
        try {
            String listenerId = "alias_" + alias;
            
            // Create listener that logs when notified
            SSLConfigChangeListener listener = new SSLConfigChangeListener() {
                @Override
                public void stateChanged(SSLConfigChangeEvent e) {
                    LOG.info("SSLConfigChangeListener notification received for alias: " + alias + 
                             ", event alias: " + e.getAlias() + 
                             ", selection type: " + e.getSelectionType());
                }
            };
            
            // Register the listener
            JSSEHelper.getInstance().registerSSLConfigChangeListener(alias, null, listener);
            
            // Store listener for cleanup
            listeners.put(listenerId, listener);
            
            return Response.ok("Registered listener for alias: " + alias).build();
            
        } catch (SSLException e) {
            LOG.severe("Failed to register SSLConfigChangeListener: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity("Failed to register listener: " + e.getMessage())
                          .build();
        }
    }

    /**
     * Register an SSLConfigChangeListener using connectionInfo map with outbound host.
     * 
     * @param host The outbound host to match in SSL configuration
     * @return Response indicating success or failure
     */
    @GET
    @Path("/registerWithConnectionInfo")
    @Produces(MediaType.TEXT_PLAIN)
    public Response registerWithConnectionInfo(@QueryParam("host") String host) {
        LOG.info("Registering SSLConfigChangeListener with connectionInfo host: " + host);
        
        try {
            String listenerId = "connInfo_" + host;
            
            // Create connectionInfo map
            Map<String, Object> connectionInfo = new HashMap<>();
            connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
            connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, host);
            
            // Create listener that logs when notified
            SSLConfigChangeListener listener = new SSLConfigChangeListener() {
                @Override
                public void stateChanged(SSLConfigChangeEvent e) {
                    LOG.info("SSLConfigChangeListener notification received for host: " + host + 
                             ", event alias: " + e.getAlias() + 
                             ", selection type: " + e.getSelectionType());
                }
            };
            
            // Register the listener with null alias and connectionInfo
            JSSEHelper.getInstance().registerSSLConfigChangeListener(null, connectionInfo, listener);
            
            // Store listener for cleanup
            listeners.put(listenerId, listener);
            
            return Response.ok("Registered listener for host: " + host).build();
            
        } catch (SSLException e) {
            LOG.severe("Failed to register SSLConfigChangeListener: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity("Failed to register listener: " + e.getMessage())
                          .build();
        }
    }

    /**
     * Deregister a listener.
     * 
     * @param listenerId The listener ID to deregister
     * @return Response indicating success or failure
     */
    @GET
    @Path("/deregister/{listenerId}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response deregister(@PathParam("listenerId") String listenerId) {
        SSLConfigChangeListener listener = listeners.remove(listenerId);
        if (listener == null) {
            return Response.status(Response.Status.NOT_FOUND)
                          .entity("Listener not found: " + listenerId)
                          .build();
        }
        
        try {
            JSSEHelper.getInstance().deregisterSSLConfigChangeListener(listener);
            LOG.info("Deregistered listener: " + listenerId);
            return Response.ok("Deregistered listener: " + listenerId).build();
        } catch (SSLException e) {
            LOG.severe("Failed to deregister listener: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity("Failed to deregister listener: " + e.getMessage())
                          .build();
        }
    }

    /**
     * Reset all listeners (for test cleanup).
     * 
     * @return Response indicating success
     */
    @GET
    @Path("/reset")
    @Produces(MediaType.TEXT_PLAIN)
    public Response reset() {
        for (Map.Entry<String, SSLConfigChangeListener> entry : listeners.entrySet()) {
            try {
                JSSEHelper.getInstance().deregisterSSLConfigChangeListener(entry.getValue());
            } catch (SSLException e) {
                LOG.warning("Failed to deregister listener during reset: " + e.getMessage());
            }
        }
        listeners.clear();
        LOG.info("Reset all listeners");
        return Response.ok("All listeners reset").build();
    }
}

// Made with Bob
