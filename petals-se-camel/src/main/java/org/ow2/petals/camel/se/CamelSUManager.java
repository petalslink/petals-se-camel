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

import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.jdt.annotation.Nullable;
import org.ow2.petals.camel.PetalsProvidesOperation;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.exceptions.AlreadyRegisteredServiceException;
import org.ow2.petals.camel.exceptions.UnknownRegisteredServiceException;
import org.ow2.petals.camel.se.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.camel.se.exceptions.NotImplementedRouteException;
import org.ow2.petals.camel.se.exceptions.PetalsCamelSEException;
import org.ow2.petals.camel.se.utils.PetalsCamelJBIHelper;
import org.ow2.petals.component.framework.api.exception.PEtALSCDKException;
import org.ow2.petals.component.framework.api.message.Exchange;
import org.ow2.petals.component.framework.jbidescriptor.generated.Jbi;
import org.ow2.petals.component.framework.su.AbstractServiceUnitManager;
import org.ow2.petals.component.framework.su.ServiceUnitDataHandler;
import org.ow2.petals.component.framework.util.ClassLoaderUtil;
import org.ow2.petals.component.framework.util.EndpointOperationKey;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 
 * This manages all the SU
 * 
 * It dispatches messages to the correct Camel instance based on the ServiceEndpoint.
 * 
 * @author vnoel
 *
 */
public class CamelSUManager extends AbstractServiceUnitManager {

    /**
     * Store the CamelSU for each SU's name
     * 
     * Used only by deploy, undeploy, start and stop that are synchronized
     * 
     */
    private final Map<String, CamelSU> su2camel = Maps.newHashMap();

    /**
     * Mapping from service endpoint operations to the route that implements them
     * 
     * Needed to know where to send an arriving exchange (coming from the JBIListener)
     */
    private final ConcurrentMap<EndpointOperationKey, PetalsProvidesOperation> eo2ppo = Maps.newConcurrentMap();

    public CamelSUManager(CamelSE component) {
        super(component);
    }

    /**
     * This is synchronised as we modify the shared collection that must stay consistent during the whole method
     */
    @Override
    protected synchronized void doDeploy(final @Nullable String serviceUnitName, final @Nullable String suRootPath,
            final @Nullable Jbi jbiDescriptor) throws PEtALSCDKException {

        Preconditions.checkNotNull(serviceUnitName);
        Preconditions.checkNotNull(suRootPath);
        Preconditions.checkNotNull(jbiDescriptor);

        // First let's do some checks w.r.t. other SUs
        if (su2camel.containsKey(serviceUnitName)) {
            throw new PEtALSCDKException("This shouldn't happen: another SU with the name " + serviceUnitName
                    + " was already deployed in this SE");
        }

        // Next let's do method-local processing
        final CamelSU camelSU = createCamelSU(serviceUnitName);

        // And finally let's register all of that in our manager
        // we already checked that the map didn't contain this service unit
        su2camel.put(serviceUnitName, camelSU);

        // TODO checks that there is at least one route per operation
    }

    private CamelSU createCamelSU(final String serviceUnitName) throws PetalsCamelSEException {

        final ServiceUnitDataHandler suDH = getSUDataHandler(serviceUnitName);

        final Map<String, ServiceEndpointOperation> sid2seo = PetalsCamelJBIHelper
                .extractServicesIdAndEndpointOperations(suDH, getCamelSE());

        final List<String> classNames = Lists.newArrayList();
        final List<String> xmlNames = Lists.newArrayList();

        try {
            PetalsCamelJBIHelper.populateRouteLists(suDH.getDescriptor().getServices(), classNames, xmlNames);
        } catch (final URISyntaxException e) {
            throw new InvalidJBIConfigurationException("Exception while parsing camel-specific configuration", e);
        }

        // TODO why use this classloader and not the thread context classloader?
        // is it the same? normally yes according to JBI specs
        // TODO why use createClassLoader which runs with privilege?...
        final URLClassLoader classLoader = ClassLoaderUtil.createClassLoader(suDH.getInstallRoot(), getClass()
                .getClassLoader());

        return new CamelSU(suDH.getName(), ImmutableMap.copyOf(sid2seo), ImmutableList.copyOf(classNames),
                ImmutableList.copyOf(xmlNames), classLoader, this);
    }

    /**
     * This is synchronised as we modify the shared collection that must stay consistent during the whole method
     */
    @Override
    protected synchronized void doUndeploy(@Nullable String serviceUnitName) throws PEtALSCDKException {
        final CamelSU camelSU = su2camel.get(serviceUnitName);
        camelSU.undeploy();
        this.su2camel.remove(serviceUnitName);
    }

    /**
     * This is synchronised as we modify the shared collection that must stay consistent during the whole method
     */
    @Override
    protected synchronized void doStart(@Nullable String serviceUnitName) throws PEtALSCDKException {
        // TODO is there something in petals corresponding to resume?
        su2camel.get(serviceUnitName).start();
    }

    /**
     * This is synchronised as we modify the shared collection that must stay consistent during the whole method
     */
    @Override
    protected synchronized void doStop(@Nullable String serviceUnitName) throws PEtALSCDKException {
        // TODO is there something in petals corresponding to suspend?
        su2camel.get(serviceUnitName).stop();
    }

    public void registerPPO(final ServiceEndpointOperation seo, final PetalsProvidesOperation ppo)
            throws AlreadyRegisteredServiceException {

        final EndpointOperationKey key = buildEOK(seo);

        if (this.eo2ppo.containsKey(key)) {
            throw new AlreadyRegisteredServiceException(seo);
        }

        this.eo2ppo.put(key, ppo);
    }

    public void unregisterPPO(final ServiceEndpointOperation seo) throws UnknownRegisteredServiceException {
        if (this.eo2ppo.remove(buildEOK(seo)) != null) {
            throw new UnknownRegisteredServiceException(seo);
        }
    }

    public void process(final Exchange exchange) throws Exception {

        final EndpointOperationKey eo = new EndpointOperationKey(exchange);

        final PetalsProvidesOperation ppo = this.eo2ppo.get(eo);

        if (ppo == null) {
            throw new NotImplementedRouteException(eo);
        }

        ppo.process(exchange);
    }

    private EndpointOperationKey buildEOK(final ServiceEndpointOperation seo) {
        return new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(), seo.getOperation());
    }

    private CamelSE getCamelSE() {
        return (CamelSE) super.component;
    }
}
