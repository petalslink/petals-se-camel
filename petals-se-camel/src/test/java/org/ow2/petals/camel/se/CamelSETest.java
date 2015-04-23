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

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessagingException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.ow2.petals.camel.se.exceptions.InvalidCamelRouteDefinitionException;
import org.ow2.petals.camel.se.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.camel.se.mocks.TestRoutesKO1;
import org.ow2.petals.camel.se.mocks.TestRoutesOK;
import org.ow2.petals.component.framework.junit.ResponseMessage;

public class CamelSETest extends AbstractComponentTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testDeploy_WSDL_KO() throws Exception {
        thrown.expect(DeploymentException.class);
        thrown.expectMessage("Failed to find provided service");
        deploy(SU_NAME, WRONG_INTERFACE, WRONG_SERVICE, WSDL11, null, VALID_ROUTES);
    }

    @Test
    public void testDeploy_WSDL11_OK() throws Exception {
        deployHello(SU_NAME, WSDL11, VALID_ROUTES);
    }

    @Test
    public void testDeploy_WSDL20_OK() throws Exception {
        deployHello(SU_NAME, WSDL20, VALID_ROUTES);
    }

    @Test
    public void testDeploy_XML_OK() throws Exception {
        deployHello(SU_NAME, WSDL11, VALID_ROUTES);
    }

    @Test
    public void testDeploy_XML_KO() throws Exception {
        thrown.expect(DeploymentException.class);
        // the cause is in the message!
        thrown.expectMessage(InvalidCamelRouteDefinitionException.class.getName());
        deployHello(SU_NAME, WSDL11, INVALID_ROUTES);
    }

    @Test
    public void testDeploy_JAVA_OK() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);
    }

    @Test
    public void testDeploy_JAVA_KO() throws Exception {
        thrown.expect(DeploymentException.class);
        // the cause is in the message!
        thrown.expectMessage(InvalidJBIConfigurationException.class.getName());
        thrown.expectMessage("Can't instantiate");
        deployHello(SU_NAME, WSDL11, TestRoutesKO1.class);
    }

    @Test
    public void testRequestHasImplementation() throws Exception {

        // no implementations are furnished
        deploy(SU_NAME, HELLO_INTERFACE, HELLO_SERVICE, WSDL11, null, null);

        // we provides an empty in just to be sure it doesn't fail because of it
        // TODO according to JBI there shouldn't be a fault in that case...
        final ResponseMessage response = sendHello(SU_NAME, "", null, "", null, false, false);

        assertTrue(response.getError() instanceof MessagingException);
        // the cause is in the message!!!
        assertTrue(response.getError().getMessage()
                .contains("org.ow2.petals.camel.se.exceptions.NotImplementedRouteException"));
    }

    @Test
    public void testRequestHasContent() throws Exception {

        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);

        // TODO according to JBI there shouldn't be a fault in that case...
        final ResponseMessage response = sendHello(SU_NAME, null, null, null, null, false, false);

        assertTrue(response.getError() instanceof MessagingException);
        assertTrue(response.getError().getMessage().contains("The exchange must be IN"));
    }
}
