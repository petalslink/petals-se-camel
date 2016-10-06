/**
 * Copyright (c) 2015-2016 Linagora
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.ow2.easywsdl.extensions.wsdl4complexwsdl.WSDL4ComplexWsdlFactory;
import org.ow2.easywsdl.extensions.wsdl4complexwsdl.api.WSDL4ComplexWsdlReader;
import org.ow2.easywsdl.schema.api.XmlException;
import org.ow2.easywsdl.wsdl.api.Binding;
import org.ow2.easywsdl.wsdl.api.BindingOperation;
import org.ow2.easywsdl.wsdl.api.Description;
import org.ow2.easywsdl.wsdl.api.Endpoint;
import org.ow2.easywsdl.wsdl.api.InterfaceType;
import org.ow2.easywsdl.wsdl.api.Service;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.se.PetalsCamelSender;
import org.ow2.petals.camel.se.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.camel.se.impl.ServiceEndpointOperationConsumes;
import org.ow2.petals.camel.se.impl.ServiceEndpointOperationProvides;
import org.ow2.petals.component.framework.api.configuration.ConfigurationExtensions;
import org.ow2.petals.component.framework.jbidescriptor.generated.Consumes;
import org.ow2.petals.component.framework.jbidescriptor.generated.Jbi;
import org.ow2.petals.component.framework.jbidescriptor.generated.Provides;
import org.ow2.petals.component.framework.jbidescriptor.generated.Services;
import org.ow2.petals.component.framework.su.ServiceUnitDataHandler;
import org.ow2.petals.component.framework.util.ServiceEndpointKey;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

/**
 * 
 * Helper class to manipulate the jbi.xml according to the schema in the resources directory.
 * 
 * TODO one day we should actually exploit in an automatised way this schema in the CDK directly.
 * 
 * @author vnoel
 *
 */
public class PetalsCamelJBIHelper implements JbiCamelConstants {

    private PetalsCamelJBIHelper() {
    }

    /**
     * returns a Map of service-id <-> service/endpoint/operation
     * 
     * @param suDH
     * @param jbiListener
     * @return
     * @throws InvalidJBIConfigurationException
     */
    public static Map<String, ServiceEndpointOperation> extractServicesIdAndEndpointOperations(
            final ServiceUnitDataHandler suDH, final PetalsCamelSender sender)
            throws InvalidJBIConfigurationException {

        final Jbi jbiDescriptor = suDH.getDescriptor();

        // let's use a bimap to accelerate checking of containsValue()
        final Map<String, ServiceEndpointOperation> sid2seo = HashBiMap.create();


        // for provides, there is one serviceId per operation of each provides
        for (final Provides p : jbiDescriptor.getServices().getProvides()) {

            final ServiceEndpointKey key = new ServiceEndpointKey(p.getServiceName(), p.getEndpointName());
            final Document wsdlDoc = suDH.getEpServiceDesc().get(key);

            final List<OperationData> seos;
            try {
                seos = getOperationsAndServiceId(wsdlDoc, p);
            } catch (final URISyntaxException e) {
                throw new InvalidJBIConfigurationException("Exception while parsing WSDL", e);
            } catch (final XmlException e) {
                throw new InvalidJBIConfigurationException("Exception while parsing WSDL", e);
            }

            for (final OperationData od : seos) {
                if (sid2seo.containsKey(od.serviceId)) {
                    throw new InvalidJBIConfigurationException("Duplicate " + ATTR_WSDL_OPERATION_SERVICEID + " ("
                            + od.serviceId
                            + ") in the operation " + od.operation);
                }
                final ServiceEndpointOperation seo = new ServiceEndpointOperationProvides(od.operation, od.mep, sender,
                        p);
                if (sid2seo.containsValue(seo)) {
                    throw new InvalidJBIConfigurationException("Duplicate service " + seo);
                }
                sid2seo.put(od.serviceId, seo);
            }
        }

        // for consumes, there is one serviceId per consumes (because it includes the operation)
        for (final Consumes c : jbiDescriptor.getServices().getConsumes()) {

            final ServiceEndpointOperation seo = new ServiceEndpointOperationConsumes(sender, c);

            final String serviceId = getServiceId(c, suDH);

            if (sid2seo.containsKey(serviceId)) {
                throw new InvalidJBIConfigurationException("Duplicate " + EL_CONSUMES_SERVICE_ID + " (" + serviceId
                        + ") in the consumes " + c.getServiceName());
            }

            sid2seo.put(serviceId, seo);
        }

        return sid2seo;
    }

    public static void populateRouteLists(final Services servicesNode, final List<String> classNames,
            final List<String> xmlNames) {
        for (final Element e : servicesNode.getAnyOrAny()) {
            if (hasQName(e, EL_SERVICES_ROUTE_CLASS)) {
                classNames.add(e.getTextContent());
            } else if (hasQName(e, EL_SERVICES_ROUTE_XML)) {
                xmlNames.add(e.getTextContent());
            }
        }
    }

    public static List<OperationData> getOperationsAndServiceId(final Document doc, final Provides provides)
            throws URISyntaxException, XmlException, InvalidJBIConfigurationException {

        final List<OperationData> results = Lists.newArrayList();

        final WSDL4ComplexWsdlReader reader = WSDL4ComplexWsdlFactory.newInstance().newWSDLReader();

        final Description desc = reader.read(doc);

        final Service service = desc.getService(provides.getServiceName());
        if (service == null) {
            throw new InvalidJBIConfigurationException(
                    "Can't find the service '" + provides.getServiceName() + "' in the description");
        }

        final Endpoint endpoint = service.getEndpoint(provides.getEndpointName());
        if (endpoint == null) {
            throw new InvalidJBIConfigurationException(
                    "Can't find the endpoint '" + provides.getEndpointName() + "' in the description");
        }

        final Binding binding = endpoint.getBinding();

        if (binding == null) {
            throw new InvalidJBIConfigurationException(
                    "No binding defined for the endpoint '" + provides.getEndpointName() + "' in the description");
        }

        final InterfaceType interfaceType = binding.getInterface();
        if (interfaceType == null) {
            throw new InvalidJBIConfigurationException("No interface defined for the binding of ednpoint '"
                    + provides.getEndpointName() + "' in the description");
        }

        if (!provides.getInterfaceName().equals(interfaceType.getQName())) {
            throw new InvalidJBIConfigurationException("The interface of the endpoint '" + provides.getEndpointName()
                    + "' is invalid: '" + interfaceType.getQName() + "' instead of '" + interfaceType + "'");
        }

        for (final BindingOperation operation : binding.getBindingOperations()) {
            final QName qName = operation.getQName();
            final MEPPatternConstants mep = interfaceType.getOperation(qName).getPattern();

            Element camelOperation = null;
            for (final Element e : operation.getOtherElements()) {
                if (hasQName(e, EL_WSDL_OPERATION)) {
                    if (camelOperation != null) {
                        throw new InvalidJBIConfigurationException(
                                "Duplicate " + EL_WSDL_OPERATION + " available for the operation " + qName);
                    }
                    camelOperation = e;
                }
            }
            if (camelOperation == null) {
                throw new InvalidJBIConfigurationException(
                        "No " + EL_WSDL_OPERATION + " available for the operation " + qName);
            }

            final String serviceId = camelOperation.getAttribute(ATTR_WSDL_OPERATION_SERVICEID);

            if (StringUtils.isEmpty(serviceId)) {
                throw new InvalidJBIConfigurationException(
                        "No " + ATTR_WSDL_OPERATION_SERVICEID + " attribute for the operation " + qName);
            }
            results.add(new OperationData(qName, mep.value(), serviceId));
        }
        return results;
    }

    public static boolean hasQName(final Node e, final QName name) {
        return new QName(e.getNamespaceURI(), e.getLocalName()).equals(name);
    }

    public static String getServiceId(final Consumes s, final ServiceUnitDataHandler suDH)
            throws InvalidJBIConfigurationException {

        final ConfigurationExtensions extensions = suDH.getConfigurationExtensions(s);
        final String serviceId = extensions.get(EL_CONSUMES_SERVICE_ID);

        if (serviceId == null || serviceId.isEmpty()) {
            throw new InvalidJBIConfigurationException("No " + EL_CONSUMES_SERVICE_ID + " defined for the consumes "
                    + s.getServiceName());
        }

        return serviceId;
    }

    @SuppressWarnings("all")
    public static class OperationData {

        public final QName operation;

        public final URI mep;

        public final String serviceId;

        public OperationData(final QName operation, final URI mep, final String serviceId) {
            this.operation = operation;
            this.mep = mep;
            this.serviceId = serviceId;
        }
    }
}
