/**
 * Copyright (c) 2015 Linagora
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
 * along with this program/library; If not, see <http://www.gnu.org/licenses/>
 * for the GNU Lesser General Public License version 2.1.
 */
package org.ow2.petals.camel.component;

import java.net.URI;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.converter.jaxp.XmlConverter;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.ExchangeTestSupport;
import org.easymock.EasyMockSupport;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Assert;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.PetalsCamelContext;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;
import org.ow2.petals.camel.component.mocks.PetalsCamelContextMock;
import org.ow2.petals.camel.component.mocks.PetalsCamelContextMock.MockSendHandler;
import org.ow2.petals.camel.component.mocks.ServiceEndpointOperationMock;

public class PetalsCamelTestSupport extends ExchangeTestSupport {

    /**
     * Converters from Camel
     */
    protected static final XmlConverter CONVERTER = new XmlConverter();

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
    protected void postProcessTest() throws Exception {
        // this method is executed after the context has been created and before the PCC is needed from the registry (to
        // instantiate mock endpoint and stuffs)

        this.pcc = new PetalsCamelContextMock(context());

        context().getRegistry(JndiRegistry.class).bind(PetalsCamelContext.class.getName(), this.pcc);
        this.initializeServices();

        super.postProcessTest();
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

    protected ServiceEndpointOperation createMockSEO(final ServiceType type) {
        return this.createMockSEO(type, MEPPatternConstants.IN_OUT.value());
    }

    protected ServiceEndpointOperation createMockSEO(final ServiceType type, final URI pattern) {
        return new ServiceEndpointOperationMock("Service", "Interface", "endpoint", "operation", type, pattern);
    }
    
    protected PetalsCamelEndpoint createEndpoint(final String serviceId) {
        Endpoint endpoint = context().getEndpoint("petals:" + serviceId);
        Assert.assertNotNull(endpoint);
        Assert.assertTrue(endpoint instanceof PetalsCamelEndpoint);
        return (PetalsCamelEndpoint) endpoint;
    }

    protected Processor emptyProcessor() {
        return new Processor() {
            @Override
            public void process(@Nullable Exchange exchange) throws Exception {

            }
        };
    }
}
