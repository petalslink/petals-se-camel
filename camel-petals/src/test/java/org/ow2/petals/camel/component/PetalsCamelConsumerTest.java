/**
 * Copyright (c) 2015-2020 Linagora
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

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Test;
import org.ow2.petals.camel.helpers.PetalsRouteBuilder;
import org.ow2.petals.component.framework.api.message.Exchange;

public class PetalsCamelConsumerTest extends CamelPetalsTestSupport {

    @Override
    protected void initializeServices() {
        super.initializeServices();
        addMockProvides("serviceId1");
    }

    @EndpointInject(uri = "mock:result")
    @Nullable
    protected MockEndpoint resultEndpoint;

    protected MockEndpoint resultEndpoint() {
        assert resultEndpoint != null;
        return resultEndpoint;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new PetalsRouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromPetals("serviceId1").to("mock:result");
            }
        };
    }

    @Test
    public void testSend() throws Exception {
        final String content = "<aa/>";

        expectBodyReceived(resultEndpoint(), content);

        final Exchange petalsExchange = pcc().createExchange("serviceId1", content);

        pcc().process("serviceId1", petalsExchange);

        resultEndpoint().assertIsSatisfied();
    }
}
