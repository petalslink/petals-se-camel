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
package org.ow2.petals.samples.camel;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spi.DataFormat;
import org.ow2.petals.SayHello;
import org.ow2.petals.SayHelloResponse;
import org.ow2.petals.anothernamespace.ObjectFactory;
import org.ow2.petals.anothernamespace.SayHello2;
import org.ow2.petals.anothernamespace.SayHelloResponse2;

public class SimpleRoute extends RouteBuilder {

    private static final String CAMEL_LOG = "log:org.ow2.petals.samples.camel?level=ERROR&showStreams=true&showAll=true";

    @Override
    public void configure() throws Exception {

        // we need to use the current classloader
        final DataFormat jaxb1 = new JaxbDataFormat("org.ow2.petals.anothernamespace");
        final DataFormat jaxb2 = new JaxbDataFormat("org.ow2.petals");

        from("petals:theProvidesId").streamCaching()
                .to(CAMEL_LOG)
                .unmarshal(jaxb1)
                .to(CAMEL_LOG)
                .bean(Normalizer.class, "transformIn")
                .to(CAMEL_LOG)
                .marshal(jaxb2)
                .to(CAMEL_LOG)
                .to("petals:theConsumesId")
                .to(CAMEL_LOG)
                .unmarshal(jaxb2).bean(Normalizer.class, "transformOut")
                .to(CAMEL_LOG)
                .marshal(jaxb1)
                .to(CAMEL_LOG);
    }

    public static class Normalizer {

        public void transformIn(Exchange exchange, @Body SayHello2 body) {
            final SayHello sayHello = new SayHello();
            sayHello.setArg0(body.getArg0());
            exchange.getOut().setBody(new org.ow2.petals.ObjectFactory().createSayHello(sayHello));
        }

        public void transformOut(Exchange exchange, @Body SayHelloResponse body) {
            final SayHelloResponse2 response = new SayHelloResponse2();
            response.setReturn(body.getReturn());
            exchange.getOut().setBody(new ObjectFactory().createSayHelloResponse(response));
        }
    }

}
