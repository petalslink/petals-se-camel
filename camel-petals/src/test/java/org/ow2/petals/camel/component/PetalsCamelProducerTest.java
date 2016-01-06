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
package org.ow2.petals.camel.component;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;

import javax.jbi.messaging.MessagingException;

import org.apache.camel.ExchangePattern;
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
import org.ow2.petals.camel.PetalsChannel.SendAsyncCallback;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;
import org.ow2.petals.camel.component.mocks.PetalsCamelContextMock.MockSendHandler;
import org.ow2.petals.component.framework.api.message.Exchange;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

@RunWith(Parameterized.class)
public class PetalsCamelProducerTest extends PetalsCamelTestSupport {

    @Nullable
    private Exchange receivedExchange = null;

    protected Exchange receivedExchange() {
        assert receivedExchange != null;
        return receivedExchange;
    }

    @SuppressWarnings("null")
    private static final Predicate<Exchange> NOTHING = Predicates.alwaysTrue();

    private static Predicate<Exchange> setWith(final String out) {
        return new Predicate<Exchange>() {
            @Override
            public boolean apply(@Nullable final Exchange input) {
                assert input != null;
                try {
                    input.setOutMessageContent(new ReaderInputStream(new StringReader(out)));
                } catch (final MessagingException e) {
                    fail(e.getMessage());
                }
                return true;
            }
        };
    }

    private final MockSendHandler handler = new MockSendHandler() {
        @Override
        public void send(final Exchange exchange) throws MessagingException {
            receivedExchange = exchange;
            assertTrue(transformer().apply(exchange));
        }

        @Override
        public void sendAsync(Exchange exchange, long timeout, SendAsyncCallback callback) throws MessagingException {
            receivedExchange = exchange;
            assertTrue(transformer().apply(exchange));
            super.sendAsync(exchange, timeout, callback);
        }

        @Override
        public boolean sendSync(Exchange exchange, long timeout) throws MessagingException {
            receivedExchange = exchange;
            assertTrue(transformer().apply(exchange));
            return super.sendSync(exchange, timeout);
        }
    };

    @SuppressWarnings("null")
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { createMockSEO(ServiceType.CONSUMES, MEPPatternConstants.IN_OUT.value()), setWith("<bb/>"),
                        ExchangePattern.InOut },
                { createMockSEO(ServiceType.CONSUMES, MEPPatternConstants.IN_OPTIONAL_OUT.value()), setWith("<bb/>"),
                        ExchangePattern.InOptionalOut },
                { createMockSEO(ServiceType.CONSUMES, MEPPatternConstants.IN_OPTIONAL_OUT.value()), NOTHING,
                        ExchangePattern.InOptionalOut },
                { createMockSEO(ServiceType.CONSUMES, MEPPatternConstants.IN_ONLY.value()), NOTHING,
                        ExchangePattern.InOnly },
                { createMockSEO(ServiceType.CONSUMES, MEPPatternConstants.ROBUST_IN_ONLY.value()), NOTHING,
                        ExchangePattern.RobustInOnly }
        });
    }

    @Parameter
    @Nullable
    public ServiceEndpointOperation seo = null;

    protected ServiceEndpointOperation seo() {
        assert seo != null;
        return seo;
    }

    @Parameter(1)
    @Nullable
    public Predicate<Exchange> transformer = null;

    protected Predicate<Exchange> transformer() {
        assert transformer != null;
        return transformer;
    }

    @Parameter(2)
    @Nullable
    public ExchangePattern mep = null;

    protected ExchangePattern mep() {
        assert mep != null;
        return mep;
    }

    @Override
    protected void initializeServices() {
        super.initializeServices();
        pcc().addMockService("serviceId1", seo(), handler);
    }

    @Before
    public void cleanReceivedExchange() {
        this.receivedExchange = null;
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
        final String content = "<aaa />";

        final Object out = template().sendBody("direct:start", mep(), content);

        assertCoherentWithSEO(receivedExchange(), seo());

        assertSimilar(new Diff(content, getContent(receivedExchange().getInMessage())));

        if ((MEPPatternConstants.IN_OPTIONAL_OUT.equals(seo().getMEP()) && transformer() != NOTHING)
                || MEPPatternConstants.IN_OUT.equals(seo().getMEP())) {
            assertSimilar(new Diff("<bb/>", context().getTypeConverter().convertTo(String.class, out)));
        }
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
