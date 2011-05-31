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

package com.bc.ceres.core;

import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class ExtensionManagerTest2 {

    @Test
    public void testThatAFailingFactoryWillReturnNull() {
        ExtensionManager em = ExtensionManager.getInstance();
        assertNotNull(em);

        // String --> Long may throw NumberFormatException
        LongFromStringFactory factory = new LongFromStringFactory() {
            @Override
            protected Long parseLong(String s) {
                return Long.parseLong(s);
            }
        };

        em.register(String.class, factory);

        // Valid String --> Long
        assertEquals(new Long(123456789L), em.getExtension("123456789", Long.class));

        // Invalid String --> null
        assertEquals(null, em.getExtension("invalid", Long.class));

        em.unregister(String.class, factory);

        assertEquals("123456789;invalid;", factory.trace);
    }

    @Test
    public void testThatThereMayBeMoreFactoriesInquired() {
        ExtensionManager em = ExtensionManager.getInstance();
        assertNotNull(em);

        // Factory 1: String --> Long (may throw NumberFormatException)
        LongFromStringFactory factory1 = new LongFromStringFactory() {
            @Override
            protected Long parseLong(String s) {
                return Long.parseLong(s);
            }
        };
        // Factory 2: String --> Long (only succeeds, if value is "one" or "two")
        LongFromStringFactory factory2 = new LongFromStringFactory() {
            @Override
            protected Long parseLong(String s) {
                return s.equals("one") ? new Long(1L) : s.equals("two") ? new Long(2L) : null;
            }
        };
        // Factory 3: String --> Long (always succeeds, may return -999L on error)
        LongFromStringFactory factory3 = new LongFromStringFactory() {
            @Override
            protected Long parseLong(String s) {
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException e) {
                    return -999L;
                }
            }
        };

        em.register(String.class, factory1);
        em.register(String.class, factory2);
        em.register(String.class, factory3);

        // factory1
        assertEquals(new Long(123456789L), em.getExtension("123456789", Long.class));
        // factory3
        assertEquals(new Long(-999L), em.getExtension("three", Long.class));
        // factory2
        assertEquals(new Long(1L), em.getExtension("one", Long.class));
        assertEquals(new Long(2L), em.getExtension("two", Long.class));

        em.unregister(String.class, factory1);
        em.unregister(String.class, factory2);
        em.unregister(String.class, factory3);

        assertEquals("123456789;three;one;two;", factory1.trace);
        assertEquals("three;one;two;", factory2.trace);
        assertEquals("three;", factory3.trace);
    }


    abstract static class LongFromStringFactory implements ExtensionFactory {
        String trace = "";

        @Override
        public Class<?>[] getExtensionTypes() {
            return new Class<?>[] {Long.class};
        }

        @Override
        public Object getExtension(Object object, Class<?> extensionType) throws NumberFormatException  {
            trace += object + ";";
            return parseLong((String) object);
        }

        protected abstract Long parseLong(String s);

    }
}
