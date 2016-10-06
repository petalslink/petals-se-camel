/**
 * Copyright (c) 2015-2016 Linagora
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
package org.ow2.petals.camel.se.impl;

import java.net.URI;
import java.util.logging.Logger;

import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import org.eclipse.jdt.annotation.Nullable;
import org.ow2.petals.camel.PetalsChannel;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.se.PetalsCamelSender;
import org.ow2.petals.camel.se.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.commons.log.PetalsExecutionContext;
import org.ow2.petals.component.framework.api.message.Exchange;

/**
 * 
 * Note: {@link PetalsCamelSender} overrides {@link PetalsCamelSender#getLogger()} in order to have a SU-specific
 * logger. Also it has no consumes nor provides, so they shouldn't be relied on.
 * 
 * @author vnoel
 *
 */
public abstract class AbstractServiceEndpointOperation implements ServiceEndpointOperation, PetalsChannel {

    private final QName interfaceName;

    @Nullable
    private final QName service;

    @Nullable
    private final String endpoint;

    @Nullable
    private final QName operation;

    @Nullable
    private final URI mep;

    protected final PetalsCamelSender sender;

    public AbstractServiceEndpointOperation(final QName interfaceName, final @Nullable QName service,
            final @Nullable String endpoint, final @Nullable QName operation, 
            @Nullable final URI mep, final PetalsCamelSender sender)
                    throws InvalidJBIConfigurationException {
        this.service = service;
        this.interfaceName = interfaceName;
        this.endpoint = endpoint;
        this.operation = operation;
        this.mep = mep;
        this.sender = sender;
    }

    @Override
    public String toString() {
        return "ServiceEndpointOperation [service=" + service + ", endpoint=" + endpoint + ", operation=" + operation
                + ", type=" + getType() + ", mep=" + mep + "]";
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
                new PetalsCamelAsyncContext(exchange, timeout, callback, PetalsExecutionContext.getFlowAttributes()));
    }

    @Override
    public void send(final Exchange exchange) throws MessagingException {
        sender.send(exchange);
    }

    @Override
    public @Nullable QName getService() {
        return service;
    }

    @Override
    public QName getInterface() {
        return interfaceName;
    }

    @Override
    public @Nullable String getEndpoint() {
        return endpoint;
    }

    @Override
    public @Nullable QName getOperation() {
        return operation;
    }

    @Override
    public @Nullable URI getMEP() {
        return mep;
    }

    @Override
    public Logger getLogger() {
        return this.sender.getLogger2();
    }
}
