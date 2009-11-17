package org.esa.beam.gpf.common.mosaic.ui;

import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.WorldMapPane;
import org.esa.beam.framework.ui.WorldMapPaneDataModel;
import org.esa.beam.gpf.common.reproject.ui.CrsSelectionPanel;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class MosaicMapProjectionPanel extends JPanel {

    private final AppContext appContext;
    private final MosaicFormModel mosaicModel;

    private CrsSelectionPanel crsSelectionPanel;
    private JPanel orthorectifyPanel;
    private JPanel mosaicBoundsPanel;
    private JComboBox demComboBox;
    private final BindingContext binding;
    private String[] demValueSet;

    MosaicMapProjectionPanel(AppContext appContext, MosaicFormModel mosaicModel) {
        this.appContext = appContext;
        this.mosaicModel = mosaicModel;
        binding = new BindingContext(mosaicModel.getPropertyContainer());
        init();
        createUI();
        binding.adjustComponents();
    }

    private void init() {
        final ElevationModelDescriptor[] descriptors = ElevationModelRegistry.getInstance().getAllDescriptors();
        demValueSet = new String[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            demValueSet[i] = descriptors[i].getName();
        }
        if (demValueSet.length > 0) {
            mosaicModel.getPropertyContainer().setValue("elevationModelName", demValueSet[0]);
        }
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
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(3, 3);
        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Mosaic Bounds"));
        final JPanel inputPanel = createBoundsInputPanel();
        panel.add(inputPanel);
        final WorldMapPane worlMapPanel = new WorldMapPane(new WorldMapPaneDataModel());
        worlMapPanel.setMinimumSize(new Dimension(250, 125));
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
        final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
        final DecimalFormat degreeFormat = new DecimalFormat("####.###°", decimalFormatSymbols);

        panel.add(new JLabel("West:"));
        final JFormattedTextField westLonField = new JFormattedTextField(degreeFormat);
        binding.bind("westBound", westLonField);
        panel.add(westLonField);
        panel.add(new JLabel("East:"));
        final JFormattedTextField eastLonField = new JFormattedTextField(degreeFormat);
        binding.bind("eastBound", eastLonField);
        panel.add(eastLonField);
        panel.add(new JLabel("Pixel size X:"));
        final JFormattedTextField pixelSizeXField = new JFormattedTextField(degreeFormat);
        binding.bind("pixelSizeX", pixelSizeXField);
        panel.add(pixelSizeXField);
        panel.add(new JLabel("North:"));
        final JFormattedTextField northLatField = new JFormattedTextField(degreeFormat);
        binding.bind("northBound", northLatField);
        panel.add(northLatField);
        panel.add(new JLabel("South:"));
        final JFormattedTextField southLatField = new JFormattedTextField(degreeFormat);
        binding.bind("southBound", southLatField);
        panel.add(southLatField);
        panel.add(new JLabel("Pixel size Y:"));
        final JFormattedTextField pixelSizeYField = new JFormattedTextField(degreeFormat);
        binding.bind("pixelSizeY", pixelSizeYField);
        panel.add(pixelSizeYField);

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
        binding.bind("orthorectify", orthoCheckBox);
        demComboBox = new JComboBox(new DefaultComboBoxModel(demValueSet));
        binding.bind("elevationModelName", demComboBox);
        binding.bindEnabledState("elevationModelName", true, "orthorectify", true);
        layout.setCellColspan(0, 0, 2);
        panel.add(orthoCheckBox);

        layout.setCellWeightX(1, 0, 0.0);
        panel.add(new JLabel("Elevation model:"));
        layout.setCellWeightX(1, 1, 1.0);
        panel.add(demComboBox);
        return panel;
    }

    public void setReferenceProduct(Product product) {
        crsSelectionPanel.setReferenceProduct(product);
    }

}
