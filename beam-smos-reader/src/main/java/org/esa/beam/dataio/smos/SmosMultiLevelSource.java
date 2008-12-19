package org.esa.beam.dataio.smos;

import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ResolutionLevel;

import javax.media.jai.ROI;
import java.awt.geom.Area;
import java.awt.image.RenderedImage;

public class SmosMultiLevelSource extends AbstractMultiLevelSource {
    private final GridPointValueProvider valueProvider;
    private final MultiLevelSource dggridMultiLevelSource;
    private final RasterDataNode node;

    public SmosMultiLevelSource(GridPointValueProvider valueProvider, MultiLevelSource dggridMultiLevelSource,
                                RasterDataNode node) {
        super(dggridMultiLevelSource.getModel());

        this.valueProvider = valueProvider;
        this.dggridMultiLevelSource = dggridMultiLevelSource;
        this.node = node;
    }

    public GridPointValueProvider getValueProvider() {
        return valueProvider;
    }

    @Override
    public RenderedImage createImage(int level) {
        final Area modelRegion = valueProvider.getRegion();
        final Area levelRegion = modelRegion.createTransformedArea(getModel().getModelToImageTransform(level));
  
        return new SmosOpImage(valueProvider, node, dggridMultiLevelSource.getImage(level),
                               ResolutionLevel.create(getModel(), level), levelRegion);
    }
}
