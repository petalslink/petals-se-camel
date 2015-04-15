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

import java.net.URL;

import javax.jbi.management.DeploymentException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
                final URL wsdl = Thread.currentThread().getContextClassLoader().getResource(WSDL11);
                assertNotNull(wsdl);
                return createTestService(SU_NAME, WRONG_INTERFACE, WRONG_SERVICE, "autogenerate", wsdl);
            }
        });
    }

    @Test
    public void testDeploy_WSDL11_OK() throws DeploymentException {
        COMPONENT_UNDER_TEST.deployService(SU_NAME, new ServiceConfigurationFactory() {
            @Override
            public ServiceConfiguration create() {
                final URL wsdl = Thread.currentThread().getContextClassLoader().getResource(WSDL11);
                assertNotNull(wsdl);
                return createTestService(SU_NAME, HELLO_INTERFACE, HELLO_SERVICE, "autogenerate", wsdl);
            }
        });
    }

    @Test
    public void testDeploy_WSDL20_OK() throws DeploymentException {
        COMPONENT_UNDER_TEST.deployService(SU_NAME, new ServiceConfigurationFactory() {
            @Override
            public ServiceConfiguration create() {
                final URL wsdl = Thread.currentThread().getContextClassLoader().getResource(WSDL20);
                assertNotNull(wsdl);
                return createTestService(SU_NAME, HELLO_INTERFACE, HELLO_SERVICE, "autogenerate", wsdl);
            }
        });
    }
}
