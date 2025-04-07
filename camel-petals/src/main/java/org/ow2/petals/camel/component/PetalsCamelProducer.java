/**
 * Copyright (c) 2015-2025 Linagora
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

import java.util.Optional;

import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultAsyncProducer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.PetalsChannel.SendAsyncCallback;
import org.ow2.petals.camel.component.exceptions.TimeoutException;
import org.ow2.petals.camel.component.utils.Conversions;
import org.ow2.petals.camel.helpers.MEPHelper;
import org.ow2.petals.commons.log.FlowAttributes;
import org.ow2.petals.commons.log.Level;
import org.ow2.petals.commons.log.PetalsExecutionContext;
import org.ow2.petals.component.framework.api.monitoring.MonitTraceLogger;
import org.ow2.petals.component.framework.logger.ConsumeExtFlowStepBeginLogData;
import org.ow2.petals.component.framework.logger.ConsumeExtFlowStepFailureLogData;
import org.ow2.petals.component.framework.logger.StepLogHelper;
import org.ow2.petals.component.framework.util.exception.InvalidFlowTracingActivationExchangePropertyValueException;

/**
 * A PetalsProducer get messages from Camel and send them to a Petals service
 * 
 * @author vnoel
 */
public class PetalsCamelProducer extends DefaultAsyncProducer {

    private final PetalsConsumesChannel consumes;

    private final PetalsCamelComponent component;

    private final MonitTraceLogger monitTraceLogger;

    public PetalsCamelProducer(final PetalsCamelEndpoint endpoint) {
        super(endpoint);
        this.component = endpoint.getComponent();
        this.consumes = this.component.getContext().getConsumesChannel(endpoint.getService());
        this.monitTraceLogger = this.component.getContext().getMonitTraceLogger();
    }

    @NonNullByDefault(false)
    @Override
    public void process(final Exchange camelExchange) {
        assert camelExchange != null;
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
        assert camelExchange != null;
        assert callback != null;
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
     * @return <code>true</code> if the processing was done synchronously
     */
    private boolean process(final Exchange camelExchange, final boolean doSync, final AsyncCallback callback) {

        final long timeout = getEndpoint().getTimeout();

        final FlowAttributes faAsBC;
        if (PetalsExecutionContext.getFlowAttributes() == null) {
            // if there is no flow attributes set, it can means 3 things:
            // 1) we received an exchange from petals without flow attributes in the beginning of this route
            // 2) we lost the context flow attributes because we switched threads (because of async execution)
            // 3) we never received a petals exchange and we are acting as a BC
            faAsBC = PetalsExecutionContext.initFlowAttributes();
            this.consumes.getLogger().log(Level.WARNING,
                    "There is no flow attributes in the Execution Context: "
                            + "either we lost them somewhere in the route, "
                            + "either we received a petals exchange without flow attributes "
                            + "or we are acting as a BC and we are starting a new flow. "
                            + "We assume the later and initialise a new flow: " + faAsBC);
            this.monitTraceLogger.logMonitTrace(
                    new ConsumeExtFlowStepBeginLogData(faAsBC.getFlowInstanceId(), faAsBC.getFlowStepId()));
        } else {
            faAsBC = null;
        }

        try {
            final org.ow2.petals.component.framework.api.message.Exchange exchange = createPetalsExchange(
                    camelExchange);

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
                    public void done(final org.ow2.petals.component.framework.api.message.Exchange exchange,
                            final boolean timedOut) {

                        if (consumes.getLogger().isLoggable(Level.FINE)) {
                            consumes.getLogger()
                                    .fine("Handling a Petals exchange (with id: " + exchange.getExchangeId()
                                            + ") back from a send in async mode "
                                            + (doneSync ? "(but executed in sync mode apparently)" : ""));
                        }

                        handleAnswer(camelExchange, exchange, timedOut, doneSync, callback, faAsBC);
                    }
                });
                return doneSync;
            }
        } catch (final Exception e) {
            // these exceptions can only happens before the message is sent (or if the send fails), so before
            // handleAnswer could be called, thus this is done synchronously in either case of doSync and the callback
            // must be called
            final boolean doneSync = true;
            this.consumes.getLogger().log(Level.SEVERE,
                    "Just set an error on the Camel Exchange " + camelExchange.getExchangeId(), e);
            if (faAsBC != null) {
                this.monitTraceLogger.logMonitTrace(StepLogHelper.getMonitExtFailureTrace(faAsBC, e, true));
            }
            camelExchange.setException(e);
            callback.done(doneSync);
            return doneSync;
        }
    }

    /**
     * we tested in deploy that the camel endpoint options do not conflict with the consumes parameters, hence the
     * assert in the code.
     */
    private org.ow2.petals.component.framework.api.message.Exchange createPetalsExchange(final Exchange camelExchange)
            throws MessagingException {

        final MEPPatternConstants mep;
        if (getEndpoint().getService().getMEP() == null && getEndpoint().getMep() == null) {
            mep = MEPHelper.fromExchangePattern2MEPPatternConstants(camelExchange.getPattern());
            if (mep == null) {
                throw new MessagingException(
                        "Can't resolve MEP on the Camel exchange: " + camelExchange.getPattern().name());
            }
        } else {
            mep = getEndpoint().getMep();
        }

        // If mep is null, the consumes one will be used, and if not, we verified in deploy it was null in consumes
        final org.ow2.petals.component.framework.api.message.Exchange exchange = consumes.newExchange(mep,
                isFlowTracingActivated(camelExchange));

        // the idea is that if there was the service name in the consumes but not the endpoint name,
        // we can still resolve the endpoint now
        final QName serviceName;
        if (getEndpoint().getServiceName() == null) {
            // could be null, but it's ok
            serviceName = exchange.getService();
        } else {
            assert exchange.getService() == null;
            serviceName = getEndpoint().getServiceName();
        }

        final String endpointName = getEndpoint().getEndpointName();
        final QName operation = getEndpoint().getOperation();

        if (serviceName != null && endpointName != null) {
            assert exchange.getEndpoint() == null;
            final ServiceEndpoint ep = consumes.resolveEndpoint(serviceName, endpointName);
            if (ep == null) {
                throw new MessagingException(
                        "Can't resolve endpoint for service " + serviceName + " and endpoint name " + endpointName);
            }
            exchange.setEndpoint(ep);
            // let's set it anyway in case it's useful
            exchange.setService(serviceName);
        } else if (serviceName != null) {
            exchange.setService(serviceName);
        }

        if (operation != null) {
            // we tested in deploy that the camel endpoint options do not conflict with the consumes parameters
            assert exchange.getOperation() == null;
            exchange.setOperation(operation);
        }

        // TODO should I check that the camel exchange has the same MEP as the consumes MEP? or compatibility?
        // for example if I have a inonly exchange sent to an inout service, then I just discard the out
        // while an InOut exchange for an InOnly service is not possible!
        // TODO and also I should take into account the MEP of the endpoint??!!

        Conversions.populateNewPetalsExchange(camelExchange, exchange);

        return exchange;
    }

    /**
     * <p>
     * Retrieve the value of the flow tracing activation in the given Camel exchange according to the property
     * '{@value PetalsCamelComponent#EXCHANGE_CURRENT_FLOW_TRACING_ACTIVATION}' defined in the given Camel exchange.
     * </p>
     * 
     * @param camelExchange
     *            A camel exchange that can contain the property driving flow tracing activation. Not {@code null}.
     * @return The flow tracing activation state set in the given exchange. If it is not set in the exchange,
     *         {@code Optional#empty()} is returned.
     * @throws InvalidFlowTracingActivationExchangePropertyValueException
     *             The value of the property is invalid.
     */
    private static final Optional<Boolean> isFlowTracingActivated(final Exchange camelExchange)
            throws InvalidFlowTracingActivationExchangePropertyValueException {
        assert camelExchange != null;

        final Object msgFlowTracingActivationObj = camelExchange
                .getProperty(PetalsConstants.EXCHANGE_CURRENT_FLOW_TRACING_ACTIVATION);
        if (msgFlowTracingActivationObj == null) {
            return Optional.empty();
        } else if (msgFlowTracingActivationObj instanceof Boolean msgFlowTracingActivation) {
            return Optional.of(msgFlowTracingActivation);
        } else {
            // Here because msgFlowTracingActivationObj == null or is not an instance of Boolean
            throw new InvalidFlowTracingActivationExchangePropertyValueException(msgFlowTracingActivationObj);
        }
    }

    private void handleAnswer(final Exchange camelExchange,
            final org.ow2.petals.component.framework.api.message.Exchange exchange, final boolean timedOut,
            final boolean doneSync, final AsyncCallback callback, @Nullable final FlowAttributes faAsBC) {
        if (timedOut) {
            // A timeout warning message is already log by Petals CDK Core
            this.consumes.getLogger().fine(
                    "The exchange I sent to the NMR never got acknowledged, it timed out: " + exchange.getExchangeId());

            final FlowAttributes currentFlowAttributes;
            if (faAsBC != null) {
                // The component is acting as a binding component.
                currentFlowAttributes = faAsBC;
            } else {
                // The component is acting as a service engine
                currentFlowAttributes = PetalsExecutionContext.getFlowAttributes();
            }

            final Exception timeoutException = new TimeoutException(
                    this.consumes.buildTimeoutErrorMsg(this.getEndpoint().getTimeout(), currentFlowAttributes));
            camelExchange.setException(timeoutException);

            if (faAsBC != null) {
                // The component is acting as a binding component.

                // We should log the trace ourselves without touching the message here because we don't have the
                // ownership!
                this.monitTraceLogger.logMonitTrace(new ConsumeExtFlowStepFailureLogData(faAsBC.getFlowInstanceId(),
                        faAsBC.getFlowStepId(), timeoutException.getMessage()));
            } else {
                // The component is acting as a service engine

                // The processing of the timeout exception is delegated to the Camel route
            }

        } else {
            this.consumes.getLogger().fine("Got an answer for the request I sent to the NMR for exchange "
                    + exchange.getExchangeId() + ", sending status DONE to provider and passing it back into Camel");

            Conversions.populateAnswerCamelExchange(exchange, camelExchange);

            if (faAsBC != null) {
                this.monitTraceLogger.logMonitTrace(
                        StepLogHelper.getMonitExtEndOrFailureTrace(exchange.getMessageExchange(), faAsBC, true));
            }

            if (exchange.isActiveStatus()) {
                // Note: even if it's InOptOut, we can send back done, so it's ok
                try {
                    // TODO We should send it only when we get the done as the provider...
                    // see also comments is PetalsCamelConsumer
                    exchange.setDoneStatus();
                    this.consumes.send(exchange);
                } catch (final MessagingException e) {
                    this.consumes.getLogger().log(Level.WARNING,
                            "Can't send back status DONE for exchange " + exchange.getExchangeId(), e);
                }
            }
        }

        callback.done(doneSync);
    }

    @SuppressWarnings("null")
    @Override
    public PetalsCamelEndpoint getEndpoint() {
        return (PetalsCamelEndpoint) super.getEndpoint();
    }
}
