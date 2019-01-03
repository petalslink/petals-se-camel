/**
 * Copyright (c) 2017-2019 Linagora
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
package org.ow2.petals.camel.helpers;

import org.apache.camel.Processor;
import org.apache.camel.Traceable;
import org.apache.camel.model.RouteDefinition;

/**
 * To be used in place of {@link Processor} with {@link RouteDefinition#process(Processor)}.
 * 
 * @author Victor Noel
 *
 */
public abstract class Step implements Processor, Traceable {

    private final String label;

    public Step(String label) {
        this.label = label;
    }

    @Override
    public String getTraceLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "Step[" + label + "]";
    }
}
