package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;

import javax.swing.SwingWorker;

public class DetachPixelGeoCodingAction extends ExecCommand {

    private static final String DETACH_TITLE = "Detach Pixel Geo-Coding";

    @Override
    public void actionPerformed(CommandEvent event) {
        detachPixelGeoCoding();
    }

    @Override
    public void updateState(CommandEvent event) {
        boolean enabled = false;
        final Product product = VisatApp.getApp().getSelectedProduct();
        if (product != null) {
            enabled = product.getGeoCoding() instanceof PixelGeoCoding;
        }
        setEnabled(enabled);
    }

    private static void detachPixelGeoCoding() {

        final SwingWorker swingWorker = new SwingWorker<Throwable, Object>() {
            @Override
            protected Throwable doInBackground() throws Exception {
                try {
                    final Product product = VisatApp.getApp().getSelectedProduct();
                    final PixelGeoCoding pixelGeoCoding = (PixelGeoCoding) product.getGeoCoding();
                    final GeoCoding delegate = pixelGeoCoding.getPixelPosEstimator();
                    product.setGeoCoding(delegate);
                    pixelGeoCoding.dispose();
                } catch (Throwable e) {
                    return e;
                }
                return null;
            }

            @Override
            public void done() {
                VisatApp visatApp = VisatApp.getApp();
                UIUtils.setRootFrameDefaultCursor(visatApp.getMainFrame());
                Throwable value;
                try {
                    value = get();
                } catch (Exception e) {
                    value = e;
                }
                if (value != null) {
                    visatApp.showErrorDialog(DETACH_TITLE,
                                             "An internal error occured:\n" + value.getMessage());
                } else {
                    visatApp.showInfoDialog(DETACH_TITLE, "Pixel geo-coding has been detached.", null);
                }
                visatApp.updateState();
            }
        };

        UIUtils.setRootFrameWaitCursor(VisatApp.getApp().getMainFrame());
        swingWorker.execute();
    }
}
