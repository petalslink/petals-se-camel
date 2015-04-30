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
package org.ow2.petals.camel.component;

import javax.jbi.messaging.MessagingException;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.custommonkey.xmlunit.Diff;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.PetalsChannel.SendAsyncCallback;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.component.mocks.PetalsCamelContextMock.MockSendHandler;
import org.ow2.petals.component.framework.api.message.Exchange;

public class PetalsCamelProducerTest extends PetalsCamelTestSupport {

    @Nullable
    private Exchange receivedExchange = null;

    protected Exchange receivedExchange() {
        assert receivedExchange != null;
        return receivedExchange;
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
        this.seo = addMockConsumes("serviceId1", new MockSendHandler() {
            @Override
            public void send(final Exchange exchange) throws MessagingException {
                receivedExchange = exchange;
            }

            @Override
            public void sendAsync(Exchange exchange, long timeout, SendAsyncCallback callback)
                    throws MessagingException {
                receivedExchange = exchange;
                super.sendAsync(exchange, timeout, callback);
            }

            @Override
            public boolean sendSync(Exchange exchange, long timeout) throws MessagingException {
                receivedExchange = exchange;
                return super.sendSync(exchange, timeout);
            }
        });
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

                from("direct:start2").to("petals:serviceId1?synchronous=true");
            }
        };
    }

    @Test
    public void testInOnly() throws Exception {
        final String content = "<aaa />";
        template().sendBody("direct:start", ExchangePattern.InOnly, content);

        assertEquals(seo().getEndpoint(), receivedExchange().getEndpointName());
        assertEquals(seo().getService(), receivedExchange().getService());
        assertEquals(seo().getOperation(), receivedExchange().getOperation());
        // there is many variation of the URI for the same MEP!
        assertEquals(MEPPatternConstants.fromURI(seo().getMEP()),
                MEPPatternConstants.fromURI(receivedExchange().getPattern()));
        assertEquals(seo().getInterface(), receivedExchange().getInterfaceName());

        final Diff diff = new Diff(content, CONVERTER.toString(receivedExchange().getInMessageContentAsSource(), null));
        assertTrue(diff.similar());

    }
}
