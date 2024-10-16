/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.CommonTests.JaxRSClientRequestFilterTests;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will run basic OpenID Connect RP tests.
 * This test class extends GenericRPTests.
 * GenericRPTests contains common code for all RP tests.
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
@RunWith(FATRunner.class)
public class OidcJaxRSClientRequestFilterTests extends JaxRSClientRequestFilterTests {

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        thisClass = OidcJaxRSClientRequestFilterTests.class;

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.HELLOWORLD_SERVLET);
            }
        };

        testSettings = new TestSettings();

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(Constants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = Constants.SIGALG_HS256; // stick with HS256
        Log.info(thisClass, "setUp", "inited tokenType to: " + tokenType);

        // Start the Generic/App Server
        genericTestServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.rs", "rs_server_clientRequestFilter.xml", Constants.GENERIC_SERVER, apps,
                                        Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.op", "op_server_clientRequestFilter.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS,
                                   Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, null, null, true, true, tokenType, certType);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.rp", "rp_server_clientRequestFilter.xml", Constants.OIDC_RP, Constants.NO_EXTRA_APPS,
                                   Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        // override actions that generic tests should use - Need to skip consent form as httpunit
        // cannot process the form because of embedded javascript

        test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
        test_FinalAction = Constants.LOGIN_USER;
        testSettings.setFlowType(Constants.RP_FLOW);
        testSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/Protected_JaxRSClient_Injecter");
        testSettings.setClientID("client01_injecter");
        testSettings.setScope("openid profile");

        Map<String, String> map = new HashMap<String, String>();
        map.put(Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld");
        map.put(Constants.WHERE, testSettings.getWhere());
        map.put(Constants.TOKEN_CONTENT, Constants.API_VALUE);
        map.put(Constants.CONTEXT_SET, "true");
        testSettings.setRequestParms(map);

        if (testSettings.getRsTokenType().equals(Constants.JWT_TOKEN)) {
            OP_Issues_JWT = true;
            Default_Issuer = OP_As_Issuer;
        } else {
            OP_Issues_JWT = false;
            Default_Issuer = Default_Jwt_Issuer;
        }
    }

}
