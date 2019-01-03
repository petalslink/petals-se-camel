/**
 * Copyright (c) 2015-2019 Linagora
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
package org.ow2.petals.camel.component.mocks;

import java.io.StringReader;
import java.util.Map;
import java.util.logging.Logger;

import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.camel.CamelContext;
import org.apache.commons.io.input.ReaderInputStream;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Assert;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.PetalsCamelContext;
import org.ow2.petals.camel.PetalsCamelRoute;
import org.ow2.petals.camel.PetalsChannel;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.PetalsChannel.PetalsProvidesChannel;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;
import org.ow2.petals.camel.exceptions.UnknownServiceException;
import org.ow2.petals.commons.log.PetalsExecutionContext;
import org.ow2.petals.component.framework.api.message.Exchange;
import org.ow2.petals.component.framework.junit.TestMessageExchangeFactory;
import org.ow2.petals.component.framework.junit.impl.mock.MockEndpointDirectory;
import org.ow2.petals.component.framework.junit.impl.mock.TestMessageExchangeFactoryImpl;
import org.ow2.petals.component.framework.message.ExchangeImpl;
import org.ow2.petals.component.framework.util.ServiceEndpointOperationKey;
import org.ow2.petals.jbi.messaging.exchange.PetalsMessageExchange;
import org.w3c.dom.DocumentFragment;

import com.ebmwebsourcing.easycommons.lang.UncheckedException;
import com.google.common.collect.Maps;

public class PetalsCamelContextMock implements PetalsCamelContext {

    private final Map<ServiceEndpointOperation, PetalsChannel> channels = Maps.newHashMap();

    private final Map<String, ServiceEndpointOperation> seos = Maps.newHashMap();

    private final Map<ServiceEndpointOperationKey, PetalsCamelRoute> ppos = Maps.newHashMap();

    private final CamelContext context;

    private final Logger logger = Logger.getLogger(PetalsCamelContextMock.class.getName());

    private final TestMessageExchangeFactory factory = new TestMessageExchangeFactoryImpl(new MockEndpointDirectory(),
            logger);

    public PetalsCamelContextMock(final CamelContext context) {
        this.context = context;
    }

    public void addMockService(final String serviceId, final ServiceEndpointOperation seo) {
        addMockService(serviceId, seo, new MockSendHandler());
    }

    private ServiceEndpointOperationKey getEOK(final ServiceEndpointOperation seo) {
        final ServiceEndpointOperationKey key = new ServiceEndpointOperationKey(seo.getService(), seo.getEndpoint(),
                seo.getOperation());
        return key;
    }

    public void addMockService(final String serviceId, final ServiceEndpointOperation seo, final MockSendHandler handler) {
        final PetalsChannel pC = this.channels.put(seo,
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
        final PetalsChannel channel = this.channels.get(seo);
        Assert.assertNotNull(channel);
        Assert.assertTrue(channel instanceof PetalsConsumesChannel);
        return (PetalsConsumesChannel) channel;
    }

    @Override
    public PetalsProvidesChannel getProvidesChannel(final ServiceEndpointOperation seo) {
        final PetalsChannel channel = this.channels.get(seo);
        Assert.assertNotNull(channel);
        Assert.assertTrue(channel instanceof PetalsProvidesChannel);
        return (PetalsProvidesChannel) channel;
    }

    @Override
    public void registerRoute(final ServiceEndpointOperation seo, final PetalsCamelRoute ppo) {
        Assert.assertEquals(ServiceType.PROVIDES, seo.getType());
        final ServiceEndpointOperationKey key = getEOK(seo);
        final PetalsCamelRoute put = this.ppos.put(key, ppo);
        assert put == null;
    }

    @Override
    public void unregisterRoute(final ServiceEndpointOperation seo) {
        Assert.assertEquals(ServiceType.PROVIDES, seo.getType());
        final ServiceEndpointOperationKey key = getEOK(seo);
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

    /**
     * Inject an exchange into Camel to a Petals's Provider (aka Camel's Consumer).
     */
    public void process(final String serviceId, final Exchange exchange) {
        PetalsExecutionContext.initFlowAttributes();
        final ServiceEndpointOperation seo = this.seos.get(serviceId);
        assert seo != null;
        final ServiceEndpointOperationKey key = getEOK(seo);
        final PetalsCamelRoute route = this.ppos.get(key);
        assert route != null;
        setRole(exchange, Role.PROVIDER);
        route.process(exchange);
        setRole(exchange, Role.CONSUMER);
    }

    public Exchange createExchange(final String serviceId, final String body) throws MessagingException {
        final Exchange exchange = createExchange(serviceId);
        exchange.setInMessageContent(new ReaderInputStream(new StringReader(body)));
        return exchange;
    }

    public Exchange createExchange(final String serviceId) {
        return createExchange(serviceId, (MEPPatternConstants) null);
    }

    private Exchange createExchange(final String serviceId, final @Nullable MEPPatternConstants mep) {
        final ServiceEndpointOperation seo = this.seos.get(serviceId);
        assert seo != null;
        final PetalsMessageExchange msg;
        try {
            msg = factory.createExchange(seo.getMEP());
        } catch (final MessagingException e) {
            throw new UncheckedException(e);
        }
        final QName service = seo.getService();
        final String endpoint = seo.getEndpoint();
        if (service != null && endpoint != null) {
            msg.setEndpoint(new ServiceEndpoint() {
                @Override
                public QName getServiceName() {
                    return service;
                }

                @Override
                public QName[] getInterfaces() {
                    return new QName[] { seo.getInterface() };
                }

                @Override
                public String getEndpointName() {
                    return endpoint;
                }

                @Override
                public @Nullable DocumentFragment getAsReference(@Nullable QName operationName) {
                    return null;
                }
            });
        }
        msg.setInterfaceName(seo.getInterface());
        msg.setService(service);
        msg.setOperation(seo.getOperation());
        return new ExchangeImpl(msg);
    }

    private @Nullable ServiceEndpoint resolveEndpoint(final String serviceId, final QName serviceName,
            final String endpointName) {
        final ServiceEndpointOperation seo = this.seos.get(serviceId);

        // they should be null, if not this shouldn't be called!
        assert seo.getService() == null;
        assert seo.getEndpoint() == null;

        return new ServiceEndpoint() {
            @Override
            public QName getServiceName() {
                return serviceName;
            }

            @Override
            public QName[] getInterfaces() {
                return new QName[] { seo.getInterface() };
            }

            @Override
            public String getEndpointName() {
                return endpointName;
            }

            @Override
            public @Nullable DocumentFragment getAsReference(@Nullable QName operationName) {
                return null;
            }
        };
    }

    public static class MockSendHandler {

        public boolean sendSync(final Exchange exchange, final long timeout) throws MessagingException {
            return false;
        }

        public void sendAsync(final Exchange exchange, final long timeout) throws MessagingException {
        }

        public void send(final Exchange exchange) throws MessagingException {
            // do nothing
        }
    }

    public abstract class MockChannel implements PetalsChannel {

        private final MockSendHandler handler;

        public MockChannel(MockSendHandler handler) {
            this.handler = handler;
        }

        @Override
        public Logger getLogger() {
            return PetalsCamelContextMock.this.logger;
        }

        public abstract void setRole(final Exchange exchange);

        public abstract void revertRole(final Exchange exchange);

        @Override
        public boolean sendSync(final Exchange exchange, final long timeout) throws MessagingException {
            setRole(exchange);
            boolean sendSync = handler.sendSync(exchange, timeout);
            revertRole(exchange);
            return sendSync;
        }

        @Override
        public void sendAsync(final Exchange exchange, final long timeout, final SendAsyncCallback callback)
                throws MessagingException {
            setRole(exchange);
            handler.sendAsync(exchange, timeout);
            revertRole(exchange);
            callback.done(exchange, false);
        }

        @Override
        public void send(final Exchange exchange) throws MessagingException {
            setRole(exchange);
            handler.send(exchange);
        }

    }

    private static void setRole(final Exchange exchange, final Role role) {
        ((PetalsMessageExchange) ((ExchangeImpl) exchange).getMessageExchange()).setRole(role);
    }

    public class MockConsumesChannel extends MockChannel implements PetalsConsumesChannel {

        private final String serviceId;

        public MockConsumesChannel(final String serviceId, final MockSendHandler handler) {
            super(handler);
            this.serviceId = serviceId;
        }

        @Override
        public Exchange newExchange(final @Nullable MEPPatternConstants mep) throws MessagingException {
            return PetalsCamelContextMock.this.createExchange(serviceId, mep);
        }

        @Override
        public @Nullable ServiceEndpoint resolveEndpoint(final QName serviceName, final String endpointName) {
            return PetalsCamelContextMock.this.resolveEndpoint(serviceId, serviceName, endpointName);
        }

        @Override
        public void setRole(Exchange exchange) {
            PetalsCamelContextMock.setRole(exchange, Role.PROVIDER);
        }

        @Override
        public void revertRole(Exchange exchange) {
            PetalsCamelContextMock.setRole(exchange, Role.CONSUMER);
        }
    }

    public class MockProvidesChannel extends MockChannel implements PetalsProvidesChannel {

        public MockProvidesChannel(final MockSendHandler handler) {
            super(handler);
        }

        @Override
        public void setRole(Exchange exchange) {
            PetalsCamelContextMock.setRole(exchange, Role.CONSUMER);
        }

        @Override
        public void revertRole(Exchange exchange) {
            PetalsCamelContextMock.setRole(exchange, Role.PROVIDER);
        }
    }
}
