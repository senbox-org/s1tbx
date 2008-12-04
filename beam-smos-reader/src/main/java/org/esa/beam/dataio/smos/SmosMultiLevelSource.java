package org.esa.beam.dataio.smos;

import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.jai.ResolutionLevel;

import java.awt.image.RenderedImage;

class SmosMultiLevelSource extends AbstractMultiLevelSource {
    private final GridPointValueProvider provider;
    private final MultiLevelSource dggridMultiLevelSource;
    private final Band band;
    private final Number noDataValue;

    public SmosMultiLevelSource(GridPointValueProvider provider, MultiLevelSource dggridMultiLevelSource,
                                Band band, Number noDataValue) {
        super(dggridMultiLevelSource.getModel());
        this.provider = provider;
        this.dggridMultiLevelSource = dggridMultiLevelSource;
        this.band = band;
        this.noDataValue = noDataValue;
    }


    @Override
    public RenderedImage createImage(int level) {
        return new SmosOpImage(provider, band, noDataValue, dggridMultiLevelSource.getImage(level),
                                 ResolutionLevel.create(getModel(), level));
    }
}
