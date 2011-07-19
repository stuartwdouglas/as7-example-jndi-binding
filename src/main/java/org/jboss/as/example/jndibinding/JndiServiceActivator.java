/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.example.jndibinding;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;

/**
 * ServiceActivators allow you to register your own services as part of the deployment process
 *
 * @author Stuart Douglas
 */
public class JndiServiceActivator implements ServiceActivator {

    /**
     * Managed references are what is actually used to retrieve a value binding.
     *
     * Release will be called when the AS is done with the binding, however in the case of programatic
     * lookups this will not be called.
     *
     */
    private class StringManagedReferenceFactory implements ManagedReferenceFactory {

        private final String value;

        public StringManagedReferenceFactory(final String value) {
            this.value = value;
        }

        public ManagedReference getReference() {

            return new ManagedReference() {
                public void release() {

                }

                public Object getInstance() {
                    return value;
                }
            };
        }
    }


    public void activate(final ServiceActivatorContext serviceActivatorContext) throws ServiceRegistryException {

        ModuleClassLoader loader = (ModuleClassLoader) getClass().getClassLoader();
        final String archiveName = loader.getModule().getIdentifier().getName().substring("deployment.".length());
        final String applicationName = archiveName.substring(0, archiveName.length() - 4);

        //create a global binding
        final ServiceName bindingServiceName = ContextNames.GLOBAL_CONTEXT_SERVICE_NAME.append(ExampleServlet.MY_GLOBAL_BINDING_NAME);
        final BinderService binderService = new BinderService(ExampleServlet.MY_GLOBAL_BINDING_NAME);
        ServiceBuilder<ManagedReferenceFactory> builder = serviceActivatorContext.getServiceTarget().addService(bindingServiceName, binderService);
        builder.addDependency(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, NamingStore.class, binderService.getNamingStoreInjector());
        binderService.getManagedObjectInjector().inject(new StringManagedReferenceFactory("Hello World!"));
        builder.install();

        final ServiceName appBindingServiceName = ContextNames.serviceNameOfContext("jndi-example", "jndi-example", "jndi-example", "java:app/" + ExampleServlet.MY_APP_BINDING_NAME);
        final BinderService appBinderService = new BinderService(ExampleServlet.MY_APP_BINDING_NAME);
        builder = serviceActivatorContext.getServiceTarget().addService(appBindingServiceName, appBinderService);
        builder.addDependency(ContextNames.contextServiceNameOfApplication(applicationName), NamingStore.class, appBinderService.getNamingStoreInjector());
        appBinderService.getManagedObjectInjector().inject(new StringManagedReferenceFactory("Hello Application!"));
        builder.install();


    }
}
