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
package org.ow2.petals.se.camel.mocks;

import javax.jbi.messaging.MessagingException;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePropertyKey;
import org.ow2.petals.camel.helpers.PetalsRouteBuilder;
import org.ow2.petals.camel.helpers.Step;

public class TestRoutesOK extends PetalsRouteBuilder {

    public static final String FAULT_INVOKING_SERVICE_PROVIDER_MSG = "A fault occurs during invocation of the service provider";

    public static final String ERROR_INVOKING_SERVICE_PROVIDER_MSG = "An exception occurs during invocation of the service provider";

    public static final String ERROR_INVOKING_SERVICE_PROVIDER_MSG_VOICE_ERR = "Voice error";

    public static final String ERROR_INVOKING_SERVICE_PROVIDER_MSG_NETWORK_ERR = "Network error";

    public static final String ERROR_INVOKING_SERVICE_PROVIDER_MSG_MINOR_ERR = "Minor error";

    public static class SampleException extends MessagingException {

        private static final long serialVersionUID = -746338213948517609L;

        public SampleException(final String msg) {
            super(msg);
        }
    }

    @Override
    public void configure() throws Exception {
        from("petals:sayHello-provider").doTry().to("petals:theConsumesId")
                .process(new Step("Processing of response of the service provider") {
                    @Override
                    public void process(final Exchange exchange) throws Exception {
                        if (isJbiFailed(exchange)) {
                            if (isJbiFault(exchange)) {
                                if (exchange.getMessage().getBody(String.class).contains("<a-fault-to-skiped/>")) {
                                    // Here: use case where the fault received is transformed into a out message
                                    exchange.getOut().setBody("<nothing-to-say/>");
                                } else if (exchange.getMessage().getBody(String.class).contains("<voiceless-fault/>")) {
                                    // Here: use case where the fault received is transformed into another fault
                                    setJbiFault(exchange, "<voiceless/>");
                                } else if (exchange.getMessage().getBody(String.class).contains("sayHelloResponse")) {
                                    // Here: use case (for unit test 'MonitTraceFileteringTest') where the fault
                                    // received is kept as-is
                                } else {
                                    // Here: use case where the fault received is transformed into an error
                                    throw new Exception(ERROR_INVOKING_SERVICE_PROVIDER_MSG);
                                }
                            } else {
                                if (ERROR_INVOKING_SERVICE_PROVIDER_MSG_VOICE_ERR
                                        .equals(exchange.getException().getMessage())) {
                                    // Here: use case where the error received is transformed into a fault
                                    setJbiFault(exchange, "<voiceless/>");
                                } else {
                                    // Here: use case where the error received is transformed into another error
                                    throw new Exception(ERROR_INVOKING_SERVICE_PROVIDER_MSG);
                                }
                            }
                        } else {
                            // Here: use case where the response received is kept as-is
                        }
                    }
                }).doCatch(SampleException.class).process(new Step("Processing of errors of the service provider") {
                    @Override
                    public void process(final Exchange exchange) throws Exception {

                        // We retrieve the error from exchange properties. The field 'exception' is previously
                        // reinitialized by the 'CatchProcessor'.
                        final SampleException error = (SampleException) exchange
                                .getProperty(ExchangePropertyKey.EXCEPTION_CAUGHT);

                        if (ERROR_INVOKING_SERVICE_PROVIDER_MSG_VOICE_ERR.equals(error.getMessage())) {
                            // Here: use case where the error received is transformed into a fault
                            setJbiFault(exchange, "<voiceless/>");
                        } else if (ERROR_INVOKING_SERVICE_PROVIDER_MSG_MINOR_ERR.equals(error.getMessage())) {
                            // Here: use case where the error received is transformed into a out message
                            exchange.getMessage().setBody("<nothing-to-say/>");
                        } else {
                            // Here: use case where the error received is transformed into another error
                            throw new SampleException(ERROR_INVOKING_SERVICE_PROVIDER_MSG);
                        }
                    }
                }).end();

        from("petals:sayHelloWithoutEcho-provider").to("petals:theConsumesId");

        from("petals:sayHelloWithoutEchoRobust-provider").to("petals:theConsumesId");
    }

}
