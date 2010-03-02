package org.esa.beam.dataio.chris;

import org.esa.beam.framework.datamodel.AngularDirection;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Pointing;
import org.esa.beam.framework.datamodel.PointingFactory;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;

public final class ChrisPointingFactory implements PointingFactory {

    @Override
    public String[] getSupportedProductTypes() {
        return new String[]{
                "CHRIS_M0_NR_AC_GC",
                "CHRIS_M1_NR_AC_GC",
                "CHRIS_M2_NR_AC_GC",
                "CHRIS_M3_NR_AC_GC",
                "CHRIS_M4_NR_AC_GC",
                "CHRIS_M5_NR_AC_GC"
        };
    }

    @Override
    public Pointing createPointing(final RasterDataNode raster) {
        return new Pointing() {
            @Override
            public GeoCoding getGeoCoding() {
                return raster.getProduct().getGeoCoding();
            }

            @Override
            public AngularDirection getSunDir(PixelPos pixelPos, AngularDirection angularDirection) {
                return null;
            }

            @Override
            public AngularDirection getViewDir(PixelPos pixelPos, AngularDirection angularDirection) {
                final ProductNodeGroup<TiePointGrid> tiePointGroup = raster.getProduct().getTiePointGridGroup();
                final int x = (int) pixelPos.x;
                final int y = (int) pixelPos.y;

                angularDirection.azimuth = tiePointGroup.get("vaa").getPixelDouble(x, y);
                angularDirection.zenith = tiePointGroup.get("vza").getPixelDouble(x, y);

                return angularDirection;
            }

            @Override
            public float getElevation(PixelPos pixelPos) {
                return 0.0f;
            }

            @Override
            public boolean canGetSunDir() {
                return false;
            }

            @Override
            public boolean canGetViewDir() {
                final ProductNodeGroup<TiePointGrid> tiePointGroup = raster.getProduct().getTiePointGridGroup();
                return tiePointGroup.contains("vaa") && tiePointGroup.contains("vza");
            }

            @Override
            public boolean canGetElevation() {
                return false;
            }
        };
    }
}
