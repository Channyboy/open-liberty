-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpMetrics-4.0
visibility=public
singleton=true
IBM-API-Package: org.eclipse.microprofile.metrics.annotation;  type="stable", \
 org.eclipse.microprofile.metrics; type="stable"
IBM-ShortName: mpMetrics-4.0
Subsystem-Name: MicroProfile Metrics 4.0
-features=io.openliberty.org.eclipse.microprofile.metrics-3.0, \
 com.ibm.websphere.appserver.cdi-2.0,\
 com.ibm.websphere.appserver.javax.annotation-1.3, \
 com.ibm.websphere.appserver.restHandler-1.0, \
 com.ibm.websphere.appserver.monitor-1.0, \
 com.ibm.websphere.appserver.servlet-4.0,\
 com.ibm.websphere.appserver.mpConfig-2.0,\
 com.ibm.websphere.appserver.internal.slf4j-1.7.7
-bundles=com.ibm.ws.microprofile.metrics.common, \
 io.openliberty.smallrye.metrics, \
 io.openliberty.io.smallrye.common, \
 com.ibm.ws.org.jboss.logging, \
 io.openliberty.microprofile.metrics.internal.4.0, \
 io.openliberty.microprofile.metrics.internal.private.4.0, \
 io.openliberty.microprofile.metrics.internal.public.4.0,\
 io.openliberty.micrometer
kind=noship
edition=core