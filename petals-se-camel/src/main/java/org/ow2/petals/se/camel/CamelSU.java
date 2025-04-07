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
package org.ow2.petals.se.camel;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.ow2.petals.camel.PetalsCamelContext;
import org.ow2.petals.camel.PetalsCamelRoute;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.PetalsChannel.PetalsProvidesChannel;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.exceptions.UnknownServiceException;
import org.ow2.petals.camel.helpers.PetalsRouteBuilder;
import org.ow2.petals.component.framework.api.monitoring.MonitTraceLogger;
import org.ow2.petals.component.framework.api.util.Placeholders;
import org.ow2.petals.se.camel.exceptions.InvalidCamelRouteDefinitionException;
import org.ow2.petals.se.camel.exceptions.PetalsCamelSEException;
import org.ow2.petals.se.camel.impl.ServiceEndpointOperationConsumes;
import org.ow2.petals.se.camel.impl.ServiceEndpointOperationProvides;
import org.ow2.petals.se.camel.utils.CamelRoutesHelper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * This handles the mapping between what is declared in a SU and a PetalsComponent (which is a Camel component for
 * communicating with Petals) It dispatches messages to the correct route based on the ServiceEnpointOperation
 * 
 * @author vnoel
 */
public class CamelSU implements PetalsCamelContext {

    /**
     * Mapping from serviceId to operations Needed by the camel endpoint to resolve the URI in a from() or a to()
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

    private final MonitTraceLogger monitTraceLogger;

    public CamelSU(final ImmutableMap<String, ServiceEndpointOperation> sid2seo, final ImmutableList<String> classNames,
            final ImmutableList<String> xmlNames, final URLClassLoader classLoader, final Logger suLogger,
            final CamelSUManager manager, final MonitTraceLogger monitTraceLogger) throws PetalsCamelSEException {
        this.classLoader = classLoader;
        this.sid2seo = sid2seo;
        this.manager = manager;
        this.suLogger = suLogger;
        this.monitTraceLogger = monitTraceLogger;

        this.context = new DefaultCamelContext();

        this.context.getShutdownStrategy().setTimeout(10);
        this.context.getShutdownStrategy().setTimeUnit(TimeUnit.SECONDS);

        // needed so that routes are executed with the correct context classloader
        // (for example JAXB uses it to load classes)
        this.context.setApplicationContextClassLoader(classLoader);

        // register us as the PetalsCamelContext for this CamelContext, it will be used by the PetalsCamelComponent to
        // initialise itself
        this.context.getRegistry().bind(PetalsCamelContext.class.getName(), this);

        for (final String className : classNames) {
            assert className != null;
            final RouteBuilder routes = CamelRoutesHelper.loadRoutesFromClass(classLoader, className, suLogger);

            try {
                this.context.addRoutes(routes);
            } catch (final Exception e) {
                throw new InvalidCamelRouteDefinitionException(
                        "Can't add routes from class " + className + " to Camel context", e);
            }

            this.classRoutes.add(routes);
        }

        for (final String xmlName : xmlNames) {
            assert xmlName != null;

            CamelRoutesHelper.loadRoutesFromXML(xmlName, this.context, getLogger());
        }

        try {
            this.context.start();
        } catch (final Exception e) {
            throw new PetalsCamelSEException("Problem starting the Camel context", e);
        }

        /*
         * Execute actions to do on deployment of the route definitions. Only for Camel routes based on
         * PetalsRouteBuilder
         */
        for (final RouteBuilder routeBuilder : this.classRoutes) {
            assert routeBuilder != null;
            if (routeBuilder instanceof PetalsRouteBuilder) {
                try {
                    ((PetalsRouteBuilder) routeBuilder).deploy();
                } catch (final Exception e) {
                    getLogger().log(Level.SEVERE, "Can't deploy the Route definitions of the SU", e);
                }
            }
        }
    }

    /**
     * Execute actions to do on init of the route definitions. Only for Camel routes based on {@link PetalsRouteBuilder}
     */
    public void init() throws PetalsCamelSEException {
        for (final RouteBuilder routeBuilder : this.classRoutes) {
            assert routeBuilder != null;
            if (routeBuilder instanceof PetalsRouteBuilder) {
                try {
                    ((PetalsRouteBuilder) routeBuilder).init();
                } catch (final Exception e) {
                    getLogger().log(Level.SEVERE, "Can't init the Route definitions of the SU", e);
                }
            }
        }
    }

    /**
     * Execute actions to do on shutdown of the route definitions. Only for Camel routes based on
     * {@link PetalsRouteBuilder}
     */
    public void shutdown() throws PetalsCamelSEException {
        for (final RouteBuilder routeBuilder : this.classRoutes) {
            assert routeBuilder != null;
            if (routeBuilder instanceof PetalsRouteBuilder) {
                try {
                    ((PetalsRouteBuilder) routeBuilder).shutdown();
                } catch (final Exception e) {
                    getLogger().log(Level.SEVERE, "Can't shutdown the Route definitions of the SU", e);
                }
            }
        }
    }

    /**
     * Execute actions to do on stop of the route definitions. Only for Camel routes based on {@link PetalsRouteBuilder}
     */
    public void stop() throws PetalsCamelSEException {
        for (final RouteBuilder routeBuilder : this.classRoutes) {
            assert routeBuilder != null;
            if (routeBuilder instanceof PetalsRouteBuilder) {
                try {
                    ((PetalsRouteBuilder) routeBuilder).stop();
                } catch (final Exception e) {
                    getLogger().log(Level.SEVERE, "Can't stop the Route definitions of the SU", e);
                }
            }
        }
    }

    /**
     * Execute actions to do on startup of the route definitions. Only for Camel routes based on
     * {@link PetalsRouteBuilder}
     */
    public void start() throws PetalsCamelSEException {
        for (final RouteBuilder routeBuilder : this.classRoutes) {
            assert routeBuilder != null;
            if (routeBuilder instanceof PetalsRouteBuilder) {
                try {
                    ((PetalsRouteBuilder) routeBuilder).start();
                } catch (final Exception e) {
                    getLogger().log(Level.SEVERE, "Can't start the Route definitions of the SU", e);
                }
            }
        }
    }

    /**
     * Execute actions to do on undeployment of the route definitions. Only for Camel routes based on
     * {@link PetalsRouteBuilder}
     */
    public void undeploy() {
        for (final RouteBuilder routeBuilder : this.classRoutes) {
            assert routeBuilder != null;
            if (routeBuilder instanceof PetalsRouteBuilder) {
                try {
                    ((PetalsRouteBuilder) routeBuilder).undeploy();
                } catch (final Exception e) {
                    getLogger().log(Level.SEVERE, "Can't undeploy the Route definitions of the SU", e);
                }
            }
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

    /**
     * Notifies placeholders reloading, only to Camel routes based on {@link PetalsRouteBuilder}
     * 
     * @param placeholders
     *            New values of placeholders
     */
    public void onPlaceHolderValuesReloaded(final Placeholders placeholders) {
        for (final RouteBuilder routeBuilder : this.classRoutes) {
            assert routeBuilder != null;
            if (routeBuilder instanceof PetalsRouteBuilder) {
                ((PetalsRouteBuilder) routeBuilder).onPlaceHolderValuesReloaded(placeholders);
            }
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

    @Override
    public MonitTraceLogger getMonitTraceLogger() {
        return this.monitTraceLogger;
    }
}
