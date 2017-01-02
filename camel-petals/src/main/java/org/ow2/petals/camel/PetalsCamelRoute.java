/**
 * Copyright (c) 2015-2017 Linagora
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
package org.ow2.petals.camel;

import org.ow2.petals.component.framework.api.message.Exchange;

/**
 * Represents an object that can process exchanges.
 * 
 * Implemented by the Camel Consumer that will pass the message to a Camel route.
 * 
 * @author vnoel
 *
 */
public interface PetalsCamelRoute {

    /**
     * No exceptions are thrown: the message will be set in error if something happens and the implementation must take
     * care of sending back the answer (except if sending itself fails).
     * 
     * @param exchange
     * @return <code>true</code> if the processing was done synchronously (i.e. it is finished when the method returns).
     */
    public boolean process(Exchange exchange);
}
