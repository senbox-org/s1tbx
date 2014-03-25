package org.esa.beam.visat.toolviews.spectrum;

import com.bc.ceres.glayer.support.ImageLayer;
import java.awt.event.MouseEvent;
import javax.swing.SwingWorker;
import org.esa.beam.framework.ui.PixelPositionListener;

public class CursorSpectrumPixelPositionListener implements PixelPositionListener {

    private final SpectrumToolView toolView;
    private final WorkerChain workerChain;
    private final WorkerChainSupport support;

    public CursorSpectrumPixelPositionListener(SpectrumToolView toolView) {
        this.toolView = toolView;
        workerChain = new WorkerChain();
        support = new WorkerChainSupport() {
            @Override
            public void removeWorkerAndStartNext(SwingWorker worker) {
                workerChain.removeCurrentWorkerAndExecuteNext(worker);
            }
        };
    }

    @Override
    public void pixelPosChanged(ImageLayer imageLayer,
                                int pixelX,
                                int pixelY,
                                int currentLevel,
                                boolean pixelPosValid,
                                MouseEvent e) {
        CursorSpectraUpdater worker = new CursorSpectraUpdater(pixelPosValid, pixelX, pixelY, currentLevel, e.isShiftDown(), support);
        workerChain.setOrExecuteNextWorker(worker, false);
    }

    @Override
    public void pixelPosNotAvailable() {
        CursorSpectraRemover worker = new CursorSpectraRemover(support);
        workerChain.setOrExecuteNextWorker(worker, false);
    }

    private boolean shouldUpdateCursorPosition() {
        return toolView.isVisible() && toolView.isShowingCursorSpectrum();
    }

    private class CursorSpectraRemover extends SwingWorker<Void, Void> {

        private final WorkerChainSupport support;

        CursorSpectraRemover(WorkerChainSupport support) {
            this.support = support;
        }

        @Override
        protected Void doInBackground() throws Exception {
            if (shouldUpdateCursorPosition()) {
                toolView.removeCursorSpectraFromDataset();
            }
            return null;
        }

        @Override
        protected void done() {
            toolView.updateChart();
            support.removeWorkerAndStartNext(this);
        }
    }

    private class CursorSpectraUpdater extends SwingWorker<Void, Void> {

        private final boolean pixelPosValid;
        private final int pixelX;
        private final int pixelY;
        private final int currentLevel;
        private final boolean adjustAxes;
        private final WorkerChainSupport support;

        CursorSpectraUpdater(boolean pixelPosValid, int pixelX, int pixelY, int currentLevel, boolean adjustAxes,
                             WorkerChainSupport support) {
            this.pixelPosValid = pixelPosValid;
            this.pixelX = pixelX;
            this.pixelY = pixelY;
            this.currentLevel = currentLevel;
            this.adjustAxes = adjustAxes;
            this.support = support;
        }

        @Override
        protected Void doInBackground() throws Exception {
            if (pixelPosValid) {
                if (shouldUpdateCursorPosition()) {
                    Waiter waiter = new Waiter();
                    waiter.execute();
                    toolView.updateData(pixelX, pixelY, currentLevel);
                    waiter.cancel(true);
                }
            } else if (toolView.hasValidCursorPosition()) {
                toolView.removeCursorSpectraFromDataset();
            }
            return null;
        }

        @Override
        protected void done() {
            toolView.updateChart(adjustAxes);
            support.removeWorkerAndStartNext(this);
        }
    }

    private class Waiter extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() throws Exception {
            Thread.sleep(1000);
            return null;
        }

        @Override
        protected void done() {
            toolView.setPrepareForUpdateMessage();
        }
    }

    //todo copied (and changed very slightly) from time-series-tool: Move to BEAM or Ceres
    static interface WorkerChainSupport {

        void removeWorkerAndStartNext(SwingWorker worker);
    }

}