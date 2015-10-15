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

package org.esa.snap.core.gpf.annotations;

import org.esa.snap.core.datamodel.Product;

public class TestPojo {

    enum TestEnum {
        P1,P2,P3
    }
    @TargetProduct
    Product vapour;

    @SourceProduct(optional = true,
                   type = "MERIS_BRR",
                   bands = {"radiance_2", "radiance_5"})
    Product brr;

    @Parameter(interval = "(0, 100]")
    double percentage;

    @Parameter(label="a nice desciption", valueSet = {"0", "13", "42"})
    double threshold;
    
    @Parameter(valueSet = {"0", "13", "42"})
    double[] thresholdArray;

    @Parameter(defaultValue = "P2")
    TestEnum aPValue;

    
    @TargetProperty(alias = "bert",
                    description = "a test property")
    int property;
}
