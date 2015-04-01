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
package org.ow2.petals.camel.se;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.logging.Logger;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RoutesDefinition;
import org.ow2.petals.camel.PetalsCamelContext;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.PetalsChannel.PetalsProvidesChannel;
import org.ow2.petals.camel.PetalsProvidesOperation;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.component.PetalsCamelComponent;
import org.ow2.petals.camel.exceptions.AlreadyRegisteredServiceException;
import org.ow2.petals.camel.exceptions.UnknownRegisteredServiceException;
import org.ow2.petals.camel.exceptions.UnknownServiceException;
import org.ow2.petals.camel.se.exceptions.InvalidCamelRouteDefinitionException;
import org.ow2.petals.camel.se.impl.ServiceEndpointOperationConsumes;
import org.ow2.petals.camel.se.impl.ServiceEndpointOperationProvides;
import org.ow2.petals.camel.se.utils.CamelRoutesHelper;
import org.ow2.petals.component.framework.api.exception.PEtALSCDKException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * This handles the mapping between what is declared in a SU and a PetalsComponent (which is a Camel component for
 * communicating with Petals)
 * 
 * It dispatches messages to the correct route based on the ServiceEnpointOperation
 * 
 * @author vnoel
 *
 */
public class CamelSU implements PetalsCamelContext {

    /**
     * Mapping from serviceId to opertaions
     * 
     * Needed by the camel endpoint to resolve the URI in a from() or a to()
     */
    private final ImmutableMap<String, ServiceEndpointOperation> sid2seo;

    /**
     * The Camel engine dedicated to this SU
     */
    private final ModelCamelContext context;

    /**
     * The classloader allocated to this SU
     */
    private final URLClassLoader classLoader;

    private final CamelSUManager manager;

    public CamelSU(final String su, final ImmutableMap<String, ServiceEndpointOperation> sid2seo,
            final ImmutableList<String> classNames, final ImmutableList<String> xmlNames,
            final URLClassLoader classLoader, final CamelSUManager manager) throws InvalidCamelRouteDefinitionException {

        this.classLoader = classLoader;
        this.sid2seo = sid2seo;
        this.manager = manager;
        this.context = new DefaultCamelContext();

        context.addComponent("petals", new PetalsCamelComponent(this));

        for (final String className : classNames) {
            try {
                final RouteBuilder routes = CamelRoutesHelper.loadRoutesFromClass(classLoader, className);
                context.addRoutes(routes);
            } catch (Exception e) {
                throw new InvalidCamelRouteDefinitionException("Can't add routes from class " + className
                        + " to Camel context", e);
            }
        }

        for (final String xmlName : xmlNames) {
            try {
                final RoutesDefinition routes = CamelRoutesHelper.loadRoutesFromXML(xmlName, context, classLoader);
                context.addRouteDefinitions(routes.getRoutes());
            } catch (Exception e) {
                throw new InvalidCamelRouteDefinitionException("Can't add routes from xml file " + xmlName
                        + " to Camel context", e);
            }
        }
    }

    public void stop() throws PEtALSCDKException {
        // TODO other things?
        try {
            context.stop();
        } catch (Exception e) {
            throw new PEtALSCDKException("Problem stopping the Camel context", e);
        }
    }

    public void start() throws PEtALSCDKException {
        // TODO other things?
        try {
            context.start();
        } catch (Exception e) {
            throw new PEtALSCDKException("Problem starting the Camel context", e);
        }
    }

    public void undeploy() throws PEtALSCDKException {
        // TODO other things?
        try {
            this.classLoader.close();
        } catch (IOException e) {
            throw new PEtALSCDKException("Problem closing the classloader", e);
        }
    }

    @Override
    public ServiceEndpointOperation getSEO(final String serviceId) throws UnknownServiceException {
        final ServiceEndpointOperation seo = this.sid2seo.get(serviceId);
        if (seo == null) {
            throw new UnknownServiceException(serviceId);
        }
        return seo;
    }

    @Override
    public void registerPPO(final ServiceEndpointOperation seo, final PetalsProvidesOperation ppo)
            throws AlreadyRegisteredServiceException {
        this.manager.registerPPO(seo, ppo);
    }

    @Override
    public void unregisterPPO(final ServiceEndpointOperation seo) throws UnknownRegisteredServiceException {
        this.manager.unregisterPPO(seo);
    }

    @Override
    public PetalsConsumesChannel getConsumesChannel(final ServiceEndpointOperation seo) {
        assert seo instanceof ServiceEndpointOperationConsumes : "This can't happen";
        assert seo instanceof PetalsConsumesChannel : "This can't happen";
        return (PetalsConsumesChannel) seo;
    }

    @Override
    public PetalsProvidesChannel getProvidesChannel(final ServiceEndpointOperation seo) {
        assert seo instanceof ServiceEndpointOperationProvides : "This can't happen";
        assert seo instanceof PetalsProvidesChannel : "This can't happen";
        return (PetalsProvidesChannel) seo;
    }
    
    @Override
    public CamelContext getCamelContext() {
        return this.context;
    }

    @Override
    public Logger getLogger() {
        return this.manager.getLogger();
    }
}
