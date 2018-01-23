/**
 * Copyright (c) 2017-2018 Linagora
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

import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.Message;

import com.ebmwebsourcing.easycommons.stream.EasyByteArrayOutputStream;
import com.ebmwebsourcing.easycommons.xml.jaxb.AbstractAttachmentMarshaller;
import com.ebmwebsourcing.easycommons.xml.jaxb.AbstractAttachmentUnmarshaller;

public class MarshallingHelper {

    private final Unmarshaller unm;

    private final Marshaller m;

    public MarshallingHelper(final JAXBContext context) throws JAXBException {
        this.unm = context.createUnmarshaller();
        this.m = context.createMarshaller();
    }

    /**
     * <p>
     * Unmarshal XML data extracting from the given Camel message body as the declared type.
     * </p>
     * <p>
     * This method is thread-safe.
     * </p>
     * 
     * @param msg
     *            Camel message containing the body to unmarshall
     * @param declaredType
     *            The expected type of the Camel message body afer unmarshalling
     * @return The Camel message body unmarshalled
     */
    @SuppressWarnings("unchecked")
    public <T> T unmarshal(final Message msg, final Class<T> declaredType) throws JAXBException {

        // we can't simply use getBody(Source.class) because StAxSource are not supported by jaxb
        // and sometimes they are returned by getBody!
        Object oBody = msg.getBody();
        Source body;
        if (oBody instanceof Source && !(oBody instanceof StAXSource)) {
            body = (Source) oBody;
        } else {
            body = msg.getBody(DOMSource.class);
        }

        synchronized (this.unm) {
            final AttachmentUnmarshaller oldAttachmentUnmarshaller = this.unm.getAttachmentUnmarshaller();
            this.unm.setAttachmentUnmarshaller(new AbstractAttachmentUnmarshaller() {
                @Override
                protected DataHandler getAttachment(final String cid) {
                    return msg.getAttachment(cid);
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
     * Marshal the given XML data {@code t} into the given Camel message body. XOP optimization is used for attachments.
     * </p>
     * <p>
     * This method is thread-safe.
     * </p>
     * 
     * @param msg
     *            Camel message
     * @param t
     *            given XML data to marshal
     */
    public <T> void marshal(final Message msg, final T t) throws JAXBException {
        this.marshal(msg, t, true);
    }

    /**
     * <p>
     * Marshal the given XML data {@code t} into the given Camel message body.
     * </p>
     * <p>
     * This method is thread-safe.
     * </p>
     * 
     * @param msg
     *            Camel message
     * @param t
     *            given XML data to marshal
     * @param xop
     *            If {@code true}, XOP optimization is used for attachments
     */
    public <T> void marshal(final Message msg, final T t, final boolean xop) throws JAXBException {

        synchronized (this.m) {
            final AttachmentMarshaller oldAttachmentMarshaller = m.getAttachmentMarshaller();
            if (xop) {
                this.m.setAttachmentMarshaller(new AbstractAttachmentMarshaller() {
                    @Override
                    protected void addAttachment(final String cid, final DataHandler data) {
                        msg.addAttachment(cid, data);
                    }
                });
            }

            try (final EasyByteArrayOutputStream out = new EasyByteArrayOutputStream()) {
                this.m.marshal(t, out);
                msg.setBody(new StreamSource(out.toByteArrayInputStream()));
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
