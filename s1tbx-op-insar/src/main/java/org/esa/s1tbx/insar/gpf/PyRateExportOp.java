package org.esa.s1tbx.insar.gpf;

import org.esa.snap.core.gpf.annotations.OperatorMetadata;

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

public class PyRateExportOp {
}
