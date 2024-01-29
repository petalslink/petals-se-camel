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
package org.ow2.petals.camel.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;
import java.net.URI;
import java.util.stream.Stream;

import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessagingException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.io.input.ReaderInputStream;
import org.custommonkey.xmlunit.Diff;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;
import org.ow2.petals.camel.component.mocks.PetalsCamelContextMock.MockSendHandler;
import org.ow2.petals.component.framework.api.message.Exchange;

import com.ebmwebsourcing.easycommons.lang.UncheckedException;

/**
 * TODO we are missing tests for InOptOut with out + fault TODO we are missing tests for parameters of the endpoint
 */
public class PetalsCamelProducerTest extends CamelPetalsTestSupport {

    static class Params implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
            return Stream.of(
                    // Camel exchange Pattern equivalent to Petals exchange pattern IN_OUT
                    Arguments.of(ExchangePattern.InOut, MEPPatternConstants.IN_OUT.value(), OUT, false),
                    Arguments.of(ExchangePattern.InOut, MEPPatternConstants.IN_OUT.value(), ERROR, false),
                    Arguments.of(ExchangePattern.InOut, MEPPatternConstants.IN_OUT.value(), FAULT, true),
                    // Camel exchange Pattern equivalent to Petals exchange pattern IN_OPTIONAL_OUT
                    Arguments.of(ExchangePattern.InOut, MEPPatternConstants.IN_OPTIONAL_OUT.value(), OUT, false),
                    Arguments.of(ExchangePattern.InOut, MEPPatternConstants.IN_OPTIONAL_OUT.value(), ERROR, false),
                    Arguments.of(ExchangePattern.InOut, MEPPatternConstants.IN_OPTIONAL_OUT.value(), FAULT, true),
                    Arguments.of(ExchangePattern.InOut, MEPPatternConstants.IN_OPTIONAL_OUT.value(), null, false),
                    // Camel exchange Pattern equivalent to Petals exchange pattern IN_ONLY
                    Arguments.of(ExchangePattern.InOnly, MEPPatternConstants.IN_ONLY.value(), null, false),
                    Arguments.of(ExchangePattern.InOnly, MEPPatternConstants.IN_ONLY.value(), ERROR, false),
                    // Camel exchange Pattern equivalent to Petals exchange pattern ROBUST_IN_ONLY
                    Arguments.of(ExchangePattern.InOnly, MEPPatternConstants.ROBUST_IN_ONLY.value(), null, false),
                    Arguments.of(ExchangePattern.InOnly, MEPPatternConstants.ROBUST_IN_ONLY.value(), ERROR, false),
                    Arguments.of(ExchangePattern.InOnly, MEPPatternConstants.ROBUST_IN_ONLY.value(), FAULT, true));
        }
    }

    protected static final String FAULT = "<c/>";

    protected static final String OUT = "<b/>";

    protected static final String IN = "<a/>";

    protected static final Exception ERROR = new Exception();
    static {
        // we don't really care about the stacktrace
        ERROR.setStackTrace(new StackTraceElement[0]);
    }

    @BeforeEach
    public void before() {
        receivedExchange = null;
        applied = false;
    }

    @Nullable
    private Exchange receivedExchange = null;

    protected Exchange receivedExchange() {
        assert receivedExchange != null;
        return receivedExchange;
    }

    private boolean applied = false;

    private void transform(final Exchange exchange, final Object content, final boolean isFault) {
        if (applied) {
            return;
        }
        final Object c = content(content);
        try {
            if (c instanceof String) {
                final Source msg = new StreamSource(new ReaderInputStream(new StringReader((String) c)));
                if (isFault(isFault)) {
                    final Fault fault = exchange.createFault();
                    fault.setContent(msg);
                    exchange.setFault(fault);
                } else {
                    exchange.setOutMessageContent(msg);
                }
            } else if (c instanceof Exception) {
                exchange.setError((Exception) c);
            } else if (c == null) {
                exchange.setDoneStatus();
            } else {
                throw new UncheckedException("Shouldn't happen");
            }
            applied = true;
        } catch (final MessagingException e) {
            throw new UncheckedException(e);
        }
    }

    private @NonNull ExchangePattern mep(final @NonNull ExchangePattern mep) {
        assert mep != null;
        return mep;
    }

    private @NonNull URI originalMep(final @NonNull URI originalMep) {
        assert originalMep != null;
        return originalMep;
    }

    protected @Nullable Object content(final @Nullable Object content) {
        return content;
    }

    protected @NonNull boolean isFault(final @NonNull Boolean isFault) {
        assert isFault != null;
        return isFault;
    }

    private ServiceEndpointOperation seo(final ServiceEndpointOperation seo) {
        assert seo != null;
        return seo;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").id("testSimilar").to("direct:replaceMe");
            }
        };
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @ParameterizedTest
    @ArgumentsSource(Params.class)
    public void testSimilar(final @NonNull ExchangePattern camelMep, final @NonNull URI petalsMep,
            final @Nullable Object content, final @NonNull Boolean isFault) throws Exception {

        // Init Petals provider mock endpoint
        final ServiceEndpointOperation seo = createMockSEO(ServiceType.CONSUMES,
                petalsMep);
        pcc().addMockService("serviceId1", seo(seo), new MockSendHandler() {
            @Override
            public void send(final Exchange exchange) throws MessagingException {
                receivedExchange = exchange;
                transform(exchange, content, isFault);
            }

            @Override
            public void sendAsync(final Exchange exchange, final long timeout) throws MessagingException {
                receivedExchange = exchange;
                transform(exchange, content, isFault);
                super.sendAsync(exchange, timeout);
            }

            @Override
            public boolean sendSync(final Exchange exchange, final long timeout) throws MessagingException {
                receivedExchange = exchange;
                transform(exchange, content, isFault);
                return super.sendSync(exchange, timeout);
            }
        });

        // .. and adjust Camel route
        AdviceWith.adviceWith(context, "testSimilar", builder -> {
            builder.weaveByToUri("direct:replaceMe").replace().to("petals:serviceId1");
        });

        startCamelContext();

        // Send message to Camel consumer
        final org.apache.camel.Exchange exchange = template().send("direct:start", mep(camelMep), new Processor() {
            @Override
            public void process(final @Nullable org.apache.camel.Exchange exchange) throws Exception {
                assert exchange != null;
                exchange.setProperty(PetalsConstants.EXCHANGE_ORIGINAL_MEP, originalMep(petalsMep));
                exchange.getIn().setBody(IN);
            }
        });

        assertCoherentWithSEO(receivedExchange(), seo(seo));

        assertSimilar(new Diff(IN, getContent(receivedExchange().getInMessage())));

        final Object c = content(content);
        if (c instanceof String) {
            assertNotNull(exchange.getMessage().getBody());
            final String expect;
            if (isFault(isFault)) {
                expect = FAULT;
                assertEquals(true, exchange.getProperty(PetalsConstants.MESSAGE_FAULT_HEADER));
            } else {
                expect = OUT;
                assertNotEquals(true, exchange.getProperty(PetalsConstants.MESSAGE_FAULT_HEADER));
                assertNull(exchange.getProperty(PetalsConstants.MESSAGE_FAULT_HEADER));
            }
            assertSimilar(new Diff(expect, getContent(exchange.getMessage())));
        } else if (c instanceof Exception) {
            assertEquals(ERROR, exchange.getException());
            assertNull(exchange.getMessage().getBody());
        }
        // TODO c == null?
    }

    private static void assertCoherentWithSEO(final Exchange exchange, final ServiceEndpointOperation seo) {

        assertEquals(seo.getEndpoint(), exchange.getEndpointName());
        assertEquals(seo.getService(), exchange.getService());
        assertEquals(seo.getOperation(), exchange.getOperation());
        // there is many variation of the URI for the same MEP!
        assertEquals(MEPPatternConstants.fromURI(seo.getMEP()), MEPPatternConstants.fromURI(exchange.getPattern()));
        assertEquals(seo.getInterface(), exchange.getInterfaceName());
    }
}
