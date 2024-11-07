-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpHealth.4.0.ee-9.0
singleton=true
-features=\
 io.openliberty.org.eclipse.microprofile.health-4.0,\
 io.openliberty.mpConfig-2.0,\
 io.openliberty.jsonp-1.2,\
 io.openliberty.cdi-2.0,\
 io.openliberty.mpCompatible-4.0,\
 com.ibm.websphere.appserver.eeCompatible-8.0
-bundles=\
  io.openliberty.microprofile.health.3.1.internal; apiJar=false; location:="lib/"
kind=noship
edition=full
WLP-Activation-Type: parallel 
