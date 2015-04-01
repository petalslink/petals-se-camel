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
package org.ow2.petals.camel.se.utils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RoutesDefinition;
import org.ow2.petals.camel.se.exceptions.InvalidCamelRouteDefinitionException;
import org.ow2.petals.camel.se.exceptions.InvalidJBIConfigurationException;

public class CamelRoutesHelper {

    private CamelRoutesHelper() {
    }

    public static RouteBuilder loadRoutesFromClass(ClassLoader classLoader, String className)
            throws InvalidJBIConfigurationException {
        try {
            final Class<?> clazz = classLoader.loadClass(className);
            if (!RouteBuilder.class.isAssignableFrom(clazz)) {
                throw new InvalidJBIConfigurationException(className + " is not a subclass of camel RouteBuilder");
            }
            return (RouteBuilder) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw new InvalidJBIConfigurationException("Can't load class " + className, e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new InvalidJBIConfigurationException("Can't instantiate " + className, e);
        }
    }

    public static RoutesDefinition loadRoutesFromXML(final String xmlName, final ModelCamelContext context,
            final ClassLoader classLoader) throws InvalidCamelRouteDefinitionException,
            InvalidJBIConfigurationException {
        try (final InputStream xml = classLoader.getResourceAsStream(xmlName)) {
            if (xml == null) {
                throw new InvalidJBIConfigurationException("Can't find xml routes definition " + xmlName);
            }
            try {
                return context.loadRoutesDefinition(xml);
            } catch (final Exception e) {
                throw new InvalidCamelRouteDefinitionException("Can't load routes from xml " + xmlName, e);
            }
        } catch (final IOException e) {
            // TODO should that happen?! shouldn't we just log a warning?!
            throw new InvalidJBIConfigurationException("Can't close xml routes definition " + xmlName, e);
        }
    }
}
