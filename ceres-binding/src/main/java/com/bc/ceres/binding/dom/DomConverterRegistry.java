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

package com.bc.ceres.binding.dom;

import java.util.HashMap;
import java.util.Map;

public class DomConverterRegistry {

    private Map<Class<?>, DomConverter> converterMap = new HashMap<Class<?>, DomConverter>(33);

    /**
     * Gets the singleton instance of the registry.
     *
     * @return The instance.
     */
    public static DomConverterRegistry getInstance() {
        return Holder.instance;
    }

    /**
     * Sets the converter to be used for the specified type.
     *
     * @param type      The type.
     * @param converter The converter.
     */
    public void setDomConverter(Class<?> type, DomConverter converter) {
        converterMap.put(type, converter);
    }

    /**
     * Gets the converter registered with the given type.
     *
     * @param type The type.
     *
     * @return The converter for the given type or {@code null} if no such converter exists.
     */
    public DomConverter getConverter(Class<?> type) {
        DomConverter domConverter = converterMap.get(type);
        while (domConverter == null && type != null && type != Object.class) {
            type = type.getSuperclass();
            domConverter = converterMap.get(type);
        }
        return domConverter;
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final DomConverterRegistry instance = new DomConverterRegistry();
    }
}
