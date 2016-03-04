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
package org.ow2.petals.camel.component.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.activation.DataHandler;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.ow2.petals.camel.component.PetalsCamelComponent;

/**
 * Utils to convert between petals exchange and camel exchange.
 * 
 * Important: Some of this code makes the assumption that an inoptionalout camel exchange MUST have an out message, even
 * though camel allows for modifying inline the IN message. This is needed because we can't know if an inoutoptional has
 * no out because it's in is modified or because the exchange is finished!
 * 
 * @author vnoel
 *
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
        for (final Object prop : from.getPropertyNames()) {
            to.setProperty(prefix + prop, from.getProperty((String) prop));
        }
    }

    /**
     * To populate a new camel exchange with an exchange coming from petals
     */
    public static void populateNewCamelExchange(final org.ow2.petals.component.framework.api.message.Exchange from,
            final Exchange to) {

        to.setExchangeId(from.getExchangeId());

        // let's first copy properties that were potentially in the new created exchange
        copyProperties(to, from, PetalsCamelComponent.EXCHANGE_ORIGINAL_PROPERTY_PREFIX);

        copyProperties(from, to, PetalsCamelComponent.EXCHANGE_ORIGINAL_PROPERTY_PREFIX);

        to.setProperty(PetalsCamelComponent.EXCHANGE_ORIGINAL_INTERFACE, from.getInterfaceName());
        to.setProperty(PetalsCamelComponent.EXCHANGE_ORIGINAL_SERVICE, from.getService());
        to.setProperty(PetalsCamelComponent.EXCHANGE_ORIGINAL_ENDPOINT, from.getEndpoint());
        to.setProperty(PetalsCamelComponent.EXCHANGE_ORIGINAL_OPERATION, from.getOperation());
        to.setProperty(PetalsCamelComponent.EXCHANGE_ORIGINAL_MEP, from.getPattern());

        populateCamelMessage(from.getInMessage(), to.getIn());
    }

    /**
     * To populate a camel exchange from the answer we got through petals
     */
    public static void populateAnswerCamelExchange(final org.ow2.petals.component.framework.api.message.Exchange from,
            final Exchange to) {

        // let's first clean the previous properties before copying those of the answer
        final Iterator<String> it = to.getProperties().keySet().iterator();
        while (it.hasNext()) {
            if (it.next().startsWith(PetalsCamelComponent.EXCHANGE_PROPERTY_PREFIX)) {
                it.remove();
            }
        }

        copyProperties(from, to, PetalsCamelComponent.EXCHANGE_PROPERTY_PREFIX);

        if (from.isErrorStatus()) {
            // there has been a technical error
            to.setException(from.getError());
        } else if (from.getFault() != null) {
            // there has been a fault
            populateCamelMessage(from.getFault(), to.getOut());
            to.getOut().setFault(true);
            // TODO add test of conversions in both direction to be sure everything is correct!
        } else if (from.isOutMessage()) {
            // this is a response
            populateCamelMessage(from.getOutMessage(), to.getOut());
        } else {
            // the exchange is finished! it corresponds to done for petals exchange, but in Camel there is
            // nothing specific to do...
        }
    }

    private static void populateCamelMessage(final NormalizedMessage from, final Message to) {

        // Normally, it is an empty message that is populated...

        @SuppressWarnings("unchecked")
        final Set<String> props = from.getPropertyNames();
        for (String prop : props) {
            to.setHeader(prop, from.getProperty(prop));
        }

        @SuppressWarnings("unchecked")
        final Set<String> attachs = from.getAttachmentNames();
        for (String attach : attachs) {
            to.addAttachment(attach, from.getAttachment(attach));
        }

        to.setBody(from.getContent());
    }

    /**
     * To populate a new petals exchange with an exchange coming from camel
     */
    public static void populateNewPetalsExchange(final Exchange from,
            final org.ow2.petals.component.framework.api.message.Exchange to)
            throws MessagingException {

        // let's first copy properties that were potentially in the new created exchange
        // (such as flow attributes or other CDK things)
        copyProperties(to, from, PetalsCamelComponent.EXCHANGE_PROPERTY_PREFIX);

        copyProperties(from, to, PetalsCamelComponent.EXCHANGE_PROPERTY_PREFIX);

        Conversions.populateNormalizedMessage(from.getIn(), to.getInMessage());
    }

    /**
     * To populate a petals exchange from the answer we got through camel
     */
    public static void populateAnswerPetalsExchange(final Exchange from,
            final org.ow2.petals.component.framework.api.message.Exchange to)
            throws MessagingException {

        // let's clean what was in the Exchange before copying from the answer
        @SuppressWarnings("unchecked")
        final Set<String> oldProps = new HashSet<String>((Set<String>) to.getPropertyNames());
        for (final String oldProp : oldProps) {
            to.setProperty(oldProp, null);
        }

        copyProperties(from, to, PetalsCamelComponent.EXCHANGE_ORIGINAL_PROPERTY_PREFIX);

        // Note: the Petals exchange checks that all is correct w.r.t. to MEP and status

        if (from.hasOut() && from.getOut().isFault()) {
            final Fault fault = to.createFault();
            Conversions.populateNormalizedMessage(from.getOut(), fault);
            to.setFault(fault);
        } else if (from.getException() != null) {
            to.setError(from.getException());
        } else {
            final ExchangePattern mep = from.getPattern();

            // TODO maybe we should be able to handle situations when the exchanges have different MEP
            if (mep == ExchangePattern.InOut) {
                final Message out;
                // sometimes camel exchange out is stored inplace of the in by Camel processors...
                if (!from.hasOut()) {
                    out = from.getIn();
                } else {
                    out = from.getOut();
                }
                Conversions.populateNormalizedMessage(out, to.getOutMessage());
            } else if (mep == ExchangePattern.InOptionalOut) {
                if (from.hasOut()) {
                    Conversions.populateNormalizedMessage(from.getOut(), to.getOutMessage());
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

    private static void populateNormalizedMessage(final Message from, final NormalizedMessage to)
            throws MessagingException {

        // Normally, it is an empty message that is populated...

        for (final Entry<String, Object> e : from.getHeaders().entrySet()) {
            to.setProperty(e.getKey(), e.getValue());
        }

        for (final Entry<String, DataHandler> e : from.getAttachments().entrySet()) {
            to.addAttachment(e.getKey(), e.getValue());
        }

        final Object body = from.getBody();
        final Source content;
        // TODO maybe replace all of that with type converters registered to Camel?
        if (body instanceof Source) {
            // let's continue with a Source then
            content = (Source) body;
        } else {
            // TODO provide an endpoint option to force the use of a desired Source implementation?
            // This uses available converters (see http://camel.apache.org/type-converter.html)
            content = from.getBody(DOMSource.class);
        }
        to.setContent(content);
    }
}
