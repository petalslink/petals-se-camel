/**
 * Copyright (c) 2017-2025 Linagora
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

import java.io.IOException;
import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.ow2.petals.camel.component.PetalsConstants;
import org.ow2.petals.component.framework.api.util.Placeholders;

import jakarta.xml.bind.JAXBException;

public abstract class PetalsRouteBuilder extends RouteBuilder {

    private final Properties placeholders = new Properties();

    /**
     * Camel route definition call-back called just after the service-unit deployment and before its initialization.
     */
    public void deploy() throws Exception {
        // NOP
    }

    /**
     * Camel route definition call-back called on the service-unit initialization.
     */
    public void init() throws Exception {
        // NOP
    }

    /**
     * Camel route definition call-back called on the service-unit startup.
     */
    public void start() throws Exception {
        // NOP
    }

    /**
     * Camel route definition call-back called on the service-unit stop.
     */
    public void stop() throws Exception {
        // NOP
    }

    /**
     * Camel route definition call-back called on the service-unit shutdown.
     */
    public void shutdown() throws Exception {
        // NOP
    }

    /**
     * Camel route definition call-back called on the service-unit undeployment.
     */
    public void undeploy() throws Exception {
        // NOP
    }

    /**
     * Camel route definition call-back called on placeholder reloading. Placeholders have their new values.
     */
    public void onPlaceHolderValuesReloaded(final Placeholders newPlaceholders) {
        synchronized (this.placeholders) {
            this.placeholders.clear();
            this.placeholders.putAll(newPlaceholders.toProperties());
        }
    }

    protected String getPlaceHolder(final String placeHolder) {
        synchronized (this.placeholders) {
            return this.placeholders.getProperty(placeHolder);
        }
    }

    protected RouteDefinition fromPetals(final String service) {
        return from("petals:" + service).routeId(service);
    }

    /**
     * Sets the fault on the exchange's out and mark it for immediate return
     */
    public static void setJbiFault(final MarshallingHelper marshalling, final Exchange exchange, final Object fault)
            throws JAXBException, IOException {
        setJbiFault(marshalling, exchange, fault, true);
    }

    public static void setJbiFault(final MarshallingHelper marshalling, final Exchange exchange, final Object fault,
            final boolean stop)
            throws JAXBException, IOException {
        marshalling.marshal(exchange, fault);
        // set this only after we are sure we properly marshaled the body!
        setIsJbiFault(exchange, stop);
    }

    /**
     * Sets the fault on the exchange's out and mark it for immediate return
     */
    public static void setJbiFault(final Exchange exchange, final Object fault) {
        setJbiFault(exchange, fault, true);
    }

    public static void setJbiFault(final Exchange exchange, final Object fault, final boolean stop) {
        exchange.getOut().setBody(fault);
        setIsJbiFault(exchange, stop);
    }

    /**
     * Mark the out message as fault and mark it for immediate return
     */
    public static void setIsJbiFault(final Exchange exchange) {
        setIsJbiFault(exchange, true);
    }

    public static void setIsJbiFault(final Exchange exchange, final boolean stop) {
        if (stop) {
            exchange.setRouteStop(true);
        }
        exchange.getOut().setHeader(PetalsConstants.MESSAGE_FAULT_HEADER, true);
    }

    /**
     * Is the service provider failed because of a fault
     * 
     * @return {@code true} if this exchange failed due to a JBI fault.
     */
    public static boolean isJbiFault(final Exchange exchange) {
        return Boolean.TRUE.equals(exchange.getMessage().getHeader(PetalsConstants.MESSAGE_FAULT_HEADER));
    }

    /**
     * Is a fault or an exception set in the Camel exchange
     * 
     * @return {@code true} if this exchange failed due to either an exception or a JBI fault.
     */
    public static boolean isJbiFailed(final Exchange exchange) {
        return exchange.isFailed() || isJbiFault(exchange);
    }
}
