/**
 * Copyright (c) 2015-2017 Linagora
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

import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.ResolveEndpointFailedException;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;
import org.ow2.petals.camel.component.exceptions.IncompatibleEndpointUsageException;
import org.ow2.petals.camel.component.exceptions.InvalidURIException;
import org.ow2.petals.camel.component.mocks.ServiceEndpointOperationMock;
import org.ow2.petals.camel.exceptions.UnknownServiceException;

public class PetalsCamelComponentTest extends CamelPetalsTestSupport {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testCreateProvidesEndpoint_OK() {
        addMockProvides("serviceId1");
        createEndpoint("serviceId1");
        // synchronous must be recognised
        createEndpoint("serviceId1?synchronous=true");
    }

    @Test
    public void testCreateProvidesEndpoint_KO1() {
        addMockProvides("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectCause(CoreMatchers.isA(UnknownServiceException.class));
        createEndpoint("serviceId2");
    }

    @Test
    public void testCreateProvidesEndpoint_KO2() {
        addMockProvides("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectMessage("The parameter timeout can't be set on a from() endpoint");
        // timeout is not authorised for provides
        createEndpoint("serviceId1?timeout=5");
    }

    @Test
    public void testCreateProvidesEndpoint_KO3() {
        addMockProvides("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectMessage("Unknown parameters");
        createEndpoint("serviceId1?wrong=true");
    }

    @Test
    public void testCreateProvidesEndpoint_KO4() {
        addMockProvides("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectMessage("The parameter serviceName can't be set on a from() endpoint");
        createEndpoint("serviceId1?serviceName={ns}name");
    }

    @Test
    public void testCreateProvidesEndpoint_KO5() {
        addMockProvides("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectMessage("The parameter endpointName can't be set on a from() endpoint");
        createEndpoint("serviceId1?endpointName=name");
    }

    @Test
    public void testCreateProvidesEndpoint_KO6() {
        addMockProvides("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectMessage("The parameter operation can't be set on a from() endpoint");
        createEndpoint("serviceId1?operation={ns}name");
    }

    @Test
    public void testCreateProvidesEndpoint_KO7() {
        addMockProvides("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectMessage("The parameter exchangePattern can't be set on a from() endpoint");
        createEndpoint("serviceId1?exchangePattern=InOut");
    }

    @Test
    public void testCreateConsumesEndpoint_OK() {
        addMockConsumes("serviceId1");
        createEndpoint("serviceId1");
        // synchronous must be recognised
        createEndpoint("serviceId1?synchronous=true");
        // timeout must be recognised
        createEndpoint("serviceId1?timeout=5");
        createEndpoint("serviceId1?synchronous=true&timeout=5");
    }

    @Test
    public void testCreateConsumesEndpoint_NoService_OK() {
        pcc().addMockService("serviceId1", new ServiceEndpointOperationMock(null, "Interface", "endpoint", "operation",
                ServiceType.CONSUMES, MEPPatternConstants.IN_OUT.value()));
        createEndpoint("serviceId1");
        createEndpoint("serviceId1?serviceName={ns}name");
    }

    @Test
    public void testCreateConsumesEndpoint_NoService_KO() {
        pcc().addMockService("serviceId1", new ServiceEndpointOperationMock(null, "Interface", "endpoint", "operation",
                ServiceType.CONSUMES, MEPPatternConstants.IN_OUT.value()));
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectCause(CoreMatchers.isA(IllegalArgumentException.class));
        createEndpoint("serviceId1?serviceName={name");
    }
    
    @Test
    public void testCreateConsumesEndpoint_NoEndpoint_OK() {
        pcc().addMockService("serviceId1", new ServiceEndpointOperationMock("Service", "Interface", null, "operation",
                ServiceType.CONSUMES, MEPPatternConstants.IN_OUT.value()));
        createEndpoint("serviceId1");
        createEndpoint("serviceId1?endpointName=name");
    }

    @Test
    public void testCreateConsumesEndpoint_NoEndpoint_OK2() {
        // no endpoint, no service, need service parameter!
        pcc().addMockService("serviceId1", new ServiceEndpointOperationMock(null, "Interface", null, "operation",
                ServiceType.CONSUMES, MEPPatternConstants.IN_OUT.value()));
        createEndpoint("serviceId1?serviceName={ns}name&endpointName=name");
    }

    @Test
    public void testCreateConsumesEndpoint_NoEndpoint_KO() {
        // no endpoint, no service, need service parameter!
        pcc().addMockService("serviceId1", new ServiceEndpointOperationMock(null, "Interface", null, "operation",
                ServiceType.CONSUMES, MEPPatternConstants.IN_OUT.value()));
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectMessage("if the endpoint nor the corresponding Consumes declares a service name");
        createEndpoint("serviceId1?endpointName=name");
    }

    @Test
    public void testCreateConsumesEndpoint_NoOperation_OK() {
        pcc().addMockService("serviceId1", new ServiceEndpointOperationMock("Service", "Interface", "endpoint", null,
                ServiceType.CONSUMES, MEPPatternConstants.IN_OUT.value()));
        createEndpoint("serviceId1");
        createEndpoint("serviceId1?operation={ns}name");
    }

    @Test
    public void testCreateConsumesEndpoint_NoOperation_KO() {
        pcc().addMockService("serviceId1", new ServiceEndpointOperationMock("Service", "Interface", "endpoint", null,
                ServiceType.CONSUMES, MEPPatternConstants.IN_OUT.value()));
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectCause(CoreMatchers.isA(IllegalArgumentException.class));
        createEndpoint("serviceId1?operation={name");
    }

    @Test
    public void testCreateConsumesEndpoint_NoMEP_OK() {
        pcc().addMockService("serviceId1", new ServiceEndpointOperationMock("Service", "Interface", "endpoint",
                "operation", ServiceType.CONSUMES, null));
        createEndpoint("serviceId1");
        createEndpoint("serviceId1?exchangePattern=InOut");
    }

    @Test
    public void testCreateConsumesEndpoint_NoMEP_KO1() {
        pcc().addMockService("serviceId1", new ServiceEndpointOperationMock("Service", "Interface", "endpoint",
                "operation", ServiceType.CONSUMES, null));
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectCause(CoreMatchers.isA(IllegalArgumentException.class));
        createEndpoint("serviceId1?exchangePattern=notGood");
    }

    @Test
    public void testCreateConsumesEndpoint_KO1() {
        addMockConsumes("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectCause(CoreMatchers.isA(UnknownServiceException.class));
        createEndpoint("serviceId2");
    }

    @Test
    public void testCreateConsumesEndpoint_KO2() {
        addMockConsumes("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectMessage("Unknown parameters");
        createEndpoint("serviceId1?wrong=true");
    }

    @Test
    public void testCreateConsumesEndpoint_KO3() {
        addMockConsumes("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectMessage("corresponding Consumes already declares a service");
        createEndpoint("serviceId1?serviceName={ns}name");
    }

    @Test
    public void testCreateConsumesEndpoint_KO4() {
        addMockConsumes("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectMessage("corresponding Consumes already declares an operation");
        createEndpoint("serviceId1?operation={ns}name");
    }

    @Test
    public void testCreateConsumesEndpoint_KO5() {
        addMockConsumes("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectMessage("corresponding Consumes already declares an endpoint name");
        createEndpoint("serviceId1?endpointName=name");
    }

    @Test
    public void testCreateConsumesEndpoint_KO6() {
        addMockConsumes("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectMessage("corresponding Consumes already declares a MEP");
        createEndpoint("serviceId1?exchangePattern=InOut");
    }

    @Test
    public void testCreateConsumesEndpoint_KO_URI1() {
        addMockConsumes("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectMessage("No component found with scheme");
        context().getEndpoint("petalsA:serviceId1");
    }

    @Test
    public void testCreateConsumesEndpoint_KO_URI2() {
        addMockConsumes("serviceId1");
        thrown.expect(ResolveEndpointFailedException.class);
        thrown.expectCause(CoreMatchers.isA(InvalidURIException.class));
        context().getEndpoint("petals:serviceId1$$");
    }

    @Test
    public void testCreateProducer_OK() throws Exception {
        // camel producers are associated to petals consumes
        addMockConsumes("serviceId1");
        Endpoint endpoint = createEndpoint("serviceId1");
        Producer producer = endpoint.createProducer();
        assertNotNull(producer);
        assertTrue(producer instanceof PetalsCamelProducer);
    }

    @Test(expected = IncompatibleEndpointUsageException.class)
    public void testCreateProducer_KO() throws Exception {
        addMockProvides("serviceId1");
        Endpoint endpoint = createEndpoint("serviceId1");
        endpoint.createProducer();
    }

    @Test
    public void testCreateConsumer_OK() throws Exception {
        // camel consumers are associated to petals provides
        addMockProvides("serviceId1");
        Endpoint endpoint = createEndpoint("serviceId1");
        Consumer consumer = endpoint.createConsumer(emptyProcessor());
        assertNotNull(consumer);
        assertTrue(consumer instanceof PetalsCamelConsumer);
    }

    @Test(expected = IncompatibleEndpointUsageException.class)
    public void testCreateConsumer_KO() throws Exception {
        addMockConsumes("serviceId1");
        Endpoint endpoint = createEndpoint("serviceId1");
        endpoint.createConsumer(emptyProcessor());
    }
}
