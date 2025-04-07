/**
 * Copyright (c) 2024-2025 Linagora
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

import org.apache.camel.spi.Metadata;

/**
 * The class that contains all the name of Camel headers that are supported by our Camel component acting as service
 * provider and service consumer.
 */
public final class PetalsConstants {

    // ------------------------------------------------------------------------------------------------------------
    // Headers set by our Camel component into Camel message, acting as Camel consumer (ie. acting as service provider)
    // receiving JBI request or returning JBI response.
    // ------------------------------------------------------------------------------------------------------------

    /**
     * <p>
     * Prefix for message properties that must be transmitted from a service provider to a Camel consumer when receiving
     * a JBI request, and from a Camel consumer to the service provider when returning a JBI response.
     * </p>
     * <p>
     * <b>Note:</b> Petals expects {@link Serializable} properties !
     * </p>
     */
    public static final String EXCHANGE_ORIGINAL_PROPERTY_PREFIX = "PetalsOriginalProperty.";

    @Metadata(
            label = "consumer", description = "The interface name of the current associated service provider", javaType = "QName"
    )
    public static final String EXCHANGE_ORIGINAL_INTERFACE = "PetalsOriginalInterface";

    @Metadata(
            label = "consumer", description = "The service name of the current associated service provider", javaType = "QName"
    )
    public static final String EXCHANGE_ORIGINAL_SERVICE = "PetalsOriginalService";

    @Metadata(
            label = "consumer", description = "The service endpoint of the current associated service provider", javaType = "javax.jbi.servicedesc.ServiceEndpoint"
    )
    public static final String EXCHANGE_ORIGINAL_ENDPOINT = "PetalsOriginalEndpoint";

    @Metadata(
            label = "consumer", description = "The operation name of the current associated service provider", javaType = "QName"
    )
    public static final String EXCHANGE_ORIGINAL_OPERATION = "PetalsOriginalOperation";

    @Metadata(
            label = "consumer", description = "The exchange pattern of the current associated service provider", javaType = "URI"
    )
    public static final String EXCHANGE_ORIGINAL_MEP = "PetalsOriginalPattern";

    @Metadata(
            label = "consumer", javaType = "Boolean", description = "The current flow tracing activation state in the JBI exchange received at service provider level"
    )
    public static final String EXCHANGE_CURRENT_FLOW_TRACING_ACTIVATION = "PetalsCurrentFlowTracingActivationStateOnJBIExchangeProcessingAtServiceProviderLevel";

    // ------------------------------------------------------------------------------------------------------------
    // Headers set by our Camel component into Camel message, acting as Camel provider (ie. acting as service consumer)
    // sending JBI request or receiving JVI response.
    // ------------------------------------------------------------------------------------------------------------

    /**
     * <p>
     * Prefix for message properties that must be transmitted from a Camel producer to a service consumer when sending a
     * JBI request, and from a service consumer to a Camel producer when receiving a JB response.
     * </p>
     * <p>
     * <b>Note:</b> Petals expects {@link Serializable} properties !
     * </p>
     */
    public static final String EXCHANGE_PROPERTY_PREFIX = "PetalsProperty.";

    @Metadata(label = "producer", javaType = "Boolean", description = "Set to `true` if the JBI respone is a fault")
    public static final String MESSAGE_FAULT_HEADER = "PetalsMessageIsFault";

    private PetalsConstants() {
        // Utility class
    }

}
