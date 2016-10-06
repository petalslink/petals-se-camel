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
package org.ow2.petals.camel.component.exceptions;

import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;

/**
 * An exception thrown when a petals endpoint is used to create a camel producer for a petals provides or a camel
 * consumer for a petals consumes.
 * 
 * @author vnoel
 *
 */
public class IncompatibleEndpointUsageException extends Exception {

    private static final long serialVersionUID = 4627238751624577968L;

    private static final String MESSAGE_PATTERN = "The service %s is not a %s";

    public IncompatibleEndpointUsageException(final ServiceEndpointOperation service, final ServiceType expectedType) {
        super(String.format(MESSAGE_PATTERN, service, expectedType));
    }
}

