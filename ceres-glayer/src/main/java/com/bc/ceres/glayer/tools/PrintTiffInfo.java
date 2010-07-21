/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.glayer.tools;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import com.sun.media.jai.codec.TIFFDirectory;
import com.sun.media.jai.codec.TIFFField;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.TIFFDescriptor;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;

public class PrintTiffInfo {
    public static void main(String[] args) throws IOException {
        final String sourceImageName = args[0];

        Tools.configureJAI();
        final FileSeekableStream seekableStream = new FileSeekableStream(sourceImageName);
        final TIFFDecodeParam tiffDecodeParam = new TIFFDecodeParam();
        RenderedImage image = TIFFDescriptor.create(seekableStream, tiffDecodeParam, 0, null);
        ParameterBlock pb = new ParameterBlock();
        pb.add(seekableStream);

        TIFFDecodeParam param = new TIFFDecodeParam();
        pb.add(param);

        for (int i = 0; ; i++) {
            RenderedOp op = JAI.create("tiff", pb);
            TIFFDirectory dir = (TIFFDirectory) op.getProperty("tiff_directory");
            dump(i, dir);
            long nextOffset = dir.getNextIFDOffset();
            if (nextOffset != 0) {
                param.setIFDOffset(nextOffset);
            } else {
                break;
            }
        }
    }

    private static void dump(int pageIndex, TIFFDirectory dir) {

        final int numEntries = dir.getNumEntries();
        System.out.println("page." + pageIndex + ".numEntries = " + numEntries);
        final TIFFField[] tiffFields = dir.getFields();
        for (int i = 0; i < tiffFields.length; i++) {
            TIFFField tiffField = tiffFields[i];
            final int tag = tiffField.getTag();
            final int type = tiffField.getType();
            final int count = tiffField.getCount();
            System.out.printf("  field.%d: tag %d, type %d, count %d\n", i, tag, type, count);
        }

    }
}