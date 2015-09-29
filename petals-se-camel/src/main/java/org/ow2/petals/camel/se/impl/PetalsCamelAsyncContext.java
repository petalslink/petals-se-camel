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

import org.ow2.petals.camel.PetalsChannel.SendAsyncCallback;
import org.ow2.petals.commons.log.FlowAttributes;
import org.ow2.petals.component.framework.process.async.AsyncContext;

public class PetalsCamelAsyncContext extends AsyncContext {

    private final SendAsyncCallback callback;

    private final FlowAttributes flowAttributes;

    public PetalsCamelAsyncContext(final org.ow2.petals.component.framework.api.message.Exchange originalExchange,
            final long ttl, final SendAsyncCallback callback, final FlowAttributes flowAttributes) {
        super(originalExchange, ttl);
        assert callback != null;
        this.callback = callback;
        this.flowAttributes = flowAttributes;
    }

    public SendAsyncCallback getCallback() {
        return callback;
    }

    public FlowAttributes getFlowAttributes() {
        return flowAttributes;
    }
}