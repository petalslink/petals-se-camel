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

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.eclipse.jdt.annotation.Nullable;
import org.ow2.petals.camel.PetalsCamelContext;
import org.ow2.petals.camel.component.exceptions.InvalidURIException;

import com.google.common.base.Preconditions;

public class PetalsCamelComponent extends UriEndpointComponent {

    @SuppressWarnings("null")
    private static final Pattern URI_PATTERN = Pattern.compile("^\\w*$");

    private final PetalsCamelContext pcc;

    public PetalsCamelComponent(final PetalsCamelContext pcc) {
        super(pcc.getCamelContext(), PetalsCamelEndpoint.class);
        this.pcc = pcc;
    }

    @Override
    protected Endpoint createEndpoint(final @Nullable String uri, final @Nullable String remaining,
            @Nullable Map<String, Object> parameters) throws Exception {

        Preconditions.checkNotNull(remaining);
        Preconditions.checkNotNull(uri);
        Preconditions.checkNotNull(parameters);

        // remaining can ONLY be the unique id attributed either in the WSDL for provides (consumers)
        // or in the JBI for consumes (providers)!!
        if (!URI_PATTERN.matcher(remaining).matches()) {
            throw new InvalidURIException(remaining);
        }

        // parameters will be set from the class
        return new PetalsCamelEndpoint(uri, this, remaining);
    }

    @Override
    protected boolean useIntrospectionOnEndpoint() {
        // we want to handle manually the setting of parameters because producers and consumers are different
        return false;
    }

    public PetalsCamelContext getContext() {
        return this.pcc;
    }
}
