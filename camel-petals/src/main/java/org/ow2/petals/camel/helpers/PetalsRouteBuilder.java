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
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.ow2.petals.camel.component.PetalsCamelComponent;

public abstract class PetalsRouteBuilder extends RouteBuilder {

    protected RouteDefinition fromPetals(String service) {
        return from("petals:" + service).routeId(service);
    }

    /**
     * Sets the fault on the exchange's out and mark it for immediate return
     */
    public static void setJbiFault(MarshallingHelper marshalling, Exchange exchange, Object fault)
            throws JAXBException {
        setJbiFault(marshalling, exchange, fault, true);
    }

    public static void setJbiFault(MarshallingHelper marshalling, Exchange exchange, Object fault, boolean stop)
            throws JAXBException {
        marshalling.marshal(exchange.getOut(), fault);
        // set this only after we are sure we properly marshaled the body!
        setIsJbiFault(exchange, stop);
    }

    /**
     * Sets the fault on the exchange's out and mark it for immediate return
     */
    public static void setJbiFault(Exchange exchange, Object fault) {
        setJbiFault(exchange, fault, true);
    }

    public static void setJbiFault(Exchange exchange, Object fault, boolean stop) {
        exchange.getOut().setBody(fault);
        setIsJbiFault(exchange, stop);
    }

    /**
     * Mark the out message as fault and mark it for immediate return
     */
    public static void setIsJbiFault(Exchange exchange) {
        setIsJbiFault(exchange, true);
    }

    public static void setIsJbiFault(Exchange exchange, boolean stop) {
        if (stop) {
            exchange.setProperty(Exchange.ROUTE_STOP, true);
        }
        exchange.getOut().setHeader(PetalsCamelComponent.MESSAGE_FAULT_HEADER, true);
    }

    public static boolean isJbiFault(Message msg) {
        return Boolean.TRUE.equals(msg.getHeader(PetalsCamelComponent.MESSAGE_FAULT_HEADER));
    }

    public static boolean isJbiFailed(Exchange exchange) {
        return exchange.isFailed() || exchange.hasOut() ? isJbiFault(exchange.getOut()) : isJbiFault(exchange.getIn());
    }
}
