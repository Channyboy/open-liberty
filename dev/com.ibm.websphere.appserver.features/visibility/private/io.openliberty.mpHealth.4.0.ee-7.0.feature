-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpHealth.4.0.ee-9.0
singleton=true
-features=\
 io.openliberty.javax.org.eclipse.microprofile.health-4.0,\
 io.openliberty.mpConfig-1.2,\
 io.openliberty.jsonp-1.1,\
 io.openliberty.cdi-1.2,\
 io.openliberty.mpCompatible-0.0,\
 com.ibm.websphere.appserver.eeCompatible-7.0
-bundles=\
  io.openliberty.microprofile.health.3.1.internal; apiJar=false; location:="lib/"
kind=noship
edition=full
WLP-Activation-Type: parallel 
