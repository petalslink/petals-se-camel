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

import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.ResolveEndpointFailedException;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.ow2.petals.camel.component.exceptions.IncompatibleEndpointUsageException;
import org.ow2.petals.camel.component.exceptions.InvalidURIException;
import org.ow2.petals.camel.exceptions.UnknownServiceException;

public class PetalsCamelComponentTest extends PetalsCamelTestSupport {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testCreateProvidesEndpoint() {
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
        thrown.expectMessage("Unknown parameters");
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
    public void testCreateConsumesEndpoint() {
        addMockConsumes("serviceId1");
        createEndpoint("serviceId1");
        // synchronous must be recognised
        createEndpoint("serviceId1?synchronous=true");
        // timeout must be recognised
        createEndpoint("serviceId1?timeout=5");
        createEndpoint("serviceId1?synchronous=true&timeout=5");
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
        Assert.assertNotNull(producer);
        Assert.assertTrue(producer instanceof PetalsCamelProducer);
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
        Assert.assertNotNull(consumer);
        Assert.assertTrue(consumer instanceof PetalsCamelConsumer);
    }

    @Test(expected = IncompatibleEndpointUsageException.class)
    public void testCreateConsumer_KO() throws Exception {
        addMockConsumes("serviceId1");
        Endpoint endpoint = createEndpoint("serviceId1");
        endpoint.createConsumer(emptyProcessor());
    }
}
