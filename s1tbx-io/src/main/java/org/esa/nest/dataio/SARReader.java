/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.nest.dataio;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.RuntimeContext;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.util.ExceptionLog;

import java.io.IOException;

/**
 * Common functions for readers
 */
public abstract class SARReader extends AbstractProductReader {

    private static String[] elemsToKeep = {"Abstracted_Metadata", "MAIN_PROCESSING_PARAMS_ADS", "DSD", "SPH", "lutSigma"};

    protected SARReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    protected abstract Product readProductNodesImpl() throws IOException;

    protected abstract void readBandRasterDataImpl(int sourceOffsetX,
                                                   int sourceOffsetY,
                                                   int sourceWidth,
                                                   int sourceHeight,
                                                   int sourceStepX,
                                                   int sourceStepY,
                                                   Band destBand,
                                                   int destOffsetX,
                                                   int destOffsetY,
                                                   int destWidth,
                                                   int destHeight,
                                                   ProductData destBuffer,
                                                   ProgressMonitor pm) throws IOException;

    protected static void setQuicklookBandName(final Product product) {

        final Band[] bands = product.getBands();
        for (Band band : bands) {
            String unit = band.getUnit();
            if (unit != null && (unit.contains("intensity") || unit.contains("amplitude"))) {
                product.setQuicklookBandName(band.getName());
                return;
            }
        }
        // if not intensity bands found find first amplitude
        for (Band band : bands) {
            String unit = band.getUnit();
            if (unit != null && (unit.contains("amplitude"))) {
                product.setQuicklookBandName(band.getName());
                return;
            }
        }
    }

    public static void createVirtualIntensityBand(final Product product, final Band band, final String countStr) {
        final String expression = band.getName() + " * " + band.getName();

        final VirtualBand virtBand = new VirtualBand("Intensity" + countStr,
                ProductData.TYPE_FLOAT32,
                band.getSceneRasterWidth(),
                band.getSceneRasterHeight(),
                expression);
        virtBand.setUnit(Unit.INTENSITY);
        virtBand.setDescription("Intensity from complex data");
        virtBand.setNoDataValueUsed(true);
        product.addBand(virtBand);
    }

    public static String findPolarizationInBandName(final String bandName) {

        final String id = bandName.toUpperCase();
        if (id.contains("HH") || id.contains("H/H") || id.contains("H-H"))
            return "HH";
        else if (id.contains("VV") || id.contains("V/V") || id.contains("V-V"))
            return "VV";
        else if (id.contains("HV") || id.contains("H/V") || id.contains("H-V"))
            return "HV";
        else if (id.contains("VH") || id.contains("V/H") || id.contains("V-H"))
            return "VH";

        return null;
    }

    public static void discardUnusedMetadata(final Product product) {
        final String dicardUnusedMetadata = RuntimeContext.getModuleContext().getRuntimeConfig().
                getContextProperty("discard.unused.metadata");
        if (dicardUnusedMetadata.equalsIgnoreCase("true")) {
            removeUnusedMetadata(AbstractMetadata.getOriginalProductMetadata(product));
        }
    }

    private static void removeUnusedMetadata(final MetadataElement root) {
        final MetadataElement[] elems = root.getElements();
        for (MetadataElement elem : elems) {
            final String name = elem.getName();
            boolean keep = false;
            for (String toKeep : elemsToKeep) {
                if (name.equals(toKeep)) {
                    keep = true;
                    break;
                }
            }
            if (!keep) {
                root.removeElement(elem);
                elem.dispose();
            }
        }
    }

    public void handleReaderException(final Throwable e) throws IOException {

        String message = this.toString() + ":\n";
        message = message.replace("[input", "\n[input");
        if (e.getMessage() != null)
            message += e.getMessage();
        else
            message += e.toString();

        if (Boolean.getBoolean("sendErrorOnException")) {
            ExceptionLog.log(message);
        }

        BeamLogManager.getSystemLogger().severe(message);
        throw new IOException(message);
    }
}
