/**
 * Copyright (c) 2015-2017 Linagora
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
package org.ow2.petals.camel.se;

import org.apache.camel.builder.RouteBuilder;
import org.junit.Before;
import org.junit.Test;

public class CamelSUTest extends AbstractComponentTest {

    private static boolean deployCalled;
    private static boolean initCalled;
    private static boolean startCalled;
    private static boolean stopCalled;
    private static boolean shutdownCalled;
    private static boolean undeployCalled;

    @Before
    public void before() {
        deployCalled = false;
        initCalled = false;
        startCalled = false;
        stopCalled = false;
        shutdownCalled = false;
        undeployCalled = false;
    }

    public static class RouteWithAllHooks extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("petals:theProvidesId").to("petals:theConsumesId");
        }

        public void deploy() {
            deployCalled = true;
        }

        public void init() {
            initCalled = true;
        }

        public void start() {
            startCalled = true;
        }

        public void stop() {
            stopCalled = true;
        }

        public void shutdown() {
            shutdownCalled = true;
        }

        public void undeploy() {
            undeployCalled = true;
        }
    }

    public static class RouteWithSomeHooks extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("petals:theProvidesId").to("petals:theConsumesId");
        }

        public void init() {
            initCalled = true;
        }

        public void start() {
            startCalled = true;
        }

        public void undeploy() {
            undeployCalled = true;
        }
    }

    @Test
    public void testAllHooksCalled() throws Exception {
        deployHello(SU_NAME, WSDL11, RouteWithAllHooks.class);

        assertTrue(deployCalled);
        assertTrue(initCalled);
        assertTrue(startCalled);

        COMPONENT_UNDER_TEST.undeployService(SU_NAME);

        assertTrue(stopCalled);
        assertTrue(shutdownCalled);
        assertTrue(undeployCalled);
    }

    @Test
    public void testSomeHooksCalled() throws Exception {
        deployHello(SU_NAME, WSDL11, RouteWithSomeHooks.class);

        assertFalse(deployCalled);
        assertTrue(initCalled);
        assertTrue(startCalled);

        COMPONENT_UNDER_TEST.undeployService(SU_NAME);

        assertFalse(stopCalled);
        assertFalse(shutdownCalled);
        assertTrue(undeployCalled);
    }
}
