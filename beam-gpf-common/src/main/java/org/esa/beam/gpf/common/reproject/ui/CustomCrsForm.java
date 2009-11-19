package org.esa.beam.gpf.common.reproject.ui;

import com.jidesoft.swing.DefaultOverlayable;
import com.jidesoft.swing.InfiniteProgressPanel;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.gpf.common.reproject.ui.projdef.CustomCrsPanel;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JComponent;
import javax.swing.SwingWorker;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class CustomCrsForm extends CrsForm {

    private CustomCrsPanel customCrsPanel;
    private DefaultOverlayable overlayable;

    protected CustomCrsForm(AppContext appContext) {
        super(appContext);
        customCrsPanel = new CustomCrsPanel(appContext.getApplicationWindow());
        customCrsPanel.addPropertyChangeListener("crs", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                fireCrsChanged();
            }
        });
        overlayable = prepareInit();

    }

    private DefaultOverlayable prepareInit() {
        final DefaultOverlayable defaultOverlayable = new DefaultOverlayable(customCrsPanel);
        final InfiniteProgressPanel progressPanel = new InfiniteProgressPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(45, 45);
            }
        };

        defaultOverlayable.addOverlayComponent(progressPanel);
        progressPanel.start();
        defaultOverlayable.setOverlayVisible(true);
        SwingWorker sw = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                customCrsPanel.initModel();
                return null;
            }

            @Override
            protected void done() {
                progressPanel.stop();
                defaultOverlayable.setOverlayVisible(false);
            }
        };
        sw.execute();
        return defaultOverlayable;
    }

    @Override
    public CoordinateReferenceSystem getCRS(GeoPos referencePos) throws FactoryException {
        return customCrsPanel.getCRS(referencePos);
    }

    @Override
    public JComponent getCrsUI() {
        return overlayable;
    }

    @Override
    public void prepareShow() {
    }

    @Override
    public void prepareHide() {
    }
}
