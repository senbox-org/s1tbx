package org.esa.beam.dataio.smos;

import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ResolutionLevel;

import java.awt.image.RenderedImage;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.Shape;
import java.util.ArrayList;

public class SmosMultiLevelSource extends AbstractMultiLevelSource {
    private final GridPointValueProvider valueProvider;
    private final MultiLevelSource dggridMultiLevelSource;
    private final RasterDataNode node;
    private final Area modelRegion;

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

    @Override
    public RenderedImage createImage(int level) {
        final AffineTransform m2i = getModel().getModelToImageTransform(level);
        final Area levelRegion = modelRegion.createTransformedArea(m2i);

        return new SmosOpImage(valueProvider, node, dggridMultiLevelSource.getImage(level), ResolutionLevel.create(getModel(), level), levelRegion);
    }
}
