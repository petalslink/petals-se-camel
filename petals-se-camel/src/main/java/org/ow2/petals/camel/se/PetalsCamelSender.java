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
package org.ow2.petals.camel.se;

import java.util.logging.Logger;

import org.ow2.petals.component.framework.listener.AbstractListener;

/**
 * This is needed to send messages.
 * 
 * It is bound to one and only one SU.
 * 
 * @author vnoel
 *
 */
public class PetalsCamelSender extends AbstractListener {

    private final Logger suLogger;

    public PetalsCamelSender(final CamelSE component, final Logger suLogger) {
        this.suLogger = suLogger;
        init(component);
    }

    /**
     * We can't override it...
     */
    public Logger getLogger2() {
        return suLogger;
    }

}
