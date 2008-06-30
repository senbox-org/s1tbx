/*
 * $Id$
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.dataio.obpg.modifier;

import java.lang.reflect.Array;

public class ModifyArray<T> extends AbstactMatcher<T> {

    final T source;

    public ModifyArray(T source) {
        assert source != null;
        assert source.getClass().isArray();
        this.source = source;
    }

    public boolean matches(Object o) {
        if (o != null && o.getClass().isAssignableFrom(source.getClass())) {
            final T dest = (T) o;
            final int sourceLength = Array.getLength(source);
            if (Array.getLength(dest) == sourceLength) {
                for (int i = 0; i < sourceLength; i++) {
                    Array.set(dest, i,Array.get(source, i));
                }
                return true;
            }
        }
        return false;
    }
}
