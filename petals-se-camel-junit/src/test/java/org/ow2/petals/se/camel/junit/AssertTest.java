/**
 * Copyright (c) 2021-2025 Linagora
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
package org.ow2.petals.se.camel.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.ow2.petals.se.camel.junit.Assert.assertWsdlCompliance;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.ow2.petals.se.camel.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.se.camel.exceptions.InvalidWSDLException;

public class AssertTest {

    /**
     * Check the compliance of valid WSDL/JBI, all is OK.
     */
    @Test
    public void assertWsdlCompliance_nominal() throws Exception {

        final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new ClassLoader(oldClassloader) {

            @Override
            public URL getResource(final String name) {
                if (name.startsWith("jbi/")) {
                    return super.getResource(name.replaceAll("jbi/", "wsdl-jbi-compliance/nominal/jbi/"));
                } else {
                    return super.getResource(name);
                }
            }
        });

        try {
            assertWsdlCompliance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }

    /**
     * Check the compliance of valid WSDL/JBI, service not existing in WSDL.
     */
    @Test
    public void assertWsdlCompliance_servicenameNotAligned() throws Exception {

        final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new ClassLoader(oldClassloader) {

            @Override
            public URL getResource(final String name) {
                if (name.startsWith("jbi/")) {
                    return super.getResource(
                            name.replaceAll("jbi/", "wsdl-jbi-compliance/servicename-not-aligned/jbi/"));
                } else {
                    return super.getResource(name);
                }
            }
        });

        try {
            final Error actualException = assertThrows(AssertionError.class, () -> {
                assertWsdlCompliance();
            }, "Service names aligned");
            assertTrue(actualException.getMessage().contains(
                    "Service '{http://petals.ow2.org/onlyoffice-5.3/wrapper/1.0}DocumentConversionWrapperServiceNotAligned' not found in WSDL 'onlyoffice.wsdl'"),
                    "Unexpected assertion");
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }

    /**
     * Check the compliance of valid WSDL/JBI, interface not existing in WSDL.
     */
    @Test
    public void assertWsdlCompliance_interfacenameNotAligned() throws Exception {

        final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new ClassLoader(oldClassloader) {

            @Override
            public URL getResource(final String name) {
                if (name.startsWith("jbi/")) {
                    return super.getResource(
                            name.replaceAll("jbi/", "wsdl-jbi-compliance/interfacename-not-aligned/jbi/"));
                } else {
                    return super.getResource(name);
                }
            }
        });

        try {
            final Error actualException = assertThrows(AssertionError.class, () -> {
                assertWsdlCompliance();
            }, "Interface names aligned");
            assertTrue(actualException.getMessage().contains(
                    "Interface '{http://petals.ow2.org/onlyoffice-5.3/wrapper/1.0}DocumentConversionWrapperNotAligned' not found in WSDL 'onlyoffice.wsdl'"),
                    "Unexpected assertion");
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }

    /**
     * Check the compliance of valid WSDL/JBI, a route identifier is missing for a service provider.
     */
    @Test
    public void assertWsdlCompliance_routeIdMissingProvider() throws Exception {

        final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new ClassLoader(oldClassloader) {

            @Override
            public URL getResource(final String name) {
                if (name.startsWith("jbi/")) {
                    return super.getResource(
                            name.replaceAll("jbi/", "wsdl-jbi-compliance/route-id-missing-into-provider/jbi/"));
                } else {
                    return super.getResource(name);
                }
            }
        });

        try {
            final Exception actualException = assertThrows(InvalidWSDLException.class, () -> {
                assertWsdlCompliance();
            }, "Route definition found into the service provider");
            assertEquals(
                    "Invalid WSDL: No element {http://petals.ow2.org/components/petals-se-camel/wsdl/version-1.0}operation available for the operation {http://petals.ow2.org/onlyoffice-5.3/wrapper/1.0}convert in WSDL",
                    actualException.getMessage(), "Unexpected assertion");
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }

    /**
     * Check the compliance of valid WSDL/JBI, a route identifier is missing for a service consumer.
     */
    @Test
    public void assertWsdlCompliance_routeIdMissingConsumer() throws Exception {

        final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new ClassLoader(oldClassloader) {

            @Override
            public URL getResource(final String name) {
                if (name.startsWith("jbi/")) {
                    return super.getResource(
                            name.replaceAll("jbi/", "wsdl-jbi-compliance/route-id-missing-into-consumer/jbi/"));
                } else {
                    return super.getResource(name);
                }
            }
        });

        try {
            final Exception actualException = assertThrows(InvalidJBIConfigurationException.class, () -> {
                assertWsdlCompliance();
            }, "Route definition found into the service consumer");
            assertEquals(
                    "Invalid JBI descriptor: No service-id defined for the consumes {http://petals.ow2.org/onlyoffice-5.3/1.0}DocumentConversionService",
                    actualException.getMessage(), "Unexpected assertion");
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }

    /**
     * Check the compliance of valid WSDL/JBI, a Java route definition is missing.
     */
    @Test
    public void assertWsdlCompliance_javaRouteDefinitionMissing() throws Exception {

        final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new ClassLoader(oldClassloader) {

            @Override
            public URL getResource(final String name) {
                if (name.startsWith("jbi/")) {
                    return super.getResource(
                            name.replaceAll("jbi/", "wsdl-jbi-compliance/java-route-definition-missing/jbi/"));
                } else {
                    return super.getResource(name);
                }
            }
        });

        try {
            final Exception actualException = assertThrows(InvalidJBIConfigurationException.class, () -> {
                assertWsdlCompliance();
            }, "Java route definition found");
            assertEquals("Invalid JBI descriptor: Can't load class org.ow2.petals.se.camel.junit.routes.RouteMissing",
                    actualException.getMessage(), "Unexpected assertion");
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }

    /**
     * Check the compliance of valid WSDL/JBI, a XML route definition is missing.
     */
    @Test
    public void assertWsdlCompliance_xmlRouteDefinitionMissing() throws Exception {

        final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new ClassLoader(oldClassloader) {

            @Override
            public URL getResource(final String name) {
                if (name.startsWith("jbi/")) {
                    return super.getResource(
                            name.replaceAll("jbi/", "wsdl-jbi-compliance/xml-route-definition-missing/jbi/"));
                } else {
                    return super.getResource(name);
                }
            }
        });

        try {
            final Exception actualException = assertThrows(InvalidJBIConfigurationException.class, () -> {
                assertWsdlCompliance();
            }, "XML route definition found");
            assertEquals("Invalid JBI descriptor: Can't find xml routes definition xml-routes-missing.xml",
                    actualException.getMessage(), "Unexpected assertion");
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }

    /**
     * Check the compliance of valid WSDL/JBI, a XML route definition (as service provider implementation) is missing.
     */
    @Test
    public void assertWsdlCompliance_xmlNoRouteExistsForRouteId() throws Exception {

        final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new ClassLoader(oldClassloader) {

            @Override
            public URL getResource(final String name) {
                if (name.startsWith("jbi/")) {
                    return super.getResource(
                            name.replaceAll("jbi/", "wsdl-jbi-compliance/xml-no-route-exists-for-route-id/jbi/"));
                } else {
                    return super.getResource(name);
                }
            }
        });

        try {
            final Error actualException = assertThrows(AssertionError.class, () -> {
                assertWsdlCompliance();
            }, "Invalid route identifier not detected");
            assertTrue(actualException.getMessage().contains(
                    "Route 'theProvidesId' defined in WSDL and implementing a service provider has no definition as Camel route."),
                    "Unexpected assertion");
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }

    /**
     * Check the compliance of valid WSDL/JBI, a Java route definition (as service provider implementation) is missing.
     */
    @Test
    public void assertWsdlCompliance_javaNoRouteExistsForRouteId() throws Exception {

        final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new ClassLoader(oldClassloader) {

            @Override
            public URL getResource(final String name) {
                if (name.startsWith("jbi/")) {
                    return super.getResource(
                            name.replaceAll("jbi/", "wsdl-jbi-compliance/java-no-route-exists-for-route-id/jbi/"));
                } else {
                    return super.getResource(name);
                }
            }
        });

        try {
            final Error actualException = assertThrows(AssertionError.class, () -> {
                assertWsdlCompliance();
            }, "Invalid route identifier not detected");
            assertTrue(actualException.getMessage().contains(
                    "Route 'another-onlyoffice-wrapper-convert' defined in WSDL and implementing a service provider has no definition as Camel route."),
                    "Unexpected assertion");
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }

    /**
     * Check the compliance of valid WSDL/JBI, a Camel consumer endpoint (as service consumer), declared in a XML route
     * definition, is missing.
     */
    @Test
    public void assertWsdlCompliance_xmlCamelConsumerEndpointMissing() throws Exception {

        final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new ClassLoader(oldClassloader) {

            @Override
            public URL getResource(final String name) {
                if (name.startsWith("jbi/")) {
                    return super.getResource(
                            name.replaceAll("jbi/", "wsdl-jbi-compliance/xml-camel-consumer-edp-missing/jbi/"));
                } else {
                    return super.getResource(name);
                }
            }
        });

        try {
            final Error actualException = assertThrows(AssertionError.class, () -> {
                assertWsdlCompliance();
            }, "Camel consumer edp is not missing");
            assertTrue(actualException.getMessage().contains(
                    "Consumer endpoint URI 'petals://theConsumesId' declared in route 'theProvidesId' but not declared in JBI descriptor as service consumer"),
                    "Unexpected assertion");
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }

    /**
     * Check the compliance of valid WSDL/JBI, a Camel consumer endpoint (as service consumer), declared in a Java route
     * definition, is missing.
     */
    @Test
    public void assertWsdlCompliance_javaCamelConsumerEndpointMissing() throws Exception {

        final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new ClassLoader(oldClassloader) {

            @Override
            public URL getResource(final String name) {
                if (name.startsWith("jbi/")) {
                    return super.getResource(
                            name.replaceAll("jbi/", "wsdl-jbi-compliance/java-camel-consumer-edp-missing/jbi/"));
                } else {
                    return super.getResource(name);
                }
            }
        });

        try {
            final Error actualException = assertThrows(AssertionError.class, () -> {
                assertWsdlCompliance();
            }, "Camel consumer edp is not missing");
            assertTrue(actualException.getMessage().contains(
                    "Consumer endpoint URI 'petals://onlyoffice-convert' declared in route 'onlyoffice-wrapper-convert' but not declared in JBI descriptor as service consumer"),
                    "Unexpected assertion");
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }

    /**
     * Check the compliance of valid WSDL/JBI, no Camel route defined in the service unit.
     */
    @Test
    public void assertWsdlCompliance_noRouteDefined() throws Exception {

        final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new ClassLoader(oldClassloader) {

            @Override
            public URL getResource(final String name) {
                if (name.startsWith("jbi/")) {
                    return super.getResource(name.replaceAll("jbi/", "wsdl-jbi-compliance/no-route-defined/jbi/"));
                } else {
                    return super.getResource(name);
                }
            }
        });

        try {
            final Error actualException = assertThrows(AssertionError.class, () -> {
                assertWsdlCompliance();
            }, "Camel consumer edp is not missing");
            assertTrue(
                    actualException.getMessage()
                            .contains("No Camel route definition loaded. Check your service unit configuration."),
                    "Unexpected assertion");
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }
}
