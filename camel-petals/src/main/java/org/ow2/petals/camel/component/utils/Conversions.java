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
package org.ow2.petals.camel.component.utils;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.camel.Exchange;
import org.apache.camel.attachment.AttachmentMessage;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.component.PetalsConstants;
import org.ow2.petals.jbi.xml.BytesSource;

import jakarta.activation.DataHandler;

/**
 * Utils to convert between petals exchange and camel exchange. Important: Some of this code makes the assumption that
 * an inoptionalout camel exchange MUST have an out message, even though camel allows for modifying inline the IN
 * message. This is needed because we can't know if an inoutoptional has no out because it's in is modified or because
 * the exchange is finished!
 * 
 * @author vnoel
 */
public class Conversions {

    private Conversions() {
    }

    /**
     * Used to update properties of an exchange we sent
     */
    private static void copyProperties(final Exchange from,
            final org.ow2.petals.component.framework.api.message.Exchange to, final String prefix) {

        for (final Entry<String, Object> prop : from.getProperties().entrySet()) {
            if (prop.getKey().startsWith(prefix)) {
                to.setProperty(prop.getKey().substring(prefix.length()), prop.getValue());
            }
        }
    }

    /**
     * Used to update properties from an exchange we received
     */
    private static void copyProperties(final org.ow2.petals.component.framework.api.message.Exchange from,
            final Exchange to, final String prefix) {
        for (final String prop : from.getPropertyNames()) {
            to.setProperty(prefix + prop, from.getProperty(prop));
        }
    }

    /**
     * Populates a new camel exchange with an exchange coming from petals
     * 
     * @param from
     *            JBI exchange received by the service provider implemented with Camel route.
     * @param currentFlowTracingActivation
     *            Current flow tracing activation state on JBI exchange processing at service provider level.
     * @param to
     *            Camel exchange to populate.
     */
    public static void populateNewCamelExchange(final org.ow2.petals.component.framework.api.message.Exchange from,
            final boolean currentFlowTracingActivation, final Exchange to) {

        to.setExchangeId(from.getExchangeId());

        // let's first copy properties that were potentially in the new created exchange
        copyProperties(to, from, PetalsConstants.EXCHANGE_ORIGINAL_PROPERTY_PREFIX);

        copyProperties(from, to, PetalsConstants.EXCHANGE_ORIGINAL_PROPERTY_PREFIX);

        to.setProperty(PetalsConstants.EXCHANGE_ORIGINAL_INTERFACE, from.getInterfaceName());
        to.setProperty(PetalsConstants.EXCHANGE_ORIGINAL_SERVICE, from.getService());
        to.setProperty(PetalsConstants.EXCHANGE_ORIGINAL_ENDPOINT, from.getEndpoint());
        to.setProperty(PetalsConstants.EXCHANGE_ORIGINAL_OPERATION, from.getOperation());
        to.setProperty(PetalsConstants.EXCHANGE_ORIGINAL_MEP, from.getPattern());

        to.setProperty(PetalsConstants.EXCHANGE_CURRENT_FLOW_TRACING_ACTIVATION,
                Boolean.valueOf(currentFlowTracingActivation));

        populateCamelMessage(from.getInMessage(), to.getIn(AttachmentMessage.class));
    }

    /**
     * Populates a camel exchange from the answer we got through petals
     */
    public static void populateAnswerCamelExchange(final org.ow2.petals.component.framework.api.message.Exchange from,
            final Exchange to) {

        // let's first clean the previous properties before copying those of the answer
        final Iterator<String> it = to.getProperties().keySet().iterator();
        while (it.hasNext()) {
            if (it.next().startsWith(PetalsConstants.EXCHANGE_PROPERTY_PREFIX)) {
                it.remove();
            }
        }

        copyProperties(from, to, PetalsConstants.EXCHANGE_PROPERTY_PREFIX);

        if (from.isErrorStatus()) {
            // there has been a technical error
            final Exception error = from.getError();
            to.setException(
                    error == null ? new Exception("Status ERROR returned without no more explanations !!") : error);
            // The request message body is removed
            to.getMessage().setBody(null);
        } else if (from.getFault() != null) {
            // there has been a fault
            populateCamelMessage(from.getFault(), to.getMessage(AttachmentMessage.class));
            to.getMessage().setHeader(PetalsConstants.MESSAGE_FAULT_HEADER, true);
            // TODO add test of conversions in both direction to be sure everything is correct!
        } else if (from.isOutMessage()) {
            // this is a response
            populateCamelMessage(from.getOutMessage(), to.getMessage(AttachmentMessage.class));
        } else {
            // the exchange is finished! it corresponds to done for petals exchange, but in Camel there is
            // nothing specific to do...
        }
    }

    private static void populateCamelMessage(final NormalizedMessage from,
            final AttachmentMessage toAttachmentMessage) {

        // Normally, it is an empty message that is populated...

        final Set<String> props = from.getPropertyNames();
        for (String prop : props) {
            toAttachmentMessage.setHeader(prop, from.getProperty(prop));
        }

        final Set<String> attachs = from.getAttachmentNames();
        for (String attach : attachs) {
            toAttachmentMessage.addAttachment(attach, from.getAttachment(attach));
        }

        final Source content = from.getContent();

        // let's take advantage of petals's BytesSource to avoid unneeded conversions
        final Source body;
        if (content instanceof BytesSource byteSource) {
            body = new org.apache.camel.util.xml.BytesSource(byteSource.getData(), content.getSystemId());
        } else {
            body = content;
        }

        toAttachmentMessage.setBody(body);
    }

    /**
     * Populates a new petals exchange with an exchange coming from camel
     */
    public static void populateNewPetalsExchange(final Exchange from,
            final org.ow2.petals.component.framework.api.message.Exchange to) throws MessagingException {

        // let's first copy properties that were potentially in the new created exchange
        // (such as flow attributes or other CDK things)
        copyProperties(to, from, PetalsConstants.EXCHANGE_PROPERTY_PREFIX);

        copyProperties(from, to, PetalsConstants.EXCHANGE_PROPERTY_PREFIX);

        populateNormalizedMessage(from.getIn(AttachmentMessage.class), to.getInMessage());
    }

    /**
     * <p>
     * Populates a Petals exchange from the answer we got through Camel.
     * </p>
     */
    public static void populateAnswerPetalsExchange(final Exchange from,
            final org.ow2.petals.component.framework.api.message.Exchange to) throws MessagingException {

        // let's clean what was in the Exchange before copying from the answer
        final Set<String> oldProps = new HashSet<>(to.getPropertyNames());
        for (final String oldProp : oldProps) {
            to.setProperty(oldProp, null);
        }

        copyProperties(from, to, PetalsConstants.EXCHANGE_ORIGINAL_PROPERTY_PREFIX);

        // Note: the Petals exchange checks that all is correct w.r.t. to MEP and status

        final AttachmentMessage outMessage = from.getMessage(AttachmentMessage.class);
        if (from.getException() != null) {
            to.setError(from.getException());
        } else if (Boolean.TRUE.equals(from.getMessage().getHeader(PetalsConstants.MESSAGE_FAULT_HEADER))) {
            final Fault fault = to.createFault();
            populateNormalizedMessage(outMessage, fault);
            to.setFault(fault);
        } else {
            MEPPatternConstants mep = MEPPatternConstants
                    .fromURI(from.getProperty(PetalsConstants.EXCHANGE_ORIGINAL_MEP, URI.class));

            // TODO maybe we should be able to handle situations when the exchanges have different MEP
            if (mep == MEPPatternConstants.IN_OUT) {
                populateNormalizedMessage(outMessage, to.getOutMessage());
            } else if (mep == MEPPatternConstants.IN_OPTIONAL_OUT) {
                if (outMessage != null) {
                    populateNormalizedMessage(outMessage, to.getOutMessage());
                } else {
                    // the exchange is finished
                    to.setDoneStatus();
                    // NOTE: we make the assumption that in the case of an optional out, the out message of
                    // the camel exchange is populated (sometimes the in message is used instead of the out in Camel
                    // processors... this is an ambiguity we can only handle by choosing this rule)
                }
            } else {
                to.setDoneStatus();
            }
        }
    }

    private static void populateNormalizedMessage(final AttachmentMessage fromAttachmentMessage,
            final NormalizedMessage to)
            throws MessagingException {

        // Normally, it is an empty message that is populated...

        for (final Entry<String, Object> e : fromAttachmentMessage.getHeaders().entrySet()) {
            to.setProperty(e.getKey(), e.getValue());
        }

        if (fromAttachmentMessage.hasAttachments()) {
            for (final Entry<String, DataHandler> e : fromAttachmentMessage.getAttachments().entrySet()) {
                to.addAttachment(e.getKey(), e.getValue());
            }
        }

        final Object body = fromAttachmentMessage.getBody();
        final Source content;
        // TODO maybe replace all of that with type converters registered to Camel?
        if (body instanceof org.apache.camel.util.xml.BytesSource bodyByteSource) {
            // let's apply the inverse transformation applied earlier
            content = new BytesSource(bodyByteSource.getData(), ((Source) body).getSystemId());
        } else if (body instanceof Source bodySource) {
            // let's continue with a Source then
            content = bodySource;
        } else {
            // TODO provide an endpoint option to force the use of a desired Source implementation?
            // This uses available converters (see http://camel.apache.org/type-converter.html)
            content = fromAttachmentMessage.getBody(DOMSource.class);
        }
        to.setContent(content);
    }
}
