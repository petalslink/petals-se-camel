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

import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.jbi.messaging.MessageExchange;
import javax.xml.namespace.QName;

import org.apache.camel.converter.jaxp.XmlConverter;
import org.apache.commons.io.input.ReaderInputStream;
import org.custommonkey.xmlunit.Diff;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation;
import org.ow2.petals.component.framework.junit.Component;
import org.ow2.petals.component.framework.junit.Message;
import org.ow2.petals.component.framework.junit.RequestMessage;
import org.ow2.petals.component.framework.junit.ResponseMessage;
import org.ow2.petals.component.framework.junit.impl.ServiceConfiguration;
import org.ow2.petals.component.framework.junit.impl.ServiceConfiguration.ServiceType;
import org.ow2.petals.component.framework.junit.impl.message.WrappedRequestToProviderMessage;
import org.ow2.petals.component.framework.junit.impl.message.WrappedResponseToConsumerMessage;
import org.ow2.petals.component.framework.junit.rule.ComponentUnderTest;
import org.ow2.petals.component.framework.junit.rule.ServiceConfigurationFactory;
import org.ow2.petals.junit.rules.log.handler.InMemoryLogHandler;

public abstract class AbstractComponentTest extends AbstractTest {

    /**
     * Converters from Camel
     */
    private static final XmlConverter CONVERTER = new XmlConverter();

    protected static final String SE_CAMEL_JBI_NS = "http://petals.ow2.org/components/petals-se-camel/jbi/version-1.0";

    protected static final URL WSDL11 = Thread.currentThread().getContextClassLoader()
            .getResource("tests/service-1.1.wsdl");

    protected static final URL WSDL20 = Thread.currentThread().getContextClassLoader()
            .getResource("tests/service-2.0.wsdl");

    protected static final URL VALID_ROUTES = Thread.currentThread().getContextClassLoader()
            .getResource("tests/routes-valid.xml");

    protected static final URL INVALID_ROUTES = Thread.currentThread().getContextClassLoader()
            .getResource("tests/routes-invalid.xml");

    protected static final String HELLO_NS = "http://petals.ow2.org";

    protected static final QName WRONG_INTERFACE = new QName(HELLO_NS, "WrongInterface");

    protected static final QName WRONG_SERVICE = new QName(HELLO_NS, "WrongInterface");

    protected static final String EXTERNAL_CAMEL_SERVICE_ID = "theConsumesId";

    protected static final String SU_NAME = "su-name";

    protected static final QName HELLO_INTERFACE = new QName(HELLO_NS, "HelloInterface");

    protected static final QName HELLO_SERVICE = new QName(HELLO_NS, "HelloService");

    protected static final QName HELLO_OPERATION = new QName(HELLO_NS, "sayHello");

    protected static final String EXTERNAL_ENDPOINT_NAME = "externalHelloEndpoint";

    protected static final long DEFAULT_TIMEOUT_FOR_COMPONENT_SEND = 10000;

    protected static final long TIMEOUTS_FOR_TESTS_SEND_AND_RECEIVE = 1000;

    protected static final InMemoryLogHandler IN_MEMORY_LOG_HANDLER = new InMemoryLogHandler();

    protected static final Component COMPONENT_UNDER_TEST = new ComponentUnderTest();

    /**
     * We use a class rule (i.e. static) so that the component lives during all the tests, this enables to test also
     * that successive deploy and undeploy do not create problems.
     */
    @ClassRule
    public static final TestRule chain = RuleChain.outerRule(IN_MEMORY_LOG_HANDLER).around(COMPONENT_UNDER_TEST);

    @BeforeClass
    public static void registerExternalService() {
        COMPONENT_UNDER_TEST.registerExternalServiceProvider(HELLO_SERVICE, EXTERNAL_ENDPOINT_NAME);
    }

    @BeforeClass
    public static void attachInMemoryLoggerToLogger() {
        Logger.getLogger("").addHandler(IN_MEMORY_LOG_HANDLER.getHandler());
    }

    /**
     * All log traces must be cleared before starting a unit test (because the log handler is static and lives during
     * the whole suite of tests)
     */
    @Before
    public void clearLogTraces() {
        IN_MEMORY_LOG_HANDLER.clear();
        COMPONENT_UNDER_TEST.clearRequestsFromConsumer();
        COMPONENT_UNDER_TEST.clearResponsesFromProvider();
    }

    /**
     * We undeploy services after each test (because the component is static and lives during the whole suite of tests)
     * 
     * @throws InterruptedException
     */
    @After
    public void after() throws InterruptedException {

        // let's wait a bit to be sure everything is terminated
        Thread.sleep(2000);

        COMPONENT_UNDER_TEST.undeployAllServices();

        // asserts are ALWAYS a bug!
        final Formatter formatter = new SimpleFormatter();
        for (final LogRecord r : IN_MEMORY_LOG_HANDLER.getAllRecords()) {
            assertFalse("Got a log with an assertion: " + formatter.format(r), r.getThrown() instanceof AssertionError
                    || r.getMessage().contains("AssertionError"));
        }
    }

    protected ServiceConfiguration createTestService(final QName interfaceName, final QName serviceName,
            final String endpointName, final URL wsdl) {
        final ServiceConfiguration provides = new ServiceConfiguration(interfaceName, serviceName, endpointName,
                ServiceType.PROVIDE, wsdl);

        final ServiceConfiguration consumes = createHelloConsumes();

        provides.addServiceConfigurationDependency(consumes);

        return provides;
    }

    protected ServiceConfiguration createHelloConsumes() {
        final ServiceConfiguration consumes = new ServiceConfiguration(HELLO_INTERFACE, HELLO_SERVICE,
                EXTERNAL_ENDPOINT_NAME, ServiceType.CONSUME);
        consumes.setParameter(new QName("http://petals.ow2.org/components/extensions/version-5", "mep"), "InOut");
        consumes.setParameter(new QName("http://petals.ow2.org/components/extensions/version-5", "operation"),
                HELLO_OPERATION.toString());
        // let's use a smaller timeout time by default
        consumes.setParameter(new QName("http://petals.ow2.org/components/extensions/version-5", "timeout"), ""
                + DEFAULT_TIMEOUT_FOR_COMPONENT_SEND);
        consumes.setParameter(new QName(SE_CAMEL_JBI_NS, "service-id"), EXTERNAL_CAMEL_SERVICE_ID);
        return consumes;
    }

    protected void deployHello(final String suName, final URL wsdl, final Class<?> clazz) throws Exception {
        deploy(suName, HELLO_INTERFACE, HELLO_SERVICE, wsdl, clazz, null);
    }

    protected void deploy(final String suName, final QName interfaceName, final QName serviceName, final URL wsdl,
            final @Nullable Class<?> clazz, final @Nullable URL routes) throws Exception {
        final String routesFileName;
        if (routes != null) {
            routesFileName = new File(routes.toURI()).getName();
        } else {
            routesFileName = null;
        }
        COMPONENT_UNDER_TEST.deployService(suName, new ServiceConfigurationFactory() {
            @Override
            public ServiceConfiguration create() {
                final ServiceConfiguration provides = createTestService(interfaceName, serviceName, "autogenerate",
                        wsdl);
                if (clazz != null) {
                    provides.setServicesSectionParameter(new QName(SE_CAMEL_JBI_NS, "java-routes"), clazz.getName());
                }
                if (routes != null) {
                    provides.setServicesSectionParameter(new QName(SE_CAMEL_JBI_NS, "xml-routes"), routesFileName);
                    provides.addResource(routes);
                }
                return provides;
            }
        });
    }

    protected void deployHello(final String suName, final URL wsdl, final URL routes) throws Exception {
        deploy(suName, HELLO_INTERFACE, HELLO_SERVICE, wsdl, null, routes);
    }

    public static abstract class ConsumerImplementation {

        public abstract ResponseMessage provides(final RequestMessage request) throws Exception;

        public final ConsumerImplementation with(final MessageChecks checks) {
            final ConsumerImplementation me = this;
            return new ConsumerImplementation() {
                @Override
                public ResponseMessage provides(final RequestMessage request) throws Exception {
                    assertFalse(checks.checks(request));
                    return me.provides(request);
                }
            };
        }

        public static ConsumerImplementation fromString(final String content) {
            return new ConsumerImplementation() {
                @Override
                public ResponseMessage provides(final RequestMessage request) throws Exception {
                    return new WrappedResponseToConsumerMessage(request.getMessageExchange(), new ReaderInputStream(
                            new StringReader(content)));
                }
            };
        }
    }

    protected ResponseMessage send(final RequestMessage request, final ConsumerImplementation consumer,
            final long timeoutForth, final long timeoutBack) throws Exception {

        COMPONENT_UNDER_TEST.pushRequestToProvider(request);

        final RequestMessage requestConsumer = COMPONENT_UNDER_TEST.pollRequestFromConsumer(timeoutForth);
        if (requestConsumer != null) {
            COMPONENT_UNDER_TEST.pushResponseToConsumer(consumer.provides(requestConsumer));
        }

        return COMPONENT_UNDER_TEST.pollResponseFromProvider(timeoutBack);
    }

    public static abstract class MessageChecks {

        /**
         * Checks fail if return true!
         * 
         * @param request
         * @return
         * @throws Exception
         */
        public abstract boolean checks(final Message request) throws Exception;

        public final MessageChecks andThen(final MessageChecks checks) {
            final MessageChecks me = this;
            return new MessageChecks() {
                @Override
                public boolean checks(final Message request) throws Exception {
                    return me.checks(request) || checks.checks(request);
                }
            };
        }

        public static MessageChecks none() {
            return new MessageChecks() {
                @Override
                public boolean checks(Message request) throws Exception {
                    return false;
                }
            };
        }
    }

    protected ResponseMessage sendAndCheck(final RequestMessage request, final ConsumerImplementation consumer,
            final MessageChecks consumerChecks, final long timeoutForth, final long timeoutBack,
            @Nullable final String expectedRequestContent, @Nullable final String expectedResponseContent)
            throws Exception {
        return sendAndCheck(request, consumer, consumerChecks, timeoutForth, timeoutBack, expectedRequestContent,
                expectedResponseContent, true, true);
    }

    protected ResponseMessage sendAndCheck(final RequestMessage request, final ConsumerImplementation consumer,
            final MessageChecks consumerChecks, final long timeoutForth, final long timeoutBack,
            @Nullable final String expectedRequestContent, @Nullable final String expectedResponseContent,
            final boolean checkResponseNoError, final boolean checkResponseNoFault) throws Exception {

        final MessageChecks checks;
        if (expectedRequestContent != null) {
            checks = hasXmlContent(expectedRequestContent).andThen(consumerChecks);
        } else {
            checks = consumerChecks;
        }
        final ResponseMessage responseMessage = send(request, consumer.with(checks), timeoutForth, timeoutBack);

        if (checkResponseNoError) {
            assertNull(responseMessage.getError());
        }

        if (checkResponseNoFault) {
            assertNull(responseMessage.getFault());
        }

        if (expectedResponseContent != null) {
            assertFalse(hasXmlContent(expectedResponseContent).checks(responseMessage));
        }

        return responseMessage;
    }

    protected ResponseMessage sendHelloIdentity(final String suName) throws Exception {
        return sendHelloIdentity(suName, MessageChecks.none());
    }

    protected ResponseMessage sendHelloIdentity(final String suName, final MessageChecks extraChecks) throws Exception {
        final String requestContent = "<sayHello xmlns=\"http://petals.ow2.org\"><arg0>John</arg0></sayHello>";
        final String responseContent = "<sayHelloResponse xmlns=\"http://petals.ow2.org\"><return>Hello John</return></sayHelloResponse>";

        return sendHello(suName, requestContent, requestContent, responseContent, responseContent, true, true,
                extraChecks);
    }

    protected ResponseMessage sendHello(final String suName, @Nullable final String request,
            @Nullable final String expectedRequest, final String response, @Nullable final String expectedResponse,
            final boolean checkResponseNoError, final boolean checkResponseNoFault, final MessageChecks extraChecks)
            throws Exception {

        // no timeouts
        return sendAndCheck(helloRequest(suName, request), ConsumerImplementation.fromString(response),
                isHelloRequest(EXTERNAL_ENDPOINT_NAME).andThen(extraChecks), TIMEOUTS_FOR_TESTS_SEND_AND_RECEIVE,
                TIMEOUTS_FOR_TESTS_SEND_AND_RECEIVE, expectedRequest, expectedResponse, checkResponseNoError,
                checkResponseNoFault);
    }

    /**
     * Arriving at the external service
     */
    protected MessageChecks isHelloRequest(final String targetEndpoint) {
        return new MessageChecks() {
            @Override
            public boolean checks(Message request) throws Exception {
                final MessageExchange exchange = request.getMessageExchange();
                assertEquals(exchange.getInterfaceName(), HELLO_INTERFACE);
                assertEquals(exchange.getService(), HELLO_SERVICE);
                assertEquals(exchange.getOperation(), HELLO_OPERATION);
                assertEquals(exchange.getEndpoint().getEndpointName(), targetEndpoint);
                return false;
            }
        };
    }

    protected MessageChecks hasXmlContent(final String expectedContent) {
        return new MessageChecks() {
            @Override
            public boolean checks(final Message request) throws Exception {
                final Diff diff = new Diff(CONVERTER.toDOMSource(request.getPayload(), null),
                        CONVERTER.toDOMSource(expectedContent));

                assertTrue(diff.similar());

                return false;
            }
        };
    }

    protected RequestMessage helloRequest(final String suName, final @Nullable String requestContent) {
        return new WrappedRequestToProviderMessage(COMPONENT_UNDER_TEST.getServiceConfiguration(suName),
                HELLO_OPERATION, AbsItfOperation.MEPPatternConstants.IN_OUT.value(), requestContent == null ? null
                        : new ReaderInputStream(new StringReader(requestContent)));
    }

}
