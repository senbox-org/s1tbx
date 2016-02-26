/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.core.datamodel.quicklooks;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Product;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luis on 29/01/2016.
 */
public interface Thumbnail {

    final List<ThumbnailListener> listenerList = new ArrayList<>();

    boolean hasImage();

    boolean hasCachedImage();

    BufferedImage getImage(final ProgressMonitor pm);

    Product getProduct();

    default void addListener(final ThumbnailListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    default void removeListener(final ThumbnailListener listener) {
        listenerList.remove(listener);
    }

    default void notifyImageUpdated() {
        for (final ThumbnailListener listener : listenerList) {
            listener.notifyImageUpdated(this);
        }
    }

    public interface ThumbnailListener {
        void notifyImageUpdated(Thumbnail thumbnail);
    }
}
