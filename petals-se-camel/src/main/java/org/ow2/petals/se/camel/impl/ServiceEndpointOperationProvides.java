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
package org.ow2.petals.se.camel.impl;

import java.net.URI;

import javax.xml.namespace.QName;

import org.eclipse.jdt.annotation.NonNull;
import org.ow2.petals.camel.PetalsChannel.PetalsProvidesChannel;
import org.ow2.petals.component.framework.api.message.Exchange;
import org.ow2.petals.component.framework.jbidescriptor.generated.Provides;
import org.ow2.petals.se.camel.PetalsCamelSender;

public class ServiceEndpointOperationProvides extends AbstractServiceEndpointOperation implements PetalsProvidesChannel {

    private final Provides provides;

    public ServiceEndpointOperationProvides(final QName operation, final URI mep, final PetalsCamelSender sender,
            final Provides provides) {
        super(provides.getInterfaceName(), provides.getServiceName(), provides.getEndpointName(), operation, mep,
                sender);
        this.provides = provides;
    }

    @Override
    public ServiceType getType() {
        return ServiceType.PROVIDES;
    }

    @Override
    public @NonNull QName getOperation() {
        final QName operation = super.getOperation();
        assert operation != null;
        return operation;
    }

    @Override
    public @NonNull URI getMEP() {
        final URI mep = super.getMEP();
        assert mep != null;
        return mep;
    }

    @Override
    public @NonNull String getEndpoint() {
        final String endpoint = super.getEndpoint();
        assert endpoint != null;
        return endpoint;
    }

    @Override
    public @NonNull QName getService() {
        final QName service = super.getService();
        assert service != null;
        return service;
    }

    @Override
    public boolean isFlowTracingActivated(final @NonNull Exchange exchange) {
        return this.sender.getComponent().isFlowTracingActivated(exchange.getMessageExchange(), this.provides);
    }

}
