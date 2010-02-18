/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui;

import org.esa.beam.framework.ui.product.ProductSceneView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.5.2
 */
class PixelInfoUpdateService {

    private final PixelInfoViewModelUpdater modelUpdater;
    private final ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> updaterFuture;
    private PixelInfoState state;
    private boolean needUpdate;
    private int numUnchangedStates;
    private Runnable updaterRunnable;

    PixelInfoUpdateService(PixelInfoViewModelUpdater modelUpdater) {
        this.modelUpdater = modelUpdater;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        state = PixelInfoState.INVALID;
        updaterRunnable = new UpdaterRunnable();
    }

    synchronized void updateState(ProductSceneView view, int pixelX, int pixelY, int level, boolean pixelPosValid) {
        if (!state.equals(view, pixelX, pixelY, level, pixelPosValid)) {
            state = new PixelInfoState(view, pixelX, pixelY, level, pixelPosValid);
            needUpdate = true;
            assertTimerStarted();
        }
    }

    synchronized void requestUpdate() {
        if (state == PixelInfoState.INVALID) {
            return;
        }
        needUpdate = true;
        assertTimerStarted();
    }

    synchronized void clearState() {
        state = PixelInfoState.INVALID;
    }


    private void assertTimerStarted() {
        if (updaterFuture == null) {
            updaterFuture = scheduledExecutorService.scheduleWithFixedDelay(updaterRunnable, 100, 100, TimeUnit.MILLISECONDS);
        }
    }

    private synchronized void stopTimer() {
        if (updaterFuture != null) {
            updaterFuture.cancel(true);
            updaterFuture = null;
        }
        clearState();
    }

    private class UpdaterRunnable implements Runnable {
        @Override
        public void run() {
            if (state == PixelInfoState.INVALID) {
                return;
            }
            if (needUpdate) {
                numUnchangedStates = 0;
                needUpdate = false;
                try {
                    modelUpdater.update(state);
                } catch (Throwable ignored) {

                }
            } else {
                numUnchangedStates++;
                if (numUnchangedStates > 100) {
                    stopTimer();
                }
            }
        }
    }
}
