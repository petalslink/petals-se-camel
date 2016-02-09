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
package org.ow2.petals.camel.se;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RoutesDefinition;
import org.ow2.petals.camel.PetalsCamelContext;
import org.ow2.petals.camel.PetalsCamelRoute;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.PetalsChannel.PetalsProvidesChannel;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.exceptions.UnknownServiceException;
import org.ow2.petals.camel.se.exceptions.InvalidCamelRouteDefinitionException;
import org.ow2.petals.camel.se.exceptions.PetalsCamelSEException;
import org.ow2.petals.camel.se.impl.ServiceEndpointOperationConsumes;
import org.ow2.petals.camel.se.impl.ServiceEndpointOperationProvides;
import org.ow2.petals.camel.se.utils.CamelRoutesHelper;

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
     * Mapping from serviceId to operations
     * 
     * Needed by the camel endpoint to resolve the URI in a from() or a to()
     */
    private final ImmutableMap<String, ServiceEndpointOperation> sid2seo;

    private final Set<RouteBuilder> classRoutes = new HashSet<>();

    /**
     * The Camel engine dedicated to this SU
     */
    private final ModelCamelContext context;

    /**
     * The classloader allocated to this SU
     */
    private final URLClassLoader classLoader;

    private final CamelSUManager manager;

    private final Logger suLogger;

    public CamelSU(final ImmutableMap<String, ServiceEndpointOperation> sid2seo, final ImmutableList<String> classNames,
            final ImmutableList<String> xmlNames, final URLClassLoader classLoader, final Logger suLogger,
            final CamelSUManager manager) throws PetalsCamelSEException {
        this.classLoader = classLoader;
        this.sid2seo = sid2seo;
        this.manager = manager;
        this.suLogger = suLogger;

        this.context = new DefaultCamelContext();

        this.context.getShutdownStrategy().setTimeout(10);
        this.context.getShutdownStrategy().setTimeUnit(TimeUnit.SECONDS);

        // needed so that routes are executed with the correct context classloader
        // (for example JAXB uses it to load classes)
        this.context.setApplicationContextClassLoader(classLoader);

        // register us as the PetalsCamelContext for this CamelContext, it will be used by the PetalsCamelComponent to
        // initialise itself
        this.context.getRegistry(JndiRegistry.class).bind(PetalsCamelContext.class.getName(), this);

        for (final String className : classNames) {
            assert className != null;
            final RouteBuilder routes = CamelRoutesHelper.loadRoutesFromClass(classLoader, className);

            try {
                context.addRoutes(routes);
            } catch (final Exception e) {
                throw new InvalidCamelRouteDefinitionException("Can't add routes from class " + className
                        + " to Camel context", e);
            }
            
            this.classRoutes.add(routes);
        }

        for (final String xmlName : xmlNames) {
            assert xmlName != null;
            final RoutesDefinition routes = CamelRoutesHelper.loadRoutesFromXML(xmlName, context, classLoader,
                    getLogger());

            try {
                context.addRouteDefinitions(routes.getRoutes());
            } catch (final Exception e) {
                throw new InvalidCamelRouteDefinitionException("Can't add routes from xml file " + xmlName
                        + " to Camel context", e);
            }
        }

        try {
            context.start();
        } catch (final Exception e) {
            throw new PetalsCamelSEException("Problem starting the Camel context", e);
        }

        callMethods("deploy");
    }

    public void init() throws PetalsCamelSEException {
        callMethods("init");
    }

    public void shutdown() throws PetalsCamelSEException {
        callMethods("shutdown");
    }

    public void stop() throws PetalsCamelSEException {
        callMethods("stop");
    }

    public void start() throws PetalsCamelSEException {
        callMethods("start");
    }

    public void undeploy() throws PetalsCamelSEException {
        try {
            callMethods("undeploy");
        } catch (final Exception e) {
            getLogger().log(Level.SEVERE, "Can't undeploy the Route definitions of the SU", e);
        }

        try {
            context.stop();
        } catch (final Exception e) {
            getLogger().log(Level.SEVERE, "Can't stop the Camel context of the SU", e);
        }

        try {
            this.classLoader.close();
        } catch (final IOException e) {
            // let's log it, it is severe because it uses memory!
            getLogger().log(Level.SEVERE, "Can't close the classloader of the SU", e);
        }
    }

    private void callMethods(final String method) throws PetalsCamelSEException {
        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            for (final RouteBuilder routeBuilder : this.classRoutes) {
                assert routeBuilder != null;
                callMethod(method, routeBuilder);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(ccl);
        }
    }

    private static void callMethod(final String methodName, final Object object) throws PetalsCamelSEException {
        try {
            final Method method = object.getClass().getMethod(methodName);
            method.invoke(object);
        } catch (final SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new PetalsCamelSEException(
                    "Incorrect " + methodName + "() method definition: it must be public and have no parameters.");
        } catch (final NoSuchMethodException e) {
            // do nothing
        }
    }

    @Override
    public ServiceEndpointOperation getService(final String serviceId) throws UnknownServiceException {
        final ServiceEndpointOperation seo = this.sid2seo.get(serviceId);
        if (seo == null) {
            throw new UnknownServiceException(serviceId);
        }
        return seo;
    }

    @Override
    public void registerRoute(final ServiceEndpointOperation service, final PetalsCamelRoute route) {
        this.manager.registerRoute(service, route);
    }

    @Override
    public void unregisterRoute(final ServiceEndpointOperation seo) {
        this.manager.unregisterRoute(seo);
    }

    @Override
    public PetalsConsumesChannel getConsumesChannel(final ServiceEndpointOperation seo) {
        assert seo instanceof PetalsConsumesChannel : "This can't happen";
        assert seo instanceof ServiceEndpointOperationConsumes : "This can't happen";
        return (PetalsConsumesChannel) seo;
    }

    @Override
    public PetalsProvidesChannel getProvidesChannel(final ServiceEndpointOperation seo) {
        assert seo instanceof PetalsProvidesChannel : "This can't happen";
        assert seo instanceof ServiceEndpointOperationProvides : "This can't happen";
        return (PetalsProvidesChannel) seo;
    }
    
    @Override
    public CamelContext getCamelContext() {
        return this.context;
    }

    @Override
    public Logger getLogger() {
        return this.suLogger;
    }
}
