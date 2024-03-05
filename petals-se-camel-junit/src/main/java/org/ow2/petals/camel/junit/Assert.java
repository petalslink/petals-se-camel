/**
 * Copyright (c) 2021-2024 Linagora
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
package org.ow2.petals.camel.junit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RouteDefinitionHelper;
import org.apache.camel.model.RoutesDefinition;
import org.ow2.easywsdl.extensions.wsdl4complexwsdl.WSDL4ComplexWsdlFactory;
import org.ow2.easywsdl.extensions.wsdl4complexwsdl.api.WSDL4ComplexWsdlReader;
import org.ow2.easywsdl.schema.api.XmlException;
import org.ow2.easywsdl.wsdl.api.Description;
import org.ow2.easywsdl.wsdl.api.InterfaceType;
import org.ow2.easywsdl.wsdl.api.Service;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.se.exceptions.InvalidCamelRouteDefinitionException;
import org.ow2.petals.camel.se.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.camel.se.exceptions.InvalidWSDLException;
import org.ow2.petals.camel.se.exceptions.PetalsCamelSEException;
import org.ow2.petals.camel.se.impl.ServiceEndpointOperationConsumes;
import org.ow2.petals.camel.se.impl.ServiceEndpointOperationProvides;
import org.ow2.petals.camel.se.utils.CamelRoutesHelper;
import org.ow2.petals.camel.se.utils.JbiCamelConstants;
import org.ow2.petals.camel.se.utils.PetalsCamelJBIHelper;
import org.ow2.petals.camel.se.utils.PetalsCamelJBIHelper.OperationData;
import org.ow2.petals.component.framework.jbidescriptor.CDKJBIDescriptorBuilder;
import org.ow2.petals.component.framework.jbidescriptor.generated.Consumes;
import org.ow2.petals.component.framework.jbidescriptor.generated.Jbi;
import org.ow2.petals.component.framework.jbidescriptor.generated.Provides;
import org.ow2.petals.component.framework.jbidescriptor.generated.Services;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.ebmwebsourcing.easycommons.xml.DocumentBuilders;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

/**
 * Assertions about SE Camel service units
 * 
 * @author Christophe DENEUX - Linagora
 *
 */
public class Assert {

    private static final Logger LOG = Logger.getLogger(Assert.class.getName());

    private Assert() {
        // Utility class --> no constructor
    }

    /**
     * <p>
     * Assertion checking the WSDL compliance of a service unit.
     * </p>
     * <p>
     * This assertion must be used to check the WSDL against JBI descriptor declarations. So this assertion is expected
     * to be used as unit test assertion of a service unit project. The JBI descriptor of the SU is expected to be the
     * resource 'jbi/jbi.xml'.
     * </p>
     */
    public static void assertWsdlCompliance() throws Exception {
        assertWsdlCompliance(new Properties());
    }

    public static void assertWsdlCompliance(final Properties componentPlaceholders) throws Exception {

        final URL jbiDescriptorUrl = Thread.currentThread().getContextClassLoader().getResource("jbi/jbi.xml");
        assertNotNull("SU JBI descriptor not found", jbiDescriptorUrl);

        final File jbiDescriptorFile = new File(jbiDescriptorUrl.toURI());
        try (final FileInputStream isJbiDescr = new FileInputStream(jbiDescriptorFile)) {
            final Jbi jbiDescriptor = CDKJBIDescriptorBuilder.getInstance().buildJavaJBIDescriptor(isJbiDescr);
            assertNotNull("Invalid JBI descriptor", jbiDescriptor);
            assertNotNull("Invalid JBI descriptor", jbiDescriptor.getServices());
            assertNotNull("Invalid JBI descriptor", jbiDescriptor.getServices().getProvides());
            assertFalse("Invalid JBI descriptor", jbiDescriptor.getServices().getProvides().isEmpty());

            final String installRoot = new File(jbiDescriptorUrl.toURI()).getParentFile().getAbsolutePath();

            // Check that all interfaces/services defined in JBI descriptor exist in the WSDL
            for (final Provides provides : jbiDescriptor.getServices().getProvides()) {

                final File wsdlFile = new File(installRoot, provides.getWsdl());

                final WSDL4ComplexWsdlReader wsdlReader = WSDL4ComplexWsdlFactory.newInstance().newWSDLReader();
                final Description wsdlDescr = wsdlReader.read(wsdlFile.toURI().toURL());
                assertNotNull(wsdlDescr);
                final InterfaceType itf = wsdlDescr.getInterface(provides.getInterfaceName());
                assertNotNull(String.format("Interface '%s' not found in WSDL '%s'",
                        provides.getInterfaceName().toString(), provides.getWsdl()), itf);
                final Service svc = wsdlDescr.getService(provides.getServiceName());
                assertNotNull(String.format("Service '%s' not found in WSDL '%s'", provides.getServiceName().toString(),
                        provides.getWsdl()), svc);
            }

            // check declaration of providers
            final Map<String, ServiceEndpointOperation> sid2seo = HashBiMap.create();
            checkProvides(jbiDescriptor, installRoot, sid2seo);

            // check declaration of consumers
            checkConsumes(jbiDescriptor, sid2seo);

            // check that routes are included
            checkCamelRoutes(jbiDescriptor, sid2seo);
        }

    }

    /**
     * Check declaration of service providers: for provides, there is one serviceId per operation of each provides
     */
    private static void checkProvides(final Jbi jbiDescriptor, final String installRoot,
            final Map<String, ServiceEndpointOperation> sid2seo)
            throws SAXException, IOException, InvalidJBIConfigurationException, InvalidWSDLException {

        final DocumentBuilder docBuilder = DocumentBuilders.takeDocumentBuilder();
        try {
            for (final Provides provides : jbiDescriptor.getServices().getProvides()) {

                final File wsdlFile = new File(installRoot, provides.getWsdl());

                final Document wsdlDoc = docBuilder.parse(wsdlFile);

                final List<OperationData> seos;
                try {
                    seos = PetalsCamelJBIHelper.getOperationsAndServiceId(wsdlDoc, provides);
                } catch (final URISyntaxException | XmlException e) {
                    throw new InvalidJBIConfigurationException("Exception while parsing WSDL", e);
                }

                for (final OperationData od : seos) {
                    if (sid2seo.containsKey(od.serviceId)) {
                        throw new InvalidJBIConfigurationException(
                                "The operation '" + od.operation + "' uised a Camel route id '" + od.serviceId
                                        + "' already declared for another operation.");
                    }
                    final ServiceEndpointOperation seo = new ServiceEndpointOperationProvides(od.operation, od.mep,
                            null, provides);
                    if (sid2seo.containsValue(seo)) {
                        throw new InvalidJBIConfigurationException("Duplicate service " + seo);
                    }
                    sid2seo.put(od.serviceId, seo);
                }
            }
        } finally {
            DocumentBuilders.releaseDocumentBuilder(docBuilder);
        }
    }

    /**
     * Check declaration of service consumers: for consumes, there is one serviceId per consumes (because it includes
     * the operation)
     */
    private static void checkConsumes(final Jbi jbiDescriptor, final Map<String, ServiceEndpointOperation> sid2seo)
            throws InvalidJBIConfigurationException {
        for (final Consumes consumes : jbiDescriptor.getServices().getConsumes()) {
            final List<Element> extraParams = consumes.getAny();
            String serviceId = null;
            for (final Element extraParam : extraParams) {
                if (JbiCamelConstants.EL_CONSUMES_SERVICE_ID.equals(extraParam.getLocalName())) {
                    serviceId = extraParam.getTextContent();
                }
            }

            if (serviceId == null || serviceId.isEmpty()) {
                throw new InvalidJBIConfigurationException("No " + JbiCamelConstants.EL_CONSUMES_SERVICE_ID
                        + " defined for the consumes " + consumes.getServiceName());
            }

            if (sid2seo.containsKey(serviceId)) {
                throw new InvalidJBIConfigurationException("Duplicate " + JbiCamelConstants.EL_CONSUMES_SERVICE_ID
                        + " (" + serviceId + ") in the consumes " + consumes.getServiceName());
            }

            sid2seo.put(serviceId, new ServiceEndpointOperationConsumes(null, consumes));
        }

    }

    /**
     * <p>
     * Check Camel routes:
     * </p>
     * <ul>
     * <li>each route can be loaded,</li>
     * <li>each service provider has a route identifier defined as 'from' in a route</li>
     * </ul>
     */
    private static void checkCamelRoutes(final Jbi jbiDescriptor, final Map<String, ServiceEndpointOperation> sid2seo)
            throws PetalsCamelSEException {
        final List<String> classNames = Lists.newArrayList();
        final List<String> xmlNames = Lists.newArrayList();

        final Services services = jbiDescriptor.getServices();
        assert services != null;
        PetalsCamelJBIHelper.populateRouteLists(services, classNames, xmlNames);

        final ModelCamelContext camelCtx = new DefaultCamelContext();
        for (final String className : classNames) {
            assert className != null;
            final RouteBuilder routes = CamelRoutesHelper
                    .loadRoutesFromClass(Thread.currentThread().getContextClassLoader(), className, LOG);
            try {
                camelCtx.addRoutes(routes);
            } catch (final Exception e) {
                throw new InvalidCamelRouteDefinitionException(
                        "Can't add routes from class " + className + " to Camel context", e);
            }
        }

        for (final String xmlName : xmlNames) {
            assert xmlName != null;
            final RoutesDefinition routes = CamelRoutesHelper.loadRoutesFromXML(xmlName, camelCtx,
                    Thread.currentThread().getContextClassLoader(), LOG);

            try {
                camelCtx.addRouteDefinitions(routes.getRoutes());
            } catch (final Exception e) {
                throw new InvalidCamelRouteDefinitionException(
                        "Can't add routes from xml file " + xmlName + " to Camel context", e);
            }
        }

        assertFalse("No Camel route definition loaded. Check your service unit configuration.",
                camelCtx.getRouteDefinitions().isEmpty());

        for (final Entry<String, ServiceEndpointOperation> entry : sid2seo.entrySet()) {
            if (entry.getValue() instanceof ServiceEndpointOperationProvides) {
                final String routeId = entry.getKey();
                final RouteDefinition routeDefinition = camelCtx.getRouteDefinition(routeId);
                assertNotNull("Route '" + routeId
                        + "' defined in WSDL and implementing a service provider has no definition as Camel route.",
                        routeDefinition);

                final Set<String> consumerEdpUris = RouteDefinitionHelper.gatherAllEndpointUris(camelCtx,
                        routeDefinition, false, true, false);
                for (final String consumerEdpUri : consumerEdpUris) {
                    if (consumerEdpUri.startsWith("petals://")) {
                        final String consumerEdp = consumerEdpUri.replaceFirst("petals://", "");
                        final ServiceEndpointOperation seoConsumer = sid2seo.get(consumerEdp);
                        assertTrue(
                                "Consumer endpoint URI '" + consumerEdpUri + "' declared in route '" + routeId
                                        + "' but not declared in JBI descriptor as service consumer",
                                seoConsumer instanceof ServiceEndpointOperationConsumes);
                    }
                }
            }
        }
    }
}
