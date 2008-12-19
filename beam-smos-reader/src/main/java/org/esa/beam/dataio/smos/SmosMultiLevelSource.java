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
    private Area modelRegion;

    public SmosMultiLevelSource(GridPointValueProvider valueProvider, MultiLevelSource dggridMultiLevelSource,
                                RasterDataNode node, Area modelRegion) {
        super(dggridMultiLevelSource.getModel());

        this.valueProvider = valueProvider;
        this.dggridMultiLevelSource = dggridMultiLevelSource;
        this.node = node;
        this.modelRegion = modelRegion;
    }

    public GridPointValueProvider getValueProvider() {
        return valueProvider;
    }

    public synchronized void setModelRegion(Area modelRegion) {
        this.modelRegion = modelRegion;
    }

    @Override
    public RenderedImage createImage(int level) {
        final Area levelRegion = modelRegion.createTransformedArea(getModel().getModelToImageTransform(level));
  
        return new SmosOpImage(valueProvider, node, dggridMultiLevelSource.getImage(level),
                               ResolutionLevel.create(getModel(), level), levelRegion);
    }
}
