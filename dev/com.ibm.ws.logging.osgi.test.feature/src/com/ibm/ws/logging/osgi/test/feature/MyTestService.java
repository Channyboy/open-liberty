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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.logging.WsLogHandler;
import com.ibm.ws.logging.osgi.MessageRouterConfigListener;

/**
 * Test DS component that activates on bundle startup, registers a TestWsLogHandler
 * as an OSGi WsLogHandler service, and subscribes it to message IDs "ABCD*" and "ABCC"
 * via the MessageRouterConfigListener service.
 */
@Component(immediate = true, property = { "service.vendor=IBM" })
public class MyTestService {

    private static final TraceComponent tc = Tr.register(MyTestService.class);

    
    // ABCDEE000I
    //"ABCD*,ABCC,CWWKF0011I";
    // "ABCDEE000I, ABCDEE023I, ABCDEE045I, BCDAEE410W, BCGAEE410A"
    //private static final String MSG_IDS = "ABCD*,ABCC,CWWKF0011I";




    private static final String MSG_IDS_SET2 = "ABCDEE111I,ABCDEE222I,ABCDEE333W,ABCDEE444E,ABCDEE045I,BCDAEE410W,BCGAEE410A,DCCDEE045I,DCCDEE046I,DCCDEE046I,DCCDEE048I,JCCDEE045I";
    
    private static final String MSG_IDS_DEBUG = "ABCDEE333W";
    
    //private static final String MSG_IDS = MSG_IDS_DEBUG;
    
    
    //private static final String MSG_IDS = System.getProperty("output.msg.ids");
    
    //private static final String HANDLER_SUB_IDS = "ABCDE*I,BCG*,DCCDEE048I";
    
    //private static final String HANDLER_SUB_IDS = System.getProperty("handler.sub.ids");
    
   // private static final String[] ARR_MSG_DS = MSG_IDS.split(",");
    
    private static boolean isFirstCall = true;

    @Reference
    private MessageRouterConfigListener configListener;

    private ServiceRegistration<WsLogHandler> handlerRegistration;

    @Activate
    protected void activate(BundleContext context) {
    	
    	
//    	String sMessageLoops = System.getProperty("loopTimes");
//    	int messageLoops = (sMessageLoops.trim() == null) ? 10000 : Integer.valueOf(sMessageLoops.trim());
//    	
//    	
//    	
//    	String sInvokeTimes = System.getProperty("invokeTimes");
//    	int invokeTimes = (sInvokeTimes.trim() == null) ? 1 : Integer.valueOf(sInvokeTimes.trim());
//    	
//    	//printout
//    	System.out.println(String.format("InvokeTimes:[%d] ----- loopTimes[%d]",invokeTimes, messageLoops));
//    	
//    	
//    	System.out.println("DId i get a ref for configListener" + configListener);
//        // Register the TestWsLogHandler as a WsLogHandler OSGi service.
//        // MessageRouterConfigurator's ServiceListener will pick this up automatically
//        // and call msgRouter.setWsLogHandler(HANDLER_ID, handler).
//        Dictionary<String, Object> props = new Hashtable<String, Object>();
//        props.put("id", TestWsLogHandler.HANDLER_ID);
//        props.put("service.vendor", "IBM");
//        handlerRegistration = context.registerService(WsLogHandler.class, new TestWsLogHandler(), props);
//
//        // Subscribe the handler to the desired message IDs via the config listener.
//        // This calls MessageRouterConfigurator.updateMessageListForHandler, which in turn
//        // calls msgRouter.addMsgToLogHandler for each ID.
//        configListener.updateMessageListForHandler(HANDLER_SUB_IDS, TestWsLogHandler.HANDLER_ID);
//
//        
//        List<Long> times = new ArrayList<Long>();
//        
//        while (invokeTimes > 0 )
//        {
//        	messageTester(messageLoops, times);
//        	invokeTimes--;
//        }
//        
//       double avg = calcAvg(times);
//        
//       System.out.println(String.format("The average time is [%f] from [%d] runs with [%d] loops", avg, invokeTimes, messageLoops) );
//       
    }

    
    private double  calcAvg(List<Long> times) {
    	long sum = 0;
    	for (long l : times) {
    		sum += l; 
    	}
    	
    	double avg = (double) (sum / times.size());
    	
    	return avg;
    }
    
    private void messageTester(int loopTimes, List<Long> times ) {
    	
    	//Only wait 5 seconds first time.
    	if (isFirstCall) {
    	       try {
    	            Thread.sleep(5000);
    	        } catch (InterruptedException e) {
    	            Thread.currentThread().interrupt();
    	        }
    	       isFirstCall = false;
    	}
 

        long startMs = System.currentTimeMillis();
        System.out.println("messageTester start time (ms from epoch): " + startMs);

        Random a = new Random();
        
        
//        for (int i = 0; i < loopTimes; i++) {
//        	String msgis =  ARR_MSG_DS[a.nextInt(ARR_MSG_DS.length)];	
//            Tr.info(tc, msgis + " : hello hello");
//        }

        long endMs = System.currentTimeMillis();
        long diff = endMs - startMs;
        times.add(diff);
        System.out.println("messageTester elapsed time (ms): " + diff);
    }

    @Deactivate
    protected void deactivate() {
        if (handlerRegistration != null) {
            handlerRegistration.unregister();
            handlerRegistration = null;
        }
    }

}
