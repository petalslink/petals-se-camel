/**
 * Copyright (c) 2017 Linagora
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

import java.util.Collection;

import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;

public abstract class PetalsCamelTestSupport extends CamelTestSupport {

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    protected abstract Collection<String> routesToMock();

    @Before
    public void mockEndpoints() throws Exception {

        context.setTracing(true);
        context.setStreamCaching(true);
        context.getProperties().put(Exchange.LOG_DEBUG_BODY_STREAMS, Boolean.TRUE.toString());

        context.removeComponent("petals");
        context.addComponent("petals", new MockComponent());
        for (final String from : routesToMock()) {
            context.getRouteDefinition(from).adviceWith(context, new AdviceWithRouteBuilder() {
                @Override
                public void configure() throws Exception {
                    replaceFromWith("direct:" + from);
                }
            });
        }
        context.start();
    }

    protected String getFrom(String service) {
        return "direct:" + service;
    }

    protected MockEndpoint getTo(String service) {
        return getMockEndpoint("petals:" + service);
    }
}
