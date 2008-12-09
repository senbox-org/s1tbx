package org.esa.beam.dataio.smos;

import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ResolutionLevel;

import java.awt.image.RenderedImage;

public class SmosMultiLevelSource extends AbstractMultiLevelSource {
    private final GridPointValueProvider gridPointValueProvider;
    private final MultiLevelSource dggridMultiLevelSource;
    private final RasterDataNode node;
    private final Number noDataValue;

    public SmosMultiLevelSource(GridPointValueProvider gridPointValueProvider, MultiLevelSource dggridMultiLevelSource,
                                RasterDataNode node, Number noDataValue) {
        super(dggridMultiLevelSource.getModel());
        this.gridPointValueProvider = gridPointValueProvider;
        this.dggridMultiLevelSource = dggridMultiLevelSource;
        this.node = node;
        this.noDataValue = noDataValue;
    }

    public GridPointValueProvider getGridPointValueProvider() {
        return gridPointValueProvider;
    }

    @Override
    public RenderedImage createImage(int level) {
        return new SmosOpImage(gridPointValueProvider, node, noDataValue, dggridMultiLevelSource.getImage(level),
                               ResolutionLevel.create(getModel(), level));
    }
}
