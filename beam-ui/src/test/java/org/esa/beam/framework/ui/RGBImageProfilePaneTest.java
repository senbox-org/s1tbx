/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RGBImageProfile;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class RGBImageProfilePaneTest {

    @Test
    public void testSelectProfile_1() throws Exception {
        RGBImageProfile[] rgbImageProfiles = new RGBImageProfile[] {
                new RGBImageProfile("p1", new String[]{"", "", ""}, new String[]{"matches", "not at", "all"}),
                new RGBImageProfile("p2", new String[]{"", "", ""}, new String[]{"some_different_type", "*name_*3", null}),
                new RGBImageProfile("p3", new String[]{"", "", ""}, new String[]{"*me_ty*", "*name_*3", null}),
                new RGBImageProfile("p4", new String[]{"", "", ""}, new String[]{"*me_ty*", "*name_*3", "*s some*"}),
        };
        Product product = new Product("some_name_123", "some_type_123", 1, 1);
        product.setDescription("This is some description text.");
        RGBImageProfile profile = RGBImageProfilePane.findProfileForProductPattern(rgbImageProfiles, product);
        assertNotNull(profile);
        assertSame(rgbImageProfiles[3], profile);   // all patterns match
    }

    @Test
    public void testSelectProfile_2() throws Exception {
        RGBImageProfile[] rgbImageProfiles = new RGBImageProfile[] {
                new RGBImageProfile("p1", new String[]{"", "", ""}, new String[]{"matches", "not at", "all"}),
                new RGBImageProfile("p2", new String[]{"", "", ""}, new String[]{"some_different_type", "*name_*3", null}),
                new RGBImageProfile("p3", new String[]{"", "", ""}, new String[]{"*me_ty*", null, null}),
                new RGBImageProfile("p4", new String[]{"", "", ""}, new String[]{null, "*name_*3", "*s some*"}),
        };
        Product product = new Product("some_name_123", "some_type_123", 1, 1);
        product.setDescription("This is some description text.");
        RGBImageProfile profile = RGBImageProfilePane.findProfileForProductPattern(rgbImageProfiles, product);
        assertNotNull(profile);
        assertSame(rgbImageProfiles[2], profile);     // type matches
    }

    @Test
    public void testSelectProfile_3() throws Exception {
        RGBImageProfile[] rgbImageProfiles = new RGBImageProfile[] {
                new RGBImageProfile("p1", new String[]{"", "", ""}, new String[]{"matches", "not at", "all"}),
                new RGBImageProfile("p2", new String[]{"", "", ""}, new String[]{"some_different_type", "*name_*3", null}),
                new RGBImageProfile("p3", new String[]{"", "", ""}, new String[]{null, "*name_*3", null}),
                new RGBImageProfile("p4", new String[]{"", "", ""}, new String[]{null, "*name_*3", "*s some*"}),
        };
        Product product = new Product("some_name_123", "some_type_123", 1, 1);
        product.setDescription("This is some description text.");
        RGBImageProfile profile = RGBImageProfilePane.findProfileForProductPattern(rgbImageProfiles, product);
        assertNotNull(profile);
        assertSame(rgbImageProfiles[3], profile);   // name and description match
    }

    @Test
    public void testSelectProfile_4() throws Exception {
        RGBImageProfile[] rgbImageProfiles = new RGBImageProfile[] {
                new RGBImageProfile("p1", new String[]{"", "", ""}, new String[]{"matches", "not at", "all"}),
                new RGBImageProfile("p2", new String[]{"", "", ""}, new String[]{"some_different_type", "*name_*3", null}),
                new RGBImageProfile("p3", new String[]{"", "", ""}, new String[]{"strange type", "*name_*3", null}),
                new RGBImageProfile("p4", new String[]{"", "", ""}, new String[]{"strange type", "*name_*3", "*s some*"}),
        };
        Product product = new Product("some_name_123", "some_type_123", 1, 1);
        product.setDescription("This is some description text.");
        RGBImageProfile profile = RGBImageProfilePane.findProfileForProductPattern(rgbImageProfiles, product);
        assertNotNull(profile);
        assertSame(rgbImageProfiles[3], profile);   // name and description match
    }

    @Test
    public void testSelectProfile_5() throws Exception {
        RGBImageProfile[] rgbImageProfiles = new RGBImageProfile[] {
                new RGBImageProfile("p1", new String[]{"", "", ""}, new String[]{"matches", "not at", "all"}),
                new RGBImageProfile("p2", new String[]{"", "", ""}, new String[]{"some_different_type", "*name_*3", null}),
                new RGBImageProfile("p3", new String[]{"", "", ""}, new String[]{"*me_ty*", "*name_*3", null}),
                new RGBImageProfile("p4", new String[]{"", "", ""}, new String[]{"*me_ty*", "*name_*3", null}),
        };
        Product product = new Product("some_name_123", "some_type_123", 1, 1);
        product.setDescription("This is some description text.");
        RGBImageProfile profile = RGBImageProfilePane.findProfileForProductPattern(rgbImageProfiles, product);
        assertNotNull(profile);
        assertSame(rgbImageProfiles[2], profile);   // equal, so earlier profile is chosen
    }
}
