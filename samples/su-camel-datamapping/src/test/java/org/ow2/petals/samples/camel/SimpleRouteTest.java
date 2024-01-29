/**
 * Copyright (c) 2017-2024 Linagora
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
package org.ow2.petals.samples.camel;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;
import org.ow2.petals.ObjectFactory;
import org.ow2.petals.SayHello;
import org.ow2.petals.SayHelloResponse;
import org.ow2.petals.anothernamespace.SayHello2;
import org.ow2.petals.anothernamespace.SayHelloResponse2;
import org.ow2.petals.camel.helpers.MarshallingHelper;
import org.ow2.petals.camel.helpers.PetalsRouteBuilder;
import org.ow2.petals.camel.helpers.Step;
import org.ow2.petals.se.camel.junit.PetalsCamelTestSupport;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;

public class SimpleRouteTest extends PetalsCamelTestSupport {

    private final MarshallingHelper marshalling;

    public SimpleRouteTest() throws JAXBException {
        // enable tracing
        super(true);
        this.marshalling = new MarshallingHelper(JAXBContext.newInstance(org.ow2.petals.ObjectFactory.class,
                org.ow2.petals.anothernamespace.ObjectFactory.class));
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new SimpleRoute();
    }

    @Override
    protected Collection<String> routesToMock() {
        // for this to work, the route should either have a routeId set
        // or fromPetals should have been used (see route definition)
        return Arrays.asList(SimpleRoute.THE_PROVIDES_ID);
    }

    @Test
    public void test() throws Exception {
        // retrieve the mock endpoint created by PetalsCamelTestSupport for all petals to() in the route definition
        MockEndpoint mockTo = getTo(SimpleRoute.THE_CONSUMES_ID);
        mockTo.whenAnyExchangeReceived(new Step("Mock To") {
            @Override
            public void process(Exchange exchange) throws Exception {
                SayHello body = marshalling.unmarshal(exchange, SayHello.class);
                assertEquals("test", body.getArg0());

                final SayHelloResponse sayHelloResponse = new SayHelloResponse();
                sayHelloResponse.setReturn("ok!");
                JAXBElement<SayHelloResponse> response = new ObjectFactory().createSayHelloResponse(sayHelloResponse);
                marshalling.marshal(exchange, response);
            }
        });
        mockTo.expectedMessageCount(1);

        Exchange exchange = template().send(getFrom(SimpleRoute.THE_PROVIDES_ID), ExchangePattern.InOut,
                new Step("Prepare test") {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        SayHello2 sayHello = new SayHello2();
                        sayHello.setArg0("test");
                        marshalling.marshal(exchange, sayHello);
                    }
                });

        assertIsSatisfied(context);

        assertFalse(PetalsRouteBuilder.isJbiFailed(exchange));
        assertNotNull(exchange.getMessage().getBody());
        SayHelloResponse2 response = marshalling.unmarshal(exchange, SayHelloResponse2.class);
        assertNotNull(response);
        assertEquals("ok!", response.getReturn());
    }
}
