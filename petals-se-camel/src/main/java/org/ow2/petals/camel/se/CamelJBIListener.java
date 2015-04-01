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

import javax.jbi.messaging.MessagingException;

import org.eclipse.jdt.annotation.Nullable;
import org.ow2.petals.camel.exceptions.TimeoutException;
import org.ow2.petals.camel.se.datatypes.PetalsCamelAsyncContext;
import org.ow2.petals.commons.log.Level;
import org.ow2.petals.component.framework.api.message.Exchange;
import org.ow2.petals.component.framework.listener.AbstractJBIListener;
import org.ow2.petals.component.framework.process.async.AsyncContext;

import com.google.common.base.Preconditions;

/**
 * This class plays the role of the bridge between the SE and the rest of Petals
 * 
 * The instance is stateless, it dispatches messages, but it is also used by other classes to send messages back
 * 
 * @author vnoel
 *
 */
public class CamelJBIListener extends AbstractJBIListener {

    @Override
    public boolean onJBIMessage(final @Nullable Exchange exchange) {
        final Logger logger = this.getLogger();

        Preconditions.checkNotNull(exchange);

        logger.fine("Start CamelJBIListener.onJBIMessage()");
        try {
            if (exchange.isActiveStatus()) {

                final String logHint = "Exchange " + exchange.getExchangeId();

                try {
                    if (!exchange.isProviderRole()) {
                        throw new MessagingException("The exchange must be Provider!");
                    }

                    if (!exchange.isInMessage()) {
                        throw new MessagingException("The exchange must be IN!");
                    }

                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(logHint + " was received and is started to be processed.");
                        logger.fine("interfaceName = " + exchange.getInterfaceName());
                        logger.fine("Service       = " + exchange.getService());
                        logger.fine("EndpointName  = " + exchange.getEndpointName());
                        logger.fine("OperationName = " + exchange.getOperationName());
                        logger.fine("Pattern " + exchange.getPattern());
                    }
                    
                    getCamelSE().getCamelSUManager().process(exchange);

                    // we always take care of answering things in the camel consumer (except if an exception happens
                    // during execution of process)
                    return false;
                } catch (final Exception e) {
                    logger.log(Level.SEVERE, "Exchange " + exchange.getExchangeId() + " encountered a problem.", e);
                    exchange.setError(e);
                }
            } else if (exchange.isErrorStatus()) {
                logger.warning("Exchange " + exchange.getExchangeId() + " received with a status 'ERROR'. Skipped !");
            }

            // something bad happened, let the CDK handle the response!
            return true;
        } finally {
            logger.fine("End CamelJBIListener.onJBIMessage()");
        }
    }

    @Override
    public boolean onAsyncJBIMessage(final @Nullable Exchange exchange, final @Nullable AsyncContext asyncContext) {

        Preconditions.checkNotNull(exchange);
        Preconditions.checkNotNull(asyncContext);

        if (!(asyncContext instanceof PetalsCamelAsyncContext)) {
            this.getLogger().warning("Got an async context not from me!!! " + asyncContext);
        } else {
            final PetalsCamelAsyncContext context = (PetalsCamelAsyncContext) asyncContext;
            // let's call the callback, the one that sent this message will take care of doing what it has to do
            context.getCallback().run();
        }

        // always return false, we will take care of answering
        return false;
    }

    @Override
    public boolean onExpiredAsyncJBIMessage(final @Nullable Exchange originalExchange,
            final @Nullable AsyncContext asyncContext) {

        Preconditions.checkNotNull(originalExchange);
        Preconditions.checkNotNull(asyncContext);

        if (!(asyncContext instanceof PetalsCamelAsyncContext)) {
            this.getLogger().warning("Got an async context not from me!!! " + asyncContext);
            return false;
        } else {
            // this is when I sent something asynchronously but it timeouted!
            final PetalsCamelAsyncContext context = (PetalsCamelAsyncContext) asyncContext;

            originalExchange.setError(new TimeoutException(originalExchange));

            context.getCallback().run();
        }

        // always return false, we will take care of answering
        return false;
    }

    public CamelSE getCamelSE() {
        return (CamelSE) super.component;
    }

}
