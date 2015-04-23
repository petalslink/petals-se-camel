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
package org.ow2.petals.camel.se;

import org.junit.Before;
import org.junit.Test;
import org.ow2.petals.camel.se.mocks.TestRoutesOK;

public class CamelSUTest extends AbstractComponentTest {

    @Before
    public void setUpSUs() throws Exception {
        deployHello(SU_NAME, WSDL11, TestRoutesOK.class);
    }

    @Test
    public void testMessageGoThrough() throws Exception {
        final String requestContent = "<sayHello xmlns=\"http://petals.ow2.org\"><arg0>John</arg0></sayHello>";
        final String responseContent = "<sayHelloResponse xmlns=\"http://petals.ow2.org\"><return>Hello John</return></sayHelloResponse>";

        // TestRoutesOK is an identity transformation expected contents are similar to contents
        sendHello(SU_NAME, requestContent, requestContent, responseContent, responseContent, true, true);
        
    }
}
