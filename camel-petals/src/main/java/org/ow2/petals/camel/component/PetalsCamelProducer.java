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
package org.ow2.petals.camel.component;

import javax.jbi.messaging.MessagingException;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultAsyncProducer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.component.utils.Conversions;
import org.ow2.petals.camel.exceptions.TimeoutException;

import com.google.common.base.Preconditions;

/**
 * A PetalsProducer get messages from Camel and send them to a Petals service
 * 
 * @author vnoel
 *
 */
public class PetalsCamelProducer extends DefaultAsyncProducer {

    private final PetalsConsumesChannel consumes;

    public PetalsCamelProducer(final PetalsCamelEndpoint endpoint) {
        super(endpoint);
        this.consumes = endpoint.getComponent().getContext().getConsumesChannel(endpoint.getSEO());
    }

    @Override
    public void process(final @Nullable Exchange camelExchange) throws Exception {
        Preconditions.checkNotNull(camelExchange);
        this.processSyncOrNot(camelExchange, null);
    }

    @Override
    public boolean process(final @Nullable Exchange camelExchange, final @Nullable AsyncCallback callback) {
        Preconditions.checkNotNull(camelExchange);
        Preconditions.checkNotNull(callback);

        try {
            if (getEndpoint().isSynchronous()) {
                this.processSyncOrNot(camelExchange, null);

                callback.done(true);

                return true;
            } else {
                return this.processSyncOrNot(camelExchange, callback);
            }

        } catch (Exception e) {
            camelExchange.setException(e);
            // true because we did that synchronously and things are finished
            callback.done(true);
            // true because we called done with true
            return true;
        }
    }

    private boolean processSyncOrNot(final Exchange camelExchange, final @Nullable AsyncCallback callback)
            throws Exception {
        final long timeout = getEndpoint().getTimeout();

        // TODO add checks that operation is set, and all the rest?
        final org.ow2.petals.component.framework.api.message.Exchange exchange = this.consumes.newExchange();

        Conversions.populateNewPetalsExchange(exchange, camelExchange);

        if (callback == null) {
            boolean timedout = this.consumes.sendSync(exchange, timeout);
            if (timedout) {
                camelExchange.setException(new TimeoutException(exchange));
            } else {
                Conversions.populateAnswerCamelExchange(camelExchange, exchange);
            }
            return true;
        } else {
            // if sendAsync fails an exception will be thrown
            this.consumes.sendAsync(exchange, timeout, new Runnable() {
                @Override
                public void run() {
                    try {
                        Conversions.populateAnswerCamelExchange(camelExchange, exchange);
                    } catch (final MessagingException e) {
                        camelExchange.setException(e);
                        // TODO should I call the callback with true in that case?!
                    }
                    callback.done(false);
                }
            });
            return false;
        }
    }

    public PetalsCamelEndpoint getEndpoint() {
        @SuppressWarnings("null")
        final @NonNull PetalsCamelEndpoint endpoint = (PetalsCamelEndpoint) super.getEndpoint();
        return endpoint;
    }
}
