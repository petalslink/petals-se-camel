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

import javax.xml.namespace.QName;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.ow2.petals.component.framework.junit.impl.ServiceConfiguration;
import org.ow2.petals.component.framework.junit.impl.ServiceConfiguration.ServiceType;
import org.ow2.petals.component.framework.junit.rule.ComponentUnderTest;
import org.ow2.petals.junit.rules.log.handler.InMemoryLogHandler;

public abstract class AbstractComponentTest extends AbstractTest {

    protected static final URL WSDL11 = Thread.currentThread().getContextClassLoader()
            .getResource("tests/service-1.1.wsdl");

    protected static final URL WSDL20 = Thread.currentThread().getContextClassLoader()
            .getResource("tests/service-2.0.wsdl");

    protected static final String HELLO_NS = "http://petals.ow2.org";

    protected static final QName WRONG_INTERFACE = new QName(HELLO_NS, "WrongInterface");

    protected static final QName WRONG_SERVICE = new QName(HELLO_NS, "WrongInterface");

    protected static final String EXTERNAL_CAMEL_SERVICE_ID = "theConsumesId";

    /**
     * TODO this information is duplicated between ServiceConfiguration and registerServiceToDeploy(), and is useless
     * for depend service! We should change the CDK-JUNIT about that...
     */
    protected static final String SU_NAME = "su-name";

    protected static final QName HELLO_INTERFACE = new QName(HELLO_NS, "HelloInterface");

    protected static final QName HELLO_SERVICE = new QName(HELLO_NS, "HelloService");

    protected static final QName HELLO_OPERATION = new QName(HELLO_NS, "sayHello");

    protected static final String EXTERNAL_ENDPOINT_NAME = "externalHelloEndpoint";

    protected static final InMemoryLogHandler IN_MEMORY_LOG_HANDLER = new InMemoryLogHandler();

    protected static final ComponentUnderTest COMPONENT_UNDER_TEST = new ComponentUnderTest().addLogHandler(
            IN_MEMORY_LOG_HANDLER.getHandler()).registerExternalServiceProvider(HELLO_SERVICE, EXTERNAL_ENDPOINT_NAME);

    /**
     * We use a class rule (i.e. static) so that the component lives during all the tests, this enables to test also
     * that successive deploy and undeploy do not create problems.
     */
    @ClassRule
    public static final TestRule chain = RuleChain.outerRule(IN_MEMORY_LOG_HANDLER).around(COMPONENT_UNDER_TEST);

    /**
     * All log traces must be cleared before starting a unit test (because the log handler is static and lives during
     * the whole suite of tests)
     */
    @Before
    public void clearLogTraces() {
        IN_MEMORY_LOG_HANDLER.clear();
    }

    /**
     * We undeploy services after each test (because the component is static and lives during the whole suite of tests)
     */
    @After
    public void undeployAllServices() {
        COMPONENT_UNDER_TEST.undeployAllServices();
    }

    protected ServiceConfiguration createTestService(final QName interfaceName, final QName serviceName,
            final String endpointName, final URL wsdl) {
        final ServiceConfiguration provides = new ServiceConfiguration(interfaceName, serviceName, endpointName,
                ServiceType.PROVIDE, wsdl);

        final ServiceConfiguration consumes = createHelloConsumes();

        // TODO we are missing the routes: need to modify the CDK for that

        provides.addServiceConfigurationDependency(consumes);

        return provides;
    }

    protected ServiceConfiguration createHelloConsumes() {
        final ServiceConfiguration consumes = new ServiceConfiguration(HELLO_INTERFACE, HELLO_SERVICE,
                EXTERNAL_ENDPOINT_NAME, ServiceType.CONSUME);
        consumes.setParameter("{http://petals.ow2.org/components/petals-se-camel/jbi/version-1.0}service-id",
                EXTERNAL_CAMEL_SERVICE_ID);
        return consumes;
    }

}
