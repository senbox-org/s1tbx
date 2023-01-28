package org.esa.s1tbx.io.strix;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.esa.s1tbx.io.ceos.CEOSProductDirectory;
import org.esa.s1tbx.io.ceos.CEOSProductReader;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.engine_utilities.datamodel.Unit;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

public class StriXProductReader extends CEOSProductReader {

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public StriXProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected CEOSProductDirectory createProductDirectory(final VirtualDir productDir) {
        return new StriXProductDirectory(productDir);
    }

    DecodeQualification checkProductQualification(final Path path) {

        try {
            dataDir = createProductDirectory(createProductDir(path));

            final StriXProductDirectory dataDir = (StriXProductDirectory) this.dataDir;
            if (dataDir.isSTRIX()) {
                return DecodeQualification.INTENDED;
            }
            return DecodeQualification.UNABLE;

        } catch (Exception e) {
            return DecodeQualification.UNABLE;
        }
    }

    @Override
    protected void addQuicklooks(final Product product, final VirtualDir productDir) throws IOException {
        final String[] files = productDir.list("");
        if(files != null) {
            for (String file : files) {
                String name = file.toLowerCase();
                if (name.startsWith("brs") && name.endsWith(".png")) {
                    addQuicklook(product, Quicklook.DEFAULT_QUICKLOOK_NAME, productDir.getFile(file));
                    return;
                }
            }
        }
    }

    public void readTiePointGridRasterData(final TiePointGrid tpg,
                                           final int destOffsetX, final int destOffsetY,
                                           final int destWidth, final int destHeight,
                                           final ProductData destBuffer, final ProgressMonitor pm)
            throws IOException {
        final StriXProductDirectory strixDataDir = (StriXProductDirectory) this.dataDir;
        Rectangle destRect = new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight);
        strixDataDir.readTiePointGridRasterData(tpg, destRect, destBuffer, pm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        try {
            final StriXProductDirectory dataDir = (StriXProductDirectory) this.dataDir;
            final StriXImageFile imageFile = (StriXImageFile) dataDir.getImageFile(destBand);
            if (dataDir.isSLC()) {
                boolean oneOf2 = destBand.getUnit().equals(Unit.REAL) || !destBand.getName().startsWith("q");

                if (dataDir.getProductLevel() == StriXConstants.LEVEL1_0) {
                    imageFile.readBandRasterDataSLCByte(sourceOffsetX, sourceOffsetY,
                            sourceWidth, sourceHeight,
                            sourceStepX, sourceStepY,
                            destWidth,
                            destBuffer, oneOf2, pm);
                } else {
                    imageFile.readBandRasterDataSLCFloat(sourceOffsetX, sourceOffsetY,
                            sourceWidth, sourceHeight,
                            sourceStepX, sourceStepY,
                            destWidth,
                            destBuffer, oneOf2, pm);
                }
            } else {
                imageFile.readBandRasterDataShort(sourceOffsetX, sourceOffsetY,
                        sourceWidth, sourceHeight,
                        sourceStepX, sourceStepY,
                        destWidth,
                        destBuffer, pm);
            }

        } catch (Throwable e) {
            //handleReaderException(e);
        }
    }
}
