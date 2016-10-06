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
 * along with this program/library; If not, see http://www.gnu.org/licenses/
 * for the GNU Lesser General Public License version 2.1.
 */
package org.ow2.petals.camel.component;

import java.io.StringReader;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessagingException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.io.input.ReaderInputStream;
import org.custommonkey.xmlunit.Diff;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;
import org.ow2.petals.camel.component.mocks.PetalsCamelContextMock.MockSendHandler;
import org.ow2.petals.component.framework.api.message.Exchange;

import com.ebmwebsourcing.easycommons.lang.UncheckedException;

/**
 * TODO we are missing tests for InOptOut with out + fault
 * 
 * TODO we are missing tests for parameters of the endpoint
 */
@RunWith(Parameterized.class)
public class PetalsCamelProducerTest extends PetalsCamelTestSupport {

    protected static final String FAULT = "<c/>";

    protected static final String OUT = "<b/>";

    protected static final String IN = "<a/>";
    
    protected static final Exception ERROR = new Exception();
    static {
        // we don't really care about the stacktrace
        ERROR.setStackTrace(new StackTraceElement[0]);
    }
    
    @Before
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

    private void transform(final Exchange exchange) {
        if (applied) {
            return;
        }
        final Object c = content();
        try {
            if (c instanceof String) {
                final Source msg = new StreamSource(new ReaderInputStream(new StringReader((String) c)));
                if (isFault()) {
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

    private final MockSendHandler handler = new MockSendHandler() {
        @Override
        public void send(final Exchange exchange) throws MessagingException {
            receivedExchange = exchange;
            transform(exchange);
        }

        @Override
        public void sendAsync(final Exchange exchange, final long timeout) throws MessagingException {
            receivedExchange = exchange;
            transform(exchange);
            super.sendAsync(exchange, timeout);
        }

        @Override
        public boolean sendSync(final Exchange exchange, final long timeout) throws MessagingException {
            receivedExchange = exchange;
            transform(exchange);
            return super.sendSync(exchange, timeout);
        }
    };

    @SuppressWarnings("null")
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { ExchangePattern.InOut, OUT, false },
                // Petals 4 is broken with InOut and Errors...
                // { ExchangePattern.InOut, ERROR, false },
                { ExchangePattern.InOut, FAULT, true },
                { ExchangePattern.InOptionalOut, OUT, false },
                { ExchangePattern.InOptionalOut, ERROR, false },
                { ExchangePattern.InOptionalOut, FAULT, true },
                { ExchangePattern.InOptionalOut, null, false },
                { ExchangePattern.InOnly, null, false },
                { ExchangePattern.InOnly, ERROR, false },
                { ExchangePattern.RobustInOnly, null, false },
                { ExchangePattern.RobustInOnly, ERROR, false },
                { ExchangePattern.RobustInOnly, FAULT, true } });
    }


    @Parameter
    @Nullable
    public ExchangePattern mep = null;

    protected ExchangePattern mep() {
        assert mep != null;
        return mep;
    }

    @Parameter(1)
    @Nullable
    public Object content = null;

    protected @Nullable Object content() {
        return content;
    }

    @Parameter(2)
    @Nullable
    public Boolean isFault = null;

    protected boolean isFault() {
        assert isFault != null;
        return isFault;
    }

    @Nullable
    private ServiceEndpointOperation seo = null;

    protected ServiceEndpointOperation seo() {
        assert seo != null;
        return seo;
    }

    @Override
    protected void initializeServices() {
        super.initializeServices();
        this.seo = createMockSEO(ServiceType.CONSUMES, MEPPatternConstants.valueOf(URI.create(mep().getWsdlUri())).value());
        pcc().addMockService("serviceId1", seo(), handler);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("petals:serviceId1");
            }
        };
    }

    @Test
    public void testSimilar() throws Exception {
        final org.apache.camel.Exchange exchange = template().send("direct:start", mep(), new Processor() {
            @Override
            public void process(final @Nullable org.apache.camel.Exchange exchange) throws Exception {
                assert exchange != null;
                exchange.getIn().setBody(IN);
            }
        });

        assertCoherentWithSEO(receivedExchange(), seo());

        assertSimilar(new Diff(IN, getContent(receivedExchange().getInMessage())));

        final Object c = content();
        if (c instanceof String) {
            assertTrue(exchange.hasOut());
            final String expect;
            if (isFault()) {
                expect = FAULT;
                assertTrue(exchange.getOut().isFault());
            } else {
                expect = OUT;
                assertFalse(exchange.getOut().isFault());
            }
            assertSimilar(new Diff(expect, getContent(exchange.getOut())));
        } else if (c instanceof Exception) {
            assertEquals(ERROR, exchange.getException());
            assertFalse(exchange.hasOut());
        }
        // TODO c == null?
    }

    private static void assertCoherentWithSEO(final Exchange exchange, final ServiceEndpointOperation seo)
            throws Exception {

        assertEquals(seo.getEndpoint(), exchange.getEndpointName());
        assertEquals(seo.getService(), exchange.getService());
        assertEquals(seo.getOperation(), exchange.getOperation());
        // there is many variation of the URI for the same MEP!
        // and they are a mess in 4.3.x...
        // assertEquals(MEPPatternConstants.valueOf(seo.getMEP()), MEPPatternConstants.valueOf(exchange.getPattern()));
        assertEquals(seo.getInterface(), exchange.getInterfaceName());
    }
}
