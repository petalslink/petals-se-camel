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

import org.ow2.petals.component.framework.api.configuration.ConfigurationExtensions;
import org.ow2.petals.component.framework.jbidescriptor.generated.Consumes;
import org.ow2.petals.component.framework.jbidescriptor.generated.Provides;
import org.ow2.petals.component.framework.listener.AbstractListener;

/**
 * This is needed to send messages.
 * 
 * It is bound to one and only consumes or provides.
 * 
 * @author vnoel
 *
 */
public class PetalsCamelSender extends AbstractListener {

    public PetalsCamelSender(final CamelSE component, final Consumes consumes) {
        init(component);
        setConsumes(consumes);
        setExtensions(new ConfigurationExtensions(consumes.getAny()));
        init();
    }

    public PetalsCamelSender(final CamelSE component, final Provides provides) {
        init(component);
        setProvides(provides);
        setExtensions(new ConfigurationExtensions(provides.getAny()));
        init();
    }
}
