/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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
package com.ibm.ws.logging.internal.osgi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import test.TestConstants;
import test.common.SharedOutputManager;

import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsLogHandler;
import com.ibm.wsspi.logging.LogHandler;

/**
 * Test the MessageRouterImpl.
 */
@RunWith(JMock.class)
public class WsMessageRouterImplTest extends MessageRouterImplTest {

    public static SharedOutputManager outputMgr = SharedOutputManager.getInstance()
            .logTo(TestConstants.BUILD_TMP)
            .trace("*=all");
    // Named differently from the parent's outputRule so JUnit does not apply the
    // same singleton rule twice (which would break captureStreams()).
    @Rule
    public TestRule outputRule = outputMgr;

    /**
     * Mockery environment for LogHandlers.
     */
    protected static Mockery mockery = null;

    /**
     * Counter for generating unique mock object names.
     */
    protected static int uniqueMockNameCount = 1;

    /**
     * WsLogHandler mock object.
     */
    protected WsLogHandler mockWsLogHandler = null;

    /**
     * Another WsLogHandler mock object.
     */
    protected WsLogHandler mockWsLogHandler1 = null;

    /**
     * Yet another WsLogHandler mock object.
     */
    protected WsLogHandler mockWsLogHandler2 = null;

    /**
     * Create a new mockery environment and MessageRouterImpl. Should be called
     * by every test method to ensure a fresh isolated test environment.
     */
    @Override
    protected MessageRouterImpl getMessageRouterImpl() {
        super.getMessageRouterImpl(); // setup mock handlers in the superclass.

        mockery = new JUnit4Mockery();

        mockWsLogHandler = mockery.mock(WsLogHandler.class, "WsLogHandler" + uniqueMockNameCount++);
        mockWsLogHandler1 = mockery.mock(WsLogHandler.class, "WsLogHandler" + uniqueMockNameCount++);
        mockWsLogHandler2 = mockery.mock(WsLogHandler.class, "WsLogHandler" + uniqueMockNameCount++);

        return new WsMessageRouterImpl();
    }

    /**
     * 
     */
    protected WsMessageRouterImpl getWsMessageRouterImpl() {
        return (WsMessageRouterImpl) getMessageRouterImpl();
    }

    /**
     * Helper method for setting expectations on the WsLogHandler.
     */
    protected void setupWsLogHandlerExpectations(final WsLogHandler mockWsLogHandler, final RoutedMessage routedMessage) {
        mockery.checking(new Expectations() {
            {
                oneOf(mockWsLogHandler).publish(with(equal(routedMessage)),with(false));
            }
        });
    }

    @Test
    public void testNullWsLogHandlerId() {
        // No NullPointerException if the id passed is null (non-existent service property)
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler(null, null);
        msgRouter.unsetWsLogHandler(null, null);
    }

    @Test
    public void testNullWsLogHandlerRef() {
        // No NullPointerException if the ref passed is null (result of bad getService)
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("id", null);
        msgRouter.unsetWsLogHandler("id", null);
    }

    /**
     * Test setting the WsLogHandler on MessageRouterImpl after MessageRouterImpl.modified
     * has been called (i.e. after the properties have been parsed).
     */
    @Test
    public void setWsLogHandlerAfterModified() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Set WsLogHandler *after* modified().
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue("msg route should return true",msgRouter.route(msg,false));
    }

    /**
     * Test setting the LogHandler on MessageRouterImpl before MessageRouterImpl.modified
     * has been called (i.e. before the properties have been parsed).
     */
    @Test
    public void setWsLogHandlerBeforeModified() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();

        // Set LogHandler *before* modified.
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue("msg route should return true",msgRouter.route(msg,false));
    }

    /**
     * Dynamically override previous settings with new settings, removing a WsLogHandler
     * from the list (via '-'). Verify that the WsLogHandler is called prior to the
     * override, but not after.
     */
    @Test
    public void overrideAndRemoveWsLogHandler() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue(msgRouter.route(msg,false));

        // Override and remove MYLOGHANDLER from the list.
        Properties props1 = new Properties();
        props1.setProperty("MYMSG1234I", "-MYLOGHANDLER");
        msgRouter.modified(props1);

        // mockLogHandler should not be called. No expectations to set up.

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue("msg route should return true",msgRouter.route(msg,false));
    }

    /**
     * Remove the default ("-DEFAULT") and verify that MessageRouterImpl.route()
     * returns false.
     */
    @Test
    public void removeDefaultWs() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "-DEFAULT,+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);

        // Since I specified -DEFAULT, route() should return false.
        assertFalse("msg route should return false",msgRouter.route(msg,false));
    }

    /**
     * Unset a LogHandler. Verify that the LogHandler is called prior to unsetting it,
     * but not after.
     */
    @Test
    public void unsetWsLogHandler() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue("msg route should return true",msgRouter.route(msg,false));

        // Remove the LogHandler.
        msgRouter.unsetWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // mockLogHandler now should not be called. No expectations to set up.

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue("msg route should return true",msgRouter.route(msg,false));
    }

    /**
     * Verify that bad property data (empty values, +/- alone) doesn't cause problems.
     */
    @Test
    public void badPropertyDataWs() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "-DEFAULT,,+,-,+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);

        // Since I specified -DEFAULT, route() should return false.
        assertFalse("msg route should return false",msgRouter.route(msg,false));
    }

    /**
     * Add multiple LogHandlers and verify they get control when appropriate.
     */
    @Test
    public void multipleWsLogHandlers() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);
        msgRouter.setWsLogHandler("MYLOGHANDLER1", mockWsLogHandler1);
        msgRouter.setWsLogHandler("MYLOGHANDLER2", mockWsLogHandler2);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER,+MYLOGHANDLER1,-DEFAULT");
        props.setProperty("MYMSG1235I", "+MYLOGHANDLER");
        props.setProperty("MYMSG1236I", "+MYLOGHANDLER1");
        props.setProperty("*", "+MYLOGHANDLER2");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg1234 = new TestRoutedMessage("MYMSG1234I: blah blah blah");
        RoutedMessage msg1235 = new TestRoutedMessage("MYMSG1235I: blah blah blah");
        RoutedMessage msg1236 = new TestRoutedMessage("MYMSG1236I: blah blah blah");
        RoutedMessage msgShort = new TestRoutedMessage("12345");

        // mockLogHandler should get called for msg1234 and msg1235.
        // mockLogHandler1 should get called for msg1234 and msg1236.
        // mockLogHandler2 should get called for msg1234, msg1235, msg1236 and msgShort
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1236);
        setupWsLogHandlerExpectations(mockWsLogHandler2, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler2, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler2, msg1236);
        setupWsLogHandlerExpectations(mockWsLogHandler2, msgShort);

        // route(msg1234,false) should return false; the others true.
        assertFalse("msg1234 route should return false",msgRouter.route(msg1234,false));
        assertTrue("msg1235 route should return true",msgRouter.route(msg1235,false));
        assertTrue("msg1236 route should return true",msgRouter.route(msg1236,false));
        assertTrue("msgShort route should return true",msgRouter.route(msgShort,false));
    }

    /**
     * Override previous settings with new settings and add a new LogHandler.
     * Verify both the existing LogHandler and the new LogHandler are both
     * called after the add.
     */
    @Test
    public void overrideAndAddWs() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);
        msgRouter.setWsLogHandler("MYLOGHANDLER1", mockWsLogHandler1);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue("msg route should return true", msgRouter.route(msg,false));

        // Override and add MYLOGHANDLER1 to the list.
        Properties props1 = new Properties();
        props1.setProperty("MYMSG1234I", "+MYLOGHANDLER1");
        msgRouter.modified(props1);

        // Both mockLogHandler and mockLogHandler1 should get called.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg);

        // Since I didn't specify -DEFAULT, route() should return true.
        assertTrue("msg route should return true", msgRouter.route(msg,false));
    }

    /**
     * Test an empty "" WsLogHandler key.
     * Nothing bad will happen (tho the WsLogHandler will never be routed to).
     */
    @Test
    public void setEmptyWsLogHandlerKey() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("", mockWsLogHandler1);
    }

    /**
     * Test an invalid message (shorter than msgId length).
     */
    @Test
    public void invalidMessageWs() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        RoutedMessage msg = new TestRoutedMessage("MYMSG");

        // mockLogHandler should not get called.  No expectations to set up.

        // Since we're not routing anywhere, route() should return true.
        assertTrue("msg route should return true",msgRouter.route(msg,false));
    }

    /**
     * Test a null message. RoutedMessage should never be null. WsMessageRouterImpl
     * ignores null msgs.
     */
    @Test
    public void nullMessageWs() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.route(null,false);
    }

    /**
     * Test when the WsLogHandler raises an unchecked exception, e.g NPE.
     * WsMessageRouterImpl propagates the NPE.
     * 
     * WsLogHandlers should *NEVER* raise exceptions (tho perhaps WsMessageRouterImpl
     * should be fortified against the possibility nonetheless).
     */
    @Test(expected = NullPointerException.class)
    public void wsLogHandlerThrowsException() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("MYMSG1234I", "+MYLOGHANDLER");
        msgRouter.modified(props);

        // Create msg.
        final RoutedMessage msg = new TestRoutedMessage("MYMSG1234I: blah blah blah");

        // mockLogHandler will get called and raise an exception
        mockery.checking(new Expectations() {
            {
                oneOf(mockWsLogHandler).publish(with(equal(msg)),with(false));
                will(throwException(new NullPointerException("null ptr from WsLogHandler")));
            }
        });

        msgRouter.route(msg,false);
    }

    /**
     * Test a WsLogHandler registered for ALL msgs.
     */
    @Test
    public void test_routeAllMsgs() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);
        msgRouter.setWsLogHandler("MYLOGHANDLER1", mockWsLogHandler1);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("*", "MYLOGHANDLER");
        props.setProperty("MYMSG1235I", "+MYLOGHANDLER1");
        msgRouter.modified(props);

        // Create msgs.
        RoutedMessage msg1234 = new TestRoutedMessage("MYMSG1234I: blah blah blah");
        RoutedMessage msg1235 = new TestRoutedMessage("MYMSG1235I: blah blah blah");
        RoutedMessage msg1236 = new TestRoutedMessage("MYMSG1236I: blah blah blah");

        // mockLogHandler should get called for all msgs
        // mockLogHandler1 should get called only for msg1235.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1236);

        // All should return true.
        assertTrue("msg1234 route should return true",msgRouter.route(msg1234,false));
        assertTrue("msg1235 route should return true",msgRouter.route(msg1235,false));
        assertTrue("msg1236 route should return true",msgRouter.route(msg1236,false));
    }

    /**
     * Add multiple LogHandlers and verify they get control when appropriate.
     */
    @Test
    public void test_routeAllMultiple() {
        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);
        msgRouter.setWsLogHandler("MYLOGHANDLER1", mockWsLogHandler1);

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("*", "+MYLOGHANDLER,+MYLOGHANDLER1,-DEFAULT"); // Note: can't turn off default (-DEFAULT is ignored)
        msgRouter.modified(props);

        // Create msgs.
        RoutedMessage msg1234 = new TestRoutedMessage("MYMSG1234I: blah blah blah");
        RoutedMessage msg1235 = new TestRoutedMessage("MYMSG1235I: blah blah blah");
        RoutedMessage msg1236 = new TestRoutedMessage("MYMSG1236I: blah blah blah");

        // Both mockLogHandlers should get called for all msgs.
        // mockLogHandler1 should get called for msg1234 and msg1236.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1236);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1236);

        // Should return true (-DEFAULT is ignored for '*' LogHandlers)
        assertTrue("msg1234 route should return true",msgRouter.route(msg1234,false));
        assertTrue("msg1235 route should return true",msgRouter.route(msg1235,false));
        assertTrue("msg1236 route should return true",msgRouter.route(msg1236,false));
    }

    /**
     * Test delivery of "earlierMessages".
     */
    @Test
    public void test_earlierMessages() {

        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();

        // Create msgs.
        RoutedMessage msg1234 = new TestRoutedMessage("MYMSG1234I: blah blah blah");
        RoutedMessage msg1235 = new TestRoutedMessage("MYMSG1235I: blah blah blah");
        RoutedMessage msg1236 = new TestRoutedMessage("MYMSG1236I: blah blah blah");

        Queue<RoutedMessage> earlierMessages = new ConcurrentLinkedQueue<RoutedMessage>();
        earlierMessages.add(msg1234);
        earlierMessages.add(msg1235);

        // Pass earlierMessages to msgRouter
        msgRouter.setEarlierMessages(earlierMessages);

        // Route another message.  Will be added to earlierMessages queue.
        // No LogHandlers yet.
        assertTrue("msg1236 route should return true",msgRouter.route(msg1236,false));

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("*", "MYLOGHANDLER");
        props.setProperty("MYMSG1235I", "+MYLOGHANDLER1");
        msgRouter.modified(props);

        // mockLogHandler should get called for all msgs
        // mockLogHandler1 should get called only for msg1235.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1236);

        // Earlier messages are routed to the handlers when they're set.
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);
        msgRouter.setWsLogHandler("MYLOGHANDLER1", mockWsLogHandler1);
    }

    /**
     * Test delivery of "earlierMessages".
     */
    @Test
    public void test_earlierMessages2() {

        WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();

        // Create msgs.
        RoutedMessage msg1234 = new TestRoutedMessage("MYMSG1234I: blah blah blah");
        RoutedMessage msg1235 = new TestRoutedMessage("MYMSG1235I: blah blah blah");
        RoutedMessage msg1236 = new TestRoutedMessage("MYMSG1236I: blah blah blah");
        RoutedMessage msgShort = new TestRoutedMessage("12345");

        Queue<RoutedMessage> earlierMessages = new ConcurrentLinkedQueue<RoutedMessage>();

        // Pass earlierMessages to msgRouter
        msgRouter.setEarlierMessages(earlierMessages);

        // Route messages.  Will be added to earlierMessages queue.
        // No LogHandlers yet.
        assertTrue("msg1234 route should return true",msgRouter.route(msg1234,false));
        assertTrue("msg1235 route should return true",msgRouter.route(msg1235,false));
        assertTrue("msg1236 route should return true",msgRouter.route(msg1236,false));
        assertTrue("msgShort route should return true",msgRouter.route(msgShort,false));

        // Set up MessageRouter.properties.
        Properties props = new Properties();
        props.setProperty("*", "MYLOGHANDLER");
        props.setProperty("MYMSG1235I", "+MYLOGHANDLER1");
        msgRouter.modified(props);

        // mockLogHandler should get called for all msgs
        // mockLogHandler1 should get called only for msg1235.
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1234);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler1, msg1235);
        setupWsLogHandlerExpectations(mockWsLogHandler, msg1236);
        setupWsLogHandlerExpectations(mockWsLogHandler, msgShort);

        // Earlier messages are routed to the handlers when they're set.
        msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);
        msgRouter.setWsLogHandler("MYLOGHANDLER1", mockWsLogHandler1);
    }

    /**
     * Test wildcard routing with a log level suffix (e.g. "ABCD*I").
     * A message matching the prefix and level ("ABCD23891I") should be dispatched
     * to the handler, while a message matching the prefix but a different level
     * ("ABCD23891W") should not be dispatched to the handler.
     */
    @Test
    public void testWildcardRoutingWithLevelSuffix() {
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

            // Subscribe handler to ABCD*I
            Properties props = new Properties();
            props.setProperty("ABCD*I", "+MYLOGHANDLER");
            msgRouter.modified(props);

            RoutedMessage matchingMsg = new TestRoutedMessage("ABCD23891I: Info message");
            RoutedMessage nonMatchingMsg = new TestRoutedMessage("ABCD23891W: Warning message");

            // Expect mockWsLogHandler to be called only for matchingMsg
            setupWsLogHandlerExpectations(mockWsLogHandler, matchingMsg);

            // matchingMsg routes to mockWsLogHandler and returns true (log normally)
            assertTrue("Matching message should route and return true", msgRouter.route(matchingMsg, false));

            // nonMatchingMsg does not match ABCD*I (level W != I), should not invoke mockWsLogHandler, returns true
            assertTrue("Non-matching message should return true", msgRouter.route(nonMatchingMsg, false));
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Test wildcard routing with a prefix-only pattern (e.g. "ABCD123*").
     * All messages starting with "ABCD123" regardless of log level (I, W, E, A)
     * should be dispatched to the handler, while messages with a different prefix
     * should not be dispatched.
     */
    @Test
    public void testWildcardRoutingPrefixOnlyAllLevels() {
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

            // Subscribe handler to ABCD123* (matches any level)
            Properties props = new Properties();
            props.setProperty("ABCD123*", "+MYLOGHANDLER");
            msgRouter.modified(props);

            RoutedMessage msgInfo = new TestRoutedMessage("ABCD1231I: Info message");
            RoutedMessage msgWarn = new TestRoutedMessage("ABCD1232W: Warning message");
            RoutedMessage msgError = new TestRoutedMessage("ABCD1233E: Error message");
            RoutedMessage msgAudit = new TestRoutedMessage("ABCD1234A: Audit message");
            RoutedMessage msgDifferentPrefix = new TestRoutedMessage("WXYZ1231I: Unrelated message");

            // Expect mockWsLogHandler to be called for all messages matching ABCD123 prefix
            setupWsLogHandlerExpectations(mockWsLogHandler, msgInfo);
            setupWsLogHandlerExpectations(mockWsLogHandler, msgWarn);
            setupWsLogHandlerExpectations(mockWsLogHandler, msgError);
            setupWsLogHandlerExpectations(mockWsLogHandler, msgAudit);

            // Route all messages
            assertTrue("ABCD1231I should route to handler and return true", msgRouter.route(msgInfo, false));
            assertTrue("ABCD1232W should route to handler and return true", msgRouter.route(msgWarn, false));
            assertTrue("ABCD1233E should route to handler and return true", msgRouter.route(msgError, false));
            assertTrue("ABCD1234A should route to handler and return true", msgRouter.route(msgAudit, false));

            // Different prefix should not invoke mockWsLogHandler
            assertTrue("WXYZ1231I should not route to handler and return true", msgRouter.route(msgDifferentPrefix, false));
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Test subscribing to wildcard patterns via the API/method call
     * addMsgToLogHandler() / updateMessageListForHandler().
     * Subscribe handler to "ABCD*I,BCGA*" via addMsgToLogHandler.
     * Verify matching messages are routed and non-matching messages are ignored.
     */
    @Test
    public void testWildcardRoutingViaAddMsgToLogHandler() {
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

            // Subscribe handler via API methods
            msgRouter.addMsgToLogHandler("ABCD*I", "MYLOGHANDLER");
            msgRouter.addMsgToLogHandler("BCGA*", "MYLOGHANDLER");

            RoutedMessage msgMatchLevel = new TestRoutedMessage("ABCD23891I: Info matching ABCD*I");
            RoutedMessage msgMismatchLevel = new TestRoutedMessage("ABCD23891W: Warning not matching ABCD*I");
            RoutedMessage msgMatchPrefix = new TestRoutedMessage("BCGA9999W: Warning matching BCGA*");
            RoutedMessage msgMismatchPrefix = new TestRoutedMessage("BCGB9999W: Warning not matching BCGA*");

            // Expect mockWsLogHandler to be called only for matching messages
            setupWsLogHandlerExpectations(mockWsLogHandler, msgMatchLevel);
            setupWsLogHandlerExpectations(mockWsLogHandler, msgMatchPrefix);

            assertTrue("ABCD23891I should route to handler", msgRouter.route(msgMatchLevel, false));
            assertTrue("ABCD23891W should not route to handler", msgRouter.route(msgMismatchLevel, false));
            assertTrue("BCGA9999W should route to handler", msgRouter.route(msgMatchPrefix, false));
            assertTrue("BCGB9999W should not route to handler", msgRouter.route(msgMismatchPrefix, false));
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Test dynamic updating (add and remove) of wildcard patterns via
     * addMsgToLogHandler() and removeMsgFromLogHandler() methods.
     */
    @Test
    public void testWildcardRemovalViaApi() {
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

            // 1. Add subscription
            msgRouter.addMsgToLogHandler("ABCD*I", "MYLOGHANDLER");

            RoutedMessage msg1 = new TestRoutedMessage("ABCD23891I: Info message 1");
            setupWsLogHandlerExpectations(mockWsLogHandler, msg1);
            assertTrue("Message should route while subscribed", msgRouter.route(msg1, false));

            // 2. Remove subscription
            msgRouter.removeMsgFromLogHandler("ABCD*I", "MYLOGHANDLER");

            // 3. Route again - should NOT be dispatched to mockWsLogHandler (no expectations set)
            RoutedMessage msg2 = new TestRoutedMessage("ABCD23891I: Info message 2");
            assertTrue("Message should not route after unsubscribing", msgRouter.route(msg2, false));
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Scenario 1: Multi-Handler Wildcard Overlap & Deduplication.
     * Multiple handlers subscribe to overlapping patterns:
     *  - mockWsLogHandler subscribes to "ABCD*"
     *  - mockWsLogHandler1 subscribes to "ABCD*I"
     *  - mockWsLogHandler2 subscribes to "*" (global route-all)
     *
     * Verify:
     *  1. "ABCD1234I" is delivered to all three handlers.
     *  2. "ABCD1234W" is delivered to mockWsLogHandler and mockWsLogHandler2 (not mockWsLogHandler1).
     *  3. "WXYZ1234I" is delivered only to mockWsLogHandler2 (*).
     *  4. A handler registered under both "*" and "ABCD*" (e.g. mockWsLogHandler2) receives
     *     each message only once (deduplication check).
     */
    @Test
    public void testMultiHandlerWildcardOverlapAndDeduplication() {
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("HANDLER_PREFIX", mockWsLogHandler);
            msgRouter.setWsLogHandler("HANDLER_LEVEL", mockWsLogHandler1);
            msgRouter.setWsLogHandler("HANDLER_GLOBAL", mockWsLogHandler2);

            // Subscriptions
            msgRouter.addMsgToLogHandler("ABCD*", "HANDLER_PREFIX");
            msgRouter.addMsgToLogHandler("ABCD*I", "HANDLER_LEVEL");
            // Also subscribe HANDLER_GLOBAL to both '*' and 'ABCD*' to test deduplication
            msgRouter.addMsgToLogHandler("*", "HANDLER_GLOBAL");
            msgRouter.addMsgToLogHandler("ABCD*", "HANDLER_GLOBAL");

            RoutedMessage msgInfo = new TestRoutedMessage("ABCD1234I: Info message");
            RoutedMessage msgWarn = new TestRoutedMessage("ABCD1234W: Warning message");
            RoutedMessage msgOther = new TestRoutedMessage("WXYZ1234I: Unrelated message");

            // msgInfo (ABCD1234I) should be dispatched to:
            // - HANDLER_PREFIX (matches ABCD*)
            // - HANDLER_LEVEL (matches ABCD*I)
            // - HANDLER_GLOBAL (matches * and ABCD*, must be called EXACTLY ONCE due to deduplication)
            setupWsLogHandlerExpectations(mockWsLogHandler, msgInfo);
            setupWsLogHandlerExpectations(mockWsLogHandler1, msgInfo);
            setupWsLogHandlerExpectations(mockWsLogHandler2, msgInfo);

            // msgWarn (ABCD1234W) should be dispatched to:
            // - HANDLER_PREFIX (matches ABCD*)
            // - HANDLER_GLOBAL (matches *)
            // NOT to HANDLER_LEVEL (W != I)
            setupWsLogHandlerExpectations(mockWsLogHandler, msgWarn);
            setupWsLogHandlerExpectations(mockWsLogHandler2, msgWarn);

            // msgOther (WXYZ1234I) should be dispatched to:
            // - HANDLER_GLOBAL only (matches *)
            setupWsLogHandlerExpectations(mockWsLogHandler2, msgOther);

            assertTrue("msgInfo should route and return true", msgRouter.route(msgInfo, false));
            assertTrue("msgWarn should route and return true", msgRouter.route(msgWarn, false));
            assertTrue("msgOther should route and return true", msgRouter.route(msgOther, false));
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Scenario 2: Early-Message Buffering and Replay with Wildcards.
     * Messages are issued and buffered before any WsLogHandler registers.
     * When a handler registers with wildcard subscriptions (e.g. "ABCD*I" and "BCGA*"),
     * only earlier messages matching those wildcard patterns should be replayed.
     */
    @Test
    public void testEarlyMessagesWildcardReplay() {
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();

            // Set up early messages queue
            Queue<RoutedMessage> earlierMessages = new ConcurrentLinkedQueue<RoutedMessage>();
            msgRouter.setEarlierMessages(earlierMessages);

            RoutedMessage msgMatchLevel = new TestRoutedMessage("ABCD23891I: Info matching ABCD*I");
            RoutedMessage msgMismatchLevel = new TestRoutedMessage("ABCD23891W: Warning not matching ABCD*I");
            RoutedMessage msgMatchPrefix = new TestRoutedMessage("BCGA9999W: Warning matching BCGA*");
            RoutedMessage msgMismatchPrefix = new TestRoutedMessage("BCGB9999W: Warning not matching BCGA*");

            // Route messages while no handlers are registered yet (all buffered into earlierMessages)
            assertTrue(msgRouter.route(msgMatchLevel, false));
            assertTrue(msgRouter.route(msgMismatchLevel, false));
            assertTrue(msgRouter.route(msgMatchPrefix, false));
            assertTrue(msgRouter.route(msgMismatchPrefix, false));

            // Subscribe handler to ABCD*I and BCGA*
            msgRouter.addMsgToLogHandler("ABCD*I", "MYLOGHANDLER");
            msgRouter.addMsgToLogHandler("BCGA*", "MYLOGHANDLER");

            // Expect only matching early messages to be replayed upon handler registration
            setupWsLogHandlerExpectations(mockWsLogHandler, msgMatchLevel);
            setupWsLogHandlerExpectations(mockWsLogHandler, msgMatchPrefix);

            // Register handler - triggers replay of matching earlierMessages
            msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Scenario 4: Invalid Wildcard Syntax Handling.
     * Invalid patterns (e.g. "ABC**", "ABC**E", "ABC*EFJ", "XYZ**I") should be rejected,
     * emit warning CWWKE0710W, and not route matching messages.
     */
    @Test
    public void testInvalidWildcardEmitsWarningAndDoesNotRoute() {
                	outputMgr.resetStreams();
        	outputMgr.restoreStreams();
        	outputMgr.captureStreams();
        try {

            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

            // Subscribing invalid patterns via addMsgToLogHandler
            msgRouter.addMsgToLogHandler("ABC**", "MYLOGHANDLER");
            msgRouter.addMsgToLogHandler("ABC**E", "MYLOGHANDLER");
            msgRouter.addMsgToLogHandler("ABC*EFJ", "MYLOGHANDLER");

            // Subscribing invalid pattern via modified()
            Properties props = new Properties();
            props.setProperty("XYZ**I", "+MYLOGHANDLER");
            msgRouter.modified(props);

            // Each invalid pattern must produce a CWWKE0710W entry naming the offending pattern.
            // '*' is a regex metacharacter — escape as \\* to match a literal asterisk.
            assertTrue("Expected CWWKE0710W warning for ABC**",
                       outputMgr.checkForMessages("CWWKE0710W.*ABC\\*\\*[^E]"));
            assertTrue("Expected CWWKE0710W warning for ABC**E",
                       outputMgr.checkForMessages("CWWKE0710W.*ABC\\*\\*E"));
            assertTrue("Expected CWWKE0710W warning for ABC*EFJ",
                       outputMgr.checkForMessages("CWWKE0710W.*ABC\\*EFJ"));

            // Verify that messages with prefixes from invalid subscriptions are NOT dispatched to handler
            RoutedMessage msg1 = new TestRoutedMessage("ABCDE1234I: Should not route");
            RoutedMessage msg2 = new TestRoutedMessage("XYZAB1234I: Should not route");

            assertTrue("msg1 should return true for normal logging without dispatching", msgRouter.route(msg1, false));
            assertTrue("msg2 should return true for normal logging without dispatching", msgRouter.route(msg2, false));
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Test that a structurally valid wildcard pattern whose level suffix character is NOT one
     * of the recognised letters (I, A, W, E) is rejected, emits warning CWWKE0710W, and does
     * not route matching messages.
     *
     * "ABCD*X" passes the structural check (exactly one '*' at second-to-last position) but
     * 'X' is not a recognised log-level letter, so WildCardMessageAndLevel throws
     * IllegalArgumentException and the pattern must be dropped entirely.
     *
     * Exercises the addMsgToLogHandler() path.
     */
    @Test
    public void testInvalidLevelCharViaAddMsgToLogHandler() {
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
        outputMgr.captureStreams();
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

            // Structurally valid (one '*', second-to-last) but invalid level chars
            msgRouter.addMsgToLogHandler("ABCD*X", "MYLOGHANDLER");
            msgRouter.addMsgToLogHandler("ABCD*Z", "MYLOGHANDLER");
            msgRouter.addMsgToLogHandler("ABCD*0", "MYLOGHANDLER");

            // Each must emit CWWKE0710W naming the offending pattern
            assertTrue("Expected CWWKE0710W warning for ABCD*X",
                       outputMgr.checkForMessages("CWWKE0710W.*ABCD\\*X"));
            assertTrue("Expected CWWKE0710W warning for ABCD*Z",
                       outputMgr.checkForMessages("CWWKE0710W.*ABCD\\*Z"));
            assertTrue("Expected CWWKE0710W warning for ABCD*0",
                       outputMgr.checkForMessages("CWWKE0710W.*ABCD\\*0"));

            // No handler invocation expected — mockWsLogHandler must NOT be called
            RoutedMessage msg1 = new TestRoutedMessage("ABCD1234X: should not route");
            RoutedMessage msg2 = new TestRoutedMessage("ABCD1234Z: should not route");
            RoutedMessage msg3 = new TestRoutedMessage("ABCD12340: should not route");

            assertTrue("ABCD*X pattern should not route", msgRouter.route(msg1, false));
            assertTrue("ABCD*Z pattern should not route", msgRouter.route(msg2, false));
            assertTrue("ABCD*0 pattern should not route", msgRouter.route(msg3, false));
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Test that a structurally valid wildcard pattern whose level suffix character is NOT one
     * of the recognised letters (I, A, W, E) is rejected, emits warning CWWKE0710W, and does
     * not route matching messages.
     *
     * Exercises the modified() / MessageRouter.properties path.
     */
    @Test
    public void testInvalidLevelCharViaModified() {
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
        outputMgr.captureStreams();
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

            // Structurally valid (one '*', second-to-last) but invalid level chars
            Properties props = new Properties();
            props.setProperty("CWWK*X", "+MYLOGHANDLER");
            props.setProperty("CWWK*Z", "+MYLOGHANDLER");
            msgRouter.modified(props);

            // Each must emit CWWKE0710W naming the offending pattern
            assertTrue("Expected CWWKE0710W warning for CWWK*X",
                       outputMgr.checkForMessages("CWWKE0710W.*CWWK\\*X"));
            assertTrue("Expected CWWKE0710W warning for CWWK*Z",
                       outputMgr.checkForMessages("CWWKE0710W.*CWWK\\*Z"));

            // No handler invocation expected — mockWsLogHandler must NOT be called
            RoutedMessage msg1 = new TestRoutedMessage("CWWK01234X: should not route");
            RoutedMessage msg2 = new TestRoutedMessage("CWWK01234Z: should not route");

            assertTrue("CWWK*X pattern should not route", msgRouter.route(msg1, false));
            assertTrue("CWWK*Z pattern should not route", msgRouter.route(msg2, false));
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Comprehensive routing test: mix of invalid wildcard patterns, valid wildcard patterns,
     * and explicit (exact) message ID subscriptions, all registered against the same handler.
     *
     * Expectations:
     *  - Each invalid pattern emits CWWKE0710W naming the offending pattern and is NOT routed.
     *  - Valid prefix-only wildcard "GOOD*" routes any message starting with "GOOD", any level.
     *  - Valid level-qualified wildcard "FINE*W" routes only WARNING messages starting with "FINE".
     *  - Explicit exact ID "EXACT0001I" routes only that exact message ID.
     *  - Messages that do not match any valid subscription are NOT dispatched to the handler.
     */
    @Test
    public void testMixedValidInvalidAndExplicitRouting() {
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
        outputMgr.captureStreams();
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

            // --- Invalid patterns (via addMsgToLogHandler) ---
            // These must emit CWWKE0710W and must not register any routing.
            msgRouter.addMsgToLogHandler("BAD**",    "MYLOGHANDLER");  // double star
            msgRouter.addMsgToLogHandler("BAD**W",   "MYLOGHANDLER");  // double star with level
            msgRouter.addMsgToLogHandler("BAD*WXY",  "MYLOGHANDLER");  // trailing chars after level

            // --- Valid wildcard patterns (via addMsgToLogHandler) ---
            msgRouter.addMsgToLogHandler("GOOD*",   "MYLOGHANDLER");   // prefix-only, any level
            msgRouter.addMsgToLogHandler("FINE*W",  "MYLOGHANDLER");   // prefix + WARNING level only

            // --- Explicit exact message ID (via addMsgToLogHandler) ---
            msgRouter.addMsgToLogHandler("EXACT0001I", "MYLOGHANDLER");

            // --- Assert CWWKE0710W was emitted for each invalid pattern ---
            assertTrue("Expected CWWKE0710W for BAD**",
                       outputMgr.checkForMessages("CWWKE0710W.*BAD\\*\\*[^W]"));
            assertTrue("Expected CWWKE0710W for BAD**W",
                       outputMgr.checkForMessages("CWWKE0710W.*BAD\\*\\*W"));
            assertTrue("Expected CWWKE0710W for BAD*WXY",
                       outputMgr.checkForMessages("CWWKE0710W.*BAD\\*WXY"));

            // --- Messages that match valid wildcard "GOOD*" (any level) ---
            RoutedMessage goodInfo  = new TestRoutedMessage("GOOD1234I: Info message");
            RoutedMessage goodWarn  = new TestRoutedMessage("GOOD1234W: Warning message");
            RoutedMessage goodError = new TestRoutedMessage("GOOD1234E: Error message");
            RoutedMessage goodAudit = new TestRoutedMessage("GOOD1234A: Audit message");

            setupWsLogHandlerExpectations(mockWsLogHandler, goodInfo);
            setupWsLogHandlerExpectations(mockWsLogHandler, goodWarn);
            setupWsLogHandlerExpectations(mockWsLogHandler, goodError);
            setupWsLogHandlerExpectations(mockWsLogHandler, goodAudit);

            assertTrue("GOOD1234I should route via GOOD*",  msgRouter.route(goodInfo,  false));
            assertTrue("GOOD1234W should route via GOOD*",  msgRouter.route(goodWarn,  false));
            assertTrue("GOOD1234E should route via GOOD*",  msgRouter.route(goodError, false));
            assertTrue("GOOD1234A should route via GOOD*",  msgRouter.route(goodAudit, false));

            // --- Messages that match level-qualified wildcard "FINE*W" ---
            RoutedMessage fineWarn    = new TestRoutedMessage("FINE1234W: Warning — matches FINE*W");
            RoutedMessage fineInfo    = new TestRoutedMessage("FINE1234I: Info — does NOT match FINE*W");
            RoutedMessage fineError   = new TestRoutedMessage("FINE1234E: Error — does NOT match FINE*W");

            setupWsLogHandlerExpectations(mockWsLogHandler, fineWarn);
            // fineInfo and fineError: no expectation — handler must NOT be called

            assertTrue("FINE1234W should route via FINE*W",       msgRouter.route(fineWarn,  false));
            assertTrue("FINE1234I should not route via FINE*W",   msgRouter.route(fineInfo,  false));
            assertTrue("FINE1234E should not route via FINE*W",   msgRouter.route(fineError, false));

            // --- Exact message ID subscription ---
            RoutedMessage exactMatch    = new TestRoutedMessage("EXACT0001I: Exact match");
            RoutedMessage exactNoMatch  = new TestRoutedMessage("EXACT0002I: Different suffix");

            setupWsLogHandlerExpectations(mockWsLogHandler, exactMatch);
            // exactNoMatch: no expectation — handler must NOT be called

            assertTrue("EXACT0001I should route via exact subscription",       msgRouter.route(exactMatch,   false));
            assertTrue("EXACT0002I should not route — no matching subscription", msgRouter.route(exactNoMatch, false));

            // --- Messages whose prefix matches an invalid (rejected) pattern must NOT route ---
            RoutedMessage badMsg1 = new TestRoutedMessage("BADDE1234I: Invalid prefix BAD**");
            RoutedMessage badMsg2 = new TestRoutedMessage("BADDE1234W: Invalid prefix BAD**W");
            RoutedMessage badMsg3 = new TestRoutedMessage("BADDE1234W: Invalid prefix BAD*WXY");
            // No expectations set — handler must NOT be called for any of these

            assertTrue("BAD** prefix should not route", msgRouter.route(badMsg1, false));
            assertTrue("BAD**W prefix should not route", msgRouter.route(badMsg2, false));
            assertTrue("BAD*WXY prefix should not route", msgRouter.route(badMsg3, false));

        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

     /**
     * Test the updateMessageListForHandler() flow in MessageRouterConfigurator,
     * which is the Strategy-2 path used by internal consumers such as zosLogging.
     *
     * A testable subclass overrides getMessageRouter() to inject a plain
     * WsMessageRouterImpl rather than the static singleton, avoiding the need
     * for a real OSGi runtime. BundleContext is mocked to satisfy the constructor.
     *
     * Verifies:
     *  - Initial call adds message IDs (including wildcards) to the router.
     *  - Second call with overlapping IDs only adds the new ones and removes the dropped ones.
     *  - Routing behaviour after each update matches the registered set.
     */
    @Test
    public void testUpdateMessageListForHandler() throws Exception {
        try {
            // Initialize the test runner's mockery instance
            mockery = new JUnit4Mockery();
            BundleContext mockCtx = mockery.mock(BundleContext.class, "BundleContext");
            WsLogHandler mockHandler = mockery.mock(WsLogHandler.class, "ConfiguratorHandler");

            // Stub OSGi calls made during MessageRouterConfigurator initialization
            mockery.checking(new Expectations() {{
                allowing(mockCtx).addServiceListener(with(any(org.osgi.framework.ServiceListener.class)), with(any(String.class)));
                allowing(mockCtx).getServiceReferences(with(any(String.class)), with(aNull(String.class)));
                    will(returnValue(null));
                allowing(mockCtx).registerService(with(any(Class.class)), with(any(Object.class)), with(any(java.util.Dictionary.class)));
                    will(returnValue(null));
                allowing(mockCtx).addBundleListener(with(any(org.osgi.framework.BundleListener.class)));
                allowing(mockCtx).getBundles();
                    will(returnValue(new Bundle[0]));
            }});

            // Build a WsMessageRouterImpl we control directly.
            final WsMessageRouterImpl router = new WsMessageRouterImpl();
            router.setWsLogHandler("WTO", mockHandler);

            // Subclass MessageRouterConfigurator to inject our router instead of the singleton.
            MessageRouterConfigurator configurator = new MessageRouterConfigurator(mockCtx) {
                @Override
                protected WsMessageRouterImpl getMessageRouter() {
                    if (msgRouter == null) {
                        msgRouter = router;
                    }
                    return msgRouter;
                }
            };

            // ---------------------------------------------------------------
            // First call: register "CWWKZ*,CWWKF001*I,EXACT0001I" for "WTO"
            // ---------------------------------------------------------------
            configurator.updateMessageListForHandler("CWWKZ*,CWWKF001*I,EXACT0001I", "WTO");

            // CWWKZ* — prefix wildcard, any level
            RoutedMessage cwwkzInfo  = new TestRoutedMessage("CWWKZ0001I: started");
            RoutedMessage cwwkzWarn  = new TestRoutedMessage("CWWKZ0002W: warning");

            // CWWKF001*I — prefix + INFO level only
            RoutedMessage cwwkfInfo  = new TestRoutedMessage("CWWKF0011I: feature started");
            RoutedMessage cwwkfWarn  = new TestRoutedMessage("CWWKF0011W: not INFO — should not route");

            // EXACT0001I — exact match only
            RoutedMessage exactMatch   = new TestRoutedMessage("EXACT0001I: exact");
            RoutedMessage exactNoMatch = new TestRoutedMessage("EXACT0002I: different");

            mockery.checking(new Expectations() {{
                oneOf(mockHandler).publish(with(equal(cwwkzInfo)),  with(false));
                oneOf(mockHandler).publish(with(equal(cwwkzWarn)),  with(false));
                oneOf(mockHandler).publish(with(equal(cwwkfInfo)),  with(false));
                // cwwkfWarn — no expectation: must NOT be called
                oneOf(mockHandler).publish(with(equal(exactMatch)), with(false));
                // exactNoMatch — no expectation: must NOT be called
            }});

            assertTrue(router.route(cwwkzInfo,    false));
            assertTrue(router.route(cwwkzWarn,    false));
            assertTrue(router.route(cwwkfInfo,    false));
            assertTrue(router.route(cwwkfWarn,    false));   // routes normally, handler not called
            assertTrue(router.route(exactMatch,   false));
            assertTrue(router.route(exactNoMatch, false));   // routes normally, handler not called

            mockery.assertIsSatisfied();

            // ---------------------------------------------------------------
            // Second call: update to "CWWKZ*,EXACT0002I"
            //  - CWWKZ*    stays (no change)
            //  - CWWKF001*I is removed
            //  - EXACT0001I is removed
            //  - EXACT0002I is added
            // ---------------------------------------------------------------
            configurator.updateMessageListForHandler("CWWKZ*,EXACT0002I", "WTO");

            RoutedMessage cwwkzInfo2   = new TestRoutedMessage("CWWKZ0001I: still active");
            RoutedMessage cwwkfInfo2   = new TestRoutedMessage("CWWKF0011I: removed — should not route");
            RoutedMessage exactMatch2  = new TestRoutedMessage("EXACT0001I: removed — should not route");
            RoutedMessage exactMatch2b = new TestRoutedMessage("EXACT0002I: newly added");

            // Set fresh expectations on mockery: only cwwkzInfo2 and exactMatch2b must be published.
            // Removed patterns (cwwkfInfo2, exactMatch2) have no expectations; invoking them will fail JMock.
            mockery.checking(new Expectations() {{
                oneOf(mockHandler).publish(with(equal(cwwkzInfo2)),   with(false));
                oneOf(mockHandler).publish(with(equal(exactMatch2b)), with(false));
                // cwwkfInfo2 and exactMatch2 — no expectation: must NOT be called
            }});

            assertTrue(router.route(cwwkzInfo2,   false));
            assertTrue(router.route(cwwkfInfo2,   false));   // routes normally, handler not called
            assertTrue(router.route(exactMatch2,  false));   // routes normally, handler not called
            assertTrue(router.route(exactMatch2b, false));

            mockery.assertIsSatisfied();

        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Test that "*I" (zero-length prefix, level only) is accepted as a valid wildcard by
     * the modified() / MessageRouter.properties path: no CWWKE0710W is emitted, INFO-level
     * message IDs are dispatched to the handler, and other levels are not.
     */
    @Test
    public void testLevelOnlyWildcardViaModifiedRoutesByLevel() {
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
        outputMgr.captureStreams();
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

            // "*I" has no prefix before the '*' — valid: matches every INFO-level message ID
            Properties props = new Properties();
            props.setProperty("*I", "+MYLOGHANDLER");
            msgRouter.modified(props);

            assertFalse("*I is valid and must not produce CWWKE0710W",
                        outputMgr.checkForMessages("CWWKE0710W"));

            RoutedMessage infoMsg1 = new TestRoutedMessage("ABCD1234I: Routes via *I");
            RoutedMessage infoMsg2 = new TestRoutedMessage("WXYZ9876I: Routes via *I, any prefix");
            RoutedMessage warnMsg  = new TestRoutedMessage("ABCD1234W: Does not match *I");
            RoutedMessage errorMsg = new TestRoutedMessage("ABCD1234E: Does not match *I");
            RoutedMessage auditMsg = new TestRoutedMessage("ABCD1234A: Does not match *I");

            // Only the INFO-level message IDs are dispatched; any other publish fails JMock
            setupWsLogHandlerExpectations(mockWsLogHandler, infoMsg1);
            setupWsLogHandlerExpectations(mockWsLogHandler, infoMsg2);

            assertTrue("Info message should route via *I",         msgRouter.route(infoMsg1, false));
            assertTrue("Info message should route via *I",         msgRouter.route(infoMsg2, false));
            assertTrue("Warning message should not route via *I",  msgRouter.route(warnMsg,  false));
            assertTrue("Error message should not route via *I",    msgRouter.route(errorMsg, false));
            assertTrue("Audit message should not route via *I",    msgRouter.route(auditMsg, false));
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Test that the level-only wildcards "*I", "*E", "*W" and "*A" are all accepted by the
     * addMsgToLogHandler() / programmatic API path, emit no CWWKE0710W, and that two
     * handlers, each subscribed to a different subset of the levels, receive only the
     * message IDs ending in a level letter they subscribed to:
     *
     *  - INFO_ERROR_HANDLER subscribes to "*I" and "*E" (not W or A)
     *  - WARN_AUDIT_HANDLER subscribes to "*W" and "*A" (not I or E)
     *
     * Only the expected deliveries are set up. JMock fails any publish() call that has no
     * matching expectation, so a handler receiving a level it did not subscribe to (or an
     * unrecognised level letter) fails the test.
     */
    @Test
    public void testLevelOnlyWildcardsViaAddMsgToLogHandlerRouteByLevel() {
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
        outputMgr.captureStreams();
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("INFO_ERROR_HANDLER", mockWsLogHandler);
            msgRouter.setWsLogHandler("WARN_AUDIT_HANDLER", mockWsLogHandler1);

            // Each handler subscribes to a subset of the levels
            msgRouter.addMsgToLogHandler("*I", "INFO_ERROR_HANDLER");
            msgRouter.addMsgToLogHandler("*E", "INFO_ERROR_HANDLER");
            msgRouter.addMsgToLogHandler("*W", "WARN_AUDIT_HANDLER");
            msgRouter.addMsgToLogHandler("*A", "WARN_AUDIT_HANDLER");

            assertFalse("*I, *E, *W and *A are valid and must not produce CWWKE0710W",
                        outputMgr.checkForMessages("CWWKE0710W"));

            RoutedMessage infoMsg  = new TestRoutedMessage("ABCD1234I: Info");
            RoutedMessage errorMsg = new TestRoutedMessage("ABCD1234E: Error");
            RoutedMessage warnMsg  = new TestRoutedMessage("ABCD1234W: Warning");
            RoutedMessage auditMsg = new TestRoutedMessage("ABCD1234A: Audit");
            RoutedMessage otherMsg = new TestRoutedMessage("ABCD1234X: Unrecognised level letter");

            // Each message is delivered once, to the handler subscribed to its level letter.
            // No other handler/message combination is expected, so none may occur.
            setupWsLogHandlerExpectations(mockWsLogHandler,  infoMsg);
            setupWsLogHandlerExpectations(mockWsLogHandler,  errorMsg);
            setupWsLogHandlerExpectations(mockWsLogHandler1, warnMsg);
            setupWsLogHandlerExpectations(mockWsLogHandler1, auditMsg);
            // otherMsg: no expectation — must not be dispatched to either handler

            assertTrue("Info message should route via *I",     msgRouter.route(infoMsg,  false));
            assertTrue("Error message should route via *E",    msgRouter.route(errorMsg, false));
            assertTrue("Warning message should route via *W",  msgRouter.route(warnMsg,  false));
            assertTrue("Audit message should route via *A",    msgRouter.route(auditMsg, false));
            assertTrue("Unrecognised level should not route",  msgRouter.route(otherMsg, false));
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Wildcard patterns must not match records below INFO, such as the CONFIG-level records
     * created for System.out / System.err output. Their text can start with anything, so
     * matching a wildcard against its first characters would give false positives.
     *
     * The same message text (starting with a real message ID) is routed twice, once in a
     * CONFIG-level record and once in an INFO-level record:
     *
     *  - WILDCARD_HANDLER subscribes to "*I", "CWWKF*" and "CWWKF*I" (empty prefix, prefix only,
     *    and prefix plus level), all of which match the message ID
     *  - EXACT_HANDLER subscribes to the exact ID "CWWKF0011I"
     *  - ALL_HANDLER subscribes to the route-all "*"
     *
     * CONFIG record: only the wildcard handler is skipped. The exact-ID and route-all
     * subscriptions are not affected by the guard.
     * INFO record: all three handlers receive the message. This control shows that the
     * record level alone decides wildcard eligibility.
     */
    @Test
    public void testConfigLevelRecordsAreNotMatchedByWildcards() {
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("WILDCARD_HANDLER", mockWsLogHandler);
            msgRouter.setWsLogHandler("EXACT_HANDLER", mockWsLogHandler1);
            msgRouter.setWsLogHandler("ALL_HANDLER", mockWsLogHandler2);

            msgRouter.addMsgToLogHandler("*I", "WILDCARD_HANDLER");
            msgRouter.addMsgToLogHandler("CWWKF*", "WILDCARD_HANDLER");
            msgRouter.addMsgToLogHandler("CWWKF*I", "WILDCARD_HANDLER");
            msgRouter.addMsgToLogHandler("CWWKF0011I", "EXACT_HANDLER");
            msgRouter.addMsgToLogHandler("*", "ALL_HANDLER");

            // Same text and message ID, differing only in the level of the LogRecord
            RoutedMessage configMsg = new TestRoutedMessage("CWWKF0011I: echoed to System.out", Level.CONFIG);
            RoutedMessage infoMsg = new TestRoutedMessage("CWWKF0011I: logged as INFO", Level.INFO);

            // CONFIG record: exact-ID and route-all handlers only. WILDCARD_HANDLER has no
            // expectation for it, so receiving it fails the test.
            setupWsLogHandlerExpectations(mockWsLogHandler1, configMsg);
            setupWsLogHandlerExpectations(mockWsLogHandler2, configMsg);

            // INFO record: all three handlers
            setupWsLogHandlerExpectations(mockWsLogHandler, infoMsg);
            setupWsLogHandlerExpectations(mockWsLogHandler1, infoMsg);
            setupWsLogHandlerExpectations(mockWsLogHandler2, infoMsg);

            assertTrue("CONFIG record should route normally", msgRouter.route(configMsg, false));
            assertTrue("INFO record should route normally", msgRouter.route(infoMsg, false));
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * The wildcard eligibility rule must also apply when earlier (buffered) messages are
     * replayed to a newly registered handler, so that replay and live routing agree.
     *
     * A CONFIG-level record and an INFO-level record, both with the text "CWWKF0011I: ...",
     * are buffered before any handler exists. When a handler subscribed to "*I" and "CWWKF*"
     * registers, only the INFO-level record is replayed to it.
     */
    @Test
    public void testEarlyMessageReplaySkipsConfigLevelRecordsForWildcards() {
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();

            Queue<RoutedMessage> earlierMessages = new ConcurrentLinkedQueue<RoutedMessage>();
            msgRouter.setEarlierMessages(earlierMessages);

            RoutedMessage configMsg = new TestRoutedMessage("CWWKF0011I: echoed to System.out", Level.CONFIG);
            RoutedMessage infoMsg = new TestRoutedMessage("CWWKF0011I: logged as INFO", Level.INFO);

            // No handlers yet: both messages are buffered
            assertTrue(msgRouter.route(configMsg, false));
            assertTrue(msgRouter.route(infoMsg, false));

            msgRouter.addMsgToLogHandler("*I", "MYLOGHANDLER");
            msgRouter.addMsgToLogHandler("CWWKF*", "MYLOGHANDLER");

            // Only the INFO-level record is replayed. configMsg has no expectation.
            setupWsLogHandlerExpectations(mockWsLogHandler, infoMsg);

            // Registering the handler triggers the replay
            msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }


    /**
     * Verify that when a single handler is registered under multiple overlapping patterns
     * — a broad prefix wildcard ("CWW*"), a narrower level-qualified wildcard ("CWW*I"),
     * and an exact message ID ("CWW12345I") — an incoming message that matches all three
     * ("CWW12345I: some text") is delivered to the handler exactly once.
     *
     * The merger in {@code getLogHandlersForMsgIdWC()} uses a {@code Set}, so the same
     * handler ID cannot appear more than once.  This test makes that guarantee explicit
     * and detectable: JMock's {@code oneOf} expectation will fail if {@code publish} is
     * called more than once.
     *
     * Also verifies that a message matching only "CWW*" (non-INFO level, e.g. "CWW12345W")
     * is still routed exactly once, and that a message matching none of the three patterns
     * ("XXXX0001I") is not routed at all.
     */
    @Test
    public void testOverlappingPatternsRouteHandlerExactlyOnce() {
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("MYLOGHANDLER", mockWsLogHandler);

            // Same handler subscribed to three overlapping patterns:
            //   "CWW*"      — any message starting with "CWW", any level
            //   "CWW*I"     — any message starting with "CWW" that is INFO level
            //   "CWW12345I" — exact message ID (goes into the exact-ID map)
            msgRouter.addMsgToLogHandler("CWW*",      "MYLOGHANDLER");
            msgRouter.addMsgToLogHandler("CWW*I",     "MYLOGHANDLER");
            msgRouter.addMsgToLogHandler("CWW12345I", "MYLOGHANDLER");

            // "CWW12345I" matches all three — handler must be called EXACTLY ONCE.
            RoutedMessage tripleMatch = new TestRoutedMessage("CWW12345I: server started");
            setupWsLogHandlerExpectations(mockWsLogHandler, tripleMatch);   // oneOf → exactly once
            assertTrue("CWW12345I should route and return true",
                       msgRouter.route(tripleMatch, false));

        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Verify that the level-only wildcard "*I" overlapping with a prefix wildcard ("CWW*"), an
     * exact ID ("CWW12345I") and the route-all "*" never delivers a message to the same
     * handler more than once.
     *
     *  - H1 (mockWsLogHandler) subscribes to "*I", "CWW*" and "CWW12345I"
     *  - H2 (mockWsLogHandler1) subscribes to the route-all "*" and to "*I"
     *
     * JMock's {@code oneOf} expectation fails if publish() is called more than once for a
     * handler/message pair. Handler/message pairs without an expectation must not occur.
     */
    @Test
    public void testLevelOnlyWildcardOverlapRoutesHandlerExactlyOnce() {
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("H1", mockWsLogHandler);
            msgRouter.setWsLogHandler("H2", mockWsLogHandler1);

            msgRouter.addMsgToLogHandler("*I",        "H1");
            msgRouter.addMsgToLogHandler("CWW*",      "H1");
            msgRouter.addMsgToLogHandler("CWW12345I", "H1");

            msgRouter.addMsgToLogHandler("*",  "H2");
            msgRouter.addMsgToLogHandler("*I", "H2");

            // Matches all of H1's patterns, and H2's "*" and "*I"
            RoutedMessage tripleMatch = new TestRoutedMessage("CWW12345I: server started");
            // Matches H1 via "CWW*" only, and H2 via "*"
            RoutedMessage prefixOnly = new TestRoutedMessage("CWW12345W: warning");
            // Matches H1 via "*I" only, and H2 via "*" and "*I"
            RoutedMessage levelOnly = new TestRoutedMessage("XXXX0001I: info");
            // Matches H2 via "*" only
            RoutedMessage routeAllOnly = new TestRoutedMessage("XXXX0001W: warning");

            setupWsLogHandlerExpectations(mockWsLogHandler,  tripleMatch);
            setupWsLogHandlerExpectations(mockWsLogHandler1, tripleMatch);
            setupWsLogHandlerExpectations(mockWsLogHandler,  prefixOnly);
            setupWsLogHandlerExpectations(mockWsLogHandler1, prefixOnly);
            setupWsLogHandlerExpectations(mockWsLogHandler,  levelOnly);
            setupWsLogHandlerExpectations(mockWsLogHandler1, levelOnly);
            setupWsLogHandlerExpectations(mockWsLogHandler1, routeAllOnly);
            // H1 must not receive routeAllOnly: no expectation

            assertTrue("CWW12345I should route and return true",  msgRouter.route(tripleMatch,  false));
            assertTrue("CWW12345W should route and return true",  msgRouter.route(prefixOnly,   false));
            assertTrue("XXXX0001I should route and return true",  msgRouter.route(levelOnly,    false));
            assertTrue("XXXX0001W should route and return true",  msgRouter.route(routeAllOnly, false));
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Verify that when two different handlers both subscribe to the same level-only wildcard
     * ("*I"), each handler receives every INFO-level message ID exactly once, and neither
     * receives other levels.
     */
    @Test
    public void testLevelOnlyWildcardWithTwoHandlersDeliversToEachOnce() {
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();
            msgRouter.setWsLogHandler("H1", mockWsLogHandler);
            msgRouter.setWsLogHandler("H2", mockWsLogHandler1);

            msgRouter.addMsgToLogHandler("*I", "H1");
            msgRouter.addMsgToLogHandler("*I", "H2");

            RoutedMessage infoMsg1 = new TestRoutedMessage("ABCD1234I: info one");
            RoutedMessage infoMsg2 = new TestRoutedMessage("WXYZ9876I: info two");
            RoutedMessage warnMsg  = new TestRoutedMessage("ABCD1234W: warning");

            setupWsLogHandlerExpectations(mockWsLogHandler,  infoMsg1);
            setupWsLogHandlerExpectations(mockWsLogHandler1, infoMsg1);
            setupWsLogHandlerExpectations(mockWsLogHandler,  infoMsg2);
            setupWsLogHandlerExpectations(mockWsLogHandler1, infoMsg2);
            // warnMsg: no expectation — neither handler may receive it

            assertTrue("Info message one should route and return true", msgRouter.route(infoMsg1, false));
            assertTrue("Info message two should route and return true", msgRouter.route(infoMsg2, false));
            assertTrue("Warning message should not route via *I",       msgRouter.route(warnMsg,  false));
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

    /**
     * Test the updateMessageListForHandler() flow in MessageRouterConfigurator (the path used
     * by internal consumers such as zosLogging) with a level-only wildcard:
     *
     *  - "*I,ABCD*" registers both a level-only and a prefix wildcard
     *  - "ABCD*" drops "*I", so INFO messages with other prefixes stop routing
     *  - ""       drops everything, so nothing routes
     */
    @Test
    public void testUpdateMessageListForHandlerWithLevelOnlyWildcard() throws Exception {
        try {
            // Initialize the test runner's mockery instance
            mockery = new JUnit4Mockery();
            BundleContext mockCtx = mockery.mock(BundleContext.class, "BundleContext");
            WsLogHandler mockHandler = mockery.mock(WsLogHandler.class, "ConfiguratorHandler");

            // Stub OSGi calls made during MessageRouterConfigurator initialization
            mockery.checking(new Expectations() {{
                allowing(mockCtx).addServiceListener(with(any(org.osgi.framework.ServiceListener.class)), with(any(String.class)));
                allowing(mockCtx).getServiceReferences(with(any(String.class)), with(aNull(String.class)));
                    will(returnValue(null));
                allowing(mockCtx).registerService(with(any(Class.class)), with(any(Object.class)), with(any(java.util.Dictionary.class)));
                    will(returnValue(null));
                allowing(mockCtx).addBundleListener(with(any(org.osgi.framework.BundleListener.class)));
                allowing(mockCtx).getBundles();
                    will(returnValue(new Bundle[0]));
            }});

            final WsMessageRouterImpl router = new WsMessageRouterImpl();
            router.setWsLogHandler("WTO", mockHandler);

            // Subclass MessageRouterConfigurator to inject our router instead of the singleton.
            MessageRouterConfigurator configurator = new MessageRouterConfigurator(mockCtx) {
                @Override
                protected WsMessageRouterImpl getMessageRouter() {
                    if (msgRouter == null) {
                        msgRouter = router;
                    }
                    return msgRouter;
                }
            };

            // ---------------------------------------------------------------
            // First call: level-only wildcard and prefix wildcard
            // ---------------------------------------------------------------
            configurator.updateMessageListForHandler("*I,ABCD*", "WTO");

            RoutedMessage otherInfo  = new TestRoutedMessage("XXXX0001I: INFO, matches *I");
            RoutedMessage abcdInfo   = new TestRoutedMessage("ABCD1234I: INFO, matches *I and ABCD*");
            RoutedMessage abcdWarn   = new TestRoutedMessage("ABCD1234W: matches ABCD* only");
            RoutedMessage otherWarn  = new TestRoutedMessage("XXXX0001W: matches nothing");

            setupWsLogHandlerExpectations(mockHandler, otherInfo);
            setupWsLogHandlerExpectations(mockHandler, abcdInfo);    // once, despite matching twice
            setupWsLogHandlerExpectations(mockHandler, abcdWarn);
            // otherWarn: no expectation — must NOT be called

            assertTrue(router.route(otherInfo, false));
            assertTrue(router.route(abcdInfo,  false));
            assertTrue(router.route(abcdWarn,  false));
            assertTrue(router.route(otherWarn, false));

            mockery.assertIsSatisfied();

            // ---------------------------------------------------------------
            // Second call: "*I" is removed, "ABCD*" stays
            // ---------------------------------------------------------------
            configurator.updateMessageListForHandler("ABCD*", "WTO");

            RoutedMessage otherInfo2 = new TestRoutedMessage("XXXX0002I: *I removed, should not route");
            RoutedMessage abcdWarn2  = new TestRoutedMessage("ABCD1234W: still routes via ABCD*");

            setupWsLogHandlerExpectations(mockHandler, abcdWarn2);
            // otherInfo2: no expectation — must NOT be called

            assertTrue(router.route(otherInfo2, false));
            assertTrue(router.route(abcdWarn2,  false));

            mockery.assertIsSatisfied();

            // ---------------------------------------------------------------
            // Third call: empty list removes everything
            // ---------------------------------------------------------------
            configurator.updateMessageListForHandler("", "WTO");

            RoutedMessage abcdWarn3  = new TestRoutedMessage("ABCD1234W: removed, should not route");
            RoutedMessage otherInfo3 = new TestRoutedMessage("XXXX0003I: removed, should not route");
            // No expectations: nothing may be published

            assertTrue(router.route(abcdWarn3,  false));
            assertTrue(router.route(otherInfo3, false));

            mockery.assertIsSatisfied();

        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }


    /**
     * Verify that wildcard subscriptions registered in the router do NOT affect
     * strict SPI LogHandler routing via MessageRouterImpl.route(String, LogRecord).
     *
     * Wildcard matching is an internal WsLogHandler feature; standard SPI LogHandlers
     * (com.ibm.wsspi.logging.LogHandler) must only receive messages matching exact ID
     * subscriptions. This covers both a prefix wildcard ("ABCD*") and a level-only
     * wildcard ("*I").
     */
    @Test
    public void testWildcardsDoNotAffectStrictSpiLogHandlers() {
        try {
            WsMessageRouterImpl msgRouter = getWsMessageRouterImpl();

            // Create an SPI LogHandler bound to the active mockery
            LogHandler spiHandler = mockery.mock(LogHandler.class, "SpiLogHandler");
            msgRouter.setLogHandler("SPI_HANDLER", spiHandler);

            // Register prefix and level-only wildcard patterns and an exact match pattern
            Properties props = new Properties();
            props.setProperty("ABCD*", "+SPI_HANDLER");
            props.setProperty("*I", "+SPI_HANDLER");
            props.setProperty("EXACT0001I", "+SPI_HANDLER");
            msgRouter.modified(props);

            String exactMsg = "EXACT0001I: Exact message for SPI handler";
            LogRecord exactRecord = new LogRecord(Level.INFO, exactMsg);

            String wildcardMsg = "ABCD1234I: Wildcard message that should NOT route to SPI handler";
            LogRecord wildcardRecord = new LogRecord(Level.INFO, wildcardMsg);

            // Matches only the level-only wildcard "*I", not "ABCD*"
            String levelOnlyMsg = "WXYZ9876I: Level-only wildcard message that should NOT route to SPI handler";
            LogRecord levelOnlyRecord = new LogRecord(Level.INFO, levelOnlyMsg);

            // Expect spiHandler to receive ONLY the exact message, NOT the wildcard message
            mockery.checking(new Expectations() {{
                oneOf(spiHandler).publish(with(equal(exactMsg)), with(equal(exactRecord)));
            }});

            // Exact message routes to SPI handler and returns true
            assertTrue("Exact message should route to SPI LogHandler", msgRouter.route(exactMsg, exactRecord));

            // Wildcard message must NOT be dispatched to SPI handler (no expectation set), returns true
            assertTrue("Wildcard message should not route to SPI LogHandler", msgRouter.route(wildcardMsg, wildcardRecord));

            // Level-only wildcard message must NOT be dispatched to SPI handler either, returns true
            assertTrue("Level-only wildcard message should not route to SPI LogHandler", msgRouter.route(levelOnlyMsg, levelOnlyRecord));
        } finally {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }
}

/**
 * Helper class, for test purposes only.
 */
class TestRoutedMessage implements RoutedMessage {
    private final String formattedMsg;
    private final LogRecord logRecord;

    public TestRoutedMessage(String formattedMsg) {
        this(formattedMsg, Level.INFO);
    }

    /**
     * @param recordLevel the level of the underlying LogRecord. This is independent of the
     *            level letter at the end of the message ID in formattedMsg, as in real
     *            Liberty messages. Use a level below INFO (e.g. Level.CONFIG) to simulate
     *            System.out/System.err echo records.
     */
    public TestRoutedMessage(String formattedMsg, Level recordLevel) {
        this.formattedMsg = formattedMsg;
        this.logRecord = new LogRecord(recordLevel, formattedMsg);
    }

    @Override
    public String getFormattedMsg() {
        return formattedMsg;
    }

    @Override
    public String getFormattedVerboseMsg() {
        return getFormattedMsg();
    }

    @Override
    public String getMessageLogFormat() {
        return getFormattedMsg();
    }

    @Override
    public LogRecord getLogRecord() {
        return logRecord;
    }
}
