/**
 * Copyright (c) 2015-2018 Linagora
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
package org.ow2.petals.camel;

import java.util.logging.Logger;

import org.apache.camel.CamelContext;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.PetalsChannel.PetalsProvidesChannel;
import org.ow2.petals.camel.exceptions.UnknownServiceException;

/**
 * 
 * Represents an object that provides methods for interacting between Petals abstractions (
 * {@link ServiceEndpointOperation}), Petals-Camel abstractions ({@link PetalsCamelRoute} and {@link PetalsChannel}) and
 * Camel abstractions ({@link CamelContext}).
 * 
 * It is mainly used by the endpoint.
 * 
 * @author vnoel
 *
 */
public interface PetalsCamelContext {

    /**
     * To get informations about the service (as an operation) designated by this serviceId.
     * 
     * @param serviceId
     *            The id of the service
     * @return
     * @throws UnknownServiceException
     *             If petals does not know this service id
     */
    public ServiceEndpointOperation getService(String serviceId) throws UnknownServiceException;

    /**
     * To get a channel to be able to create exchange as well as send them
     * 
     * @param seo
     *            the service that this channel will exchange with
     * @return
     */
    public PetalsConsumesChannel getConsumesChannel(ServiceEndpointOperation service);

    /**
     * To get a channel to be able to send (back as a provider) exchange
     * 
     * @param seo
     *            the service that this channel will exchange with
     * @return
     */
    public PetalsProvidesChannel getProvidesChannel(ServiceEndpointOperation service);

    /**
     * Register a Camel route to which exchange for the given seo should be dispatched
     * 
     * @param seo
     *            The service concerned by the route
     * @param ppo
     *            The callback to pass exchange to the route
     */
    public void registerRoute(ServiceEndpointOperation service, PetalsCamelRoute route);

    /**
     * Unregister a route
     * 
     * @param service
     *            The service concerned by the route
     */
    public void unregisterRoute(ServiceEndpointOperation service);

    public CamelContext getCamelContext();

    public Logger getLogger();

}
