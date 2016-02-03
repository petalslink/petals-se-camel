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

import java.io.Serializable;
import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.ow2.petals.camel.PetalsCamelContext;
import org.ow2.petals.camel.component.exceptions.InvalidURIException;

public class PetalsCamelComponent extends UriEndpointComponent {
    
    @SuppressWarnings("null")
    private static final Pattern URI_PATTERN = Pattern.compile("^\\w*$");

    /**
     * Prefix for a property that will be converted from a header in a new Petals exchange in Petals Consumers.
     */
    public static final String EXCHANGE_ORIGINAL_HEADER_PREFIX = "PetalsOriginalHeader.";

    /**
     * Prefix for a property that will be converted to a header in a new Petals exchange in Petals Producers.
     * 
     * Note that Petals expects {@link Serializable} properties!
     */
    public static final String EXCHANGE_HEADER_PREFIX = "PetalsHeader.";

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

    @Nullable
    private PetalsCamelContext pcc;

    public PetalsCamelComponent() {
        super(PetalsCamelEndpoint.class);
    }

    @NonNullByDefault(false)
    @Override
    protected Endpoint createEndpoint(final String uri, final String remaining, final Map<String, Object> parameters)
            throws Exception {

        // remaining can ONLY be the unique id attributed either in the WSDL for provides (consumers)
        // or in the JBI for consumes (providers)!!
        if (!URI_PATTERN.matcher(remaining).matches()) {
            throw new InvalidURIException(remaining);
        }

        // parameters will be set from the class's configureProperties
        return new PetalsCamelEndpoint(uri, this, remaining);
    }

    @Override
    protected boolean useIntrospectionOnEndpoint() {
        // we want to handle manually the setting of parameters because producers and consumers are different
        return false;
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
