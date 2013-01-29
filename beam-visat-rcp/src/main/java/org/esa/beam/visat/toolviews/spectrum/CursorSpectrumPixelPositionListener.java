package org.esa.beam.visat.toolviews.spectrum;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.ui.PixelPositionListener;

import javax.swing.SwingWorker;
import java.awt.event.MouseEvent;

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
        SpectrumToolViewUpdater worker = new SpectrumToolViewUpdater(pixelPosValid, pixelX, pixelY, currentLevel, e.isShiftDown(), support);
        workerChain.setOrExecuteNextWorker(worker, false);
    }

    @Override
    public void pixelPosNotAvailable() {
        SpectrumToolViewDisabler worker = new SpectrumToolViewDisabler(support);
        workerChain.setOrExecuteNextWorker(worker, false);
    }

    private boolean isActive() {
        return toolView.isVisible() && toolView.isShowingCursorSpectrum() && toolView.getSpectraDiagram() != null;
    }

    private class SpectrumToolViewDisabler extends SwingWorker<Void, Void> {

        private final WorkerChainSupport support;

        SpectrumToolViewDisabler(WorkerChainSupport support) {
            this.support = support;
        }

        @Override
        protected Void doInBackground() throws Exception {
            if (isActive()) {
                toolView.getSpectraDiagram().removeCursorSpectrumGraph();
                toolView.getDiagramCanvas().repaint();
            }
            return null;
        }

        @Override
        protected void done() {
            support.removeWorkerAndStartNext(this);
        }
    }

    private class SpectrumToolViewUpdater extends SwingWorker<Void, Void> {

        private final boolean pixelPosValid;
        private final int pixelX;
        private final int pixelY;
        private final int currentLevel;
        private final boolean adjustAxes;
        private final WorkerChainSupport support;

        SpectrumToolViewUpdater(boolean pixelPosValid, int pixelX, int pixelY, int currentLevel, boolean adjustAxes,
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
            toolView.getDiagramCanvas().setMessageText(null);
            if (pixelPosValid && isActive()) {
                toolView.getSpectraDiagram().addCursorSpectrumGraphs();
                toolView.updateSpectra(pixelX, pixelY, currentLevel);
            }
            if (adjustAxes) {
                toolView.getSpectraDiagram().adjustAxes(true);
            }
            return null;
        }

        @Override
        protected void done() {
            support.removeWorkerAndStartNext(this);
        }
    }

    //todo copied (and changed very slightly) from time-series-tool: Move to BEAM or Ceres
    static interface WorkerChainSupport {

        void removeWorkerAndStartNext(SwingWorker worker);
    }

}