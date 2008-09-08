package org.esa.beam.jai;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelModel;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class ResolutionLevel {
    public final static ResolutionLevel MAXRES = new ResolutionLevel(0, 1.0);

    private final int index;
    private final double scale;

    public static ResolutionLevel create(MultiLevelModel model, int level) {
        return new ResolutionLevel(level, model.getScale(level));
    }

    public ResolutionLevel(int index, double scale) {
        Assert.argument(index >= 0, "index >= 0");
        Assert.argument(scale >= 1.0, "scale >= 1.0");
        this.index = index;
        this.scale = scale;
    }

    public int getIndex() {
        return index;
    }

    public double getScale() {
        return scale;
    }
}
