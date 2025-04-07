/**
 * Copyright (c) 2017-2025 Linagora
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.Exchange;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.util.xml.StreamSourceCache;

import com.ebmwebsourcing.easycommons.stream.EasyByteArrayOutputStream;
import com.ebmwebsourcing.easycommons.xml.jaxb.AbstractAttachmentMarshaller;
import com.ebmwebsourcing.easycommons.xml.jaxb.AbstractAttachmentUnmarshaller;

import jakarta.activation.DataHandler;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.attachment.AttachmentMarshaller;
import jakarta.xml.bind.attachment.AttachmentUnmarshaller;

public class MarshallingHelper {

    private final Unmarshaller unm;

    private final Marshaller m;

    public MarshallingHelper(final JAXBContext context) throws JAXBException {
        this.unm = context.createUnmarshaller();
        this.m = context.createMarshaller();
    }

    /**
     * <p>
     * Unmarshal XML data, as the declared type, extracting from the given Camel exchange, as 'IN' message body. XOP
     * optimization is used for attachments.
     * </p>
     * <p>
     * This method is thread-safe.
     * </p>
     * 
     * @param camelExchange
     *            Camel exchange containing the 'IN' body to unmarshall
     * @param declaredType
     *            The expected type of the Camel message body afer unmarshalling
     * @return The Camel message body unmarshalled
     */
    @SuppressWarnings("unchecked")
    public <T> T unmarshal(final Exchange camelExchange, final Class<T> declaredType) throws JAXBException {

        // we can't simply use getBody(Source.class) because StAxSource are not supported by jaxb
        // and sometimes they are returned by getBody!
        final Object oBody = camelExchange.getMessage().getBody();
        final Source body;
        if (oBody instanceof Source bodySource && !(oBody instanceof StAXSource)) {
            body = bodySource;
        } else {
            body = camelExchange.getMessage().getBody(DOMSource.class);
        }

        synchronized (this.unm) {
            final AttachmentUnmarshaller oldAttachmentUnmarshaller = this.unm.getAttachmentUnmarshaller();
            this.unm.setAttachmentUnmarshaller(new AbstractAttachmentUnmarshaller() {
                @Override
                protected DataHandler getAttachment(final String cid) {
                    final AttachmentMessage am = camelExchange.getIn(AttachmentMessage.class);
                    return am.getAttachment(cid);
                }
            });

            try {
                if (Object.class.equals(declaredType)) {
                    return (T) this.unm.unmarshal(body);
                } else {
                    return this.unm.unmarshal(body, declaredType).getValue();
                }
            } finally {
                this.unm.setAttachmentUnmarshaller(oldAttachmentUnmarshaller);
            }
        }
    }

    /**
     * <p>
     * Marshal the given XML data {@code t} into the given Camel exchange, as 'OUT' message body. XOP optimization is
     * used for attachments.
     * </p>
     * <p>
     * This method is thread-safe.
     * </p>
     * 
     * @param camelExchange
     *            Camel exchange
     * @param t
     *            given XML data to marshal
     */
    public <T> void marshal(final Exchange camelExchange, final T t) throws JAXBException, IOException {
        this.marshal(camelExchange, t, true);
    }

    /**
     * <p>
     * Marshal the given XML data {@code t} into the given Camel exchange, as 'OUT' message body. XOP optimization is
     * used for attachments.
     * </p>
     * <p>
     * This method is thread-safe.
     * </p>
     * 
     * @param camelExchange
     *            Camel exchange
     * @param t
     *            given XML data to marshal
     * @param xop
     *            If {@code true}, XOP optimization is used for attachments
     */
    public <T> void marshal(final Exchange camelExchange, final T t, final boolean xop)
            throws JAXBException, IOException {

        synchronized (this.m) {
            final AttachmentMarshaller oldAttachmentMarshaller = m.getAttachmentMarshaller();
            if (xop) {
                this.m.setAttachmentMarshaller(new AbstractAttachmentMarshaller() {
                    @Override
                    protected void addAttachment(final String cid, final DataHandler data) {
                        final AttachmentMessage am = camelExchange.getMessage(AttachmentMessage.class);
                        am.addAttachment(cid, data);
                    }
                });
            }

            try (final EasyByteArrayOutputStream out = new EasyByteArrayOutputStream()) {
                this.m.marshal(t, out);

                // We use a 'StreamSourceCache' instead of 'StreamSource' to workaround a problem of Apache Camel 4.0.x
                // about its message logging in unit test through PetalsCamelTestSupport(true). The property
                // LOG_DEBUG_BODY_STREAMS should not log body based on java.xml.transform.stream.StreamSource (see
                // https://camel.apache.org/manual/faq/how-do-i-enable-streams-when-debug-logging-messages-in-camel.html)
                // but they are logged, and next the stream can not be read.
                camelExchange.getOut()
                        .setBody(new StreamSourceCache(new StreamSource(out.toByteArrayInputStream()), camelExchange));
            } finally {
                this.m.setAttachmentMarshaller(oldAttachmentMarshaller);
            }
        }
    }

    /**
     * <p>
     * Basic marshalling. It is thread-safe.
     * </p>
     * 
     * @param out
     * @param object
     */
    public void basicMarshal(final OutputStream out, final Object object) throws JAXBException {
        synchronized (this.m) {
            this.m.marshal(object, out);
        }
    }

    /**
     * <p>
     * Basic unmarshalling. It is thread-safe.
     * </p>
     * 
     * @param in
     */
    public Object basicUnmarshal(final InputStream in) throws JAXBException {
        synchronized (this.unm) {
            return this.unm.unmarshal(in);
        }
    }
}
