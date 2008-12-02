package org.esa.beam.dataio.smos;

import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.jai.ResolutionLevel;

import java.awt.image.RenderedImage;

class SmosMultiLevelSource extends AbstractMultiLevelSource {
    private final MultiLevelSource dggridMultiLevelSource;
    private final SmosFile smosFile;
    private final Band band;
    private final boolean scientific;
    private final int polMode;
    private final int fieldIndex;
    private final Number noDataValue;

    public SmosMultiLevelSource(MultiLevelSource dggridMultiLevelSource,
                                SmosFile smosFile,
                                Band band,
                                boolean scientific,
                                int polMode,
                                int fieldIndex,
                                Number noDataValue) {
        super(dggridMultiLevelSource.getModel());
        this.dggridMultiLevelSource = dggridMultiLevelSource;
        this.smosFile = smosFile;
        this.band = band;
        this.scientific = scientific;
        this.polMode = polMode;
        this.fieldIndex = fieldIndex;
        this.noDataValue = noDataValue;
    }


    @Override
    public RenderedImage createImage(int level) {
        return new SmosL1cOpImage(smosFile,
                                  band,
                                  scientific,
                                  polMode,
                                  fieldIndex,
                                  noDataValue,
                                  dggridMultiLevelSource.getImage(level),
                                  ResolutionLevel.create(getModel(), level));
    }

}
