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

import java.util.Map;

import org.apache.camel.CamelContext;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.ow2.petals.camel.PetalsCamelContext;
import org.ow2.petals.camel.PetalsChannel;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.PetalsChannel.PetalsProvidesChannel;
import org.ow2.petals.camel.PetalsProvidesOperation;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;
import org.ow2.petals.camel.exceptions.AlreadyRegisteredServiceException;
import org.ow2.petals.camel.exceptions.UnknownRegisteredServiceException;
import org.ow2.petals.camel.exceptions.UnknownServiceException;
import org.ow2.petals.component.framework.util.EndpointOperationKey;

import com.google.common.collect.Maps;

public class PetalsCamelContextMock implements PetalsCamelContext {

    private final Map<EndpointOperationKey, PetalsChannel> channels = Maps.newHashMap();
    
    private final Map<String, ServiceEndpointOperation> seos = Maps.newHashMap();

    private final Map<EndpointOperationKey, PetalsProvidesOperation> ppos = Maps.newHashMap();

    private final CamelContext context;

    public PetalsCamelContextMock(final CamelContext context) {
        this.context = context;
    }

    public void addMockService(final String serviceId, final ServiceEndpointOperation seo) {
        this.addMockService(serviceId, seo,
                EasyMock.createMock(seo.getType() == ServiceType.Consumes ? PetalsConsumesChannel.class
                        : PetalsProvidesChannel.class));
    }

    public void addMockService(final String serviceId, final ServiceEndpointOperation seo, final PetalsChannel channel) {
        final EndpointOperationKey key = new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(),
                seo.getOperation());
        final PetalsChannel pC = this.channels.put(key, channel);
        Assert.assertNull(pC);
        final ServiceEndpointOperation pS = this.seos.put(serviceId, seo);
        Assert.assertNull(pS);
    }

    @Override
    public ServiceEndpointOperation getSEO(final String serviceId) throws UnknownServiceException {
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
    public void registerPPO(final ServiceEndpointOperation seo, final PetalsProvidesOperation ppo)
            throws AlreadyRegisteredServiceException {
        final EndpointOperationKey key = new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(),
                seo.getOperation());
        if (this.ppos.containsKey(key)) {
            throw new AlreadyRegisteredServiceException(seo);
        } else {
            this.ppos.put(key, ppo);
        }
    }

    @Override
    public void unregisterPPO(ServiceEndpointOperation seo) throws UnknownRegisteredServiceException {
        final EndpointOperationKey key = new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(),
                seo.getOperation());
        if (this.ppos.remove(key) != null) {
            throw new UnknownRegisteredServiceException(seo);
        }
    }

    @Override
    public CamelContext getCamelContext() {
        return this.context;
    }

}
