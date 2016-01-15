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

import java.net.URI;
import java.util.logging.Level;

import javax.jbi.messaging.MessagingException;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.ow2.petals.camel.PetalsCamelRoute;
import org.ow2.petals.camel.PetalsChannel.PetalsProvidesChannel;
import org.ow2.petals.camel.PetalsChannel.SendAsyncCallback;
import org.ow2.petals.camel.component.utils.Conversions;
import org.ow2.petals.commons.log.FlowAttributes;
import org.ow2.petals.commons.log.PetalsExecutionContext;
import org.ow2.petals.component.framework.api.Message.MEPConstants;

// TODO should I be suspendable?
public class PetalsCamelConsumer extends DefaultConsumer implements PetalsCamelRoute {

    private final PetalsProvidesChannel provides;

    public PetalsCamelConsumer(final PetalsCamelEndpoint endpoint, final Processor processor) {
        super(endpoint, processor);
        this.provides = endpoint.getComponent().getContext().getProvidesChannel(endpoint.getService());
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        // let's register so that when MEX are received, they will be passed to us
        getEndpoint().getComponent().getContext().registerRoute(getEndpoint().getService(), this);
    }

    @Override
    protected void doStop() throws Exception {
        // we unregister
        getEndpoint().getComponent().getContext().unregisterRoute(getEndpoint().getService());
        super.doStop();
    }


    @Override
    public boolean process(final org.ow2.petals.component.framework.api.message.Exchange exchange) {

        final Exchange camelExchange = getEndpoint().createExchange();

        Conversions.populateNewCamelExchange(camelExchange, exchange);

        if (getEndpoint().isSynchronous()) {
            // in that case, this method won't return until the route is fully executed

            if (this.provides.getLogger().isLoggable(Level.FINE)) {
                this.provides.getLogger().fine("Processing a Camel exchange (with id: " + exchange.getExchangeId()
                        + ") with the route in sync mode");
            }

            try {
                getProcessor().process(camelExchange);
            } catch (final Exception e) {
                this.provides.getLogger().log(Level.SEVERE,
                        "Just set an error on the Petals Exchange " + exchange.getExchangeId(), e);
                exchange.setError(e);
            }

            if (PetalsCamelConsumer.this.provides.getLogger().isLoggable(Level.FINE)) {
                PetalsCamelConsumer.this.provides.getLogger().fine("Handling a Camel exchange (with id: "
                        + exchange.getExchangeId() + ") processed by the route in sync mode ");
            }

            handleAnswer(camelExchange, exchange);
            return true;
        } else {
            if (this.provides.getLogger().isLoggable(Level.FINE)) {
                this.provides.getLogger().fine("Processing a Camel exchange (with id: " + exchange.getExchangeId()
                        + ") with the route in async mode");
            }
            return getAsyncProcessor().process(camelExchange, new AsyncCallback() {
                @Override
                public void done(final boolean doneSync) {
                    // no need to use doneSync: if it is true it just means we are being executed synchronously (w.r.t.
                    // the execution of process from this class).

                    if (PetalsCamelConsumer.this.provides.getLogger().isLoggable(Level.FINE)) {
                        PetalsCamelConsumer.this.provides.getLogger()
                                .fine("Handling a Camel exchange (with id: " + exchange.getExchangeId()
                                        + ") processed by the route in async mode "
                                        + (doneSync ? "(but executed in sync mode apparently)" : ""));
                    }

                    // Here, we are not sure if we are in the same thread as before because we went potentially went
                    // through Camel, and we need the context to be filled if possible for the sendAsync to work as best
                    // as possible
                    if (PetalsExecutionContext.getFlowAttributes() == null) {
                        final FlowAttributes flowAttributes = exchange.getFlowAttributes();

                        if (PetalsCamelConsumer.this.provides.getLogger().isLoggable(Level.FINE)) {
                            PetalsCamelConsumer.this.provides.getLogger()
                                    .fine("Missing flow attributes in the context (we were in async mode in Camel, so we may have switched threads or something like that...), trying to set it with those from the Petals exchange (with id "
                                            + exchange.getExchangeId() + "): " + flowAttributes);
                        }

                        if (flowAttributes != null) {
                            PetalsExecutionContext.putFlowAttributes(flowAttributes);
                        }
                    }

                    handleAnswer(camelExchange, exchange);
                }
            });
        }
    }

    private void handleAnswer(final Exchange camelExchange,
            final org.ow2.petals.component.framework.api.message.Exchange exchange) {

        // TODO should I update the poperties of the exchange with those of the camel exchange?

        try {
            Conversions.populateAnswerPetalsExchange(exchange, camelExchange);
        } catch (final MessagingException e) {
            // this must be caught before sending to be sure that if an error happens here it is sent back!
            this.provides.getLogger().log(Level.SEVERE,
                    "Just set an error on the Petals Exchange " + exchange.getExchangeId(), e);
            exchange.setError(e);
        }

        try {
            // TODO maybe we should render that answer synchronicity configurable... for now let's use sendAsync in
            // order not to tie resources for simple acknowledging (there is no need to block the current execution nor
            // anything)
            // TODO and actually, shouldn't I WAIT for it before letting other continue, or before letting this method
            // to return... which others? maybe not then!
            // TODO for now the timeout is not settable, let's use -1 as a starter...
            final boolean wasFault = exchange.getFault() != null;
            final boolean wasOut = exchange.isOutMessage();
            final boolean expectingAnswer = wasFault || wasOut;
            this.provides.sendAsync(exchange, -1L, new SendAsyncCallback() {
                @Override
                public void done(final org.ow2.petals.component.framework.api.message.Exchange exchange,
                        final boolean timedOut) {
                    if (timedOut) {
                        provides.getLogger().warning(
                                "The exchange I sent back to the NMR never got acknowledged, it timed out: "
                                        + exchange.getExchangeId());
                    } else {
                        final URI mep = exchange.getPattern();
                        if (expectingAnswer && exchange.isDoneStatus()) {
                            if (provides.getLogger().isLoggable(Level.FINE)) {
                                provides.getLogger()
                                        .fine("Correctly received acknowledgment for our previous answer (id: "
                                                + exchange.getExchangeId() + ")");
                            }
                            // TODO shouldn't that be transfered back to the original caller?!
                        } else if (MEPConstants.IN_OPTIONAL_OUT_PATTERN.equals(mep) && wasOut
                                && exchange.getFault() != null) {
                            try {
                                exchange.setDoneStatus();
                                PetalsCamelConsumer.this.provides.send(exchange);
                            } catch (final MessagingException e) {
                                provides.getLogger().log(Level.SEVERE,
                                        "An exchange (" + exchange.getExchangeId() + ") couldn't be sent back", e);
                            }
                        }
                        // TODO log for other (invalid) cases...
                        // TODO and add tests for all of this!
                    }
                }
            });
        } catch (final MessagingException e) {
            // if the send fails, there is nothing we can do except logging the error
            provides.getLogger().log(Level.SEVERE,
                    "An exchange (" + exchange.getExchangeId() + ") couldn't be sent back", e);
        }
    }

    @SuppressWarnings("null")
    @Override
    public PetalsCamelEndpoint getEndpoint() {
        return (PetalsCamelEndpoint) super.getEndpoint();
    }
}
