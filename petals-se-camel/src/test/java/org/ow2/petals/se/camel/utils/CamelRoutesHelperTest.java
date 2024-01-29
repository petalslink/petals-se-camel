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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;

import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;
import org.ow2.petals.se.camel.exceptions.InvalidCamelRouteDefinitionException;
import org.ow2.petals.se.camel.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.se.camel.mocks.TestRoutesKO1;
import org.ow2.petals.se.camel.mocks.TestRoutesOK;

public class CamelRoutesHelperTest {

    private static final Logger LOG = Logger.getLogger(CamelRoutesHelperTest.class.getName());

    private static final String XML_ROUTES_OK = "tests/routes-valid-1-1.xml";

    private static final String XML_ROUTES_KO = "tests/routes-invalid.xml";

    private static final String CLASS_ROUTES_OK = TestRoutesOK.class.getName();

    private static final String CLASS_ROUTES_KO_SUB = CamelRoutesHelperTest.class.getName();

    private static final String CLASS_ROUTES_KO_NO = "fakefakefake.fake";

    @Test
    public void testLoadRoutesClass_ok() throws Exception {
        CamelRoutesHelper.loadRoutesFromClass(getClass().getClassLoader(), CLASS_ROUTES_OK, LOG);
    }

    @Test
    public void testLoadRoutesClass_ok_with_spaces() throws Exception {
        CamelRoutesHelper.loadRoutesFromClass(getClass().getClassLoader(), "\n\t" + CLASS_ROUTES_OK + "  \n", LOG);
    }

    @Test
    public void testLoadRoutesClass_ko_subclass() throws InvalidJBIConfigurationException {
        final Exception actualException = assertThrows(InvalidJBIConfigurationException.class, () -> {
            CamelRoutesHelper.loadRoutesFromClass(getClass().getClassLoader(), CLASS_ROUTES_KO_SUB, LOG);
        });
        assertTrue(actualException.getMessage().contains("is not a subclass of Camel RouteBuilder"));
    }

    @Test
    public void testLoadRoutesClass_ko_noclass() throws InvalidJBIConfigurationException {
        final Exception actualException = assertThrows(InvalidJBIConfigurationException.class, () -> {
            CamelRoutesHelper.loadRoutesFromClass(getClass().getClassLoader(), CLASS_ROUTES_KO_NO, LOG);
        });
        assertInstanceOf(ClassNotFoundException.class, actualException.getCause());
    }

    @Test
    public void testLoadRoutesClass_ko_noninstantiable() throws InvalidJBIConfigurationException {
        final Exception actualException = assertThrows(InvalidJBIConfigurationException.class, () -> {
            CamelRoutesHelper.loadRoutesFromClass(getClass().getClassLoader(), TestRoutesKO1.class.getName(), LOG);
        });
        assertTrue(actualException.getMessage().contains("Can't instantiate"));
    }

    @Test
    public void testLoadRoutesXML_ok() throws Exception {
        CamelRoutesHelper.loadRoutesFromXML(XML_ROUTES_OK, new DefaultCamelContext(),
                Logger.getLogger("TEST"));
    }

    @Test
    public void testLoadRoutesXML_ko_nofile() throws Exception {
        final Exception actualException = assertThrows(InvalidJBIConfigurationException.class, () -> {
            CamelRoutesHelper.loadRoutesFromXML("fakefakefake.xml", new DefaultCamelContext(),
                Logger.getLogger("TEST"));
        });
        assertTrue(actualException.getMessage().contains("Can't find xml routes definition"));
    }

    @Test
    public void testLoadRoutesClass_ko_nonloadable() throws Exception {
        final Exception actualException = assertThrows(InvalidCamelRouteDefinitionException.class, () -> {
            CamelRoutesHelper.loadRoutesFromXML(XML_ROUTES_KO, new DefaultCamelContext(),
                Logger.getLogger("TEST"));
        });
        assertTrue(actualException.getMessage().contains("Can't load routes from xml"));
    }

    @Test
    public void testLoadRoutesClass_ko_empty_className() throws InvalidJBIConfigurationException {
        final Exception actualException = assertThrows(InvalidJBIConfigurationException.class, () -> {
            CamelRoutesHelper.loadRoutesFromClass(getClass().getClassLoader(), "", LOG);
        });
        assertTrue(actualException.getMessage().contains("className must be not empty"));
    }
}
