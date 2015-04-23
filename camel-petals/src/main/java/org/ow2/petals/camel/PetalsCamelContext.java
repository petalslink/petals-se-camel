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
package org.ow2.petals.camel;

import java.util.logging.Logger;

import org.apache.camel.CamelContext;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.PetalsChannel.PetalsProvidesChannel;
import org.ow2.petals.camel.exceptions.UnknownServiceException;

/**
 * 
 * Represents an object that provides methods for interacting between Petals abstractions (
 * {@link ServiceEndpointOperation}), Petals-Camel abstractions ({@link PetalsProvidesOperation} and
 * {@link PetalsChannel}) and Camel abstractions ({@link CamelContext}).
 * 
 * It is mainly used by the endpoint.
 * 
 * @author vnoel
 *
 */
public interface PetalsCamelContext {

    public ServiceEndpointOperation getSEO(String serviceId) throws UnknownServiceException;

    public PetalsConsumesChannel getConsumesChannel(ServiceEndpointOperation seo);

    public PetalsProvidesChannel getProvidesChannel(ServiceEndpointOperation seo);

    public void registerPPO(ServiceEndpointOperation seo, PetalsProvidesOperation ppo);

    public void unregisterPPO(ServiceEndpointOperation seo);

    public CamelContext getCamelContext();

    public Logger getLogger();

}
