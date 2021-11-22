/**
 * Copyright (c) 2015-2021 Linagora
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
package org.ow2.petals.camel.se.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RoutesDefinition;
import org.ow2.petals.camel.se.exceptions.InvalidCamelRouteDefinitionException;
import org.ow2.petals.camel.se.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.camel.se.exceptions.PetalsCamelSEException;

public class CamelRoutesHelper {

    private CamelRoutesHelper() {
    }

    public static RouteBuilder loadRoutesFromClass(final ClassLoader classLoader, final String className,
            final Logger logger)
            throws InvalidJBIConfigurationException {
        try {
            final Class<?> clazz = classLoader.loadClass(className);
            final Object o = clazz.newInstance();
            if (!(o instanceof RouteBuilder)) {
                throw new InvalidJBIConfigurationException(className + " is not a subclass of Camel RouteBuilder");
            }

            if (logger.isLoggable(Level.CONFIG)) {
                logger.config(String.format("Route(s) loaded from class '%s'", className));
            }

            return (RouteBuilder) o;
        } catch (final ClassNotFoundException e) {
            throw new InvalidJBIConfigurationException("Can't load class " + className, e);
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new InvalidJBIConfigurationException("Can't instantiate " + className, e);
        }
    }

    public static RoutesDefinition loadRoutesFromXML(final String xmlName, final ModelCamelContext context,
            final ClassLoader classLoader, final Logger logger) throws PetalsCamelSEException {

        final InputStream xml = classLoader.getResourceAsStream(xmlName);

        if (xml == null) {
            throw new InvalidJBIConfigurationException("Can't find xml routes definition " + xmlName);
        }

        final RoutesDefinition routes;
        try {
            routes = context.loadRoutesDefinition(xml);

            if (logger.isLoggable(Level.CONFIG)) {
                logger.config(String.format("Route(s) loaded from XML definition file '%s'", xmlName));
            }
        } catch (final Exception e) {
            throw new InvalidCamelRouteDefinitionException("Can't load routes from xml " + xmlName, e);
        } finally {
            try {
                xml.close();
            } catch (final IOException e) {
                logger.log(Level.WARNING, "Can't close the xml stream for xml " + xmlName, e);
            }
        }

        if (routes == null) {
            throw new InvalidCamelRouteDefinitionException("Can't load routes from xml " + xmlName);
        }

        return routes;
    }
}
