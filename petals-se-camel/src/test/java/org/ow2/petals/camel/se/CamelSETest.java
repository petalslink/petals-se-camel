/**
 * Copyright (c) 2015-2019 Linagora
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
package org.ow2.petals.camel.se;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;
import org.ow2.petals.camel.se.exceptions.InvalidCamelRouteDefinitionException;
import org.ow2.petals.camel.se.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.camel.se.exceptions.NotImplementedRouteException;
import org.ow2.petals.camel.se.mocks.TestRoutesKO1;
import org.ow2.petals.camel.se.mocks.TestRoutesOK;
import org.ow2.petals.component.framework.junit.StatusMessage;
import org.ow2.petals.component.framework.junit.impl.ProvidesServiceConfiguration;
import org.ow2.petals.component.framework.junit.impl.ServiceConfiguration;
import org.ow2.petals.component.framework.junit.rule.ServiceConfigurationFactory;

/**
 * Tests for {@link CamelSE}, {@link CamelSU}, {@link CamelSUManager}, {@link CamelJBIListener} and co.
 * 
 * @author vnoel
 *
 */
public class CamelSETest extends AbstractComponentTest {

    protected static final QName WRONG_INTERFACE = new QName(HELLO_NS, "WrongInterface");

    protected static final QName WRONG_SERVICE = new QName(HELLO_NS, "WrongInterface");

    @Test
    public void testDeploy_WSDL_KO() throws Exception {
        thrown.expect(DeploymentException.class);
        thrown.expectMessage("Failed to find provided service");
        COMPONENT_UNDER_TEST.deployService(SU_NAME, new ServiceConfigurationFactory() {
            @Override
            public ServiceConfiguration create() {
                return new ProvidesServiceConfiguration(WRONG_INTERFACE, WRONG_SERVICE, "autogenerate", WSDL11);
            }
        });
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
    public void testRequestHasNoImplementation() throws Exception {

        // no implementations are provided
        COMPONENT_UNDER_TEST.deployService(SU_NAME, createHelloService(WSDL11, null, null));

        final StatusMessage response = COMPONENT.sendAndGetStatus(helloRequest(SU_NAME, "<aa/>"));

        assertTrue(response.getError() instanceof MessagingException);
        // the cause is in the message!!!
        assertTrue(response.getError().getMessage().contains(NotImplementedRouteException.class.getName()));
    }

    @Test
    public void testMessageGoThrough() throws Exception {

        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);

        sendHelloIdentity(SU_NAME);

    }

    public static class RouteWrongFromServiceId extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("petals:theWrongProvidesId").to("petals:theConsumesId");
        }
    }

    public static class RouteWrongToServiceId extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("petals:theProvidesId").to("petals:theWrongConsumesId");
        }
    }

    @Test
    public void testUnknownFromServiceId() throws Exception {
        thrown.expect(DeploymentException.class);
        // TODO better checks for the cause of the error
        deployHello(SU_NAME, WSDL11, RouteWrongFromServiceId.class);
    }

    @Test
    public void testUnknownToServiceId() throws Exception {
        thrown.expect(DeploymentException.class);
        // TODO better checks for the cause of the error
        deployHello(SU_NAME, WSDL11, RouteWrongToServiceId.class);
    }
}
