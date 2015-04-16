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

import org.eclipse.jdt.annotation.Nullable;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;

public class PetalsCamelProducerTest extends PetalsCamelTestSupport {

    private @Nullable PetalsCamelProducer producer;

    protected PetalsCamelProducer producer() {
        assert producer != null;
        return producer;
    }

    @Override
    protected void doPostSetup() throws Exception {
        super.doPostSetup();

        pcc().addMockService("serviceId1", createMockSEO(ServiceType.CONSUMES));
        final PetalsCamelEndpoint endpoint = createEndpoint("serviceId1");

        this.producer = (PetalsCamelProducer) endpoint.createProducer();

        this.exchange = producer().createExchange();
        assertNotNull(this.exchange);
        populateExchange(exchange);
    }
}
