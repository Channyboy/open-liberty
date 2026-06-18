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
package com.ibm.ws.ssl.fat.listener;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test SSL configuration change notifications when keystore files are updated.
 * This test verifies that registered SSLConfigChangeListeners are notified when
 * a keystore file is modified and the updateTrigger="polled" mechanism detects the change.
 */
@RunWith(FATRunner.class)
public class SSLKeystoreUpdateTest {

    private static final Class<?> c = SSLKeystoreUpdateTest.class;
    private static final String APP_NAME = "keystoreUpdateListener";
    private static final String SERVER_NAME = "KeystoreUpdateServer";
    private static final String SSL_ALIAS = "testSSLConfig";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static File keystoreFile;
    private static File swapKeystoreFile;

    @BeforeClass
    public static void setUp() throws Exception {
        // Deploy the test application
        //ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.ssl.fat.keystore.app");
        
        WebArchive testWAR = ShrinkWrap
                .create(WebArchive.class, APP_NAME+".war")
                .addPackage(
                            "com.ibm.ws.ssl.fat.keystore.app");

        ShrinkHelper.exportDropinAppToServer(server, testWAR,
                                     DeployOptions.SERVER_ONLY);

        // Generate two different keystores for testing
        generateKeystores();

        // Start the server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Generate two keystores using the securityUtility command.
     * Create the second keystore first, move it to swap directory,
     * then create the first keystore which will be used initially.
     */
    private static void generateKeystores() throws Exception {
        String securityUtility = server.getInstallRoot() + "/bin/securityUtility";
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            securityUtility = securityUtility + ".bat";
        }

        Properties env = new Properties();

        // Create the second keystore first (will be swapped in later)
        String[] cmd2 = new String[] {
                                       "createSSLCertificate",
                                       "--server=" + server.getServerName(),
                                       "--password=Liberty",
                                       "--subject=CN=localhost,OU=Test2,O=IBM,C=US"
        };

        ProgramOutput output2 = server.getMachine().execute(securityUtility, cmd2, server.getInstallRoot(), env);

        Log.info(c, "generateKeystores", "Second keystore creation - RC: " + output2.getReturnCode()
                                         + "\nstdout:\n" + output2.getStdout()
                                         + "\nstderr:\n" + output2.getStderr());

        if (output2.getReturnCode() != 0) {
            throw new IllegalStateException("Failed to create second keystore, RC=" + output2.getReturnCode());
        }

        keystoreFile = new File(server.getServerRoot() + "/resources/security/key.p12");
        if (!keystoreFile.exists()) {
            throw new IllegalStateException("Second keystore was not created: " + keystoreFile.getAbsolutePath());
        }

        // Create swap directory and move second keystore there
        File swapDir = new File(server.getServerRoot() + "/resources/swap");
        if (!swapDir.exists()) {
            swapDir.mkdirs();
        }
        swapKeystoreFile = new File(swapDir, "key.p12");
        Files.move(keystoreFile.toPath(), swapKeystoreFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Now create the first keystore (will be used initially)
        String[] cmd1 = new String[] {
                                       "createSSLCertificate",
                                       "--server=" + server.getServerName(),
                                       "--password=Liberty",
                                       "--subject=CN=localhost,OU=Test1,O=IBM,C=US"
        };

        ProgramOutput output1 = server.getMachine().execute(securityUtility, cmd1, server.getInstallRoot(), env);

        Log.info(c, "generateKeystores", "First keystore creation - RC: " + output1.getReturnCode()
                                         + "\nstdout:\n" + output1.getStdout()
                                         + "\nstderr:\n" + output1.getStderr());

        if (output1.getReturnCode() != 0) {
            throw new IllegalStateException("Failed to create first keystore, RC=" + output1.getReturnCode());
        }

        if (!keystoreFile.exists()) {
            throw new IllegalStateException("First keystore was not created: " + keystoreFile.getAbsolutePath());
        }

        // Extract the encrypted password and update server.xml
        String encryptedPassword = extractPasswordValue(output1.getStdout());
        updateServerXmlWithPassword(encryptedPassword);

        Log.info(c, "generateKeystores", "Generated keystores:\n  Primary: " + keystoreFile.getAbsolutePath()
                                         + "\n  Swap: " + swapKeystoreFile.getAbsolutePath());
    }

    /**
     * Extract the password value from securityUtility output.
     * Example output line: <keyStore id="defaultKeyStore" password="{xor}Piw7OTg3" />
     * We want to extract: {xor}Piw7OTg3
     */
    private static String extractPasswordValue(String stdout) {
        Pattern pattern = Pattern.compile("password=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(stdout);
        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new IllegalStateException("Unable to find password value in securityUtility output:\n" + stdout);
    }

    /**
     * Update the server.xml file to add the password attribute to the testKeyStore element.
     */
    private static void updateServerXmlWithPassword(String encryptedPassword) throws Exception {
        File serverXmlFile = new File(server.getServerRoot() + "/server.xml");
        
        // Read the current server.xml content
        String content = new String(Files.readAllBytes(serverXmlFile.toPath()), StandardCharsets.UTF_8);
        
        // Replace the keyStore element to add the password attribute
        // Match: <keyStore location="${server.config.dir}/resources/security/key.p12" id="testKeyStore" updateTrigger="polled" pollingRate="500ms"/>
        // Replace with the password attribute inserted
        String updatedContent = content.replaceFirst(
            "(<keyStore[^>]+id=\"testKeyStore\"[^>]*)(/>)",
            "$1 password=\"" + encryptedPassword + "\" $2"
        );
        
        // Verify the replacement was successful
        if (content.equals(updatedContent)) {
            throw new IllegalStateException("Failed to update server.xml - testKeyStore element not found or already has password");
        }
        
        // Write the updated content back to server.xml
        Files.write(serverXmlFile.toPath(), updatedContent.getBytes(StandardCharsets.UTF_8));
        
        Log.info(c, "updateServerXmlWithPassword", "Updated server.xml with encrypted password: " + encryptedPassword);
    }

    /**
     * Test that SSLConfigChangeListener receives notification when the keystore file
     * is updated and the polled updateTrigger detects the change.
     * 
     * Steps:
     * 1. Register listener for testSSLConfig
     * 2. Swap the keystore file (replace key.p12 with the alternate version)
     * 3. Wait for polling to detect the change
     * 4. Verify listener received notification in logs
     */
    @Test
    public void testKeystoreFileUpdateNotification() throws Exception {
        // Register the listener
        String response = HttpUtils.getHttpResponseAsString(server,
                                                            "/" + APP_NAME + "/keystoreUpdateListener/register/" + SSL_ALIAS);
        assertNotNull("Failed to register listener", response);
        assertTrue("Registration failed: " + response, response.contains("Registered listener"));

        // Mark the log for searching
        server.setMarkToEndOfLog();

        // Swap the keystore file
        Log.info(c, "testKeystoreFileUpdateNotification", "Swapping keystore file...");
        Files.copy(swapKeystoreFile.toPath(), keystoreFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Update the file's last modified time to ensure it's detected as changed
        keystoreFile.setLastModified(System.currentTimeMillis());

        Log.info(c, "testKeystoreFileUpdateNotification", "Keystore swapped, waiting for notification...");

        // Wait for the audit message that keystore was modified
        assertNotNull("Keystore modification Audit message not found",
                      server.waitForStringInLogUsingMark("CWPKI0811I", 30000));

        // Verify the listener was notified
        assertNotNull("KeystoreUpdateListener notification not found in logs",
                      server.waitForStringInLogUsingMark("KeystoreUpdateListener notification received for alias: " + SSL_ALIAS, 5000));

        // Cleanup: deregister the listener
        HttpUtils.getHttpResponseAsString(server,
                                          "/" + APP_NAME + "/keystoreUpdateListener/deregister");
    }
}

// Made with Bob