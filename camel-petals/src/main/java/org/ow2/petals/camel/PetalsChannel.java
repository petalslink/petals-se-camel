/**
 * Copyright (c) 2015-2021 Linagora
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

import java.util.logging.Logger;

import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.eclipse.jdt.annotation.Nullable;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.component.framework.api.message.Exchange;

/**
 * Gives access to JBI messaging operations.
 * 
 * Used by the Camel Provider that needs to send message to Petals
 * 
 * @author vnoel
 *
 */
public interface PetalsChannel {
    
    /**
     * This logger may be the same for all channels coming from a given {@link PetalsCamelContext}.
     * 
     * @return
     */
    public Logger getLogger();

    /**
     * If timeout is less than 0 then we use the consumes or provides default timeout value, if equals to 0 then no
     * timeout.
     * 
     * @return true if the send did not timeout
     * @throws MessagingException
     * 
     */
    public boolean sendSync(Exchange exchange, long timeout) throws MessagingException;
    
    /**
     * If timeout is less than 0 then we use the consumes or provides default timeout value, if equals to 0 then no
     * timeout.
     * 
     * If an exception is thrown, then the callback will never be called.
     * 
     * @throws MessagingException
     * 
     */
    public void sendAsync(Exchange exchange, long timeout, SendAsyncCallback callback) throws MessagingException;

    public void send(Exchange exchange) throws MessagingException;

    public interface PetalsConsumesChannel extends PetalsChannel {

        public @Nullable ServiceEndpoint resolveEndpoint(QName serviceName, String endpointName);

        public Exchange newExchange(@Nullable MEPPatternConstants mep) throws MessagingException;

    }

    public interface PetalsProvidesChannel extends PetalsChannel {

    }

    public interface SendAsyncCallback {

        /**
         * 
         * @param exchange
         *            the exchange of the send async (which is different from the exchange that was sent!)
         * @param timedOut
         *            is true if the send async timed out
         */
        public void done(Exchange exchange, boolean timedOut);
    }
}
