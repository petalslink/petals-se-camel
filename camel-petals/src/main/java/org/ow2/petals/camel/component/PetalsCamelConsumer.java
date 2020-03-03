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
package org.ow2.petals.camel.component;

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
        assert camelExchange != null;

        Conversions.populateNewCamelExchange(exchange, camelExchange);

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

            // let's store it for later in case we come back in a different thread
            final FlowAttributes current = PetalsExecutionContext.getFlowAttributes();
            assert current != null;

            return getAsyncProcessor().process(camelExchange, new AsyncCallback() {
                @Override
                public void done(final boolean doneSync) {
                    // no need to use doneSync: if it is true it just means we are being executed synchronously (w.r.t.
                    // the execution of process from this class).

                    // Here, we are not sure if we are in the same thread as before because we went potentially went
                    // through Camel, and we need the context to be filled if possible for the sendAsync to work as best
                    // as possible
                    PetalsExecutionContext.putFlowAttributes(current);

                    if (PetalsCamelConsumer.this.provides.getLogger().isLoggable(Level.FINE)) {
                        PetalsCamelConsumer.this.provides.getLogger()
                                .fine("Handling a Camel exchange (with id: " + exchange.getExchangeId()
                                        + ") processed by the route in async mode "
                                        + (doneSync ? "(but executed in sync mode apparently)" : ""));
                    }

                    handleAnswer(camelExchange, exchange);
                }
            });
        }
    }

    private void handleAnswer(final Exchange camelExchange,
            final org.ow2.petals.component.framework.api.message.Exchange exchange) {

        // it costs nothing to also support isFault for outbound messages
        if (camelExchange.hasOut() && camelExchange.getOut().isFault()) {
            this.provides.getLogger().log(Level.WARNING,
                    "Camel's isFault() abstraction is deprecated and should not be used: "
                            + "prefer using the PetalsCamelComponent.MESSAGE_FAULT_HEADER header");
            camelExchange.getOut().setHeader(PetalsCamelComponent.MESSAGE_FAULT_HEADER, true);
        }

        try {
            Conversions.populateAnswerPetalsExchange(camelExchange, exchange);
        } catch (final MessagingException e) {
            // this must be caught before sending to be sure that if an error happens here it is sent back!
            this.provides.getLogger().log(Level.SEVERE,
                    "Just set an error on the Petals Exchange " + exchange.getExchangeId(), e);
            exchange.setError(e);
        }

        try {
            if (!exchange.isActiveStatus()) {
                this.provides.send(exchange);
            } else {
                // TODO maybe we should render that answer synchronicity configurable... for now let's use sendAsync in
                // order not to tie resources for simple acknowledging (there is no need to block the current execution
                // nor anything)
                // TODO and actually, shouldn't I WAIT for it before letting other continue, or before letting this
                // method to return... which others? maybe not then!
                // TODO for now the timeout is not settable, let's use default provides value (-1) as a starter...
                final boolean wasFault = exchange.getFault() != null;
                final boolean wasOut = exchange.isOutMessage();
                final boolean expectingAnswer = wasFault || wasOut;
                if (getEndpoint().isSynchronous()) {
                    final boolean ok = this.provides.sendSync(exchange, -1L);
                    handleAnswerAnswer(wasOut, expectingAnswer, exchange, !ok);
                } else {
                    this.provides.sendAsync(exchange, -1L, new SendAsyncCallback() {
                        @Override
                        public void done(final org.ow2.petals.component.framework.api.message.Exchange exchange,
                                final boolean timedOut) {
                            handleAnswerAnswer(wasOut, expectingAnswer, exchange, timedOut);
                        }
                    });
                }
            }
        } catch (final MessagingException e) {
            // if the send fails, there is nothing we can do except logging the error
            provides.getLogger().log(Level.SEVERE,
                    "An exchange (" + exchange.getExchangeId() + ") couldn't be sent back", e);
        }
    }

    private void handleAnswerAnswer(final boolean wasOut, final boolean expectingAnswer,
            final org.ow2.petals.component.framework.api.message.Exchange exchange, final boolean timedOut) {
        if (timedOut) {
            provides.getLogger().warning("The exchange I sent back to the NMR never got acknowledged, it timed out: "
                    + exchange.getExchangeId());
        } else {
            provides.getLogger()
                    .fine("Got an answer from my request I sent to the NMR for exchange " + exchange.getExchangeId());

            if (expectingAnswer && exchange.isDoneStatus()) {
                if (provides.getLogger().isLoggable(Level.FINE)) {
                    provides.getLogger().fine("Correctly received acknowledgment for our previous answer (id: "
                            + exchange.getExchangeId() + ")");
                }
                // TODO that should be transfered back to the original caller!!! see PetalsCamelProducer
            } else if (exchange.isInOptionalOutPattern() && wasOut && exchange.getFault() != null) {
                try {
                    // TODO the fault should be transfered back to the original caller before we
                    // answer!!!
                    exchange.setDoneStatus();
                    PetalsCamelConsumer.this.provides.send(exchange);
                } catch (final MessagingException e) {
                    provides.getLogger().log(Level.SEVERE,
                            "An exchange (" + exchange.getExchangeId() + ") couldn't be sent back", e);
                }
            } else {
                // TODO log nicely for other (invalid) cases...
                provides.getLogger().warning("Unknown situation in MEP for exchange " + exchange.getExchangeId());
            }
            // TODO and add tests for all of this!
        }
    }

    @SuppressWarnings("null")
    @Override
    public PetalsCamelEndpoint getEndpoint() {
        return (PetalsCamelEndpoint) super.getEndpoint();
    }
}
