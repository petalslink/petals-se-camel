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
package org.ow2.petals.camel.component;

import java.io.Serializable;
import java.net.URI;
import java.util.Map;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.camel.Endpoint;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.ow2.petals.camel.PetalsCamelContext;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;

public class PetalsCamelComponent extends UriEndpointComponent {

    /**
     * Prefix for a property that will be converted from a property in a new Petals exchange in Petals Consumers.
     */
    public static final String EXCHANGE_ORIGINAL_PROPERTY_PREFIX = "PetalsOriginalProperty.";

    /**
     * Prefix for a property that will be converted to a property in a new Petals exchange in Petals Producers.
     * 
     * Note that Petals expects {@link Serializable} properties!
     */
    public static final String EXCHANGE_PROPERTY_PREFIX = "PetalsProperty.";

    /**
     * Type is {@link QName}
     */
    public static final String EXCHANGE_ORIGINAL_INTERFACE = "PetalsOriginalInterface";

    /**
     * Type is {@link QName}
     */
    public static final String EXCHANGE_ORIGINAL_SERVICE = "PetalsOriginalService";

    /**
     * Type is {@link ServiceEndpoint}
     */
    public static final String EXCHANGE_ORIGINAL_ENDPOINT = "PetalsOriginalEndpoint";

    /**
     * Type is {@link QName}
     */
    public static final String EXCHANGE_ORIGINAL_OPERATION = "PetalsOriginalOperation";

    /**
     * Type is {@link URI}
     */
    public static final String EXCHANGE_ORIGINAL_MEP = "PetalsOriginalPattern";

    /**
     * Current flow tracing activation state on JBI exchange processing at service provider level. Type is
     * {@link Boolean}
     */
    public static final String EXCHANGE_CURRENT_FLOW_TRACING_ACTIVATION = "PetalsCurrentFlowTracingActivationStateOnJBIExchangeProcessingAtServiceProviderLevel";

    /**
     * Set to <code>true</code> if the message is a fault ({@link Message#isFault()} is legacy and limited in Camel and
     * so should not be used!)
     * 
     * Type is {@link Boolean}
     */
    public static final String MESSAGE_FAULT_HEADER = "PetalsMessageIsFault";

    @Nullable
    private PetalsCamelContext pcc;

    public PetalsCamelComponent() {
        super(PetalsCamelEndpoint.class);
    }

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
        if (endpoint instanceof PetalsCamelEndpoint) {
            final PetalsCamelEndpoint pce = (PetalsCamelEndpoint) endpoint;
            if (pce.getService().getType() == ServiceType.CONSUMES && pce.getMep() == null
                    && pce.getService().getMEP() == null) {
                getContext().getLogger()
                        .warning("No MEP specified neither as an endpoint parameter or in the corresponding Consumes:"
                                + " the MEP specified on the Camel exchange will be used when creating a Petals exchange");
            }
        }
    }

    public PetalsCamelContext getContext() {
        PetalsCamelContext result = this.pcc;
        if (result == null) {
            final PetalsCamelContext found = getCamelContext().getRegistry().lookupByNameAndType(
                    PetalsCamelContext.class.getName(), PetalsCamelContext.class);
            if (found == null) {
                throw new IllegalArgumentException("No instance of PetalsCamelContext available in the Camel registry.");
            }
            this.pcc = found;
            result = found;
        }
        return result;
    }
}
