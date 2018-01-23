/**
 * Copyright (c) 2016-2018 Linagora
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

import javax.xml.namespace.QName;

public interface JbiCamelConstants {

    public static final String CAMEL_JBI_NS_URI = "http://petals.ow2.org/components/petals-se-camel/jbi/version-1.0";

    public static final String CAMEL_WSDL_NS_URI = "http://petals.ow2.org/components/petals-se-camel/wsdl/version-1.0";

    public static final String EL_CONSUMES_SERVICE_ID = "service-id";

    public static final QName EL_SERVICES_ROUTE_CLASS = new QName(CAMEL_JBI_NS_URI, "java-routes");

    public static final QName EL_SERVICES_ROUTE_XML = new QName(CAMEL_JBI_NS_URI, "xml-routes");

    public static final QName EL_WSDL_OPERATION = new QName(CAMEL_WSDL_NS_URI, "operation");

    public static final String ATTR_WSDL_OPERATION_SERVICEID = "service-id";
}
