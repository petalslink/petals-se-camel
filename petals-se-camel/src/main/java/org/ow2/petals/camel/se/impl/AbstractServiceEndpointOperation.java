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
package org.ow2.petals.camel.se.impl;

import java.net.URI;
import java.util.logging.Logger;

import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import org.ow2.petals.camel.PetalsChannel;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.se.PetalsCamelSender;
import org.ow2.petals.camel.se.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.commons.log.PetalsExecutionContext;
import org.ow2.petals.component.framework.api.message.Exchange;

public abstract class AbstractServiceEndpointOperation implements ServiceEndpointOperation, PetalsChannel {

    private final QName service;

    private final QName interfaceName;

    private final String endpoint;

    private final QName operation;

    private final ServiceType type;

    private final URI mep;

    private final PetalsCamelSender sender;

    public AbstractServiceEndpointOperation(final QName service, final QName interfaceName, final String endpoint,
            final QName operation, final ServiceType type, final URI mep, final PetalsCamelSender sender)
            throws InvalidJBIConfigurationException {
        this.service = service;
        this.interfaceName = interfaceName;
        this.endpoint = endpoint;
        this.operation = operation;
        this.type = type;
        this.mep = mep;
        this.sender = sender;
    }

    @Override
    public String toString() {
        return "ServiceEndpointOperation [service=" + service + ", endpoint=" + endpoint + ", operation=" + operation
                + ", type=" + type + ", mep=" + mep + "]";
    }


    @Override
    public boolean sendSync(final Exchange exchange, final long timeout) throws MessagingException {
        if (timeout < 0) {
            return sender.sendSync(exchange);
        } else {
            return sender.sendSync(exchange, timeout);
        }
    }

    @Override
    public void sendAsync(final Exchange exchange, final long timeout, final SendAsyncCallback callback)
            throws MessagingException {
        sender.sendAsync(exchange,
                new PetalsCamelAsyncContext(timeout, callback, PetalsExecutionContext.getFlowAttributes()));
    }

    @Override
    public void send(final Exchange exchange) throws MessagingException {
        sender.send(exchange);
    }

    @Override
    public QName getService() {
        return service;
    }

    @Override
    public QName getInterface() {
        return interfaceName;
    }

    @Override
    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public QName getOperation() {
        return operation;
    }

    @Override
    public ServiceType getType() {
        return type;
    }

    @Override
    public URI getMEP() {
        return mep;
    }

    @SuppressWarnings("null")
    @Override
    public Logger getLogger() {
        return sender.getLogger();
    }
}
