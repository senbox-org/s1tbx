package org.esa.beam.glevel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.beam.jai.TiledFileOpImage;
import org.esa.beam.util.StringUtils;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;


public class TiledFileMultiLevelSource extends AbstractMultiLevelSource {

    private final File imageDir;
    private final Properties imageProperties;
    boolean visualDebug;

    public static MultiLevelSource create(File imageDir, boolean visualDebug) throws IOException {
        Assert.notNull(imageDir);
        final Properties imageProperties = new Properties();
        imageProperties.load(new FileReader(new File(imageDir, "image.properties")));
        int levelCount = Integer.parseInt(imageProperties.getProperty("numLevels"));
        int sourceWidth = Integer.parseInt(imageProperties.getProperty("width"));
        int sourceHeight = Integer.parseInt(imageProperties.getProperty("height"));
        final String s = imageProperties.getProperty("i2mTransform");
        AffineTransform i2mTransform = new AffineTransform();
        if (s != null) {
            try {
                double[] matrix = StringUtils.toDoubleArray(s, ",");
                if (matrix.length == 6) {
                    i2mTransform = new AffineTransform(matrix);
                }
            } catch (IllegalArgumentException e) {
                // may be thrown by StringUtils.toDoubleArray(), use identity instead
            }
        }
        final MultiLevelModel model = new DefaultMultiLevelModel(levelCount, i2mTransform, sourceWidth, sourceHeight);
        return new TiledFileMultiLevelSource(model, imageDir, imageProperties, visualDebug);
    }

    public TiledFileMultiLevelSource(MultiLevelModel model, File imageDir, Properties imageProperties, boolean visualDebug) {
        super(model);
        this.imageDir = imageDir;
        this.imageProperties = imageProperties;
        this.visualDebug = visualDebug;
    }

    @Override
    public RenderedImage createImage(int level) {
        PlanarImage image;
        try {
            image = TiledFileOpImage.create(new File(imageDir, level + ""), imageProperties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (visualDebug) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(image);
            pb.add(new int[]{0x0F});
            image = JAI.create("andconst", pb, null);

            pb = new ParameterBlock();
            pb.addSource(image);
            pb.add(new double[]{16});
            pb.add(new double[]{0});
            image = JAI.create("rescale", pb, null);

            pb = new ParameterBlock();
            pb.addSource(image);
            pb.add(DataBuffer.TYPE_BYTE);
            image = JAI.create("format", pb, null);
        }
        return image;
    }
}