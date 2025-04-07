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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.xml.namespace.QName;

import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.ExchangePattern;
import org.apache.camel.NoSuchEndpointException;
import org.apache.camel.Producer;
import org.apache.camel.ResolveEndpointFailedException;
import org.junit.jupiter.api.Test;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;
import org.ow2.petals.camel.component.exceptions.IncompatibleEndpointUsageException;
import org.ow2.petals.camel.component.exceptions.InvalidURIException;
import org.ow2.petals.camel.component.mocks.ServiceEndpointOperationMock;
import org.ow2.petals.camel.exceptions.UnknownServiceException;

public class PetalsCamelComponentTest extends CamelPetalsTestSupport {

    private static final String SERVICE_ID_1 = "serviceId1";

    @Test
    public void testCreateProvidesEndpoint_OK() {
        addMockProvides(SERVICE_ID_1);
        createEndpoint(SERVICE_ID_1);
        // synchronous must be recognised
        createEndpoint(SERVICE_ID_1 + "?synchronous=true");
    }

    @Test
    public void testCreateProvidesEndpoint_KO1() {
        addMockProvides(SERVICE_ID_1);
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint("serviceId2");
        });
        assertInstanceOf(UnknownServiceException.class, actualException.getCause());
    }

    @Test
    public void testCreateProvidesEndpoint_KO2() {
        addMockProvides(SERVICE_ID_1);
        // timeout is not authorised for provides
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?timeout=5");
        });
        assertTrue(actualException.getMessage().contains("The parameter timeout can't be set on a from() endpoint"));
    }

    @Test
    public void testCreateProvidesEndpoint_KO3() {
        addMockProvides(SERVICE_ID_1);
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?wrong=true");
        });
        assertTrue(actualException.getMessage().contains("Unknown parameters=[{wrong=true}]"));
    }

    @Test
    public void testCreateProvidesEndpoint_KO4() {
        addMockProvides(SERVICE_ID_1);
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?serviceName={ns}name");
        });
        assertTrue(
                actualException.getMessage().contains("The parameter serviceName can't be set on a from() endpoint"));
    }

    @Test
    public void testCreateProvidesEndpoint_KO5() {
        addMockProvides(SERVICE_ID_1);
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?endpointName=name");
        });
        assertTrue(
                actualException.getMessage().contains("The parameter endpointName can't be set on a from() endpoint"));
    }

    @Test
    public void testCreateProvidesEndpoint_KO6() {
        addMockProvides(SERVICE_ID_1);
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?operation={ns}name");
        });
        assertTrue(actualException.getMessage().contains("The parameter operation can't be set on a from() endpoint"));
    }

    @Test
    public void testCreateProvidesEndpoint_KO7() {
        addMockProvides(SERVICE_ID_1);
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?exchangePattern=InOut");
        });
        assertTrue(actualException.getMessage()
                .contains("The parameter exchangePattern can't be set on a from() endpoint"));
    }

    private static void assertServiceEndpointOperation(final ServiceEndpointOperation expected,
            final ServiceEndpointOperation actual) {
        assertNotNull(expected);
        assertNotNull(actual);
        assertEquals(expected.getInterface(), actual.getInterface());
        assertEquals(expected.getService(), actual.getService());
        assertEquals(expected.getEndpoint(), actual.getEndpoint());
        assertEquals(expected.getOperation(), actual.getOperation());
        assertEquals(expected.getMEP(), actual.getMEP());
        assertEquals(expected.getType(), actual.getType());
    }

    @Test
    public void testCreateConsumesEndpoint_OK() {
        final ServiceEndpointOperation expectedService = addMockConsumes(SERVICE_ID_1);

        {
            final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1);

            assertServiceEndpointOperation(expectedService, edp.getService());
            assertNull(edp.getServiceName());
            assertNull(edp.getEndpointName());
            assertNull(edp.getOperation());
            assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
            assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
            assertFalse(edp.isSynchronous());
            assertEquals(-1, edp.getTimeout());
        }

        // synchronous must be recognised
        {
            final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1 + "?synchronous=true");

            assertServiceEndpointOperation(expectedService, edp.getService());
            assertNull(edp.getServiceName());
            assertNull(edp.getEndpointName());
            assertNull(edp.getOperation());
            assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
            assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
            assertTrue(edp.isSynchronous());
            assertEquals(-1, edp.getTimeout());
        }

        // timeout must be recognised
        {
            final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1 + "?timeout=5");

            assertServiceEndpointOperation(expectedService, edp.getService());
            assertNull(edp.getServiceName());
            assertNull(edp.getEndpointName());
            assertNull(edp.getOperation());
            assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
            assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
            assertFalse(edp.isSynchronous());
            assertEquals(5, edp.getTimeout());
        }
        {
            final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1 + "?synchronous=true&timeout=5");

            assertServiceEndpointOperation(expectedService, edp.getService());
            assertNull(edp.getServiceName());
            assertNull(edp.getEndpointName());
            assertNull(edp.getOperation());
            assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
            assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
            assertTrue(edp.isSynchronous());
            assertEquals(5, edp.getTimeout());
        }
    }

    @Test
    public void testCreateConsumesEndpoint_NoService_OK() {
        final ServiceEndpointOperation expectedService = new ServiceEndpointOperationMock(null, TEST_INTERFACE_NAME,
                TEST_ENDPOINT_NAME, TEST_OPERATION_NAME, ServiceType.CONSUMES, MEPPatternConstants.IN_OUT.value());
        pcc().addMockService(SERVICE_ID_1, expectedService);

        {
            final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1);

            assertServiceEndpointOperation(expectedService, edp.getService());
            assertNull(edp.getServiceName());
            assertNull(edp.getEndpointName());
            assertNull(edp.getOperation());
            assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
            assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
            assertFalse(edp.isSynchronous());
            assertEquals(-1, edp.getTimeout());
        }

        {
            final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1 + "?serviceName={ns}name");

            assertServiceEndpointOperation(expectedService, edp.getService());
            assertEquals(new QName("ns", "name"), edp.getServiceName());
            assertNull(edp.getEndpointName());
            assertNull(edp.getOperation());
            assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
            assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
            assertFalse(edp.isSynchronous());
            assertEquals(-1, edp.getTimeout());
        }
    }

    @Test
    public void testCreateConsumesEndpoint_NoService_KO() {
        pcc().addMockService(SERVICE_ID_1, new ServiceEndpointOperationMock(null, TEST_INTERFACE_NAME,
                TEST_ENDPOINT_NAME, TEST_OPERATION_NAME, ServiceType.CONSUMES, MEPPatternConstants.IN_OUT.value()));
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?serviceName={name");
        });
        assertInstanceOf(IllegalArgumentException.class, actualException.getCause());
    }

    @Test
    public void testCreateConsumesEndpoint_NoEndpoint_OK() {
        final ServiceEndpointOperation expectedService = new ServiceEndpointOperationMock(TEST_SERVICE_NAME,
                TEST_INTERFACE_NAME, null, TEST_OPERATION_NAME, ServiceType.CONSUMES,
                MEPPatternConstants.IN_OUT.value());
        pcc().addMockService(SERVICE_ID_1, expectedService);

        {
            final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1);

            assertServiceEndpointOperation(expectedService, edp.getService());
            assertNull(edp.getServiceName());
            assertNull(edp.getEndpointName());
            assertNull(edp.getOperation());
            assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
            assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
            assertFalse(edp.isSynchronous());
            assertEquals(-1, edp.getTimeout());
        }

        {
            final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1 + "?endpointName=name");

            assertServiceEndpointOperation(expectedService, edp.getService());
            assertNull(edp.getServiceName());
            assertEquals("name", edp.getEndpointName());
            assertNull(edp.getOperation());
            assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
            assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
            assertFalse(edp.isSynchronous());
            assertEquals(-1, edp.getTimeout());
        }
    }

    @Test
    public void testCreateConsumesEndpoint_NoEndpoint_OK2() {
        // no endpoint, no service, need service parameter!
        final ServiceEndpointOperation expectedService = new ServiceEndpointOperationMock(null, TEST_INTERFACE_NAME,
                null, TEST_OPERATION_NAME, ServiceType.CONSUMES, MEPPatternConstants.IN_OUT.value());
        pcc().addMockService(SERVICE_ID_1, expectedService);

        {
            final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1 + "?serviceName={ns}name&endpointName=name");
            assertServiceEndpointOperation(expectedService, edp.getService());
            assertEquals(new QName("ns", "name"), edp.getServiceName());
            assertEquals("name", edp.getEndpointName());
            assertNull(edp.getOperation());
            assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
            assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
            assertFalse(edp.isSynchronous());
            assertEquals(-1, edp.getTimeout());
        }
    }

    @Test
    public void testCreateConsumesEndpoint_NoEndpoint_KO() {
        // no endpoint, no service, need service parameter!
        pcc().addMockService(SERVICE_ID_1, new ServiceEndpointOperationMock(null, TEST_INTERFACE_NAME, null,
                TEST_OPERATION_NAME, ServiceType.CONSUMES, MEPPatternConstants.IN_OUT.value()));
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?endpointName=name");
        });
        assertTrue(actualException.getMessage()
                .contains("if the endpoint nor the corresponding Consumes declares a service name"));
    }

    @Test
    public void testCreateConsumesEndpoint_NoOperation_OK() {
        final ServiceEndpointOperation expectedService = new ServiceEndpointOperationMock(TEST_SERVICE_NAME,
                TEST_INTERFACE_NAME, TEST_ENDPOINT_NAME, null, ServiceType.CONSUMES,
                MEPPatternConstants.IN_OUT.value());
        pcc().addMockService(SERVICE_ID_1, expectedService);

        {
            final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1);
            assertServiceEndpointOperation(expectedService, edp.getService());
            assertNull(edp.getServiceName());
            assertNull(edp.getEndpointName());
            assertNull(edp.getOperation());
            assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
            assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
            assertFalse(edp.isSynchronous());
            assertEquals(-1, edp.getTimeout());
        }

        {
            final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1 + "?operation={ns}name");
            assertServiceEndpointOperation(expectedService, edp.getService());
            assertNull(edp.getServiceName());
            assertNull(edp.getEndpointName());
            assertEquals(new QName("ns", "name"), edp.getOperation());
            assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
            assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
            assertFalse(edp.isSynchronous());
            assertEquals(-1, edp.getTimeout());
        }
    }

    @Test
    public void testCreateConsumesEndpoint_NoOperation_KO() {
        pcc().addMockService(SERVICE_ID_1,
                new ServiceEndpointOperationMock(TEST_SERVICE_NAME, TEST_INTERFACE_NAME, TEST_ENDPOINT_NAME, null,
                ServiceType.CONSUMES, MEPPatternConstants.IN_OUT.value()));
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?operation={name");
        });
        assertInstanceOf(IllegalArgumentException.class, actualException.getCause());
    }

    @Test
    public void testCreateConsumesEndpoint_MEPInConsumer_NotOverriden() {
        final ServiceEndpointOperation expectedService = new ServiceEndpointOperationMock(TEST_SERVICE_NAME,
                TEST_INTERFACE_NAME, TEST_ENDPOINT_NAME, TEST_OPERATION_NAME, ServiceType.CONSUMES,
                MEPPatternConstants.IN_OUT.value());
        pcc().addMockService(SERVICE_ID_1, expectedService);

        final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1);
        assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
        assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
    }

    @Test
    public void testCreateConsumesEndpoint_RobustInOnlyConsumer() {
        pcc().addMockService(SERVICE_ID_1,
                new ServiceEndpointOperationMock(TEST_SERVICE_NAME, TEST_INTERFACE_NAME, TEST_ENDPOINT_NAME,
                        TEST_OPERATION_NAME, ServiceType.CONSUMES, MEPPatternConstants.ROBUST_IN_ONLY.value()));
        final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1);
        assertEquals(MEPPatternConstants.ROBUST_IN_ONLY, edp.getMep());
        // The Camel exchange pattern 'InOnly' is the closest from Petals Exchange pattern 'RobustInOnlyOut'.
        assertEquals(ExchangePattern.InOnly, edp.getExchangePattern());
    }

    @Test
    public void testCreateConsumesEndpoint_NoMEPInConsumer_NotOverriden() {
        pcc().addMockService(SERVICE_ID_1, new ServiceEndpointOperationMock(TEST_SERVICE_NAME, TEST_INTERFACE_NAME,
                TEST_ENDPOINT_NAME, TEST_OPERATION_NAME, ServiceType.CONSUMES, null));
        final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1);
        assertEquals(MEPPatternConstants.IN_ONLY, edp.getMep());
        assertEquals(ExchangePattern.InOnly, edp.getExchangePattern());
    }

    @Test
    public void testCreateConsumesEndpoint_NoMEPInConsumer_Overriden() {
        pcc().addMockService(SERVICE_ID_1, new ServiceEndpointOperationMock(TEST_SERVICE_NAME, TEST_INTERFACE_NAME,
                TEST_ENDPOINT_NAME, TEST_OPERATION_NAME, ServiceType.CONSUMES, null));
        final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1 + "?exchangePattern=InOptionalOut");
        assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
        // The Camel exchange pattern 'InOut' is the closest from Petals Exchange pattern 'InOptionalOut'.
        assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
    }

    @Test
    public void testCreateConsumesEndpoint_NoMEP_KO1() {
        final String patternNotGood = "notGood";
        pcc().addMockService(SERVICE_ID_1, new ServiceEndpointOperationMock(TEST_SERVICE_NAME, TEST_INTERFACE_NAME,
                TEST_ENDPOINT_NAME, TEST_OPERATION_NAME, ServiceType.CONSUMES, null));
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?exchangePattern=" + patternNotGood);
        });
        assertInstanceOf(IllegalArgumentException.class, actualException.getCause());
    }

    @Test
    public void testCreateConsumesEndpoint_KO1() {
        addMockConsumes(SERVICE_ID_1);
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint("serviceId2");
        });
        assertInstanceOf(UnknownServiceException.class, actualException.getCause());
    }

    @Test
    public void testCreateConsumesEndpoint_KO2() {
        addMockConsumes(SERVICE_ID_1);
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?wrong=true");
        });
        assertTrue(actualException.getMessage().contains("Unknown parameters=[{wrong=true}]"));
    }

    @Test
    public void testCreateConsumesEndpoint_KO3() {
        addMockConsumes(SERVICE_ID_1);
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?serviceName={ns}name");
        });
        assertTrue(actualException.getMessage().contains("corresponding Consumes already declares a service name"));
    }

    @Test
    public void testCreateConsumesEndpoint_KO4() {
        addMockConsumes(SERVICE_ID_1);
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?operation={ns}name");
        });
        assertTrue(actualException.getMessage().contains("corresponding Consumes already declares an operation"));
    }

    @Test
    public void testCreateConsumesEndpoint_KO5() {
        addMockConsumes(SERVICE_ID_1);
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?endpointName=name");
        });
        assertTrue(actualException.getMessage().contains("corresponding Consumes already declares an endpoint name"));
    }

    @Test
    public void testCreateConsumesEndpoint_KO6() {
        addMockConsumes(SERVICE_ID_1);
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            createEndpoint(SERVICE_ID_1 + "?exchangePattern=InOut");
        });
        assertTrue(actualException.getMessage().contains("corresponding Consumes already declares a MEP"));
    }

    @Test
    public void testCreateConsumesEndpoint_KO_URI1() {
        addMockConsumes(SERVICE_ID_1);
        final CamelContext context = context();
        final Exception actualException = assertThrows(NoSuchEndpointException.class, () -> {
            context.getEndpoint("petalsA:" + SERVICE_ID_1);
        });
        assertTrue(actualException.getMessage().contains("No endpoint could be found for: petalsA://serviceId1"));
    }

    @Test
    public void testCreateConsumesEndpoint_KO_URI2() {
        addMockConsumes(SERVICE_ID_1);
        final Exception actualException = assertThrows(ResolveEndpointFailedException.class, () -> {
            context().getEndpoint("petals:" + SERVICE_ID_1 + "$$");
        });
        assertInstanceOf(InvalidURIException.class, actualException.getCause());
    }

    @Test
    public void testCreateProducer_OK() throws Exception {
        // camel producers are associated to petals consumes
        final ServiceEndpointOperation expectedService = addMockConsumes(SERVICE_ID_1);
        final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1);

        assertServiceEndpointOperation(expectedService, edp.getService());
        assertNull(edp.getServiceName());
        assertNull(edp.getEndpointName());
        assertNull(edp.getOperation());
        assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
        assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
        assertFalse(edp.isSynchronous());
        assertEquals(-1, edp.getTimeout());

        final Producer producer = edp.createProducer();
        assertNotNull(producer);
        assertInstanceOf(PetalsCamelProducer.class, producer);
    }

    @Test
    public void testCreateProducer_KO() throws Exception {
        addMockProvides(SERVICE_ID_1);
        final Endpoint endpoint = createEndpoint(SERVICE_ID_1);
        assertThrows(IncompatibleEndpointUsageException.class, () -> {
            endpoint.createProducer();
        });
    }

    @Test
    public void testCreateConsumer_OK() throws Exception {
        // camel consumers are associated to petals provides
        final ServiceEndpointOperation expectedService = addMockProvides(SERVICE_ID_1);
        final PetalsCamelEndpoint edp = createEndpoint(SERVICE_ID_1);

        assertServiceEndpointOperation(expectedService, edp.getService());
        assertNull(edp.getServiceName());
        assertNull(edp.getEndpointName());
        assertNull(edp.getOperation());
        assertEquals(MEPPatternConstants.IN_OUT, edp.getMep());
        assertEquals(ExchangePattern.InOut, edp.getExchangePattern());
        assertFalse(edp.isSynchronous());
        assertEquals(-1, edp.getTimeout());

        final Consumer consumer = edp.createConsumer(emptyProcessor());
        assertNotNull(consumer);
        assertInstanceOf(PetalsCamelConsumer.class, consumer);
    }

    @Test
    public void testCreateConsumer_KO() throws Exception {
        addMockConsumes(SERVICE_ID_1);
        final Endpoint endpoint = createEndpoint(SERVICE_ID_1);
        assertThrows(IncompatibleEndpointUsageException.class, () -> {
            endpoint.createConsumer(emptyProcessor());
        });
    }
}
