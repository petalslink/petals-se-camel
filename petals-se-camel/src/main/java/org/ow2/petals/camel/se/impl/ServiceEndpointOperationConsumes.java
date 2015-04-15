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
package org.ow2.petals.camel.se.impl;

import java.net.URI;

import javax.jbi.JBIException;

import org.eclipse.jdt.annotation.Nullable;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.se.PetalsCamelSender;
import org.ow2.petals.camel.se.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.component.framework.api.message.Exchange;
import org.ow2.petals.component.framework.jbidescriptor.generated.Consumes;
import org.ow2.petals.component.framework.jbidescriptor.generated.MEPType;

public class ServiceEndpointOperationConsumes extends AbstractServiceEndpointOperation implements PetalsConsumesChannel {

    private final PetalsCamelSender sender;

    public ServiceEndpointOperationConsumes(final PetalsCamelSender sender)
            throws InvalidJBIConfigurationException {
        super(sender.getConsumes().getServiceName(), sender.getConsumes().getInterfaceName(), sender.getConsumes()
                .getEndpointName(), sender.getConsumes().getOperation(), ServiceType.CONSUMES, toMEP(sender
                .getConsumes().getMep(), sender.getConsumes()), sender);
        this.sender = sender;
    }

    @Override
    public Exchange newExchange() throws JBIException {
        return sender.createConsumeExchange(sender.getConsumes());
    }

    private static URI toMEP(@Nullable final MEPType mep, final Consumes c) throws InvalidJBIConfigurationException {

        // default MEP in Camel SE
        if (mep == null) {
            return MEPPatternConstants.IN_OUT.value();
        }

        switch (mep) {
            case IN_ONLY:
                return MEPPatternConstants.IN_ONLY.value();
            case IN_OPTIONAL_OUT:
                return MEPPatternConstants.IN_OPTIONAL_OUT.value();
            case IN_OUT:
                return MEPPatternConstants.IN_OUT.value();
            case ROBUST_IN_ONLY:
                return MEPPatternConstants.ROBUST_IN_ONLY.value();
            default:
                throw new InvalidJBIConfigurationException("No MEP set for operation " + c.getOperation()
                        + " of service " + c.getServiceName());
        }
    }
}
