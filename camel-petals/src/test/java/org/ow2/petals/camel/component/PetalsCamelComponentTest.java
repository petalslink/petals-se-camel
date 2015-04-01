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
import org.junit.Assert;
import org.junit.Test;
import org.ow2.petals.camel.component.exceptions.IncompatibleEndpointUsageException;
import org.ow2.petals.camel.component.exceptions.InvalidURIException;
import org.ow2.petals.camel.exceptions.UnknownServiceException;

public class PetalsCamelComponentTest extends PetalsCamelTestSupport {

    @Test
    public void testCreateProvidesEndpoint() {
        addMockProvides("serviceId1");
        createEndpoint("serviceId1");
        // synchronous must be recognised
        createEndpoint("serviceId1?synchronous=true");
    }

    @Test
    public void testCreateProvidesEndpoint_KO() {
        addMockProvides("serviceId1");
        try {
            createEndpoint("serviceId2");
            fail();
        } catch (ResolveEndpointFailedException e) {
            assertTrue(e.getCause() instanceof UnknownServiceException);
        }
        try {
            // timeout is not authorised for provides
            createEndpoint("serviceId1?timeout=5");
            fail();
        } catch (ResolveEndpointFailedException e) {
            assertTrue(e.getMessage().contains("Unknown parameters"));
        }
        try {
            createEndpoint("serviceId1?wrong=true");
            fail();
        } catch (ResolveEndpointFailedException e) {
            assertTrue(e.getMessage().contains("Unknown parameters"));
        }
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
    public void testCreateConsumesEndpoint_KO() {
        addMockConsumes("serviceId1");
        try {
            createEndpoint("serviceId2");
            fail();
        } catch (ResolveEndpointFailedException e) {
            assertTrue(e.getCause() instanceof UnknownServiceException);
        }
        try {
            createEndpoint("serviceId1?wrong=true");
            fail();
        } catch (ResolveEndpointFailedException e) {
            assertTrue(e.getMessage().contains("Unknown parameters"));
        }
    }

    @Test
    public void testCreateConsumesEndpoint_KO_URI() {
        addMockConsumes("serviceId1");
        try {
            context().getEndpoint("petalsA:serviceId1");
            fail();
        } catch (ResolveEndpointFailedException e) {
            assertTrue(e.getMessage().contains("No component found with scheme"));
        }
        try {
            context().getEndpoint("petals:serviceId1$$");
            fail();
        } catch (ResolveEndpointFailedException e) {
            assertTrue(e.getCause() instanceof InvalidURIException);
        }
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
