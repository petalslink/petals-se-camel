/**
 * Copyright (c) 2015-2025 Linagora
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

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.DefaultEndpoint;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.ow2.petals.camel.PetalsCamelContext;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;

/**
 * <p>
 * Our custom Camel component for Petals acting as service provider (receiving JBI request) or as service consumer
 * (sending JBI request).
 * </p>
 */
@Component("petals")
public class PetalsCamelComponent extends DefaultComponent {

    @Nullable
    private PetalsCamelContext pcc;

    @NonNullByDefault(false)
    @Override
    protected Endpoint createEndpoint(final String uri, final String remaining, final Map<String, Object> parameters)
            throws Exception {
        assert uri != null;
        assert remaining != null;
        // parameters will be set from the class's configureProperties
        return new PetalsCamelEndpoint(uri, this, remaining);
    }

    @Override
    protected boolean useIntrospectionOnEndpoint() {
        // we want to handle manually the setting of parameters because producers and consumers are different
        return false;
    }

    /**
     * We have to do this test here because {@link DefaultEndpoint#configureProperties(Map)} is not called if there is
     * no parameter set on the endpoint.
     */
    @Override
    protected void afterConfiguration(final @Nullable String uri, final @Nullable String remaining,
            final @Nullable Endpoint endpoint, @Nullable Map<String, Object> parameters) throws Exception {
        if (endpoint instanceof PetalsCamelEndpoint pce && pce.getService().getType() == ServiceType.CONSUMES
                && pce.getMep() == null && pce.getService().getMEP() == null) {
            getContext().getLogger()
                    .warning("No MEP specified neither as an endpoint parameter or in the corresponding Consumes:"
                            + " the MEP specified on the Camel exchange will be used when creating a Petals exchange");
        }
    }

    public PetalsCamelContext getContext() {
        PetalsCamelContext result = this.pcc;
        if (result == null) {
            final PetalsCamelContext found = getCamelContext().getRegistry()
                    .lookupByNameAndType(PetalsCamelContext.class.getName(), PetalsCamelContext.class);
            if (found == null) {
                throw new IllegalArgumentException(
                        "No instance of PetalsCamelContext available in the Camel registry.");
            }
            this.pcc = found;
            result = found;
        }
        return result;
    }
}
