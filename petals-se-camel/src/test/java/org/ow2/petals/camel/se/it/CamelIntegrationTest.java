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
package org.ow2.petals.camel.se.it;

import javax.jbi.messaging.MessagingException;

import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;
import org.ow2.petals.camel.component.exceptions.TimeoutException;
import org.ow2.petals.camel.se.AbstractComponentTest;
import org.ow2.petals.camel.se.mocks.TestRoutesOK;
import org.ow2.petals.component.framework.junit.Component;
import org.ow2.petals.component.framework.junit.RequestMessage;
import org.ow2.petals.component.framework.junit.ResponseMessage;

/**
 * Contains tests that cover both petals-se-camel and camel-petals classes.
 * 
 * @author vnoel
 *
 */
public class CamelIntegrationTest extends AbstractComponentTest {

    @Test
    public void testMessageGoThrough() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);
        sendHelloIdentity(SU_NAME);
    }

    public static class RouteSyncFrom extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("petals:theProvidesId?synchronous=true").to("petals:theConsumesId");
        }
    }

    @Test
    public void testMessageGoThroughFromSynchronous() throws Exception {
        deployHello(SU_NAME, WSDL11, RouteSyncFrom.class);
        // if the from is sync but not the to, then it shouldn't be send synchronously...
        // the only thing that should happen is that the route execution blocks the caller
        sendHelloIdentity(SU_NAME, MessageChecks.propertyNotExists(Component.SENDSYNC_EXCHANGE_PROPERTY));
    }

    public static class RouteSyncTo extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("petals:theProvidesId").to("petals:theConsumesId?synchronous=true");
        }
    }

    @Test
    public void testMessageGoThroughToSynchronous() throws Exception {
        deployHello(SU_NAME, WSDL11, RouteSyncTo.class);
        sendHelloIdentity(SU_NAME, MessageChecks.propertyExists(Component.SENDSYNC_EXCHANGE_PROPERTY));
    }

    @Test
    public void testMessageTimeoutAndSUStillWorks() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);

        final ResponseMessage response = sendAndCheckConsumer(helloRequest(SU_NAME, ""), new ExternalServiceImplementation() {
            @Override
            public ResponseMessage provides(final RequestMessage request) throws Exception {
                // let's wait more than the timeout duration
                Thread.sleep(2000);
                // this shouldn't be returned normally...
                return ExternalServiceImplementation.outMessage("").provides(request);
            }
        }, isHelloRequest());

        assertNotNull(response.getError());
        assertTrue(response.getError() instanceof MessagingException);
        assertTrue(response.getError().getMessage().contains(TimeoutException.class.getName()));

        // let's wait for the external service to finally answer before clearing the channel
        Thread.sleep(2000);

        // there will be left-overs (the timeout answer to the external service), let's remove them!
        COMPONENT_UNDER_TEST.clearRequestsFromConsumer();

        // and now let's send another message that should work
        sendHelloIdentity(SU_NAME);
    }
}
