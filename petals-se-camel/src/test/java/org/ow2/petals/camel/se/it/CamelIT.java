/**
 * Copyright (c) 2015-2024 Linagora
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
package org.ow2.petals.camel.se.it;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.to;
import static com.jayway.awaitility.Duration.TWO_SECONDS;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.logging.LogRecord;

import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;
import org.ow2.petals.camel.component.PetalsCamelProducer;
import org.ow2.petals.camel.se.AbstractComponentTest;
import org.ow2.petals.camel.se.mocks.TestRoutesOK;
import org.ow2.petals.commons.log.FlowLogData;
import org.ow2.petals.commons.log.Level;
import org.ow2.petals.component.framework.junit.Message;
import org.ow2.petals.component.framework.junit.StatusMessage;
import org.ow2.petals.component.framework.junit.helpers.MessageChecks;
import org.ow2.petals.component.framework.junit.helpers.ServiceProviderImplementation;
import org.ow2.petals.jbi.messaging.PetalsDeliveryChannel;

/**
 * Contains tests that cover both petals-se-camel and camel-petals classes.
 * 
 * @author vnoel
 *
 */
public class CamelIT extends AbstractComponentTest {

    @Test
    public void testMessageGoThrough() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);
        sendHelloIdentity(SU_NAME);
        assertMONITOk();
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
        sendHelloIdentity(SU_NAME, MessageChecks.propertyNotExists(PetalsDeliveryChannel.PROPERTY_SENDSYNC));

        assertMONITOk();
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
        sendHelloIdentity(SU_NAME, MessageChecks.propertyExists(PetalsDeliveryChannel.PROPERTY_SENDSYNC));

        assertMONITOk();
    }

    @Test
    public void testMessageTimeoutAndSUStillWorks() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);

        final StatusMessage response = COMPONENT.sendAndGetStatus(helloRequest(SU_NAME, "<aa/>"),
                ServiceProviderImplementation.outMessage("<bb/>").with(new MessageChecks() {
                    @SuppressWarnings("squid:S2925")
                    @Override
                    public void checks(final Message message) throws Exception {
                        // let's wait more than the configured timeout duration
                        Thread.sleep(DEFAULT_TIMEOUT_FOR_COMPONENT_SEND + 1000);
                    }
                }));

        assertNotNull(response.getError());
        assertTrue(response.getError() == PetalsCamelProducer.TIMEOUT_EXCEPTION);
        
        // let's wait for the answer from the ServiceProvider to have been handled by the CDK
        await().atMost(TWO_SECONDS).untilCall(to(COMPONENT_UNDER_TEST).getExchangesInDeliveryChannelCount(),
                equalTo(0));

        // clear the potential errors the CDK sent back to the service that ansewered too late
        COMPONENT_UNDER_TEST.clearRequestsFromConsumer();

        assertMONITFailureOK();

        // let's clear logs
        IN_MEMORY_LOG_HANDLER.clear();

        // and now let's send another message that should work
        sendHelloIdentity(SU_NAME);

        assertMONITOk();
    }

    public static class RouteBC extends RouteBuilder {
        @Override
        public void configure() throws Exception {

            from("timer://petalsTimer?delay=500&period=500&repeatCount=1")
                    .to("petals:theConsumesId");
        }
    }

    @Test
    public void testAsBC() throws Exception {
        // we won't be using the provides, but it's ok
        deployHello(SU_NAME, WSDL11, RouteBC.class);

        // TODO for now we have to disable acknoledgement check (with the null parameter) because we don't forward DONE
        // in Camel (see PetalsCamelConsumer)
        // Note: we need to wait for the end of the processing of the message by the component to be sure all the logs
        // are here.
        COMPONENT.receiveAsExternalProvider(ServiceProviderImplementation.outMessage("<bb/>", null), true);

        assertMONITasBCOk();

    }

    public void assertMONITFailureOK() {
        final List<LogRecord> monitLogs = IN_MEMORY_LOG_HANDLER.getAllRecords(Level.MONIT);
        assertEquals(4, monitLogs.size());
        final FlowLogData firstLog = assertMonitProviderBeginLog(HELLO_INTERFACE, HELLO_SERVICE, HELLO_ENDPOINT,
                HELLO_OPERATION, monitLogs.get(0));

        final FlowLogData secondLog = assertMonitProviderBeginLog(firstLog, HELLO_INTERFACE,
                HELLO_SERVICE, EXTERNAL_ENDPOINT_NAME, HELLO_OPERATION, monitLogs.get(1));

        // it must be the third one (idx 2) because the fourth one (idx 3) is the monit end from the provider that
        // doesn't see the timeout
        assertMonitProviderFailureLog(firstLog, monitLogs.get(2));

        // the provider answers, but too late, so it happens AFTER the failure of the consumer
        assertMonitProviderEndLog(secondLog, monitLogs.get(3));
    }

    public void assertMONITOk() {
        final List<LogRecord> monitLogs = IN_MEMORY_LOG_HANDLER.getAllRecords(Level.MONIT);
        assertEquals(4, monitLogs.size());
        final FlowLogData firstLog = assertMonitProviderBeginLog(HELLO_INTERFACE, HELLO_SERVICE, HELLO_ENDPOINT,
                HELLO_OPERATION, monitLogs.get(0));
        assertMonitProviderEndLog(firstLog, monitLogs.get(3));

        final FlowLogData secondLog = assertMonitProviderBeginLog(firstLog, HELLO_INTERFACE,
                HELLO_SERVICE, EXTERNAL_ENDPOINT_NAME, HELLO_OPERATION, monitLogs.get(1));
        assertMonitProviderEndLog(secondLog, monitLogs.get(2));
    }

    private void assertMONITasBCOk() {
        final List<LogRecord> monitLogs = IN_MEMORY_LOG_HANDLER.getAllRecords(Level.MONIT);
        assertEquals(4, monitLogs.size());

        final FlowLogData consumeExtLog = assertMonitConsumerExtBeginLog(monitLogs.get(0));
        final FlowLogData provideLog = assertMonitProviderBeginLog(consumeExtLog, HELLO_INTERFACE, HELLO_SERVICE,
                EXTERNAL_ENDPOINT_NAME, HELLO_OPERATION, monitLogs.get(1));
        assertMonitProviderEndLog(provideLog, monitLogs.get(2));
        assertMonitConsumerExtEndLog(consumeExtLog, monitLogs.get(3));
    }
}
