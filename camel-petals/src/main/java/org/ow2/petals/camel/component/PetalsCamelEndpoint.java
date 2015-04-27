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

import org.apache.camel.Consumer;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;
import org.ow2.petals.camel.component.exceptions.IncompatibleEndpointUsageException;
import org.ow2.petals.camel.exceptions.UnknownServiceException;

@UriEndpoint(scheme = "petals", syntax = "petals:serviceId", consumerClass = PetalsCamelConsumer.class)
public class PetalsCamelEndpoint extends DefaultEndpoint {

    private static final String PARAMETER_TIMEOUT = "timeout";

    private static final String PARAMETER_SYNCHRONOUS = "synchronous";

    private static final String PARAMETER_MEP = "exchangePattern";

    @UriPath
    @Metadata(required = "true")
    private String serviceId;
    
    @UriParam(defaultValue = "-1", name = PARAMETER_TIMEOUT, description = "If 0 then no timeout, if <0 then use the default timeout from the component else specify the timeout in milliseconds")
    private long timeout = -1;

    private final ServiceEndpointOperation service;

    public PetalsCamelEndpoint(final String endpointUri, final PetalsCamelComponent component, final String serviceId)
            throws UnknownServiceException {

        super(endpointUri, component);

        this.serviceId = serviceId;
        
        this.service = component.getContext().getService(serviceId);

        setExchangePattern(ExchangePattern.fromWsdlUri(service.getMEP().toString()));
    }

    /**
     * If there is parameters left in options, then Camel will complain, this serves as syntax checking
     */
    @NonNullByDefault(false)
    @Override
    public void configureProperties(final Map<String, Object> options) {

        // this will setup some specific properties...
        super.configureProperties(options);

        // timeout is only supported if this is a to() (i.e. a consumes in the SU)
        if (this.service.getType() == ServiceType.CONSUMES) {
            final String s = (String) options.remove(PARAMETER_TIMEOUT);
            if (s != null) {
                this.timeout = Long.parseLong(s);
            }
        }
        // TODO add the possibility to change that at runtime (or rather, to force something using an MBean for example
        // for debugging...)
        this.setSynchronous(Boolean.parseBoolean((String) options.remove(PARAMETER_SYNCHRONOUS)));

        // the mep is not used because we use the one declared in the SU (see the constructor)
        // TODO should I verify that the patterns match (with the one of the endpoint)
        // is there some kind of subtyping of MEP? see JBI spec!
        // (ResolveEndpointFailedException should be thrown in case of problems)
    }

    public long getTimeout() {
        return timeout;
    }

    public ServiceEndpointOperation getService() {
        return service;
    }

    /**
     * It's a to()
     * 
     * It can be one of our jbi-consumes
     */
    @Override
    public Producer createProducer() throws IncompatibleEndpointUsageException {
        if (this.service.getType() != ServiceType.CONSUMES) {
            throw new IncompatibleEndpointUsageException(this.service, ServiceType.PROVIDES);
        }
        return new PetalsCamelProducer(this);
    }

    /**
     * It's a from()
     * 
     * It can be one of our jbi-provides
     */
    @NonNullByDefault(false)
    @Override
    public Consumer createConsumer(final Processor processor) throws IncompatibleEndpointUsageException {

        if (this.service.getType() != ServiceType.PROVIDES) {
            throw new IncompatibleEndpointUsageException(this.service, ServiceType.PROVIDES);
        }

        // create the consumer for our JBI provides
        return new PetalsCamelConsumer(this, processor);
    }

    @Override
    public boolean isSingleton() {
        // See
        // http://camel.465427.n5.nabble.com/Endpoint-and-Producer-isSingleton-behavior-in-the-OSGi-tp5721537p5721538.html
        // It seems that a singleton is reused if it has the same URI, it also applies to its producers...
        // -> it's not clear what is the impact of all of that, in the end, it's just a few objects created.
        // Question: should we try to limit the number of objects created? it's not so
        // big if they are duplicated as they don't have many things to store... and anyway, is there really the need
        // for having the same object just because the uri is the same?!
        // -> for now use false
        return false;
    }

    @SuppressWarnings("null")
    @Override
    public PetalsCamelComponent getComponent() {
        return (PetalsCamelComponent) super.getComponent();
    }
}
