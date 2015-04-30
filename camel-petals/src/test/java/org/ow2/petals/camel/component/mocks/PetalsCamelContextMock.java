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
package org.ow2.petals.camel.component.mocks;

import java.io.StringReader;
import java.util.Map;
import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.camel.CamelContext;
import org.apache.commons.io.input.ReaderInputStream;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Assert;
import org.ow2.petals.camel.PetalsCamelContext;
import org.ow2.petals.camel.PetalsCamelRoute;
import org.ow2.petals.camel.PetalsChannel;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.PetalsChannel.PetalsProvidesChannel;
import org.ow2.petals.camel.PetalsChannel.SendAsyncCallback;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;
import org.ow2.petals.camel.exceptions.UnknownServiceException;
import org.ow2.petals.commons.Constants;
import org.ow2.petals.component.framework.api.message.Exchange;
import org.ow2.petals.component.framework.message.ExchangeImpl;
import org.ow2.petals.component.framework.util.EndpointOperationKey;
import org.ow2.petals.jbi.messaging.exchange.MessageExchangeImpl;
import org.w3c.dom.DocumentFragment;

import com.ebmwebsourcing.easycommons.uuid.QualifiedUUIDGenerator;
import com.google.common.collect.Maps;

public class PetalsCamelContextMock implements PetalsCamelContext {

    private final Map<EndpointOperationKey, PetalsChannel> channels = Maps.newHashMap();

    private final Map<String, ServiceEndpointOperation> seos = Maps.newHashMap();

    private final Map<EndpointOperationKey, PetalsCamelRoute> ppos = Maps.newHashMap();

    private final CamelContext context;

    private final Logger logger = Logger.getLogger(PetalsCamelContextMock.class.getName());

    public PetalsCamelContextMock(final CamelContext context) {
        this.context = context;
    }

    public void addMockService(final String serviceId, final ServiceEndpointOperation seo) {
        addMockService(serviceId, seo, new MockSendHandler());
    }

    public void addMockService(final String serviceId, final ServiceEndpointOperation seo, final MockSendHandler handler) {

        final EndpointOperationKey key = new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(),
                seo.getOperation());
        final PetalsChannel pC = this.channels.put(key,
                seo.getType() == ServiceType.CONSUMES ? new MockConsumesChannel(serviceId, handler)
                        : new MockProvidesChannel(handler));
        Assert.assertNull(pC);
        final ServiceEndpointOperation pS = this.seos.put(serviceId, seo);
        Assert.assertNull(pS);
    }

    @Override
    public ServiceEndpointOperation getService(final String serviceId) throws UnknownServiceException {
        final ServiceEndpointOperation seo = this.seos.get(serviceId);
        if (seo == null) {
            throw new UnknownServiceException(serviceId);
        }
        return seo;
    }

    @Override
    public PetalsConsumesChannel getConsumesChannel(final ServiceEndpointOperation seo) {
        final EndpointOperationKey key = new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(),
                seo.getOperation());
        final PetalsChannel channel = this.channels.get(key);
        Assert.assertNotNull(channel);
        Assert.assertTrue(channel instanceof PetalsConsumesChannel);
        return (PetalsConsumesChannel) channel;
    }

    @Override
    public PetalsProvidesChannel getProvidesChannel(final ServiceEndpointOperation seo) {
        final EndpointOperationKey key = new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(),
                seo.getOperation());
        final PetalsChannel channel = this.channels.get(key);
        Assert.assertNotNull(channel);
        Assert.assertTrue(channel instanceof PetalsProvidesChannel);
        return (PetalsProvidesChannel) channel;
    }

    @Override
    public void registerRoute(final ServiceEndpointOperation seo, final PetalsCamelRoute ppo) {
        final EndpointOperationKey key = new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(),
                seo.getOperation());
        final PetalsCamelRoute put = this.ppos.put(key, ppo);
        assert put == null;
    }

    @Override
    public void unregisterRoute(ServiceEndpointOperation seo) {
        final EndpointOperationKey key = new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(),
                seo.getOperation());
        final PetalsCamelRoute removed = this.ppos.remove(key);
        assert removed != null;
    }

    @Override
    public CamelContext getCamelContext() {
        return this.context;
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    public void process(final String serviceId, final Exchange exchange) {
        final ServiceEndpointOperation seo = this.seos.get(serviceId);
        assert seo != null;
        final EndpointOperationKey key = new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(),
                seo.getOperation());
        final PetalsCamelRoute route = this.ppos.get(key);
        assert route != null;
        route.process(exchange);
    }

    public Exchange createExchange(final String serviceId, final String body) throws MessagingException {
        final Exchange exchange = createExchange(serviceId);
        exchange.setInMessageContent(new ReaderInputStream(new StringReader(body)));
        return exchange;
    }

    public Exchange createExchange(final String serviceId) {
        final ServiceEndpointOperation seo = this.seos.get(serviceId);
        assert seo != null;
        final MessageExchangeImpl msg = new MessageExchangeImpl();
        msg.setExchangeId(new QualifiedUUIDGenerator(Constants.UUID_DOMAIN).getNewID());
        // msg.setConsumerEndpoint(consumerEndpoint);
        msg.setEndpoint(new ServiceEndpoint() {

            @Override
            public QName getServiceName() {
                return seo.getService();
            }

            @Override
            public QName[] getInterfaces() {
                return new QName[] { seo.getInterface() };
            }

            @Override
            public String getEndpointName() {
                return seo.getEndpoint();
            }

            @Override
            public @Nullable DocumentFragment getAsReference(@Nullable QName operationName) {
                return null;
            }
        });
        msg.setInterfaceName(seo.getInterface());
        msg.setService(seo.getService());
        msg.setPattern(seo.getMEP());
        msg.setOperation(seo.getOperation());
        return new ExchangeImpl(msg);
    }

    public static class MockSendHandler {

        public boolean sendSync(final Exchange exchange, final long timeout) throws MessagingException {
            return false;
        }

        public void sendAsync(final Exchange exchange, final long timeout, final SendAsyncCallback callback)
                throws MessagingException {
            callback.done(false);
        }

        public void send(final Exchange exchange) throws MessagingException {
            // do nothing
        }
    }

    public class MockChannel implements PetalsChannel {

        private final MockSendHandler handler;

        public MockChannel(MockSendHandler handler) {
            this.handler = handler;
        }

        @Override
        public Logger getLogger() {
            return PetalsCamelContextMock.this.logger;
        }

        @Override
        public boolean sendSync(final Exchange exchange, final long timeout) throws MessagingException {
            return handler.sendSync(exchange, timeout);
        }

        @Override
        public void sendAsync(final Exchange exchange, final long timeout, final SendAsyncCallback callback)
                throws MessagingException {
            handler.sendAsync(exchange, timeout, callback);
        }

        @Override
        public void send(final Exchange exchange) throws MessagingException {
            handler.send(exchange);
        }

    }

    public class MockConsumesChannel extends MockChannel implements PetalsConsumesChannel {

        private final String serviceId;

        public MockConsumesChannel(final String serviceId, final MockSendHandler handler) {
            super(handler);
            this.serviceId = serviceId;
        }

        @Override
        public Exchange newExchange() throws JBIException {
            return PetalsCamelContextMock.this.createExchange(serviceId);
        }

    }

    public class MockProvidesChannel extends MockChannel implements PetalsProvidesChannel {

        public MockProvidesChannel(final MockSendHandler handler) {
            super(handler);
        }

    }
}
