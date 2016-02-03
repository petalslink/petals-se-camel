/**
 * Copyright (c) 2015-2016 Linagora
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

import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.apache.camel.Consumer;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.ResolveEndpointFailedException;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;
import org.ow2.petals.camel.component.exceptions.IncompatibleEndpointUsageException;
import org.ow2.petals.camel.component.exceptions.InvalidURIException;

@UriEndpoint(scheme = "petals", syntax = "petals:serviceId", title = "Petals ESB", label = "jbi,soa,esb", consumerClass = PetalsCamelConsumer.class)
public class PetalsCamelEndpoint extends DefaultEndpoint {

    @SuppressWarnings("null")
    private static final Pattern URI_PATTERN = Pattern.compile("^[a-zA-Z][\\w.-]*$");

    private static final String PARAMETER_TIMEOUT = "timeout";

    private static final String PARAMETER_OPERATION = "operation";

    private static final String PARAMETER_SERVICE = "serviceName";

    private static final String PARAMETER_ENDPOINT = "endpointName";

    private static final String PARAMETER_SYNCHRONOUS = "synchronous";

    private static final String PARAMETER_MEP = "exchangePattern";

    private final ServiceEndpointOperation service;

    @UriPath
    @Metadata(required = "true")
    private String serviceId;
    
    @UriParam(defaultValue = "-1", name = PARAMETER_TIMEOUT, description = "If 0 then no timeout, if <0 then use the default timeout from the component else specify the timeout in milliseconds")
    private long timeout = -1;

    @Nullable
    @UriParam(name = PARAMETER_OPERATION, description = "If set and the Consumes does not declare any operation, this will be used as the operation of created Exchanges for this endpoint")
    private QName operation;

    @Nullable
    @UriParam(name = PARAMETER_MEP, description = "If set and the Consumes does not declare any MEP, this will be used as the MEP of created Exchanges for this endpoint")
    private MEPPatternConstants mep;

    @Nullable
    @UriParam(name = PARAMETER_SERVICE, description = "If set and the Consumes does not declare any service name, this will be used as the service of created Exchanges for this endpoint")
    private QName serviceName;

    @Nullable
    @UriParam(name = PARAMETER_ENDPOINT, description = "If set and the Consumes does not declare any endpoint name, this will be used as the endpoint name of created Exchanges for this endpoint. A service name must also be set.")
    private String endpointName;

    public PetalsCamelEndpoint(final String endpointUri, final PetalsCamelComponent component, final String remaining)
            throws Exception {
        super(endpointUri, component);

        // remaining can ONLY be the unique id attributed either in the WSDL for provides (consumers)
        // or in the JBI for consumes (providers)!!
        if (!URI_PATTERN.matcher(remaining).matches()) {
            throw new InvalidURIException(remaining);
        }

        this.serviceId = remaining;
        
        this.service = component.getContext().getService(serviceId);

        if (this.service.getType() == ServiceType.PROVIDES) {
            final URI serviceMEP = this.service.getMEP();
            assert serviceMEP != null;

            // this will be used to set the Camel Exchange MEP
            setExchangePattern(ExchangePattern.fromWsdlUri(serviceMEP.toString()));
        }
    }

    /**
     * If there is parameters left in options, then Camel will complain, this serves as syntax checking
     * 
     * Optionally, we can throw an exception to customize the error (it will be wrapped in a
     * {@link ResolveEndpointFailedException} by Camel).
     */
    @NonNullByDefault(false)
    @Override
    public void configureProperties(final Map<String, Object> options) {
        // this will setup some specific properties...
        super.configureProperties(options);

        final String timeoutParameter = (String) options.remove(PARAMETER_TIMEOUT);
        if (timeoutParameter != null) {
            // timeout is only supported if this is a to() (i.e. a consumes in the SU)
            if (this.service.getType() == ServiceType.CONSUMES) {
                this.timeout = Long.parseLong(timeoutParameter);
            } else {
                throw new RuntimeCamelException(
                        "The parameter " + PARAMETER_TIMEOUT + " can't be set on a Provides endpoint.");
            }
        }

        // TODO add the possibility to change that at runtime (or rather, to force something using an MBean for example
        // for debugging...)
        final String synchronousParameter = (String) options.remove(PARAMETER_SYNCHRONOUS);
        if (synchronousParameter != null) {
            this.setSynchronous(Boolean.parseBoolean(synchronousParameter));
        }

        final String serviceParameter = (String) options.remove(PARAMETER_SERVICE);
        if (serviceParameter != null) {
            if (this.service.getType() == ServiceType.PROVIDES) {
                throw new RuntimeCamelException(
                        "The parameter " + PARAMETER_SERVICE + " can't be set on a Provides endpoint.");
            }

            if (this.service.getService() != null) {
                throw new RuntimeCamelException("The parameter " + PARAMETER_SERVICE
                        + " can't be set on a Consumes endpoint if it already declares a service.");
            }

            // TODO shouldn't I use the namespace of... the consumes? the interface?
            this.serviceName = QName.valueOf(serviceParameter);
        }

        final String endpointParameter = (String) options.remove(PARAMETER_ENDPOINT);
        if (endpointParameter != null) {
            if (this.service.getType() == ServiceType.PROVIDES) {
                throw new RuntimeCamelException(
                        "The parameter " + PARAMETER_ENDPOINT + " can't be set on a Provides endpoint.");
            }

            if (this.service.getService() != null) {
                throw new RuntimeCamelException("The parameter " + PARAMETER_ENDPOINT
                        + " can't be set on a Consumes endpoint if it already declares an endpoint name.");
            }

            if (this.serviceName == null) {
                throw new RuntimeCamelException("The parameter " + PARAMETER_ENDPOINT
                        + " can't be set on an endpoint if it does not also declare a service name.");
            }

            this.endpointName = endpointParameter;
        }

        final String mepParameter = (String) options.remove(PARAMETER_MEP);
        if (mepParameter != null) {
            if (this.service.getType() == ServiceType.PROVIDES) {
                // TODO instead, we should handle these cases with some kind of subtyping...
                throw new RuntimeCamelException(
                        "The parameter " + PARAMETER_MEP + " can't be set on a Provides endpoint.");
            }

            if (this.service.getMEP() != null) {
                throw new RuntimeCamelException("The parameter " + PARAMETER_MEP
                        + " can't be set on a Consumes endpoint if it already declares a MEP.");
            }

            this.mep = MEPPatternConstants.fromString(ExchangePattern.valueOf(mepParameter).getWsdlUri());
        }
        
        final String operation = (String) options.remove(PARAMETER_OPERATION);
        if (operation != null) {
            if (this.service.getType() == ServiceType.PROVIDES) {
                throw new RuntimeCamelException(
                        "The parameter " + PARAMETER_OPERATION + " can't be set on a Provides endpoint.");
            }

            if (this.service.getOperation() != null) {
                throw new RuntimeCamelException("The parameter " + PARAMETER_OPERATION
                        + " can't be set on a Consumes endpoint if it already declares an operation.");
            }

            // TODO shouldn't I use the namespace of... the consumes? the interface?
            this.operation = QName.valueOf(operation);
        }
    }

    public long getTimeout() {
        return timeout;
    }

    public @Nullable QName getOperation() {
        return operation;
    }

    public @Nullable QName getServiceName() {
        return serviceName;
    }

    public @Nullable String getEndpointName() {
        return endpointName;
    }

    public @Nullable MEPPatternConstants getMep() {
        return mep;
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
            throw new IncompatibleEndpointUsageException(this.service, ServiceType.CONSUMES);
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

        assert processor != null;

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
