package org.esa.beam.gpf.common.reproject.ui;

import com.jidesoft.swing.DefaultOverlayable;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.gpf.common.reproject.ui.projdef.CustomCrsPanel;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
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
        overlayable = new DefaultOverlayable(customCrsPanel);

    }

    private void initCustomCrsPanelModel() {
        final JProgressBar progressBar = new JProgressBar(0, 100);
        overlayable.addOverlayComponent(progressBar);
        progressBar.setIndeterminate(true);
        overlayable.setOverlayVisible(true);
        SwingWorker sw = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                customCrsPanel.initModel();
                return null;
            }

            @Override
            protected void done() {
                overlayable.setOverlayVisible(false);
            }
        };
        sw.execute();
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
        initCustomCrsPanelModel();
    }

    @Override
    public void prepareHide() {
    }
}
