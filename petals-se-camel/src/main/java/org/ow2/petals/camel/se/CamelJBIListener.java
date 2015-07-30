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
package org.ow2.petals.camel.se;

import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.messaging.MessagingException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.ow2.petals.camel.PetalsCamelRoute;
import org.ow2.petals.camel.se.impl.PetalsCamelAsyncContext;
import org.ow2.petals.commons.log.Level;
import org.ow2.petals.commons.log.PetalsExecutionContext;
import org.ow2.petals.component.framework.api.message.Exchange;
import org.ow2.petals.component.framework.listener.AbstractJBIListener;
import org.ow2.petals.component.framework.process.async.AsyncContext;

/**
 * This class plays the role of the bridge between the SE and the rest of Petals
 * 
 * The instance is stateless, it dispatches messages, but it is also used by other classes to send messages back
 * 
 * @author vnoel
 *
 */
public class CamelJBIListener extends AbstractJBIListener {

    @NonNullByDefault(false)
    @Override
    public boolean onJBIMessage(final Exchange exchange) {
        final Logger logger = this.getLogger();

        logger.fine("Start CamelJBIListener.onJBIMessage()");

        final String logHint = "Exchange " + exchange.getExchangeId();

        try {
            if (exchange.isActiveStatus()) {

                try {
                    // TODO actually this should never happen, or there is a bug
                    // (well there is a bug because our camel consumer sends with send and not sendasync so we may
                    // receive answers to our answers...
                    if (!exchange.isProviderRole()) {
                        // caught in the catch clause below
                        throw new MessagingException("The exchange must be Provider!");
                    }

                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(logHint + " was received and is started to be processed.");
                        logger.fine("interfaceName = " + exchange.getInterfaceName());
                        logger.fine("Service       = " + exchange.getService());
                        logger.fine("EndpointName  = " + exchange.getEndpointName());
                        logger.fine("OperationName = " + exchange.getOperationName());
                        logger.fine("Pattern " + exchange.getPattern());
                    }
                    
                    final PetalsCamelRoute route = getCamelSE().getCamelSUManager().getRoute(exchange);

                    logger.info("Let's start processing " + logHint + " with Camel");

                    final boolean doneSync = route.process(exchange);

                    if (doneSync) {
                        logger.info("Processing of " + logHint + " with Camel is finished (happened synchronously)");
                    } else {
                        logger.info("Processing of " + logHint + " with Camel will finish asynchronously");
                    }

                    // we always take care of answering things in the processing!
                    return false;
                } catch (final JBIException e) {
                    // This concerns all exceptions but the processing of the message itself!
                    logger.log(Level.SEVERE, logHint + " encountered a problem.", e);
                    exchange.setError(e);
                }
            } else if (exchange.isErrorStatus()) {
                logger.warning(logHint + " received with a status 'ERROR'. Skipped!");
            } else if (exchange.isDoneStatus()) {
                logger.info(logHint + " received with a status 'DONE'. Skipped");
            }

            // let the CDK handle the response (either an error occured or it is an error message or a done message)
            return true;
        } finally {
            logger.fine("End CamelJBIListener.onJBIMessage()");
        }
    }

    @NonNullByDefault(false)
    @Override
    public boolean onAsyncJBIMessage(final Exchange exchange, final AsyncContext asyncContext) {
        // let's call the callback, the one that sent this message will take care of doing what it has to do
        return handleAsyncJBIMessage(exchange, asyncContext, false);
    }

    @NonNullByDefault(false)
    @Override
    public boolean onExpiredAsyncJBIMessage(final Exchange originalExchange, final AsyncContext asyncContext) {
        // this is when I sent something asynchronously but it timeouted!
        // let's call the callback, the one that sent this message will take care of doing what it has to do
        return handleAsyncJBIMessage(originalExchange, asyncContext, true);
    }

    private boolean handleAsyncJBIMessage(final Exchange exchange, final AsyncContext asyncContext,
            final boolean timedOut) {
        if (!(asyncContext instanceof PetalsCamelAsyncContext)) {
            this.getLogger().warning(
                    "Got an async context not from me for the exchange "
                            + asyncContext.getOriginalExchange().getExchangeId());
        } else {
            this.getLogger().info(
                    "Received an async answer, let's continue our execution for the exchange "
                            + asyncContext.getOriginalExchange().getExchangeId());

            final PetalsCamelAsyncContext context = (PetalsCamelAsyncContext) asyncContext;

            // let's reinitialise the flow attributes of the current thread with the right data
            PetalsExecutionContext.putFlowAttributes(context.getFlowAttributes());

            context.getCallback().done(timedOut);
        }

        // always return false, we will take care of answering
        return false;
    }

    @SuppressWarnings("null")
    public CamelSE getCamelSE() {
        return (CamelSE) super.component;
    }

}
