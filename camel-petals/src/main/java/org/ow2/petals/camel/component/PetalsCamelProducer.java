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


import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.messaging.MessagingException;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultAsyncProducer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.PetalsChannel.SendAsyncCallback;
import org.ow2.petals.camel.component.utils.Conversions;
import org.ow2.petals.commons.log.FlowAttributes;
import org.ow2.petals.commons.log.Level;
import org.ow2.petals.commons.log.PetalsExecutionContext;
import org.ow2.petals.component.framework.logger.ConsumeExtFlowStepBeginLogData;
import org.ow2.petals.component.framework.logger.Utils;

import com.ebmwebsourcing.easycommons.lang.StringHelper;

/**
 * A PetalsProducer get messages from Camel and send them to a Petals service
 * 
 * @author vnoel
 *
 */
public class PetalsCamelProducer extends DefaultAsyncProducer {

    // this is not really used as an exception for knowing where it happened
    // we can thus reuse it and avoid the overhead of creating the exception
    public static final MessagingException TIMEOUT_EXCEPTION = new MessagingException(
            "A timeout happened while Camel sent an exchange to a JBI service");

    static {
        TIMEOUT_EXCEPTION.setStackTrace(new StackTraceElement[0]);
    }

    private final PetalsConsumesChannel consumes;

    public PetalsCamelProducer(final PetalsCamelEndpoint endpoint) {
        super(endpoint);
        this.consumes = endpoint.getComponent().getContext().getConsumesChannel(endpoint.getService());
    }

    @NonNullByDefault(false)
    @Override
    public void process(final Exchange camelExchange) {
        final boolean sync = this.process(camelExchange, true, new AsyncCallback() {
            @Override
            public void done(final boolean doneSync) {
                // nothing to do
            }
        });
        assert sync;
    }

    @NonNullByDefault(false)
    @Override
    public boolean process(final Exchange camelExchange, final AsyncCallback callback) {
        if (getEndpoint().isSynchronous()) {
            final boolean sync = this.process(camelExchange, true, callback);
            assert sync;
            return sync;
        } else {
            return this.process(camelExchange, false, callback);
        }
    }

    /**
     * There is two possible ways of doing the processing:
     * <ul>
     * <li>Asynchronously with a callback</li>
     * <li>Synchronously with a callback</li>
     * </ul>
     * 
     * @param camelExchange
     * @param doSync
     *            if this processing must be done synchronously
     * @param callback
     *            a callback to call after, can't be null but can be no-op
     * 
     * @return <code>true</code> if the processing was done synchronously
     */
    private boolean process(final Exchange camelExchange, final boolean doSync, final AsyncCallback callback) {

        final long timeout = getEndpoint().getTimeout();

        final FlowAttributes faAsBC;
        if (PetalsExecutionContext.getFlowAttributes() == null) {
            // if there is no flow attributes set, it can means 3 things:
            // 1) we received an exchange from petals without flow attributes in the beginning of this route
            // 2) we lost the context flow attributes because we switched threads (because of async execution)
            // 3) we never received a petals and we are acting as a BC
            this.consumes.getLogger().log(Level.WARNING,
                    "There is no flow attributes in the Execution Context: "
                            + "either we lost them somewhere in the route, "
                            + "either we received a petals exchange without flow attributes "
                            + "or we are acting as a BC and we are starting a new flow. "
                            + "We assume the later and initialise a new flow.");
            faAsBC = PetalsExecutionContext.initFlowAttributes();
        } else {
            faAsBC = null;
        }

        try {

            final org.ow2.petals.component.framework.api.message.Exchange exchange = this.consumes.newExchange();

            // TODO should I check that the camel exchange has the same MEP as the consumes MEP? or compatibility?
            // for example if I have a inonly exchange sent to an inout service, then I just discard the out
            // while an InOut exchange for an InOnly service is not possible!
            // TODO and also I should take into account the MEP of the endpoint??!!

            Conversions.populateNewPetalsExchange(exchange, camelExchange);

            if (faAsBC != null) {
                this.consumes.getLogger().log(Level.MONIT, "",
                        new ConsumeExtFlowStepBeginLogData(faAsBC.getFlowInstanceId(), faAsBC.getFlowStepId(),
                                StringHelper.nonNullValue(exchange.getInterfaceName()),
                                StringHelper.nonNullValue(exchange.getService()),
                                StringHelper.nonNullValue(exchange.getEndpointName()),
                                StringHelper.nonNullValue(exchange.getOperation())));
            }

            if (doSync) {

                if (this.consumes.getLogger().isLoggable(Level.FINE)) {
                    this.consumes.getLogger().log(Level.FINE,
                            "Sending a Petals exchange (with id: " + exchange.getExchangeId() + ") in sync mode");
                }

                // false means timed out!
                final boolean timedOut = !this.consumes.sendSync(exchange, timeout);
                // this has been done synchronously
                final boolean doneSync = true;

                if (this.consumes.getLogger().isLoggable(Level.FINE)) {
                    this.consumes.getLogger().log(Level.FINE, "Handling a Petals exchange (with id: "
                            + exchange.getExchangeId() + ") back from a send in sync mode ");
                }

                handleAnswer(camelExchange, exchange, timedOut, doneSync, callback, faAsBC);
                return doneSync;
            } else {
                // this is done asynchronously (except if the send fail, but then the value of this variable won't be
                // used because the callback will never be called)
                final boolean doneSync = false;

                if (this.consumes.getLogger().isLoggable(Level.FINE)) {
                    this.consumes.getLogger().log(Level.FINE,
                            "Sending a Petals exchange (with id: " + exchange.getExchangeId() + ") in async mode");
                }

                this.consumes.sendAsync(exchange, timeout, new SendAsyncCallback() {
                    @Override
                    public void done(final boolean timedOut) {

                        if (PetalsCamelProducer.this.consumes.getLogger().isLoggable(Level.FINE)) {
                            PetalsCamelProducer.this.consumes.getLogger().log(Level.FINE,
                                    "Handling a Petals exchange (with id: " + exchange.getExchangeId()
                                            + ") back from a send in async mode "
                                            + (doneSync ? "(but executed in sync mode apparently)" : ""));
                        }

                        handleAnswer(camelExchange, exchange, timedOut, doneSync, callback, faAsBC);
                    }
                });
                return doneSync;
            }
        } catch (final MessagingException e) {
            // these exceptions can only happens before the message is sent (or if the send fails), so before
            // handleAnswer could be called, thus this is done synchronously in either case of doSync and the callback
            // must be called
            final boolean doneSync = true;
            this.consumes.getLogger().log(Level.SEVERE,
                    "Just set an error on the Camel Exchange " + camelExchange.getExchangeId(), e);
            if (faAsBC != null) {
                Utils.addMonitFailureTrace(this.consumes.getLogger(), faAsBC, e.getMessage(), Role.CONSUMER);
            }
            camelExchange.setException(e);
            callback.done(doneSync);
            return doneSync;
        }
    }

    private void handleAnswer(final Exchange camelExchange,
            final org.ow2.petals.component.framework.api.message.Exchange exchange, final boolean timedOut,
            final boolean doneSync, final AsyncCallback callback, @Nullable final FlowAttributes faAsBC) {
        if (timedOut) {
            camelExchange.setException(TIMEOUT_EXCEPTION);
        } else {
            // TODO should properties of the camel exchange be updated with those of the received response?!
            Conversions.populateAnswerCamelExchange(camelExchange, exchange);
        }
        if (faAsBC != null) {
            Utils.addMonitEndOrFailureTrace(this.consumes.getLogger(), exchange, faAsBC);
        }

        callback.done(doneSync);
    }

    @SuppressWarnings("null")
    @Override
    public PetalsCamelEndpoint getEndpoint() {
        return (PetalsCamelEndpoint) super.getEndpoint();
    }
}
