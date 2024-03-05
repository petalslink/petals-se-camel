/**
 * Copyright (c) 2015-2024 Linagora
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
package org.ow2.petals.se.camel.impl;

import java.net.URI;
import java.util.Optional;

import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.commons.log.FlowAttributes;
import org.ow2.petals.component.framework.api.message.Exchange;
import org.ow2.petals.component.framework.jbidescriptor.generated.Consumes;
import org.ow2.petals.component.framework.logger.StepLogHelper;
import org.ow2.petals.component.framework.util.ServiceUnitUtil;
import org.ow2.petals.se.camel.PetalsCamelSender;
import org.ow2.petals.se.camel.exceptions.InvalidJBIConfigurationException;

public class ServiceEndpointOperationConsumes extends AbstractServiceEndpointOperation implements PetalsConsumesChannel {

    private final Consumes consumes;

    public ServiceEndpointOperationConsumes(final PetalsCamelSender sender, final Consumes consumes)
            throws InvalidJBIConfigurationException {
        super(consumes.getInterfaceName(), consumes.getServiceName(), consumes.getEndpointName(),
                consumes.getOperation(), toMEP(consumes), sender);
        this.consumes = consumes;
    }

    @Override
    public ServiceType getType() {
        return ServiceType.CONSUMES;
    }

    @Override
    public Exchange newExchange(final @Nullable MEPPatternConstants mep,
            final @NonNull Optional<Boolean> currentFlowTracingActivationState) throws MessagingException {
        final Exchange exchange = sender.createExchange(consumes, mep, currentFlowTracingActivationState);
        assert exchange != null;
        return exchange;
    }

    @Override
    public @Nullable ServiceEndpoint resolveEndpoint(final QName serviceName, final String endpointName) {
        return sender.getComponent().getContext().getEndpoint(serviceName, endpointName);
    }

    @Override
    public String buildTimeoutErrorMsg(final long timeout, final FlowAttributes currentFlowAttributes) {

        final String interfaceName = this.getInterface().toString();
        final String serviceName = this.getService() == null ? StepLogHelper.TIMEOUT_ERROR_MSG_UNDEFINED_REF
                : this.getService().toString();
        final String endpointName = this.getEndpoint() == null ? StepLogHelper.TIMEOUT_ERROR_MSG_UNDEFINED_REF
                : this.getEndpoint();
        final String operationName = this.getOperation() == null ? StepLogHelper.TIMEOUT_ERROR_MSG_UNDEFINED_REF
                : this.getOperation().toString();

        return String.format(StepLogHelper.TIMEOUT_ERROR_MSG_PATTERN,
                timeout < 0 ? this.sender.getTimeout(this.consumes) : timeout, interfaceName, serviceName, endpointName,
                operationName, currentFlowAttributes.getFlowInstanceId(), currentFlowAttributes.getFlowStepId());
    }

    private static @Nullable URI toMEP(final Consumes c) throws InvalidJBIConfigurationException {
        try {
            final MEPPatternConstants mep = ServiceUnitUtil.retrievePattern(c);
            return mep == null ? null : mep.value();
        } catch (final MessagingException e) {
            throw new InvalidJBIConfigurationException("Can't retrieve pattern", e);
        }
    }
}
