package org.esa.s1tbx.analysis.rcp.toolviews.timeseries.actions;

import com.bc.ceres.swing.TableLayout;
import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.TimeSeriesSettings;
import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.graphs.VectorGraph;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.ui.ModalDialog;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * A dialog which lets the user select from a product's bands and tie-point grids.
 */
public class TimeSeriesFiltersDlg extends ModalDialog {

    private final boolean selectAtLeastOneBand = true;
    private final TimeSeriesSettings settings;
    private final BandsFilterPane bandsFilterPane;
    private final VectorsFilterPane vectorsFilterPane;

    public TimeSeriesFiltersDlg(Window parent, String title, String helpID, Band[] allBands, Band[] selectedBands,
                                final String[] allVectors,
                                final TimeSeriesSettings settings) {
        super(parent, title, ModalDialog.ID_OK_CANCEL, helpID);

        boolean multipleProducts = bandsFromMoreThanOneProduct(allBands);
        this.settings = settings;
        settings.populateColorMaps(allBands, null, allVectors);

        this.bandsFilterPane = new BandsFilterPane(allBands, selectedBands, multipleProducts);
        //bandsFilterPane.setBandColorMap(settings.getBandColorMap());

        this.vectorsFilterPane = new VectorsFilterPane(allVectors, settings.getSelectedVectorNames(), multipleProducts);
        vectorsFilterPane.setVectorColorMap(settings.getVectorColorMap());

        initUI();
    }

    private boolean bandsFromMoreThanOneProduct(final Band[] allBands) {
        final Set<Product> productSet = new HashSet<>();
        if (allBands != null) {
            for (Band allBand : allBands) {
                productSet.add(allBand.getProduct());
            }
        }
        return productSet.size() > 1;
    }

    @Override
    public int show() {
        bandsFilterPane.updateCheckBoxStates();
        return super.show();
    }

    private void initUI() {
        JPanel bandsPane = bandsFilterPane.createCheckersPane();
        JPanel vectorsPane = vectorsFilterPane.createCheckersPane(settings.getVectorStatistic());

        TableLayout layout = new TableLayout(1);
        layout.setTablePadding(4, 4);

        final JPanel content = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(bandsPane);
        final Dimension preferredSize = bandsPane.getPreferredSize();
        scrollPane.setPreferredSize(new Dimension(Math.min(preferredSize.width + 20, 400),
                Math.min(preferredSize.height + 10, 300)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        content.add(scrollPane, BorderLayout.CENTER);
        content.setMinimumSize(new Dimension(0, 100));

        final JPanel content2 = new JPanel(new BorderLayout());
        JScrollPane scrollPane2 = new JScrollPane(vectorsPane);
        content2.add(scrollPane2, BorderLayout.CENTER);

        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Bands", content);
        tabbedPane.add("Vectors", content2);
        tabbedPane.setSelectedIndex(0);

        setContent(tabbedPane);
    }

    @Override
    protected boolean verifyUserInput() {
        if (!bandsFilterPane.atLeastOneBandSelected() && selectAtLeastOneBand) {
            showInformationDialog("No bands selected.\nPlease select at least one band.");
            return false;
        }
        return true;
    }

    public Band[] getSelectedBands() {
        return bandsFilterPane.getSelectedBands();
    }

    public String[] getSelectedVectors() {
        return vectorsFilterPane.getSelectedVectors();
    }

    public VectorGraph.TYPE getStatistic() {
        return vectorsFilterPane.getStatistic();
    }
}
