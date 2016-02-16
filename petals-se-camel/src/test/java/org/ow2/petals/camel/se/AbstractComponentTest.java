/**
 * Copyright (c) 2015-2016 Linagora
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

import java.io.File;
import java.net.URL;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import javax.jbi.messaging.MessageExchange;
import javax.xml.namespace.QName;

import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation;
import org.ow2.petals.camel.se.utils.JbiCamelConstants;
import org.ow2.petals.commons.log.PetalsExecutionContext;
import org.ow2.petals.component.framework.api.Constants;
import org.ow2.petals.component.framework.jbidescriptor.generated.MEPType;
import org.ow2.petals.component.framework.junit.Component;
import org.ow2.petals.component.framework.junit.JbiConstants;
import org.ow2.petals.component.framework.junit.Message;
import org.ow2.petals.component.framework.junit.RequestMessage;
import org.ow2.petals.component.framework.junit.ResponseMessage;
import org.ow2.petals.component.framework.junit.helpers.MessageChecks;
import org.ow2.petals.component.framework.junit.helpers.ServiceProviderImplementation;
import org.ow2.petals.component.framework.junit.helpers.SimpleComponent;
import org.ow2.petals.component.framework.junit.impl.ServiceConfiguration;
import org.ow2.petals.component.framework.junit.impl.ServiceConfiguration.ServiceType;
import org.ow2.petals.component.framework.junit.impl.message.RequestToProviderMessage;
import org.ow2.petals.component.framework.junit.rule.ComponentUnderTest;
import org.ow2.petals.component.framework.junit.rule.ServiceConfigurationFactory;
import org.ow2.petals.junit.rules.log.handler.InMemoryLogHandler;

public abstract class AbstractComponentTest extends AbstractTest implements JbiCamelConstants, JbiConstants {

    protected static final URL WSDL11 = Thread.currentThread().getContextClassLoader()
            .getResource("tests/service-1.1.wsdl");

    protected static final URL WSDL20 = Thread.currentThread().getContextClassLoader()
            .getResource("tests/service-2.0.wsdl");

    protected static final URL VALID_ROUTES = Thread.currentThread().getContextClassLoader()
            .getResource("tests/routes-valid.xml");

    protected static final URL INVALID_ROUTES = Thread.currentThread().getContextClassLoader()
            .getResource("tests/routes-invalid.xml");

    protected static final String HELLO_NS = "http://petals.ow2.org";

    protected static final String EXTERNAL_CAMEL_SERVICE_ID = "theConsumesId";

    protected static final String SU_NAME = "su-name";

    protected static final QName HELLO_INTERFACE = new QName(HELLO_NS, "HelloInterface");

    protected static final QName HELLO_SERVICE = new QName(HELLO_NS, "HelloService");

    protected static final QName HELLO_OPERATION = new QName(HELLO_NS, "sayHello");

    protected static final String HELLO_ENDPOINT = Constants.Component.AUTOGENERATED_ENDPOINT_NAME;

    protected static final String EXTERNAL_ENDPOINT_NAME = "externalHelloEndpoint";

    protected static final long DEFAULT_TIMEOUT_FOR_COMPONENT_SEND = 2000;

    protected static final InMemoryLogHandler IN_MEMORY_LOG_HANDLER = new InMemoryLogHandler();

    protected static final Component COMPONENT_UNDER_TEST = new ComponentUnderTest()
            // we need faster checks for our tests, 2000 is too long!
            .setParameter(new QName(CDK_NAMESPACE_URI, "time-beetween-async-cleaner-runs"), "100")
            .registerExternalServiceProvider(HELLO_SERVICE, EXTERNAL_ENDPOINT_NAME)
            .addLogHandler(IN_MEMORY_LOG_HANDLER.getHandler());

    protected static final SimpleComponent COMPONENT = new SimpleComponent(COMPONENT_UNDER_TEST);

    /**
     * We use a class rule (i.e. static) so that the component lives during all the tests, this enables to test also
     * that successive deploy and undeploy do not create problems.
     * 
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
        // we want to clear them inbetween tests
        COMPONENT_UNDER_TEST.clearRequestsFromConsumer();
        COMPONENT_UNDER_TEST.clearResponsesFromProvider();
        // note: incoming messages queue can't be cleared because it is the job of the tested component to well handle
        // any situation
        // JUnit is susceptible to reuse threads apparently
        PetalsExecutionContext.clear();
    }

    /**
     * We undeploy services after each test (because the component is static and lives during the whole suite of tests)
     */
    @After
    public void after() {

        COMPONENT_UNDER_TEST.undeployAllServices();

        // asserts are ALWAYS a bug!
        final Formatter formatter = new SimpleFormatter();
        for (final LogRecord r : IN_MEMORY_LOG_HANDLER.getAllRecords()) {
            assertFalse("Got a log with an assertion: " + formatter.format(r), r.getThrown() instanceof AssertionError
                    || r.getMessage().contains("AssertionError"));
        }
    }

    protected static ServiceConfiguration createHelloConsumes() {
        final ServiceConfiguration consumes = new ServiceConfiguration(HELLO_INTERFACE, HELLO_SERVICE,
                EXTERNAL_ENDPOINT_NAME, ServiceType.CONSUME);
        consumes.setOperation(HELLO_OPERATION);
        consumes.setMEP(MEPType.IN_OUT);
        // let's use a smaller timeout time by default
        consumes.setTimeout(DEFAULT_TIMEOUT_FOR_COMPONENT_SEND);
        consumes.setParameter(new QName(CAMEL_JBI_NS_URI, EL_CONSUMES_SERVICE_ID), EXTERNAL_CAMEL_SERVICE_ID);
        return consumes;
    }

    protected static ServiceConfigurationFactory createHelloService(final URL wsdl, final @Nullable Class<?> clazz,
            final @Nullable URL routes) throws Exception {

        final ServiceConfiguration provides = new ServiceConfiguration(HELLO_INTERFACE, HELLO_SERVICE, HELLO_ENDPOINT,
                ServiceType.PROVIDE, wsdl);

        provides.addServiceConfigurationDependency(createHelloConsumes());

        if (clazz != null) {
            provides.setServicesSectionParameter(EL_SERVICES_ROUTE_CLASS, clazz.getName());
        }

        if (routes != null) {
            provides.setServicesSectionParameter(EL_SERVICES_ROUTE_XML, new File(routes.toURI()).getName());
            provides.addResource(routes);
        }

        return new ServiceConfigurationFactory() {
            @Override
            public ServiceConfiguration create() {
                return provides;
            }
        };
    }

    protected static void deployHello(final String suName, final URL wsdl, final Class<?> clazz) throws Exception {
        COMPONENT_UNDER_TEST.deployService(suName, createHelloService(wsdl, clazz, null));
    }

    protected static void deployHello(final String suName, final URL wsdl, final URL routes) throws Exception {
        COMPONENT_UNDER_TEST.deployService(suName, createHelloService(wsdl, null, routes));
    }

    protected static void sendHelloIdentity(final String suName) throws Exception {
        sendHelloIdentity(suName, MessageChecks.none());
    }

    protected static void sendHelloIdentity(final String suName, final MessageChecks serviceChecks)
            throws Exception {
        final String requestContent = "<aaa/>";
        final String responseContent = "<bbb/>";

        sendHello(suName, requestContent, requestContent, responseContent, responseContent, serviceChecks);
    }

    protected static void sendHello(final String suName, @Nullable final String request,
            @Nullable final String expectedRequest, final String response, @Nullable final String expectedResponse,
            final MessageChecks serviceChecks)
            throws Exception {

        MessageChecks reqChecks = isHelloRequest().andThen(serviceChecks);
        if (expectedRequest != null) {
            reqChecks = reqChecks.andThen(MessageChecks.hasXmlContent(expectedRequest));
        }

        MessageChecks respChecks = MessageChecks.noError().andThen(MessageChecks.noFault());
        if (expectedResponse != null) {
            respChecks = respChecks.andThen(MessageChecks.hasXmlContent(expectedResponse));
        }

        // TODO for now we have to disable acknoledgement check because we don't forward DONE in Camel (see
        // PetalsCamelConsumer)
        final ResponseMessage responseM = COMPONENT.sendAndGetResponse(helloRequest(suName, request),
                ServiceProviderImplementation.outMessage(response, null).with(reqChecks));

        respChecks.checks(responseM);

        COMPONENT.sendDoneStatus(responseM);
    }

    protected static MessageChecks isHelloRequest() {
        return new MessageChecks() {
            @Override
            public void checks(@Nullable Message request) throws Exception {
                assert request != null;
                final MessageExchange exchange = request.getMessageExchange();
                assertEquals(exchange.getInterfaceName(), HELLO_INTERFACE);
                assertEquals(exchange.getService(), HELLO_SERVICE);
                assertEquals(exchange.getOperation(), HELLO_OPERATION);
                assertEquals(exchange.getEndpoint().getEndpointName(), EXTERNAL_ENDPOINT_NAME);
            }
        };
    }

    protected static RequestMessage helloRequest(final String suName, final @Nullable String requestContent) {
        return new RequestToProviderMessage(COMPONENT_UNDER_TEST.getServiceConfiguration(suName), HELLO_OPERATION,
                AbsItfOperation.MEPPatternConstants.IN_OUT.value(), requestContent);
    }
}
