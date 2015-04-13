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

import java.util.logging.Level;

import javax.jbi.messaging.MessagingException;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.ow2.petals.camel.PetalsChannel.PetalsProvidesChannel;
import org.ow2.petals.camel.PetalsProvidesOperation;
import org.ow2.petals.camel.component.utils.Conversions;

// TODO should I be suspendable?
public class PetalsCamelConsumer extends DefaultConsumer implements PetalsProvidesOperation {

    private final PetalsProvidesChannel provides;

    public PetalsCamelConsumer(final PetalsCamelEndpoint endpoint, final Processor processor) {
        super(endpoint, processor);
        this.provides = endpoint.getComponent().getContext().getProvidesChannel(endpoint.getSEO());
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        // let's register so that when MEX are received, they will be passed to us
        getEndpoint().getComponent().getContext().registerPPO(getEndpoint().getSEO(), this);
    }

    @Override
    protected void doStop() throws Exception {
        // we unregister
        getEndpoint().getComponent().getContext().unregisterPPO(getEndpoint().getSEO());
        super.doStop();
    }


    @Override
    public boolean process(final org.ow2.petals.component.framework.api.message.Exchange exchange) {

        final Exchange camelExchange = getEndpoint().createExchange();

        Conversions.populateNewCamelExchange(camelExchange, exchange);

        if (getEndpoint().isSynchronous()) {
            try {
                getProcessor().process(camelExchange);
            } catch (final Exception e) {
                this.provides.getLogger().log(Level.SEVERE,
                        "Just set an error on the Petals Exchange " + exchange.getExchangeId(), e);
                exchange.setError(e);
            }
            handleAnswer(camelExchange, exchange);
            return true;
        } else {
            getAsyncProcessor().process(camelExchange, new AsyncCallback() {
                @Override
                public void done(final boolean doneSync) {
                    handleAnswer(camelExchange, exchange);
                }
            });
            // TODO sould I return true in case an error happened? (and thus process would have returned true)
            return false;
        }
    }

    private void handleAnswer(final Exchange camelExchange,
            final org.ow2.petals.component.framework.api.message.Exchange exchange) {

        // this must be caught before sending to be sure that if an error happens here it is sent back!
        try {
            Conversions.populateAnswerPetalsExchange(exchange, camelExchange);
        } catch (final MessagingException e) {
            this.provides.getLogger().log(Level.SEVERE,
                    "Just set an error on the Petals Exchange " + exchange.getExchangeId(), e);
            exchange.setError(e);
        }

        // if the send fails, there is nothing we can do except logging the error
        try {
            // TODO shouldn't we use sendAsync so that we can handle received done or fault or error messages?
            this.provides.send(exchange);
            // TODO and actually, shouldn't I WAIT for it before letting other continue... which others? maybe not then!
        } catch (final MessagingException e) {
            provides.getLogger().log(Level.SEVERE, "An exchange (" + exchange + ") couldn't be sent back", e);
        }
    }

    @SuppressWarnings("null")
    @Override
    public PetalsCamelEndpoint getEndpoint() {
        return (PetalsCamelEndpoint) super.getEndpoint();
    }
}
