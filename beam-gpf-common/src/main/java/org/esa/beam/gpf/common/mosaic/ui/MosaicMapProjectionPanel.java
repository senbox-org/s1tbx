package org.esa.beam.gpf.common.mosaic.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.gpf.common.reproject.ui.CrsSelectionPanel;

import javax.swing.JPanel;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class MosaicMapProjectionPanel extends JPanel {

    private final AppContext appContext;
    private CrsSelectionPanel crsSelectionPanel;
    private Product refProduct;

    MosaicMapProjectionPanel(AppContext appContext) {
        this.appContext = appContext;

        init();
    }

    private void init() {
        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(3, 3);

        crsSelectionPanel = new CrsSelectionPanel(appContext, false);
        add(crsSelectionPanel);
    }


    public void setReferenceProduct(Product product){
        refProduct = product;
    }

}
