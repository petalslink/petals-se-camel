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

import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Assert;
import org.junit.Test;
import org.ow2.petals.camel.se.exceptions.InvalidCamelRouteDefinitionException;
import org.ow2.petals.camel.se.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.camel.se.mocks.TestRoutesKO1;
import org.ow2.petals.camel.se.mocks.TestRoutesOK;
import org.ow2.petals.camel.se.utils.CamelRoutesHelper;

public class CamelRoutesHelperTest extends Assert {

    public static final String CLASS_ROUTES_OK = TestRoutesOK.class.getName();

    public static final String CLASS_ROUTES_KO_SUB = CamelRoutesHelperTest.class.getName();

    public static final String CLASS_ROUTES_KO_NO = "fakefakefake.fake";

    @Test
    public void testLoadRoutesClass_ok() throws Exception {
        CamelRoutesHelper.loadRoutesFromClass(getClass().getClassLoader(), CLASS_ROUTES_OK);
    }

    @Test
    public void testLoadRoutesClass_ko_subclass() {
        try {
            CamelRoutesHelper.loadRoutesFromClass(getClass().getClassLoader(), CLASS_ROUTES_KO_SUB);
            fail();
        } catch (InvalidJBIConfigurationException e) {
            assertTrue(e.getMessage().contains("is not a subclass of camel RouteBuilder"));
        }
    }

    @Test
    public void testLoadRoutesClass_ko_noclass() {
        try {
            CamelRoutesHelper.loadRoutesFromClass(getClass().getClassLoader(), CLASS_ROUTES_KO_NO);
            fail();
        } catch (InvalidJBIConfigurationException e) {
            assertTrue(e.getCause() instanceof ClassNotFoundException);
        }
    }

    @Test
    public void testLoadRoutesClass_ko_noninstantiable() {
        try {
            CamelRoutesHelper.loadRoutesFromClass(getClass().getClassLoader(), TestRoutesKO1.class.getName());
            fail();
        } catch (InvalidJBIConfigurationException e) {
            assertTrue(e.getMessage().contains("Can't instantiate"));
        }
    }

    public static final String XML_ROUTES_OK = "sus/valid-xml-wsdl-1.1/routes.xml";

    public static final String XML_ROUTES_KO = "sus/invalid-xml/routes.xml";

    @Test
    public void testLoadRoutesXML_ok() throws Exception {
        CamelRoutesHelper.loadRoutesFromXML(XML_ROUTES_OK, new DefaultCamelContext(), getClass().getClassLoader());
    }

    @Test
    public void testLoadRoutesXML_ko_nofile() throws Exception {
        try {
            CamelRoutesHelper.loadRoutesFromXML("fakefakefake.xml", new DefaultCamelContext(), getClass()
                    .getClassLoader());
            fail();
        } catch (InvalidJBIConfigurationException e) {
            assertTrue(e.getMessage().contains("Can't find xml routes definition"));
        }
    }

    @Test
    public void testLoadRoutesClass_ko_nonloadable() throws Exception {
        try {
            CamelRoutesHelper.loadRoutesFromXML(XML_ROUTES_KO, new DefaultCamelContext(), getClass().getClassLoader());
            fail();
        } catch (InvalidCamelRouteDefinitionException e) {
            assertTrue(e.getMessage().contains("Can't load routes from xml"));
        }
    }

}
