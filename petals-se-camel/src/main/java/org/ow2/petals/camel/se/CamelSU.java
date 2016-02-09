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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.impl.PropertyPlaceholderDelegateRegistry;
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
import org.ow2.petals.commons.log.Level;

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

    private final Set<RouteBuilder> classRoutes = new HashSet<RouteBuilder>();

    /**
     * The Camel engine dedicated to this SU
     */
    private final ModelCamelContext context;

    /**
     * The classloader allocated to this SU
     */
    private final URLClassLoader classLoader;

    private final CamelSUManager manager;

    public CamelSU(final ImmutableMap<String, ServiceEndpointOperation> sid2seo,
            final ImmutableList<String> classNames, final ImmutableList<String> xmlNames,
            final URLClassLoader classLoader, final CamelSUManager manager) throws PetalsCamelSEException {
        this.classLoader = classLoader;
        this.sid2seo = sid2seo;
        this.manager = manager;

        this.context = new DefaultCamelContext();

        this.context.getShutdownStrategy().setTimeout(10);
        this.context.getShutdownStrategy().setTimeUnit(TimeUnit.SECONDS);

        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();

        try {
            // this is needed because this version of Camel does not properly use
            // the application class loader during initialisation and start.
            Thread.currentThread().setContextClassLoader(classLoader);

            // needed so that routes are executed with the correct context classloader
            // (for example JAXB uses it to load classes)
            this.context.setApplicationContextClassLoader(classLoader);

            // register us as the PetalsCamelContext for this CamelContext, it will be used by the PetalsCamelComponent
            // to
            // initialise itself
            ((JndiRegistry) ((PropertyPlaceholderDelegateRegistry) this.context.getRegistry()).getRegistry())
                    .bind(PetalsCamelContext.class.getName(), this);

            for (final String className : classNames) {
                final RouteBuilder routes = CamelRoutesHelper.loadRoutesFromClass(classLoader, className);

                try {
                    context.addRoutes(routes);
                } catch (final Exception e) {
                    throw new InvalidCamelRouteDefinitionException(
                            "Can't add routes from class " + className + " to Camel context", e);
                }

                this.classRoutes.add(routes);
            }
            
            for (final String xmlName : xmlNames) {
                final RoutesDefinition routes = CamelRoutesHelper.loadRoutesFromXML(xmlName, context, classLoader,
                        getLogger());

                try {
                    context.addRouteDefinitions(routes.getRoutes());
                } catch (final Exception e) {
                    throw new InvalidCamelRouteDefinitionException(
                            "Can't add routes from xml file " + xmlName + " to Camel context", e);
                }
            }

            try {
                context.start();
            } catch (final Exception e) {
                throw new PetalsCamelSEException("Problem starting the Camel context", e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(ccl);
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

        // TODO normally we should close the classloader, but there is nothing to do so in Java6
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
        } catch (final NoSuchMethodException e) {
            // do nothing
        } catch (IllegalAccessException e) {
            throw new PetalsCamelSEException(
                    "Incorrect " + methodName + "() method definition: it must be public and have no parameters.");
        } catch (IllegalArgumentException e) {
            throw new PetalsCamelSEException(
                    "Incorrect " + methodName + "() method definition: it must be public and have no parameters.");
        } catch (InvocationTargetException e) {
            throw new PetalsCamelSEException(
                    "Incorrect " + methodName + "() method definition: it must be public and have no parameters.");
        } catch (final SecurityException e) {
            throw new PetalsCamelSEException(
                    "Incorrect " + methodName + "() method definition: it must be public and have no parameters.");
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
        return this.manager.getLogger();
    }
}
