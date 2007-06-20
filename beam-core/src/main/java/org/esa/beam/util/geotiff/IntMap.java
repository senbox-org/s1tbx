/*
 * $Id: IntMap.java,v 1.1.1.1 2006/09/11 08:16:47 norman Exp $
 *
 * Copyright (C) 2004 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.util.geotiff;

import java.lang.reflect.Field;
import java.util.HashMap;

public class IntMap {

    private static HashMap _valueMap;
    private static HashMap _nameMap;

    protected static void init(Field[] fields) {
        _valueMap = new HashMap();
        _nameMap = new HashMap();
        for (int i = 0; i < fields.length; i++) {
            try {
                final Field field = fields[i];
                final String name = field.getName();
                final Integer value = (Integer) field.get(null);
                _valueMap.put(name, value);
                _nameMap.put(value, name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static int getCode(String name) {
        return (Integer) _valueMap.get(name);
    }

    public static String getName(int code) {
        return (String) _nameMap.get(new Integer(code));
    }

    protected IntMap() {
    }
}

