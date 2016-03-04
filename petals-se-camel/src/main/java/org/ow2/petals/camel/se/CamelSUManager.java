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

import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import javax.jbi.JBIException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.ow2.petals.camel.PetalsCamelRoute;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.se.exceptions.NotImplementedRouteException;
import org.ow2.petals.camel.se.exceptions.PetalsCamelSEException;
import org.ow2.petals.camel.se.utils.PetalsCamelJBIHelper;
import org.ow2.petals.component.framework.api.exception.PEtALSCDKException;
import org.ow2.petals.component.framework.api.message.Exchange;
import org.ow2.petals.component.framework.jbidescriptor.generated.Jbi;
import org.ow2.petals.component.framework.jbidescriptor.generated.Services;
import org.ow2.petals.component.framework.su.ServiceEngineServiceUnitManager;
import org.ow2.petals.component.framework.su.ServiceUnitDataHandler;
import org.ow2.petals.component.framework.util.ClassLoaderUtil;
import org.ow2.petals.component.framework.util.EndpointOperationKey;

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
public class CamelSUManager extends ServiceEngineServiceUnitManager {

    /**
     * Store the CamelSU for each SU's name
     * 
     * Used only by deploy, undeploy, start and stop that are synchronized
     * 
     */
    @SuppressWarnings("null")
    private final Map<String, CamelSU> su2camel = Maps.newHashMap();

    /**
     * Mapping from service endpoint operations to the route that implements them
     * 
     * Needed to know where to send an arriving exchange (coming from the JBIListener)
     */
    @SuppressWarnings("null")
    private final ConcurrentMap<EndpointOperationKey, PetalsCamelRoute> eo2routes = Maps.newConcurrentMap();

    public CamelSUManager(final CamelSE component) {
        super(component);
    }

    @NonNullByDefault(false)
    @Override
    protected void doDeploy(final String serviceUnitName, final String suRootPath, final Jbi jbiDescriptor)
            throws PetalsCamelSEException {
        assert serviceUnitName != null;
        final CamelSU camelSU = createCamelSU(serviceUnitName);

        // No need to check if it isn't here: the CDK did that for us.
        su2camel.put(serviceUnitName, camelSU);

        // TODO checks that there is at least one route per operation
    }

    private CamelSU createCamelSU(final String serviceUnitName) throws PetalsCamelSEException {
        final ServiceUnitDataHandler suDH = getServiceUnitDataHandlers().get(serviceUnitName);
        assert suDH != null;

        final Logger suLogger;
        try {
            suLogger = getComponent().getContext().getLogger(serviceUnitName, null);
            assert suLogger != null;
        } catch (final MissingResourceException e) {
            throw new PetalsCamelSEException("Error when getting logger for SU " + serviceUnitName, e);
        } catch (final JBIException e) {
            throw new PetalsCamelSEException("Error when getting logger for SU " + serviceUnitName, e);
        }

        final Map<String, ServiceEndpointOperation> sid2seo = PetalsCamelJBIHelper
                .extractServicesIdAndEndpointOperations(suDH, new PetalsCamelSender(getComponent(), suLogger));

        final List<String> classNames = Lists.newArrayList();
        final List<String> xmlNames = Lists.newArrayList();

        final Services services = suDH.getDescriptor().getServices();
        assert services != null;
        PetalsCamelJBIHelper.populateRouteLists(services, classNames, xmlNames);

        // TODO why use this classloader and not the thread context classloader?
        // is it the same? normally yes according to JBI specs
        // TODO why use createClassLoader which runs with privilege?...
        final URLClassLoader classLoader = ClassLoaderUtil.createClassLoader(suDH.getInstallRoot(), getClass()
                .getClassLoader());
        assert classLoader != null;

        return new CamelSU(ImmutableMap.copyOf(sid2seo), ImmutableList.copyOf(classNames),
                ImmutableList.copyOf(xmlNames), classLoader, suLogger, this);
    }

    @NonNullByDefault(false)
    @Override
    protected void doUndeploy(final String serviceUnitName) throws PetalsCamelSEException {
        final CamelSU camelSU = this.su2camel.remove(serviceUnitName);
        camelSU.undeploy();
    }

    @NonNullByDefault(false)
    @Override
    protected void doInit(final String serviceUnitName, final String suRootPath) throws PEtALSCDKException {
        this.su2camel.get(serviceUnitName).init();
    }

    @NonNullByDefault(false)
    @Override
    protected void doShutdown(final String serviceUnitName) throws PEtALSCDKException {
        this.su2camel.get(serviceUnitName).shutdown();
    }

    @NonNullByDefault(false)
    @Override
    protected void doStart(final String serviceUnitName) throws PEtALSCDKException {
        this.su2camel.get(serviceUnitName).start();
        // TODO handle resume/suspend
    }

    @NonNullByDefault(false)
    @Override
    protected void doStop(final String serviceUnitName) throws PEtALSCDKException {
        // TODO handle resume/suspend
        this.su2camel.get(serviceUnitName).stop();
    }

    public void registerRoute(final ServiceEndpointOperation service, final PetalsCamelRoute route) {

        final EndpointOperationKey key = buildEOK(service);

        final PetalsCamelRoute put = this.eo2routes.put(key, route);

        assert put == null;
    }

    public void unregisterRoute(final ServiceEndpointOperation service) {

        final EndpointOperationKey key = buildEOK(service);

        final PetalsCamelRoute removed = this.eo2routes.remove(key);

        assert removed != null;
    }

    public PetalsCamelRoute getRoute(final Exchange exchange) throws NotImplementedRouteException {
        final EndpointOperationKey eo = new EndpointOperationKey(exchange);

        final PetalsCamelRoute ppo = this.eo2routes.get(eo);

        if (ppo == null) {
            throw new NotImplementedRouteException(eo);
        }

        return ppo;
    }

    private EndpointOperationKey buildEOK(final ServiceEndpointOperation seo) {
        return new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(), seo.getOperation());
    }

    @SuppressWarnings("null")
    protected CamelSE getComponent() {
        return (CamelSE) super.component;
    }
}
