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
package org.ow2.petals.se.camel.utils;

import java.util.logging.Logger;

import org.apache.camel.impl.DefaultCamelContext;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.ow2.petals.se.camel.exceptions.InvalidCamelRouteDefinitionException;
import org.ow2.petals.se.camel.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.se.camel.mocks.TestRoutesKO1;
import org.ow2.petals.se.camel.mocks.TestRoutesOK;

public class CamelRoutesHelperTest extends Assert {

    private static final Logger LOG = Logger.getLogger(CamelRoutesHelperTest.class.getName());

    private static final String XML_ROUTES_OK = "tests/routes-valid-1.1.xml";

    private static final String XML_ROUTES_KO = "tests/routes-invalid.xml";

    private static final String CLASS_ROUTES_OK = TestRoutesOK.class.getName();

    private static final String CLASS_ROUTES_KO_SUB = CamelRoutesHelperTest.class.getName();

    private static final String CLASS_ROUTES_KO_NO = "fakefakefake.fake";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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
        thrown.expect(InvalidJBIConfigurationException.class);
        thrown.expectMessage("is not a subclass of Camel RouteBuilder");
        CamelRoutesHelper.loadRoutesFromClass(getClass().getClassLoader(), CLASS_ROUTES_KO_SUB, LOG);
    }

    @Test
    public void testLoadRoutesClass_ko_noclass() throws InvalidJBIConfigurationException {
        thrown.expect(InvalidJBIConfigurationException.class);
        thrown.expectCause(CoreMatchers.isA(ClassNotFoundException.class));
        CamelRoutesHelper.loadRoutesFromClass(getClass().getClassLoader(), CLASS_ROUTES_KO_NO, LOG);
    }

    @Test
    public void testLoadRoutesClass_ko_noninstantiable() throws InvalidJBIConfigurationException {
        thrown.expect(InvalidJBIConfigurationException.class);
        thrown.expectMessage("Can't instantiate");
        CamelRoutesHelper.loadRoutesFromClass(getClass().getClassLoader(), TestRoutesKO1.class.getName(), LOG);
    }

    @Test
    public void testLoadRoutesXML_ok() throws Exception {
        CamelRoutesHelper.loadRoutesFromXML(XML_ROUTES_OK, new DefaultCamelContext(), getClass().getClassLoader(),
                Logger.getLogger("TEST"));
    }

    @Test
    public void testLoadRoutesXML_ko_nofile() throws Exception {
        thrown.expect(InvalidJBIConfigurationException.class);
        thrown.expectMessage("Can't find xml routes definition");
        CamelRoutesHelper.loadRoutesFromXML("fakefakefake.xml", new DefaultCamelContext(), getClass().getClassLoader(),
                Logger.getLogger("TEST"));
    }

    @Test
    public void testLoadRoutesClass_ko_nonloadable() throws Exception {
        thrown.expect(InvalidCamelRouteDefinitionException.class);
        thrown.expectMessage("Can't load routes from xml");
        CamelRoutesHelper.loadRoutesFromXML(XML_ROUTES_KO, new DefaultCamelContext(), getClass().getClassLoader(),
                Logger.getLogger("TEST"));
    }

    @Test
    public void testLoadRoutesClass_ko_empty_className() throws InvalidJBIConfigurationException {
        thrown.expect(InvalidJBIConfigurationException.class);
        thrown.expectMessage("className must be not empty");
        CamelRoutesHelper.loadRoutesFromClass(getClass().getClassLoader(), "", LOG);
    }
}
