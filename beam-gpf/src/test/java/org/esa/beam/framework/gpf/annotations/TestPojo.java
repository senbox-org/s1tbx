package org.esa.beam.framework.gpf.annotations;

import org.esa.beam.framework.datamodel.Product;

public class TestPojo {
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
    
    @TargetProperty(alias = "bert",
                    description = "a test property")
    int property;
}
