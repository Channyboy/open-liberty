-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.loggingTest-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-ShortName: loggingTest-1.0
Subsystem-Name: Logging Test 1.0
-features=io.openliberty.restHandler.internal-1.0, \
  io.openliberty.servlet.internal-6.0; ibm.tolerates:="6.1"
-bundles=com.ibm.ws.logging.osgi.test.feature
kind=noship
edition=core
