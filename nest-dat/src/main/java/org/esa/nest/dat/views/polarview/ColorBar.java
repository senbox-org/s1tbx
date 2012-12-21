/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.views.polarview;

import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


class ColorBar implements ImageProducer {
    private final ColourScale colourScale;
    private ColorModel model;
    private static final Dimension barSize = new Dimension(24, 256);
    private static final byte barPixels[] = new byte[barSize.height];
    private static final int barRGBPixels[] = new int[barSize.height];

    private final Dimension imageSize = new Dimension(barSize.width, barSize.height);
    private final Rectangle imageArea = new Rectangle(imageSize);
    protected static final Point p0 = new Point(0, 0);
    private final Hashtable properties = new Hashtable(1);
    private final Vector<ImageConsumer> theConsumers = new Vector<ImageConsumer>(1);
    private static final int hints = 3 & 0xffffffef;

    static {
        int p = 0;
        final float scale = 0xffffff / (barSize.height - 1);
        for (int i = barSize.height - 1; i >= 0; i--) {
            barPixels[p] = (byte) i;
            barRGBPixels[p] = (int) ((float) p * scale);
            p++;
        }
    }

    public ColorBar(ColourScale colourScale) {
        this.colourScale = colourScale;
        colourScale.addColoredObject(this);
        model = colourScale.getColorModel();
    }

    public synchronized boolean isConsumer(ImageConsumer ic) {
        return theConsumers.contains(ic);
    }

    public synchronized void removeConsumer(ImageConsumer ic) {
        theConsumers.removeElement(ic);
    }

    private synchronized void removeAllConsumers() {
        for (Enumeration elem = theConsumers.elements(); elem.hasMoreElements();) {
            ImageConsumer ic = (ImageConsumer) elem.nextElement();
            ic.imageComplete(3);
            if (isConsumer(ic))
                ic.imageComplete(1);
        }

        theConsumers.removeAllElements();
    }

    public void startProduction(ImageConsumer ic) {
        addConsumer(ic);
    }

    public void requestTopDownLeftRightResend(ImageConsumer imageconsumer) {
    }

    private void initConsumer(ImageConsumer ic) {
        if (isConsumer(ic))
            ic.setDimensions(imageSize.width, imageSize.height);
        if (isConsumer(ic))
            ic.setProperties(properties);
        if (isConsumer(ic))
            ic.setColorModel(model);
        if (isConsumer(ic))
            ic.setHints(hints);
    }

    private synchronized Enumeration getConsumers() {
        return theConsumers.elements();
    }

    private synchronized void addConsumerToList(ImageConsumer ic) {
        theConsumers.addElement(ic);
    }

    @Override
    protected void finalize() {
        removeAllConsumers();
    }

    public synchronized void addConsumer(ImageConsumer ic) {
        if (isConsumer(ic))
            return;
        addConsumerToList(ic);
        try {
            initConsumer(ic);
            deliverPixels(ic, imageArea);
            if (isConsumer(ic))
                ic.imageComplete(2);
        }
        catch (Exception e) {
            if (isConsumer(ic))
                ic.imageComplete(1);
        }
    }

    private synchronized void resend() {
        resend(imageArea);
    }

    void updatedColorMap() {
        model = colourScale.getColorModel();
        resendColorModel();
        resend();
    }

    public void updatedColorScale() {
        updatedColorMap();
    }

    private synchronized void resend(Rectangle area) {
        final Enumeration con = getConsumers();
        while (con.hasMoreElements()) {
            final ImageConsumer ic = (ImageConsumer) con.nextElement();
            try {
                deliverPixels(ic, area);
                if (isConsumer(ic))
                    ic.imageComplete(2);
            }
            catch (Exception e) {
                if (isConsumer(ic))
                    ic.imageComplete(1);
            }
        }
    }

    private synchronized void resendColorModel() {
        ImageConsumer ic;
        for (Enumeration elem = getConsumers(); elem.hasMoreElements(); ic.setColorModel(model))
            ic = (ImageConsumer) elem.nextElement();
    }

    public static Dimension getBarSize() {
        return new Dimension(barSize);
    }

    private void deliverPixels(ImageConsumer ic, Rectangle area) {
        if (model instanceof DirectColorModel) {
            for (int i = 0; i < barSize.width; i++) {
                ic.setPixels(i, 0, 1, barSize.height, model, barRGBPixels, 0, 1);
            }
        } else {
            for (int i = 0; i < barSize.width; i++) {
                ic.setPixels(i, 0, 1, barSize.height, model, barPixels, 0, 1);
            }
        }
    }
}
