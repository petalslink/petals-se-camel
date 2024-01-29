/**
 * Copyright (c) 2015-2024 Linagora
 * 
 * This program/library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or (at your
 * option) any later version.
 * 
 * This program/library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program/library; If not, see http://www.gnu.org/licenses/
 * for the GNU Lesser General Public License version 2.1.
 */
package org.ow2.petals.camel.component;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.jbi.messaging.NormalizedMessage;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.custommonkey.xmlunit.Diff;
import org.easymock.EasyMockSupport;
import org.eclipse.jdt.annotation.Nullable;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.PetalsCamelContext;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;
import org.ow2.petals.camel.component.mocks.PetalsCamelContextMock;
import org.ow2.petals.camel.component.mocks.PetalsCamelContextMock.MockSendHandler;
import org.ow2.petals.camel.component.mocks.ServiceEndpointOperationMock;
import org.xml.sax.SAXException;

public abstract class CamelPetalsTestSupport extends CamelTestSupport {

    protected static final String TEST_INTERFACE_NAME = "Interface";
    protected static final String TEST_SERVICE_NAME = "Service";
    protected static final String TEST_ENDPOINT_NAME = "endpoint";
    protected static final String TEST_OPERATION_NAME = "operation";

    protected final EasyMockSupport easyMock = new EasyMockSupport();

    private @Nullable PetalsCamelContextMock pcc;

    protected PetalsCamelContextMock pcc() {
        assert pcc != null;
        return pcc;
    }

    /**
     * Override to initialize services in the PCC
     */
    protected void initializeServices() {
        // empty
    }

    @Override
    protected void doPreSetup() throws Exception {
        System.setProperty("skipStartingCamelContext", "true");
    }

    @Override
    protected void doPostSetup() throws Exception {
        // this method is executed after the context has been created and before the PCC is needed from the registry (to
        // instantiate mock endpoint and stuffs) at context start
        this.pcc = new PetalsCamelContextMock(context());

        context().getRegistry().bind(PetalsCamelContext.class.getName(), this.pcc);
        this.initializeServices();

        context().start();
    }

    protected ServiceEndpointOperation addMockConsumes(final String serviceId, final MockSendHandler handler) {
        final ServiceEndpointOperation seo = createMockSEO(ServiceType.CONSUMES);
        pcc().addMockService(serviceId, seo, handler);
        return seo;
    }

    protected ServiceEndpointOperation addMockConsumes(final String serviceId) {
        final ServiceEndpointOperation seo = createMockSEO(ServiceType.CONSUMES);
        pcc().addMockService(serviceId, seo);
        return seo;
    }

    protected ServiceEndpointOperation addMockProvides(final String serviceId) {
        final ServiceEndpointOperation seo = createMockSEO(ServiceType.PROVIDES);
        pcc().addMockService(serviceId, seo);
        return seo;
    }

    protected ServiceEndpointOperation addMockProvides(final String serviceId, final MockSendHandler handler) {
        final ServiceEndpointOperation seo = createMockSEO(ServiceType.PROVIDES);
        pcc().addMockService(serviceId, seo, handler);
        return seo;
    }

    protected static ServiceEndpointOperation createMockSEO(final ServiceType type) {
        return createMockSEO(type, MEPPatternConstants.IN_OUT.value());
    }

    protected static ServiceEndpointOperation createMockSEO(final ServiceType type, final URI pattern) {
        return new ServiceEndpointOperationMock(TEST_SERVICE_NAME, TEST_INTERFACE_NAME, TEST_ENDPOINT_NAME,
                TEST_OPERATION_NAME, type, pattern);
    }

    protected PetalsCamelEndpoint createEndpoint(final String serviceId) {
        Endpoint endpoint = context().getEndpoint("petals:" + serviceId);
        assertNotNull(endpoint);
        assertInstanceOf(PetalsCamelEndpoint.class, endpoint);
        return (PetalsCamelEndpoint) endpoint;
    }

    protected Processor emptyProcessor() {
        return new Processor() {
            @Override
            public void process(@Nullable Exchange exchange) throws Exception {

            }
        };
    }

    protected void expectBodyReceived(final MockEndpoint endpoint, final String content) {
        endpoint.expects(new Runnable() {
            public void run() {
                List<org.apache.camel.Exchange> receivedExchanges = endpoint.getReceivedExchanges();
                org.apache.camel.Exchange exchange = receivedExchanges.iterator().next();
                assert exchange != null;

                try {
                    assertSimilar(new Diff(content, getContent(exchange.getIn())));
                } catch (final SAXException | IOException e) {
                    throw new RuntimeCamelException(e);
                }
            }
        });
    }

    protected void assertSimilar(final Diff diff) {
        assertTrue(diff.similar(), diff.toString());
    }

    protected String getContent(final NormalizedMessage msg) {
        return context().getTypeConverter().convertTo(String.class, msg.getContent());
    }

    protected String getContent(final Message msg) {
        return msg.getBody(String.class);
    }
}
