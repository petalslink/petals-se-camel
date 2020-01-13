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
package org.ow2.petals.camel.component.mocks;

import java.net.URI;

import javax.xml.namespace.QName;

import org.eclipse.jdt.annotation.Nullable;
import org.ow2.petals.camel.ServiceEndpointOperation;

public class ServiceEndpointOperationMock implements ServiceEndpointOperation {

    private final @Nullable QName service;

    private final QName interfaceName;

    private final @Nullable String endpoint;

    private final @Nullable QName operation;

    private final ServiceType type;

    private final @Nullable URI mep;

    public ServiceEndpointOperationMock(final @Nullable String service, final String interfaceName,
            final @Nullable String endpoint, final @Nullable String operation, final ServiceType type,
            final @Nullable URI mep) {
        this.service = service == null ? null : new QName("tests", service);
        this.interfaceName = new QName("tests", interfaceName);
        this.endpoint = endpoint;
        this.operation = operation == null ? null : new QName("tests", operation);
        this.type = type;
        this.mep = mep;
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
    public ServiceType getType() {
        return type;
    }

    @Override
    public @Nullable URI getMEP() {
        return mep;
    }
}
