/**
 * Copyright (c) 2017 Linagora
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
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.Message;

import com.ebmwebsourcing.easycommons.stream.EasyByteArrayOutputStream;
import com.ebmwebsourcing.easycommons.xml.jaxb.AbstractAttachmentMarshaller;
import com.ebmwebsourcing.easycommons.xml.jaxb.AbstractAttachmentUnmarshaller;

public class MarshallingHelper {

    private final JAXBContext context;

    public MarshallingHelper(final JAXBContext context) {
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    public <T> T unmarshal(final Message msg, final Class<T> t) throws JAXBException {
        final Unmarshaller unm = this.context.createUnmarshaller();
        unm.setAttachmentUnmarshaller(new AbstractAttachmentUnmarshaller() {
            @Override
            protected DataHandler getAttachment(final String cid) {
                return msg.getAttachment(cid);
            }
        });
        // do not use Source as StAxSource are not supported by jaxb and sometimes are returned by getBody!
        Source body = msg.getBody(DOMSource.class);
        if (Object.class.equals(t)) {
            return (T) unm.unmarshal(body);
        } else {
            return unm.unmarshal(body, t).getValue();
        }
    }

    public <T> void marshal(final Message msg, final T t) throws JAXBException {
        this.marshal(msg, t, true);
    }

    public <T> void marshal(final Message msg, final T t, final boolean xop) throws JAXBException {
        final Marshaller m = this.context.createMarshaller();
        if (xop) {
            m.setAttachmentMarshaller(new AbstractAttachmentMarshaller() {
                @Override
                protected void addAttachment(final String cid, final DataHandler data) {
                    msg.addAttachment(cid, data);
                }
            });
        }
        try (final EasyByteArrayOutputStream out = new EasyByteArrayOutputStream()) {
            m.marshal(t, out);
            msg.setBody(new StreamSource(out.toByteArrayInputStream()));
        }
    }

    public void basicMarshal(final OutputStream out, final Object object) throws JAXBException {
        this.context.createMarshaller().marshal(object, out);
    }

    public Object basicUnmarshal(final InputStream in) throws JAXBException {
        return this.context.createUnmarshaller().unmarshal(in);
    }
}
