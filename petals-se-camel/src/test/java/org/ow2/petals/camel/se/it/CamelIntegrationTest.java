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

import java.io.File;
import java.util.List;
import java.util.logging.LogRecord;

import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ow2.petals.camel.component.PetalsCamelProducer;
import org.ow2.petals.camel.se.AbstractComponentTest;
import org.ow2.petals.camel.se.mocks.TestRoutesOK;
import org.ow2.petals.commons.log.FlowLogData;
import org.ow2.petals.commons.log.Level;
import org.ow2.petals.commons.log.PetalsExecutionContext;
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
        sendHelloIdentity(SU_NAME, MessageChecks.propertyNotExists(Component.SENDSYNC_EXCHANGE_PROPERTY));

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
        sendHelloIdentity(SU_NAME, MessageChecks.propertyExists(Component.SENDSYNC_EXCHANGE_PROPERTY));

        assertMONITOk();
    }

    @Test
    public void testMessageTimeoutAndSUStillWorks() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);

        final ResponseMessage response = sendAndCheckConsumer(helloRequest(SU_NAME, "<aa/>"),
                new ExternalServiceImplementation() {
            @Override
            public ResponseMessage provides(final RequestMessage request) throws Exception {
                // let's wait more than the timeout duration
                Thread.sleep(2000);
                // this shouldn't be returned normally...
                return outMessage("<bb/>").provides(request);
            }
        }, isHelloRequest());

        assertNotNull(response.getError());
        assertTrue(response.getError() == PetalsCamelProducer.TIMEOUT_EXCEPTION);

        // let's wait for the external service to finally answer before continuing
        Thread.sleep(2000);

        // the answer should be handled by the CDK at that point
        assertEquals(0, COMPONENT_UNDER_TEST.getRequestsFromConsumerCount());

        assertMONITFailureOK();

        // let's clear logs
        IN_MEMORY_LOG_HANDLER.clear();

        // and now let's send another message that should work
        sendHelloIdentity(SU_NAME);

        assertMONITOk();
    }

    @ClassRule
    public static final TemporaryFolder AS_BC_FOLDER = new TemporaryFolder();

    public static final String CAMEL_IN_FOLDER = "in";

    public static class RouteBC extends RouteBuilder {
        @Override
        public void configure() throws Exception {

            from("file://" + new File(AS_BC_FOLDER.getRoot(), CAMEL_IN_FOLDER).getAbsolutePath())
                    .to("petals:theConsumesId");
        }
    }

    @Test
    public void testAsBC() throws Exception {
        // we won't be using the provides, but it's ok
        deployHello(SU_NAME, WSDL11, RouteBC.class);

        final File file = AS_BC_FOLDER.newFile("test");
        FileUtils.writeStringToFile(file, "<a />");
        final File camelInFolder = new File(AS_BC_FOLDER.getRoot(), CAMEL_IN_FOLDER);

        final File fileInCamelFolder = new File(camelInFolder, file.getName());

        assertTrue(file.renameTo(fileInCamelFolder));

        assertTrue(fileInCamelFolder.exists());

        receiveAsExternalProvider(ExternalServiceImplementation.outMessage("<b />"), 3000);

        // let's sleep a bit for things to end
        Thread.sleep(1000);

        assertFalse(fileInCamelFolder.exists());
        assertTrue(new File(new File(camelInFolder, ".camel"), fileInCamelFolder.getName()).exists());

        assertMONITasBCOk();

    }

    public void assertMONITFailureOK() {
        final List<LogRecord> monitLogs = IN_MEMORY_LOG_HANDLER.getAllRecords(Level.MONIT);
        assertEquals(4, monitLogs.size());
        final FlowLogData firstLog = assertMonitProviderBeginLog(HELLO_INTERFACE, HELLO_SERVICE, HELLO_ENDPOINT,
                HELLO_OPERATION, monitLogs.get(0));
        
        // it must be the third one (idx 2) because the fourth one (idx 3) is the monit end from the provider that
        // doesn't see the timeout
        assertMonitProviderFailureLog(firstLog, monitLogs.get(2));

        final FlowLogData secondLog = assertMonitProviderBeginLog(
                (String) firstLog.get(PetalsExecutionContext.FLOW_INSTANCE_ID_PROPERTY_NAME),
                (String) firstLog.get(PetalsExecutionContext.FLOW_STEP_ID_PROPERTY_NAME), HELLO_INTERFACE,
                HELLO_SERVICE, EXTERNAL_ENDPOINT_NAME, HELLO_OPERATION, monitLogs.get(1));
        // TODO this assert won't work because of the timeout and the fact that the provider can't see it, see also
        // PETALSCDK-101
        // assertMonitProviderFailureLog(secondLog, monitLogs.get(2));
    }

    public void assertMONITOk() {
        final List<LogRecord> monitLogs = IN_MEMORY_LOG_HANDLER.getAllRecords(Level.MONIT);
        assertEquals(4, monitLogs.size());
        final FlowLogData firstLog = assertMonitProviderBeginLog(HELLO_INTERFACE, HELLO_SERVICE, HELLO_ENDPOINT,
                HELLO_OPERATION, monitLogs.get(0));
        assertMonitProviderEndLog(firstLog, monitLogs.get(3));

        final FlowLogData secondLog = assertMonitProviderBeginLog(
                (String) firstLog.get(PetalsExecutionContext.FLOW_INSTANCE_ID_PROPERTY_NAME),
                (String) firstLog.get(PetalsExecutionContext.FLOW_STEP_ID_PROPERTY_NAME), HELLO_INTERFACE,
                HELLO_SERVICE, EXTERNAL_ENDPOINT_NAME, HELLO_OPERATION, monitLogs.get(1));
        assertMonitProviderEndLog(secondLog, monitLogs.get(2));
    }

    private void assertMONITasBCOk() {
        final List<LogRecord> monitLogs = IN_MEMORY_LOG_HANDLER.getAllRecords(Level.MONIT);
        assertEquals(4, monitLogs.size());

        final FlowLogData firstLog = assertMonitConsumerExtBeginLog(HELLO_INTERFACE, HELLO_SERVICE,
                EXTERNAL_ENDPOINT_NAME, HELLO_OPERATION, monitLogs.get(0));
        assertMonitConsumerExtEndLog(firstLog, monitLogs.get(3));

        final FlowLogData secondLog = assertMonitProviderBeginLog(
                (String) firstLog.get(PetalsExecutionContext.FLOW_INSTANCE_ID_PROPERTY_NAME),
                (String) firstLog.get(PetalsExecutionContext.FLOW_STEP_ID_PROPERTY_NAME), HELLO_INTERFACE,
                HELLO_SERVICE, EXTERNAL_ENDPOINT_NAME, HELLO_OPERATION, monitLogs.get(1));
        assertMonitProviderEndLog(secondLog, monitLogs.get(2));
    }
}
