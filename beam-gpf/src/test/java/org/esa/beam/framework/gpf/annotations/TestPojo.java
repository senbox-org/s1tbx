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
}
