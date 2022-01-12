/**
 * Copyright (c) 2015-2022 Linagora
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
package org.ow2.petals.samples.camel;

import org.ow2.petals.camel.helpers.PetalsRouteBuilder;

public class SimpleRoute extends PetalsRouteBuilder {

    public static final String THE_CONSUMES_ID = "theConsumesId";

    public static final String THE_PROVIDES_ID = "theProvidesId";

    @Override
    public void configure() throws Exception {
        fromPetals(THE_PROVIDES_ID).to("petals:" + THE_CONSUMES_ID);
    }

}
