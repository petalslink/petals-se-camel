/**
 * Copyright (c) 2015-2024 Linagora
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
package org.ow2.petals.se.camel.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesLoader;
import org.apache.camel.support.ResourceHelper;
import org.ow2.petals.se.camel.exceptions.InvalidCamelRouteDefinitionException;
import org.ow2.petals.se.camel.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.se.camel.exceptions.PetalsCamelSEException;

public class CamelRoutesHelper {

    private CamelRoutesHelper() {
    }

    /**
     * @param classLoader
     * @param className
     *            Class name containing the Camel route definitions. Not {@code null}.
     * @param logger
     * @return
     * @throws InvalidJBIConfigurationException
     */
    public static RouteBuilder loadRoutesFromClass(final ClassLoader classLoader, final String className,
            final Logger logger) throws InvalidJBIConfigurationException {
        assert className != null;

        if (className.isEmpty()) {
            throw new InvalidJBIConfigurationException("className must be not empty");
        }
        try {
            final Class<?> clazz = classLoader.loadClass(className.trim());
            final Object o = clazz.getConstructor().newInstance();
            if (!(o instanceof RouteBuilder routeBuilder)) {
                throw new InvalidJBIConfigurationException(className + " is not a subclass of Camel RouteBuilder");
            }

            if (logger.isLoggable(Level.CONFIG)) {
                logger.config(String.format("Route(s) loaded from class '%s'", className));
            }

            return routeBuilder;
        } catch (final ClassNotFoundException e) {
            throw new InvalidJBIConfigurationException("Can't load class " + className, e);
        } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException
                | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            throw new InvalidJBIConfigurationException("Can't instantiate " + className, e);
        }
    }

    public static void loadRoutesFromXML(final String xmlName, final ModelCamelContext context,
            final Logger logger) throws PetalsCamelSEException {

        final Resource resource = ResourceHelper.resolveResource(context, xmlName);
        if (resource == null || !resource.exists()) {
            throw new InvalidJBIConfigurationException("Can't find xml routes definition " + xmlName);
        }

        final RoutesLoader loader = context.getCamelContextExtension().getContextPlugin(RoutesLoader.class);
        try {
            loader.loadRoutes(resource);

            if (logger.isLoggable(Level.CONFIG)) {
                logger.config(String.format("Route(s) loaded from XML definition file '%s'", xmlName));
            }
        } catch (final Exception e) {
            throw new InvalidCamelRouteDefinitionException("Can't load routes from xml " + xmlName, e);
        }
    }
}
