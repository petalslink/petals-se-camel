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
package org.ow2.petals.samples.camel;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spi.DataFormat;
import org.ow2.petals.ObjectFactory;
import org.ow2.petals.SayHello;
import org.ow2.petals.SayHelloResponse;
import org.ow2.petals.anothernamespace.SayHello2;
import org.ow2.petals.anothernamespace.SayHelloResponse2;
import org.ow2.petals.camel.helpers.PetalsRouteBuilder;

import jakarta.xml.bind.JAXBContext;

public class SimpleRoute extends PetalsRouteBuilder {

    public static final String THE_CONSUMES_ID = "theConsumesId";

    public static final String THE_PROVIDES_ID = "theProvidesId";

    private static final String CAMEL_LOG = "log:org.ow2.petals.samples.camel?level=ERROR&showStreams=true&showAll=true";

    @Override
    public void configure() throws Exception {
        // it is also possible to use org.ow2.petals.camel.helpers.MarshallingHelper
        final DataFormat jaxb = new JaxbDataFormat(JAXBContext.newInstance(
                org.ow2.petals.ObjectFactory.class,
                org.ow2.petals.anothernamespace.ObjectFactory.class));

        fromPetals(THE_PROVIDES_ID).streamCaching()
                .to(CAMEL_LOG)
                .unmarshal(jaxb)
                .to(CAMEL_LOG)
                .bean(Normalizer.class, "transformIn")
                .to(CAMEL_LOG)
                .marshal(jaxb)
                .to(CAMEL_LOG)
                .to("petals:" + THE_CONSUMES_ID)
                .to(CAMEL_LOG)
                .unmarshal(jaxb)
                .bean(Normalizer.class, "transformOut")
                .to(CAMEL_LOG)
                .marshal(jaxb)
                .to(CAMEL_LOG);
    }

    public static class Normalizer {

        public void transformIn(Exchange exchange, @Body SayHello2 body) {
            final SayHello sayHello = new SayHello();
            sayHello.setArg0(body.getArg0());
            exchange.getMessage().setBody(new ObjectFactory().createSayHello(sayHello));
        }

        public void transformOut(Exchange exchange, @Body SayHelloResponse body) {
            final SayHelloResponse2 response = new SayHelloResponse2();
            response.setReturn(body.getReturn());
            exchange.getMessage().setBody(response);
        }
    }

}
