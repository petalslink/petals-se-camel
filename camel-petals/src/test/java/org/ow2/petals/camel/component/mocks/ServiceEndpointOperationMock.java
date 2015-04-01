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
package org.ow2.petals.camel.component.mocks;

import java.net.URI;

import javax.xml.namespace.QName;

import org.ow2.petals.camel.ServiceEndpointOperation;

public class ServiceEndpointOperationMock implements ServiceEndpointOperation {

    private final QName service;

    private final QName interfaceName;

    private final String endpoint;

    private final QName operation;

    private final ServiceType type;

    private final URI mep;

    public ServiceEndpointOperationMock(final String service, final String interfaceName, final String endpoint,
            final String operation, final ServiceType type, final URI mep) {
        this.service = new QName("tests", service);
        this.interfaceName = new QName("tests", interfaceName);
        this.endpoint = endpoint;
        this.operation = new QName("tests", operation);
        this.type = type;
        this.mep = mep;
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

}
