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
package org.ow2.petals.se.camel.junit.routes;

import org.apache.camel.Exchange;
import org.ow2.petals.camel.helpers.PetalsRouteBuilder;
import org.ow2.petals.camel.helpers.Step;

public class Nominal extends PetalsRouteBuilder {

    public static final String ONLYOFFICE_WRAPPER_CONVERT = "onlyoffice-wrapper-convert";

    public static final String ONLYOFFICE_UPLOAD = "onlyoffice-upload";

    public static final String ONLYOFFICE_CONVERT = "onlyoffice-convert";

    public static final String ONLYOFFICE_DELETE = "onlyoffice-delete";

    @Override
    public void configure() throws Exception {
        this.configureOpRouteConvert();
    }

    private void configureOpRouteConvert() {
        fromPetals(ONLYOFFICE_WRAPPER_CONVERT)
                .process(new Step("Prepare to upload document on Onlyoffice Document Storage area") {
                    @Override
                    public void process(final Exchange exchange) throws Exception {
                        // NOP
                    }
                }).to("petals:" + ONLYOFFICE_UPLOAD).process(new Step("Prepare to convert the uploaded document") {
                    @Override
                    public void process(final Exchange exchange) throws Exception {
                        // NOP
                    }
                }).doTry().to("petals:" + ONLYOFFICE_CONVERT)
                .process(new Step("Pre-preparation to download the converted document") {
                    @Override
                    public void process(final Exchange exchange) throws Exception {


                    }
                }).doFinally().process(new Step("Remove document to convert from Onlyoffice Document Storage area") {
                    @Override
                    public void process(final Exchange exchange) throws Exception {
                        // NOP
                    }
                }).to("petals:" + ONLYOFFICE_DELETE).end()
                .choice()
                    .when(simple("${exchangeProperty.FAULT_TO_RETURN} != null")).process(new Step("Return fault") {

                            @Override
                            public void process(Exchange exchange) throws Exception {
                                // NOP
                            }
                        })
                    .otherwise()
                        .process(new Step("Prepare to download the converted document") {
                            @Override
                            public void process(final Exchange exchange) throws Exception {
                                // NOP
                            }
                        });
    }
}
