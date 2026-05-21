package io.openliberty.microprofile.telemetry20.internal.test.rest;

import java.io.IOException;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

import io.openliberty.microprofile.telemetry20.internal.exporters.OpenLibertyMetricExporterProvider;

@Component(service = { RESTHandler.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM",
                                                                                                                             RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "="
                                                                                                                                                   + "/debugging",
                                                                                                                             RESTHandler.PROPERTY_REST_HANDLER_ROOT + "="
                                                                                                                                                                   + "/" })
public class MyRESTHandler implements RESTHandler {

    private static final TraceComponent tc = Tr.register(MyRESTHandler.class);

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {

    }

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) throws IOException {
        System.out.println("Trigger for updating SSL keys through mpTelemetry exporter");

        OpenLibertyMetricExporterProvider instance = OpenLibertyMetricExporterProvider.getInstance();

        instance.doSSlUpdate();

    }

}
