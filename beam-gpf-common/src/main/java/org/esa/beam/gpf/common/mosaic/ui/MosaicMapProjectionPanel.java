package org.esa.beam.gpf.common.mosaic.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.WorldMapPane;
import org.esa.beam.framework.ui.WorldMapPaneDataModel;
import org.esa.beam.gpf.common.reproject.ui.CrsSelectionPanel;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class MosaicMapProjectionPanel extends JPanel {

    private final AppContext appContext;

    private CrsSelectionPanel crsSelectionPanel;
    private JPanel orthorectifyPanel;
    private JPanel mosaicBoundsPanel;
    private ButtonModel orthorectifyButtonModel;
    private DefaultComboBoxModel demModel;
    private JComboBox demComboBox;
    private MosaicMapProjectionPanel.UpdateUIStateActionListener uiStateListener;

    MosaicMapProjectionPanel(AppContext appContext) {
        this.appContext = appContext;
        init();
        createUI();
        updateUIState();
    }

    private void init() {
        uiStateListener = new UpdateUIStateActionListener();

        final ElevationModelDescriptor[] descriptors = ElevationModelRegistry.getInstance().getAllDescriptors();
        final String[] demValueSet = new String[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            demValueSet[i] = descriptors[i].getName();
        }
        demModel = new DefaultComboBoxModel(demValueSet);
    }

    private void createUI() {
        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(3, 3);
        setLayout(layout);
        crsSelectionPanel = new CrsSelectionPanel(appContext, false);
        orthorectifyPanel = createOrthorectifyPanel();
        mosaicBoundsPanel = createMosaicBoundsPanel();
        add(crsSelectionPanel);
        add(orthorectifyPanel);
        add(mosaicBoundsPanel);
    }

    private JPanel createMosaicBoundsPanel() {
        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(3, 3);
        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Mosaic Bounds"));
        final JPanel inputPanel = createBoundsInputPanel();
        panel.add(inputPanel);
        final WorldMapPane worlMapPanel = new WorldMapPane(new WorldMapPaneDataModel());
        worlMapPanel.setBorder(BorderFactory.createEtchedBorder());
        panel.add(worlMapPanel);

        return panel;
    }

    private JPanel createBoundsInputPanel() {
        final TableLayout layout = new TableLayout(6);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(3, 3);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setColumnWeightX(2, 0.0);
        layout.setColumnWeightX(3, 1.0);
        layout.setColumnWeightX(4, 0.0);
        layout.setColumnWeightX(5, 1.0);
        layout.setColumnPadding(1, new Insets(3, 3, 3, 9));
        layout.setColumnPadding(3, new Insets(3, 3, 3, 9));
        final JPanel panel = new JPanel(layout);
        panel.add(new JLabel("West:"));
        panel.add(new JFormattedTextField());
        panel.add(new JLabel("East:"));
        panel.add(new JFormattedTextField());
        panel.add(new JLabel("Pixel size X:"));
        panel.add(new JFormattedTextField());
        panel.add(new JLabel("North:"));
        panel.add(new JFormattedTextField());
        panel.add(new JLabel("South:"));
        panel.add(new JFormattedTextField());
        panel.add(new JLabel("Pixel size Y:"));
        panel.add(new JFormattedTextField());

        return panel;
    }

    private JPanel createOrthorectifyPanel() {
        final TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(3, 3);
        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Orthorectification"));

        final JCheckBox orthoCheckBox = new JCheckBox("Orthorectify input products");
        orthorectifyButtonModel = orthoCheckBox.getModel();
        demComboBox = new JComboBox(demModel);
        orthorectifyButtonModel.addActionListener(uiStateListener);
        layout.setCellColspan(0, 0, 2);
        panel.add(orthoCheckBox);

        layout.setCellWeightX(1, 0, 0.0);
        panel.add(new JLabel("Elevation model:"));
        layout.setCellWeightX(1, 1, 1.0);
        panel.add(demComboBox);
        return panel;
    }

    private void updateUIState() {
        demComboBox.setEnabled(orthorectifyButtonModel.isSelected());
    }

    public void setReferenceProduct(Product product) {
        crsSelectionPanel.setReferenceProduct(product);
    }

    private class UpdateUIStateActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            updateUIState();
        }
    }
}
