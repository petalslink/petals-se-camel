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
import javax.xml.namespace.QName;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.ow2.petals.camel.se.exceptions.InvalidCamelRouteDefinitionException;
import org.ow2.petals.camel.se.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.camel.se.mocks.TestRoutesKO1;
import org.ow2.petals.camel.se.mocks.TestRoutesOK;
import org.ow2.petals.component.framework.junit.impl.ServiceConfiguration;
import org.ow2.petals.component.framework.junit.rule.ServiceConfigurationFactory;

public class CamelSETest extends AbstractComponentTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testDeploy_KO() throws DeploymentException {
        thrown.expect(DeploymentException.class);
        thrown.expectMessage("Failed to find provided service");
        COMPONENT_UNDER_TEST.deployService(SU_NAME, new ServiceConfigurationFactory() {
            @Override
            public ServiceConfiguration create() {
                return createTestService(WRONG_INTERFACE, WRONG_SERVICE, "autogenerate", WSDL11);
            }
        });
    }

    @Test
    public void testDeploy_WSDL11_OK() throws DeploymentException {
        COMPONENT_UNDER_TEST.deployService(SU_NAME, new ServiceConfigurationFactory() {
            @Override
            public ServiceConfiguration create() {
                return createTestService(HELLO_INTERFACE, HELLO_SERVICE, "autogenerate", WSDL11);
            }
        });
    }

    @Test
    public void testDeploy_WSDL20_OK() throws DeploymentException {
        COMPONENT_UNDER_TEST.deployService(SU_NAME, new ServiceConfigurationFactory() {
            @Override
            public ServiceConfiguration create() {
                return createTestService(HELLO_INTERFACE, HELLO_SERVICE, "autogenerate", WSDL20);
            }
        });
    }

    @Test
    public void testDeploy_XML_OK() throws DeploymentException {
        COMPONENT_UNDER_TEST.deployService(SU_NAME, new ServiceConfigurationFactory() {
            @Override
            public ServiceConfiguration create() {
                final ServiceConfiguration provides = createTestService(HELLO_INTERFACE, HELLO_SERVICE, "autogenerate",
                        WSDL11);
                provides.setServicesSectionParameter(new QName(SE_CAMEL_JBI_NS, "xml-routes"), "routes-valid.xml");
                provides.addResource(VALID_ROUTES);
                return provides;
            }
        });
    }

    @Test
    public void testDeploy_XML_KO() throws DeploymentException {
        thrown.expect(DeploymentException.class);
        // the cause is in the message!
        thrown.expectMessage(InvalidCamelRouteDefinitionException.class.getName());
        COMPONENT_UNDER_TEST.deployService(SU_NAME, new ServiceConfigurationFactory() {
            @Override
            public ServiceConfiguration create() {
                final ServiceConfiguration provides = createTestService(HELLO_INTERFACE, HELLO_SERVICE, "autogenerate",
                        WSDL11);
                provides.setServicesSectionParameter(new QName(SE_CAMEL_JBI_NS, "xml-routes"), "routes-invalid.xml");
                provides.addResource(INVALID_ROUTES);
                return provides;
            }
        });
    }

    @Test
    public void testDeploy_JAVA_OK() throws DeploymentException {
        COMPONENT_UNDER_TEST.deployService(SU_NAME, new ServiceConfigurationFactory() {
            @Override
            public ServiceConfiguration create() {
                final ServiceConfiguration provides = createTestService(HELLO_INTERFACE, HELLO_SERVICE, "autogenerate",
                        WSDL11);
                provides.setServicesSectionParameter(new QName(SE_CAMEL_JBI_NS, "java-routes"),
                        TestRoutesOK.class.getName());
                return provides;
            }
        });
    }

    @Test
    public void testDeploy_JAVA_KO() throws DeploymentException {
        thrown.expect(DeploymentException.class);
        // the cause is in the message!
        thrown.expectMessage(InvalidJBIConfigurationException.class.getName());
        thrown.expectMessage("Can't instantiate");
        COMPONENT_UNDER_TEST.deployService(SU_NAME, new ServiceConfigurationFactory() {
            @Override
            public ServiceConfiguration create() {
                final ServiceConfiguration provides = createTestService(HELLO_INTERFACE, HELLO_SERVICE, "autogenerate",
                        WSDL11);
                provides.setServicesSectionParameter(new QName(SE_CAMEL_JBI_NS, "java-routes"),
                        TestRoutesKO1.class.getName());
                return provides;
            }
        });
    }
}
