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
package org.esa.snap.core.util.jai;

import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.ImageUtils;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

//@todo 1 se/** - add (more) class documentation

public class JAIDebug extends Debug {

    public static final int F_ATTRIBUTES = 0x0001;
    public static final int F_TILEINFO = 0x0002;
    public static final int F_STATISTICS = 0x0004;
    public static final int F_PROPERTIES = 0x0008;
    public static final int F_ALL = F_ATTRIBUTES
                                    | F_TILEINFO
                                    | F_STATISTICS
                                    | F_PROPERTIES;


    private static final int INDENT_INCR = 4;

    private static int _defaultTraceMask = 0x0000;
    private static StringBuffer _stringBuffer = new StringBuffer();

    public static void setDefaultTraceMask(int traceMask) {
        _defaultTraceMask = traceMask;
    }

    public static int getDefaultTraceMask() {
        return _defaultTraceMask;
    }

    private static StringBuffer getStringBuffer() {
        _stringBuffer.ensureCapacity(1024);
        return _stringBuffer;
    }

    public static void trace(RenderedImage im) {
        if (isEnabled() && im != null) {
            trace("RenderedImage", im);
        }
    }

    public static void trace(String label, RenderedImage im) {
        if (isEnabled() && im != null) {
            trace(label, im, _defaultTraceMask);
        }
    }

    public static void trace(String label, RenderedImage im, int traceMask) {
        if (isEnabled() && im != null) {
            StringBuffer sb = getStringBuffer();
            synchronized (sb) {
                sb.setLength(0);
                traceObj(label, im, traceMask, sb, 0);
                trace(sb.toString());
            }
        }
    }

    private static void traceObj(String label,
                                 RenderedImage im,
                                 int traceMask,
                                 StringBuffer sb,
                                 int indent) {
        if (!isEnabled()) {
            return;
        }

        if (im != null) {
            traceObjStart(label, im, sb, indent);
            indent += INDENT_INCR;

            if (isFlagSet(traceMask, F_ATTRIBUTES)) {
                traceField("minX", im.getMinX(), sb, indent);
                traceField("minY", im.getMinY(), sb, indent);
                traceField("width", im.getWidth(), sb, indent);
                traceField("height", im.getHeight(), sb, indent);
                if (isFlagSet(traceMask, F_TILEINFO)) {
                    traceField("minTileX", im.getMinTileX(), sb, indent);
                    traceField("minTileY", im.getMinTileY(), sb, indent);
                    traceField("tileWidth", im.getTileWidth(), sb, indent);
                    traceField("tileHeight", im.getTileHeight(), sb, indent);
                    traceField("tileGridXOffset", im.getTileGridXOffset(), sb, indent);
                    traceField("tileGridYOffset", im.getTileGridYOffset(), sb, indent);
                    traceField("numXTiles", im.getNumXTiles(), sb, indent);
                    traceField("numYTiles", im.getNumYTiles(), sb, indent);
                }
            }

            if (isFlagSet(traceMask, F_STATISTICS)) {
                double[][] extrema = JAIUtils.getExtrema(im);
                if (extrema != null) {
                    appendLineStart(sb, indent);
                    sb.append("extrema = { ");
                    for (int i = 0; i < extrema[0].length; i++) {
                        if (i > 0) {
                            sb.append(", ");
                        }
                        sb.append("{ ");
                        sb.append(extrema[0][i]);
                        sb.append(", ");
                        sb.append(extrema[1][i]);
                        sb.append(" }");
                    }
                    sb.append(" }");
                    appendLineEnd(sb);
                } else {
                    traceField("extrema", (Object) null, sb, indent);
                }
            }

            if (isFlagSet(traceMask, F_PROPERTIES)) {
                String[] propertyNames = im.getPropertyNames();
                if (propertyNames != null) {
                    appendLineStart(sb, indent);
                    sb.append("properties = { ");
                    appendLineEnd(sb);
                    for (int i = 0; i < propertyNames.length; i++) {
                        appendLineStart(sb, indent + INDENT_INCR);
                        sb.append(propertyNames[i]);
                        sb.append(" = ");
                        Object propertyValue = im.getProperty(propertyNames[i]);
                        if (propertyValue != null) {
                            sb.append(propertyValue.toString());
                        } else {
                            sb.append("(null)");
                        }
                        appendLineEnd(sb);
                    }
                    appendLineStart(sb, indent);
                    sb.append(" }");
                    appendLineEnd(sb);
                } else {
                    traceField("properties", (String) null, sb, indent);
                }
            }

            if (im instanceof BufferedImage) {
                BufferedImage bim = (BufferedImage) im;
                traceObj("raster", bim.getRaster(), traceMask, sb, indent);
            } else {
                traceObj("sampleModel", im.getSampleModel(), traceMask, sb, indent);
            }

            traceObj("colorModel", im.getColorModel(), traceMask, sb, indent);

            indent -= INDENT_INCR;
            traceObjEnd(sb, indent);
        } else {
            traceField(label, im, sb, indent);
        }
    }

    private static void traceObj(String label,
                                 Raster ras,
                                 int traceMask,
                                 StringBuffer sb,
                                 int indent) {

        if (!isEnabled()) {
            return;
        }

        if (ras != null) {

            traceObjStart(label, ras, sb, indent);
            indent += INDENT_INCR;

            if (isFlagSet(traceMask, F_ATTRIBUTES)) {
                traceField("width", ras.getWidth(), sb, indent);
                traceField("height", ras.getHeight(), sb, indent);
                traceField("numBands", ras.getNumDataElements(), sb, indent);
                traceField("numDataElements", ras.getNumDataElements(), sb, indent);
                traceField("transferType", ImageUtils.getDataTypeName(ras.getTransferType()), sb, indent);
            }

            traceObj("dataBuffer", ras.getDataBuffer(), traceMask, sb, indent);
            traceObj("sampleModel", ras.getSampleModel(), traceMask, sb, indent);

            indent -= INDENT_INCR;
            traceObjEnd(sb, indent);
        } else {
            traceField(label, ras, sb, indent);
        }
    }

    private static void traceObj(String label,
                                 DataBuffer buf,
                                 int traceMask,
                                 StringBuffer sb,
                                 int indent) {
        if (!isEnabled()) {
            return;
        }

        if (buf != null) {
            traceObjStart(label, buf, sb, indent);
            indent += INDENT_INCR;

            if (isFlagSet(traceMask, F_ATTRIBUTES)) {
                traceField("dataType", ImageUtils.getDataTypeName(buf.getDataType()), sb, indent);
                traceField("size", buf.getSize(), sb, indent);
                traceField("numBanks", buf.getNumBanks(), sb, indent);
                traceField("offsets", buf.getOffsets(), sb, indent);
            }

            indent -= INDENT_INCR;
            traceObjEnd(sb, indent);
        } else {
            traceField(label, buf, sb, indent);
        }
    }


    private static void traceObj(String label,
                                 SampleModel sm,
                                 int traceMask,
                                 StringBuffer sb,
                                 int indent) {
        if (!isEnabled()) {
            return;
        }

        if (sm != null) {
            traceObjStart(label, sm, sb, indent);
            indent += INDENT_INCR;

            if (isFlagSet(traceMask, F_ATTRIBUTES)) {
                traceField("width", sm.getWidth(), sb, indent);
                traceField("height", sm.getHeight(), sb, indent);
                traceField("numBands", sm.getNumBands(), sb, indent);
                traceField("numDataElements", sm.getNumDataElements(), sb, indent);
                traceField("transferType", ImageUtils.getDataTypeName(sm.getTransferType()), sb, indent);
            }

            indent -= INDENT_INCR;
            traceObjEnd(sb, indent);
        } else {
            traceField(label, sm, sb, indent);
        }
    }

    private static void traceObj(String label,
                                 ColorModel cm,
                                 int traceMask,
                                 StringBuffer sb,
                                 int indent) {
        if (!isEnabled()) {
            return;
        }

        if (cm != null) {
            traceObjStart(label, cm, sb, indent);
            indent += INDENT_INCR;

            if (isFlagSet(traceMask, F_ATTRIBUTES)) {
                traceField("numComponents", cm.getNumComponents(), sb, indent);
                traceField("numColorComponents", cm.getNumColorComponents(), sb, indent);
            }

            traceObj("colorSpace", cm.getColorSpace(), traceMask, sb, indent);

            indent -= INDENT_INCR;
            traceObjEnd(sb, indent);
        } else {
            traceField(label, sb, sb, indent);
        }
    }

    private static void traceObj(String label,
                                 ColorSpace cs,
                                 int traceMask,
                                 StringBuffer sb,
                                 int indent) {
        if (!isEnabled()) {
            return;
        }

        if (cs != null) {
            traceObjStart(label, cs, sb, indent);
            indent += INDENT_INCR;

            if (isFlagSet(traceMask, F_ATTRIBUTES)) {
                traceField("type", ImageUtils.getColorSpaceName(cs.getType()), sb, indent);
                traceField("numComponents", cs.getNumComponents(), sb, indent);

/*
                appendLineStart(sb, indent);
                sb.append("componentNames = { ");
                for (int i = 0; i < cs.getNumComponents(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(cs.getTypeID(i));
                }
                sb.append(" }");
                appendLineEnd(sb);
                */
            }

            indent -= INDENT_INCR;
            traceObjEnd(sb, indent);
        } else {
            traceField(label, cs, sb, indent);
        }
    }

    private static void traceObjStart(String label, Object value, StringBuffer sb, int indent) {
        if (!isEnabled()) {
            return;
        }

        appendLineStart(sb, indent);
        sb.append("<");
        sb.append(label);
        sb.append("> (class ");
        sb.append(value.getClass().getName());
        sb.append(") = {");
        appendLineEnd(sb);
    }

    private static void traceObjEnd(StringBuffer sb, int indent) {
        if (!isEnabled()) {
            return;
        }

        appendLineStart(sb, indent);
        sb.append('}');
        appendLineEnd(sb);
    }

    private static void traceField(String name, int value, StringBuffer sb, int indent) {
        if (!isEnabled()) {
            return;
        }

        appendLineStart(sb, indent);
        sb.append(name);
        sb.append(" = ");
        sb.append(value);
        appendLineEnd(sb);
    }

    private static void traceField(String name, String value, StringBuffer sb, int indent) {
        if (!isEnabled()) {
            return;
        }

        appendLineStart(sb, indent);
        sb.append(name);
        sb.append(" = ");
        sb.append(value != null ? value : "(null)");
        appendLineEnd(sb);
    }

    private static void traceField(String name, int[] values, StringBuffer sb, int indent) {
        if (!isEnabled()) {
            return;
        }

        appendLineStart(sb, indent);
        sb.append("offsets = { ");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append(" }");
        appendLineEnd(sb);
    }

    private static void traceField(String name, Object value, StringBuffer sb, int indent) {
        if (!isEnabled()) {
            return;
        }

        appendLineStart(sb, indent);
        sb.append(name);
        sb.append(" = ");
        sb.append(value != null ? value.toString() : "(null)");
        appendLineEnd(sb);
    }

    private static void appendLineStart(StringBuffer sb, int indent) {
        if (!isEnabled()) {
            return;
        }
        for (int i = 0; i < indent; i++) {
            sb.append(' ');
        }
    }

    private static void appendLineEnd(StringBuffer sb) {
        if (!isEnabled()) {
            return;
        }
        sb.append('\n');
    }

    private static boolean isFlagSet(int mask, int flag) {
        return (mask & flag) != 0;
    }

    private JAIDebug() {
    }


}


