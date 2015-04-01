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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import org.apache.camel.AsyncCallback;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Ignore;
import org.junit.Test;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;
import org.ow2.petals.component.framework.api.message.Exchange;

import com.google.common.base.Preconditions;

public class PetalsCamelProducerTest extends PetalsCamelTestSupport {

    private @Nullable PetalsConsumesChannel channel;

    private @Nullable PetalsCamelProducer producer;

    protected PetalsConsumesChannel channel() {
        Preconditions.checkNotNull(channel);
        return channel;
    }

    protected PetalsCamelProducer producer() {
        Preconditions.checkNotNull(producer);
        return producer;
    }

    @Override
    protected void doPostSetup() throws Exception {
        super.doPostSetup();

        this.channel = easyMock.createMock(PetalsConsumesChannel.class);

        pcc().addMockService("serviceId1", createMockSEO(ServiceType.Consumes), channel());
        final PetalsCamelEndpoint endpoint = createEndpoint("serviceId1");
        this.producer = (PetalsCamelProducer) endpoint.createProducer();

        this.exchange = producer.createExchange();
        assertNotNull(this.exchange);
        populateExchange(exchange);
    }

    @Test
    @Ignore("there seems to be a bug in easymock...")
    public void testProducer() throws Exception {
        final Exchange petalsExchange = easyMock.createNiceMock(Exchange.class);
        final AsyncCallback callback = easyMock.createNiceMock(AsyncCallback.class);
        expect(channel().newExchange()).andReturn(petalsExchange);
        channel().sendAsync(eq(petalsExchange), eq(-1), anyObject(Runnable.class));
        expectLastCall();

        easyMock.replayAll();
        final boolean doneSync = producer().process(exchange, callback);

        assertFalse(doneSync);
        easyMock.verifyAll();
    }
}
