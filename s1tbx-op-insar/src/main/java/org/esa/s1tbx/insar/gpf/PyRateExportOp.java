package org.esa.s1tbx.insar.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;

import java.io.File;

/**
 * Export products into format suitable for import to PyRate
 */
@OperatorMetadata(alias = "PyrateExport",
        category = "Radar/Interferometric/PSI \\ SBAS",
        authors = "Alex McVittie",
        version = "1.0",
        copyright = "Copyright (C) 2023 SkyWatch Space Applications Inc.",
        autoWriteDisabled = true,
        description = "Export data for PyRate processing")

public class PyRateExportOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The output folder to which the data product is written.")
    private File targetFolder;

    @Parameter(description = "Include coherence", defaultValue = "true")
    private Boolean includeCoherenceBands = true;

    @Parameter(description = "SNAPHU binary location", defaultValue = "snaphu")
    private String snaphuLocation = "snaphu";

    @Parameter(description = "Run TOPSAR Deburst", defaultValue = "false")
    private boolean runTOPSARDeburst = false;



    @Override
    public void initialize() throws OperatorException {

    }

    private void validateIfMultiMaster(){


    }

    private void process(){
        // Step 1: Validate input product to check if it is a multi-reference InSAR stack

        // Step 2: Produce interferograms

        // Step 3: Run de-burst, if


    }
}
