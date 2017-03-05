/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.util;

import com.bc.ceres.core.ServiceRegistry;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.logging.Level;

/**
 * <p><i><b>IMPORTANT NOTE:</b>
 * This class does not belong to the public API.
 * It is not intended to be used by clients.
 *
 * @author Marco Peters
 */
public class ServiceLoader {

    public static <T> void loadServices(ServiceRegistry<T> registry) {
        Iterable<T> iterable = SystemUtils.loadServices(registry.getServiceType());
        final Iterator<T> iterator = iterable.iterator();

        //noinspection WhileLoopReplaceableByForEach
        while (iterator.hasNext()) {
            try {
                registry.addService(iterator.next());
            } catch (ServiceConfigurationError e) {
                SystemUtils.LOG.log(Level.WARNING, e.getMessage(), e.getCause());
            }
        }
    }

}
