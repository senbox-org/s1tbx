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

package org.esa.beam.framework.gpf.main;


import com.bc.ceres.binding.dom.DefaultDomElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.junit.Test;

import static org.junit.Assert.*;

public class CommandLineUsageTest {
    @Test
    public void testConvertSourceProductFieldToDom() throws NoSuchFieldException {
        DefaultDomElement parent = new DefaultDomElement("parent");
        CommandLineUsage.convertSourceProductFieldToDom(Foo.class.getDeclaredField("source1"), parent);
        assertEquals("<parent>\n" +
                             "    <input1>${input1}</input1>\n" +
                             "</parent>",
                     parent.toXml());

        parent = new DefaultDomElement("parent");
        CommandLineUsage.convertSourceProductFieldToDom(Foo.class.getDeclaredField("source2"), parent);
        assertEquals("<parent>\n" +
                             "    <source2>${source2}</source2>\n" +
                             "</parent>",
                     parent.toXml());

        parent = new DefaultDomElement("parent");
        CommandLineUsage.convertSourceProductFieldToDom(Foo.class.getDeclaredField("notMe1"), parent);
        assertEquals("<parent/>", parent.toXml());

        parent = new DefaultDomElement("parent");
        CommandLineUsage.convertSourceProductFieldToDom(Foo.class.getDeclaredField("notMe2"), parent);
        assertEquals("<parent/>", parent.toXml());

        parent = new DefaultDomElement("parent");
        CommandLineUsage.convertSourceProductFieldToDom(Foo.class.getDeclaredField("notMe3"), parent);
        assertEquals("<parent/>", parent.toXml());
    }

    @Test
    public void testConvertParameterFieldToDom() throws NoSuchFieldException {
        DefaultDomElement parent = new DefaultDomElement("parent");
        CommandLineUsage.convertParameterFieldToDom(Foo.class.getDeclaredField("x"), parent);
        assertEquals("<parent>\n" +
                             "    <z>float</z>\n" +
                             "</parent>",
                     parent.toXml());

        parent = new DefaultDomElement("parent");
        CommandLineUsage.convertParameterFieldToDom(Foo.class.getDeclaredField("y"), parent);
        assertEquals("<parent>\n" +
                             "    <y>string</y>\n" +
                             "</parent>",
                     parent.toXml());

        parent = new DefaultDomElement("parent");
        CommandLineUsage.convertParameterFieldToDom(Foo.class.getDeclaredField("bar"), parent);
        assertEquals("<parent>\n" +
                             "    <bar>\n" +
                             "        <c>float</c>\n" +
                             "        <b>string</b>\n" +
                             "    </bar>\n" +
                             "</parent>",
                     parent.toXml());
    }

    static class Foo {
        @SourceProduct(alias = "input1")
        Product source1;

        Product source2;

        @SourceProduct()
        static Product notMe1;
        @SourceProduct()
        final Product notMe2 = new Product("name", "t", 10,10);
        @SourceProduct()
        transient Product notMe3;

        @Parameter(alias = "z")
        float x;

        String y;


        Bar bar;
    }

    static class Bar {

        @Parameter(alias = "c")
        float a;

        String b;

        final double notMe1 = 0.42;
        static String notMe2 ;
        transient int notMe3;
    }
}