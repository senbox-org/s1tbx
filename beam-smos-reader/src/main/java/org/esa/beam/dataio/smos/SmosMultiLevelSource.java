package org.esa.beam.dataio.smos;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.jai.ResolutionLevel;

import java.awt.image.RenderedImage;

class SmosMultiLevelSource extends AbstractMultiLevelSource {
    private final MultiLevelSource dggridMultiLevelSource;
    private final SmosFile smosFile;
    private final Band band;
    private final int fieldIndex;
    private final Number noDataValue;

    public SmosMultiLevelSource(MultiLevelSource dggridMultiLevelSource,
                                 SmosFile smosFile,
                                 Band band,
                                 int fieldIndex,
                                 Number noDataValue) {
        super(dggridMultiLevelSource.getModel());
        this.dggridMultiLevelSource = dggridMultiLevelSource;
        this.smosFile = smosFile;
        this.band = band;
        this.fieldIndex = fieldIndex;
        this.noDataValue = noDataValue;
    }



    @Override
        public RenderedImage createImage(int level) {
        return new SmosBandOpImage(smosFile,
                                     band,
                                     fieldIndex,
                                     noDataValue,
                                     dggridMultiLevelSource.getImage(level),
                                     ResolutionLevel.create(getModel(), level));
    }

}
