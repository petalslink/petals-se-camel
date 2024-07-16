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
package org.ow2.petals.se.camel.it;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.ow2.petals.component.framework.test.Assert.assertMonitConsumerExtBeginLog;
import static org.ow2.petals.component.framework.test.Assert.assertMonitConsumerExtEndLog;
import static org.ow2.petals.component.framework.test.Assert.assertMonitConsumerExtTimeoutLog;
import static org.ow2.petals.component.framework.test.Assert.assertMonitProviderBeginLog;
import static org.ow2.petals.component.framework.test.Assert.assertMonitProviderEndLog;
import static org.ow2.petals.component.framework.test.Assert.assertMonitProviderFailureLog;
import static org.ow2.petals.component.framework.test.Assert.assertMonitProviderTimeoutLog;

import java.time.Duration;
import java.util.List;
import java.util.logging.LogRecord;

import javax.jbi.messaging.ExchangeStatus;

import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Test;
import org.ow2.petals.camel.component.exceptions.TimeoutException;
import org.ow2.petals.commons.log.FlowLogData;
import org.ow2.petals.commons.log.Level;
import org.ow2.petals.component.framework.junit.Message;
import org.ow2.petals.component.framework.junit.RequestMessage;
import org.ow2.petals.component.framework.junit.StatusMessage;
import org.ow2.petals.component.framework.junit.helpers.MessageChecks;
import org.ow2.petals.component.framework.junit.helpers.ServiceProviderImplementation;
import org.ow2.petals.component.framework.junit.impl.message.ResponseToConsumerMessage;
import org.ow2.petals.component.framework.listener.AbstractListener;
import org.ow2.petals.jbi.messaging.PetalsDeliveryChannel;
import org.ow2.petals.se.camel.AbstractComponentTest;
import org.ow2.petals.se.camel.mocks.TestRoutesOK;
import org.ow2.petals.se.camel.mocks.TestRoutesOK.SampleException;

/**
 * Contains tests that cover both petals-se-camel and camel-petals classes.
 * 
 * @author vnoel
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
            from("petals:sayHello-provider?synchronous=true").to("petals:theConsumesId");
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
            from("petals:sayHello-provider").to("petals:theConsumesId?synchronous=true");
        }
    }

    @Test
    public void testMessageGoThroughToSynchronous() throws Exception {
        deployHello(SU_NAME, WSDL11, RouteSyncTo.class);
        sendHelloIdentity(SU_NAME, MessageChecks.propertyExists(PetalsDeliveryChannel.PROPERTY_SENDSYNC));

        assertMONITOk();
    }

    @Test
    public void faultReplacedByResponse() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);

        COMPONENT.sendAndCheckResponseAndSendStatus(helloRequest(SU_NAME, "<aa/>"),
                ServiceProviderImplementation.faultMessage("<a-fault-to-skiped/>"),
                MessageChecks.noError().andThen(MessageChecks.noFault()).andThen(MessageChecks.hasOut())
                        .andThen(MessageChecks.hasXmlContent("<nothing-to-say/>")),
                ExchangeStatus.DONE);
    }

    @Test
    public void faultReplacedByAnotherOne() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);

        COMPONENT.sendAndCheckResponseAndSendStatus(helloRequest(SU_NAME, "<aa/>"),
                ServiceProviderImplementation.faultMessage("<voiceless-fault/>"),
                MessageChecks.noError().andThen(MessageChecks.hasFault()).andThen(MessageChecks.noOut())
                        .andThen(MessageChecks.hasXmlContent("<voiceless/>")),
                ExchangeStatus.DONE);
    }

    @Test
    public void faultReplacedByError() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);

        final StatusMessage status = COMPONENT.sendAndGetStatus(helloRequest(SU_NAME, "<aa/>"),
                ServiceProviderImplementation.faultMessage("<error-fault/>"));

        assertEquals(ExchangeStatus.ERROR, status.getStatus());
        assertEquals(TestRoutesOK.ERROR_INVOKING_SERVICE_PROVIDER_MSG, status.getError().getMessage());
    }

    @Test
    public void errorReplacedByResponse() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);

        COMPONENT.sendAndCheckResponseAndSendStatus(helloRequest(SU_NAME, "<aa/>"),
                ServiceProviderImplementation.errorMessage(
                        new SampleException(TestRoutesOK.ERROR_INVOKING_SERVICE_PROVIDER_MSG_MINOR_ERR)),
                MessageChecks.noError().andThen(MessageChecks.noFault()).andThen(MessageChecks.hasOut())
                        .andThen(MessageChecks.hasXmlContent("<nothing-to-say/>")),
                ExchangeStatus.DONE);
    }

    @Test
    public void errorReplacedByFault() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);

        COMPONENT.sendAndCheckResponseAndSendStatus(helloRequest(SU_NAME, "<aa/>"),
                ServiceProviderImplementation
                        .errorMessage(
                                new SampleException(TestRoutesOK.ERROR_INVOKING_SERVICE_PROVIDER_MSG_VOICE_ERR)),
                MessageChecks.noError().andThen(MessageChecks.hasFault()).andThen(MessageChecks.noOut())
                        .andThen(MessageChecks.hasXmlContent("<voiceless/>")),
                ExchangeStatus.DONE);
    }

    @Test
    public void errorReplacedByAnotherError() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);

        final StatusMessage status = COMPONENT.sendAndGetStatus(helloRequest(SU_NAME, "<aa/>"),
                ServiceProviderImplementation.errorMessage(
                        new SampleException(TestRoutesOK.ERROR_INVOKING_SERVICE_PROVIDER_MSG_NETWORK_ERR)));

        assertEquals(ExchangeStatus.ERROR, status.getStatus());
        assertEquals(TestRoutesOK.ERROR_INVOKING_SERVICE_PROVIDER_MSG, status.getError().getMessage());
    }

    @Test
    public void testMessageTimeoutAndSUStillWorks() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);

        final StatusMessage response = COMPONENT.sendAndGetStatus(helloRequest(SU_NAME, "<aa/>"),
                ServiceProviderImplementation.outMessage("<bb/>").with(new MessageChecks() {
                    @Override
                    public void checks(final Message message) throws Exception {
                        // let's wait more than the configured timeout duration
                        Thread.sleep(DEFAULT_TIMEOUT_FOR_COMPONENT_SEND + 1000);
                    }
                }));

        assertNotNull(response.getError());
        assertInstanceOf(TimeoutException.class, response.getError());

        // let's wait for the answer from the ServiceProvider to have been handled by the CDK
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(0, COMPONENT_UNDER_TEST.getExchangesInDeliveryChannelCount()));

        // clear the potential errors the CDK sent back to the service that ansewered too late
        COMPONENT_UNDER_TEST.clearRequestsFromConsumer();

        assertMONITFailureOK();

        // let's clear logs
        COMPONENT_UNDER_TEST.getInMemoryLogHandler().clear();

        // and now let's send another message that should work
        sendHelloIdentity(SU_NAME);

        assertMONITOk();
    }

    public static class RouteBC extends RouteBuilder {
        @Override
        public void configure() throws Exception {

            from("timer://petalsTimer?delay=500&period=500&repeatCount=1").to("petals:theConsumesId");
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

    @Test
    public void timeoutAsyncAsSE() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);

        final StatusMessage response = COMPONENT.sendAndGetStatus(helloRequest(SU_NAME, "<aa/>"),
                slowProvider("<bb/>"));

        assertNotNull(response.getError());
        assertInstanceOf(TimeoutException.class, response.getError());

        // let's wait for the answer from the ServiceProvider to have been handled by the CDK
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(0, COMPONENT_UNDER_TEST.getExchangesInDeliveryChannelCount()));

        assertTimeoutProviderLogWARNandMONIT(false);
    }

    @Test
    public void timeoutSyncAsSE() throws Exception {
        deployHello(SU_NAME, WSDL11, RouteSyncTo.class);

        final StatusMessage response = COMPONENT.sendAndGetStatus(helloRequest(SU_NAME, "<aa/>"),
                slowProvider("<bb/>"));

        assertNotNull(response.getError());
        assertInstanceOf(TimeoutException.class, response.getError());

        // let's wait for the answer from the ServiceProvider to have been handled by the CDK
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertEquals(0, COMPONENT_UNDER_TEST.getExchangesInDeliveryChannelCount()));

        assertTimeoutProviderLogWARNandMONIT(true);
    }

    @Test
    public void timeoutAsyncAsBC() throws Exception {
        // we won't be using the provides, but it's ok
        deployHello(SU_NAME, WSDL11, RouteBC.class);

        // TODO for now we have to disable acknoledgement check (with the null parameter) because we don't forward DONE
        // in Camel (see PetalsCamelConsumer)
        // Note: we need to wait for the end of the processing of the message by the component to be sure all the logs
        // are here.
        COMPONENT.receiveAsExternalProvider(slowProvider("<bb/>", null), true);

        assertTimeoutConsumerLogWARNandMONIT();

    }

    private static ServiceProviderImplementation slowProvider(final String content) {
        return slowProvider(content, MessageChecks.status(ExchangeStatus.DONE));
    }

    private static ServiceProviderImplementation slowProvider(final String content, final MessageChecks statusChecks) {
        return new ServiceProviderImplementation() {
            @Override
            public Message provides(final RequestMessage request) throws Exception {
                final Message response = new ResponseToConsumerMessage(request, content);
                Thread.sleep(DEFAULT_TIMEOUT_FOR_COMPONENT_SEND + 1000);
                return response;
            }

            @Override
            public void handleStatus(final StatusMessage status) throws Exception {
                assert statusChecks != null;
                statusChecks.checks(status);
            }

            @Override
            public boolean statusExpected() {
                return statusChecks != null;
            }
        };
    }

    private void assertTimeoutProviderLogWARNandMONIT(final boolean isSyncCall) {
        final List<LogRecord> monitLogs = COMPONENT_UNDER_TEST.getInMemoryLogHandler().getAllRecords(Level.MONIT);
        assertEquals(4, monitLogs.size());
        final FlowLogData firstLog = assertMonitProviderBeginLog(HELLO_INTERFACE, HELLO_SERVICE, HELLO_ENDPOINT,
                HELLO_OPERATION, monitLogs.get(0));

        final FlowLogData secondLog = assertMonitProviderBeginLog(firstLog, HELLO_INTERFACE, HELLO_SERVICE,
                EXTERNAL_ENDPOINT_NAME, HELLO_OPERATION, monitLogs.get(1));

        // it must be the third one (idx 2) because the fourth one (idx 3) is the monit end from the provider that
        // doesn't see the timeout
        assertMonitProviderTimeoutLog(DEFAULT_TIMEOUT_FOR_COMPONENT_SEND, HELLO_INTERFACE, HELLO_SERVICE,
                EXTERNAL_ENDPOINT_NAME, HELLO_OPERATION, firstLog, monitLogs.get(2));

        if (isSyncCall) {
            // The instance message exchanges between the Camel service consumer and the Camel service provider (not the
            // external service provider) is the same for synchronous call, so the end status of the Camel service
            // provider can not be DONE because set as ERROR by the timeout processing.
            // TODO: Fix this problem in the Petals CDK Junit
        } else {
            // the provider answers, but too late, so it happens AFTER the failure of the consumer
            assertMonitProviderEndLog(secondLog, monitLogs.get(3));
        }

        // Assertion about the timeout warning message
        assertTimeoutWarnLog(1, 0, firstLog);
    }

    private void assertTimeoutConsumerLogWARNandMONIT() {
        final List<LogRecord> monitLogs = COMPONENT_UNDER_TEST.getInMemoryLogHandler().getAllRecords(Level.MONIT);
        assertEquals(4, monitLogs.size());
        final FlowLogData firstLog = assertMonitConsumerExtBeginLog(monitLogs.get(0));

        final FlowLogData secondLog = assertMonitProviderBeginLog(firstLog, HELLO_INTERFACE, HELLO_SERVICE,
                EXTERNAL_ENDPOINT_NAME, HELLO_OPERATION, monitLogs.get(1));

        // it must be the third one (idx 2) because the fourth one (idx 3) is the monit end from the provider that
        // doesn't see the timeout
        assertMonitConsumerExtTimeoutLog(DEFAULT_TIMEOUT_FOR_COMPONENT_SEND, HELLO_INTERFACE, HELLO_SERVICE,
                EXTERNAL_ENDPOINT_NAME, HELLO_OPERATION, firstLog, monitLogs.get(2));

        // the provider answers, but too late, so it happens AFTER the failure of the consumer
        assertMonitProviderEndLog(secondLog, monitLogs.get(3));

        // Assertion about the timeout warning message (Caution, another warning message about missing flow attributes
        // exists)
        assertTimeoutWarnLog(2, 1, firstLog);
    }

    private void assertTimeoutWarnLog(final int expectedWarnLogMsgNb, final int expectedWarnLogMsgPos,
            final FlowLogData firstLog) {
        assert expectedWarnLogMsgPos < expectedWarnLogMsgNb;

        final List<LogRecord> warnRecords = COMPONENT_UNDER_TEST.getInMemoryLogHandler()
                .getAllRecords(java.util.logging.Level.WARNING);
        assertEquals(expectedWarnLogMsgNb, warnRecords.size());
        assertEquals(String.format(AbstractListener.TIMEOUT_WARN_LOG_MSG_PATTERN, DEFAULT_TIMEOUT_FOR_COMPONENT_SEND,
                HELLO_INTERFACE.toString(), HELLO_SERVICE.toString(), EXTERNAL_ENDPOINT_NAME,
                HELLO_OPERATION.toString(), firstLog.get(FlowLogData.FLOW_INSTANCE_ID_PROPERTY_NAME),
                firstLog.get(FlowLogData.FLOW_STEP_ID_PROPERTY_NAME)),
                warnRecords.get(expectedWarnLogMsgPos).getMessage());
    }

    public void assertMONITFailureOK() {
        final List<LogRecord> monitLogs = COMPONENT_UNDER_TEST.getInMemoryLogHandler().getAllRecords(Level.MONIT);
        assertEquals(4, monitLogs.size());
        final FlowLogData firstLog = assertMonitProviderBeginLog(HELLO_INTERFACE, HELLO_SERVICE, HELLO_ENDPOINT,
                HELLO_OPERATION, monitLogs.get(0));

        final FlowLogData secondLog = assertMonitProviderBeginLog(firstLog, HELLO_INTERFACE, HELLO_SERVICE,
                EXTERNAL_ENDPOINT_NAME, HELLO_OPERATION, monitLogs.get(1));

        // it must be the third one (idx 2) because the fourth one (idx 3) is the monit end from the provider that
        // doesn't see the timeout
        assertMonitProviderFailureLog(firstLog, monitLogs.get(2));

        // the provider answers, but too late, so it happens AFTER the failure of the consumer
        assertMonitProviderEndLog(secondLog, monitLogs.get(3));
    }

    public void assertMONITOk() {
        final List<LogRecord> monitLogs = COMPONENT_UNDER_TEST.getInMemoryLogHandler().getAllRecords(Level.MONIT);
        assertEquals(4, monitLogs.size());
        final FlowLogData firstLog = assertMonitProviderBeginLog(HELLO_INTERFACE, HELLO_SERVICE, HELLO_ENDPOINT,
                HELLO_OPERATION, monitLogs.get(0));
        assertMonitProviderEndLog(firstLog, monitLogs.get(3));

        final FlowLogData secondLog = assertMonitProviderBeginLog(firstLog, HELLO_INTERFACE, HELLO_SERVICE,
                EXTERNAL_ENDPOINT_NAME, HELLO_OPERATION, monitLogs.get(1));
        assertMonitProviderEndLog(secondLog, monitLogs.get(2));
    }

    private void assertMONITasBCOk() {
        final List<LogRecord> monitLogs = COMPONENT_UNDER_TEST.getInMemoryLogHandler().getAllRecords(Level.MONIT);
        assertEquals(4, monitLogs.size());

        final FlowLogData consumeExtLog = assertMonitConsumerExtBeginLog(monitLogs.get(0));
        final FlowLogData provideLog = assertMonitProviderBeginLog(consumeExtLog, HELLO_INTERFACE, HELLO_SERVICE,
                EXTERNAL_ENDPOINT_NAME, HELLO_OPERATION, monitLogs.get(1));
        assertMonitProviderEndLog(provideLog, monitLogs.get(2));
        assertMonitConsumerExtEndLog(consumeExtLog, monitLogs.get(3));
    }
}
