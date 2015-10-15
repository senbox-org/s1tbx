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
package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;

public class RGBChannelDef implements Cloneable {

    private final static int R = 0;
    private final static int G = 1;
    private final static int B = 2;
    private final static int A = 3;

    private ChannelDef[] channelDefs;

    public RGBChannelDef() {
        this.channelDefs = new ChannelDef[]{
                new ChannelDef(),
                new ChannelDef(),
                new ChannelDef(),
                new ChannelDef()
        };
    }

    public RGBChannelDef(String[] bandNames) {
        this();
        setSourceNames(bandNames);
    }

    public String getSourceName(int index) {
        return channelDefs[index].sourceName;
    }

    public void setSourceName(int index, String sourceName) {
        Assert.notNull(sourceName, "sourceName");
        channelDefs[index].sourceName = sourceName;
    }

    public String[] getSourceNames() {
        if (isAlphaUsed()) {
            return new String[]{
                    getSourceName(R),
                    getSourceName(G),
                    getSourceName(B),
                    getSourceName(A)
            };
        } else {
            return new String[]{
                    getSourceName(R),
                    getSourceName(G),
                    getSourceName(B)
            };
        }
    }

    public void setSourceNames(String[] bandNames) {
        Assert.notNull(bandNames, "bandNames");
        setSourceName(R, bandNames[R]);
        setSourceName(G, bandNames[G]);
        setSourceName(B, bandNames[B]);
        setSourceName(A, bandNames.length > 3 ? bandNames[A] : "");
    }


    public boolean isAlphaUsed() {
        return !"".equals(getSourceName(A));
    }

    public boolean isGammaUsed(int index) {
        final double gamma = getGamma(index);
        final double v = gamma - 1.0;
        return Math.abs(v) > 1e-5;
    }

    public double getGamma(int index) {
        return channelDefs[index].gamma;
    }

    public void setGamma(int index, double gamma) {
        Assert.argument(gamma >= 0.0, "gamma");
        channelDefs[index].gamma = gamma;
    }

    public double getMinDisplaySample(int index) {
        return channelDefs[index].minDisplaySample;
    }

    public void setMinDisplaySample(int index, double min) {
        channelDefs[index].minDisplaySample = min;
    }

    public double getMaxDisplaySample(int index) {
        return channelDefs[index].maxDisplaySample;
    }

    public void setMaxDisplaySample(int index, double max) {
        channelDefs[index].maxDisplaySample = max;
    }

    @Override
    public final Object clone() {
        try {
            final RGBChannelDef copy = (RGBChannelDef) super.clone();
            copy.channelDefs = channelDefs.clone();
            for (int i = 0; i < channelDefs.length; i++) {
                copy.channelDefs[i] = (ChannelDef) channelDefs[i].clone();
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ChannelDef implements Cloneable {
        private String sourceName;
        private double minDisplaySample;
        private double maxDisplaySample;
        private double gamma;

        private ChannelDef() {
            this("", 0, 1, 1);
        }

        private ChannelDef(String sourceName, double minDisplaySample, double maxDisplaySample, double gamma) {
            this.sourceName = sourceName;
            this.gamma = gamma;
            this.minDisplaySample = minDisplaySample;
            this.maxDisplaySample = maxDisplaySample;
        }

        @Override
        public final Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
