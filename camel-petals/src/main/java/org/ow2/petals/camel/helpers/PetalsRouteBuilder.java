/**
 * Copyright (c) 2017 Linagora
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
package org.ow2.petals.camel.helpers;

import javax.xml.bind.JAXBException;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;

public abstract class PetalsRouteBuilder extends RouteBuilder {

    protected RouteDefinition fromPetals(String service) {
        return from("petals:" + service).routeId(service);
    }

    /**
     * Sets the fault on the exchange and mark it for immediate return
     */
    public static void setFault(MarshallingHelper marshalling, Exchange exchange, Object fault) throws JAXBException {
        exchange.setProperty(Exchange.ROUTE_STOP, true);
        exchange.getOut().setFault(true);
        marshalling.marshal(exchange.getOut(), fault);
    }
}
