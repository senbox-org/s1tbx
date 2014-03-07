package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.ModalDialog;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A dialog which lets the user select from a product's bands and tie-point grids.
 */
public class BandChooser extends ModalDialog {

    private final boolean selectAtLeastOneBand;
    private BandChoosingStrategy strategy;

    public BandChooser(Window parent, String title, String helpID,
                       Band[] allBands, Band[] selectedBands, Product.AutoGrouping autoGrouping) {
        super(parent, title, ModalDialog.ID_OK_CANCEL, helpID);
        boolean multipleProducts = bandsAndGridsFromMoreThanOneProduct(allBands, null);
        strategy = new GroupedBandChoosingStrategy(allBands, selectedBands, null, null, autoGrouping, multipleProducts);
        selectAtLeastOneBand = false;
        initUI();
    }

    public BandChooser(Window parent, String title, String helpID,
                       Band[] allBands, Band[] selectedBands) {
        this(parent, title, helpID, true, allBands, selectedBands, null, null);
    }

    public BandChooser(Window parent, String title, String helpID, boolean selectAtLeastOneBand,
                       Band[] allBands, Band[] selectedBands,
                       TiePointGrid[] allTiePointGrids, TiePointGrid[] selectedTiePointGrids) {
        super(parent, title, ModalDialog.ID_OK_CANCEL, helpID);
        boolean multipleProducts = bandsAndGridsFromMoreThanOneProduct(allBands, allTiePointGrids);
        strategy = new DefaultBandChoosingStrategy(allBands, selectedBands, allTiePointGrids, selectedTiePointGrids,
                                                   multipleProducts);
        this.selectAtLeastOneBand = selectAtLeastOneBand;
        initUI();
    }

    // package local for testing reasons only
    static void sort(Band[] allBands) {
        // first, sort by name without digits
        Arrays.sort(allBands, new Comparator<Band>() {
            @Override
            public int compare(Band band1, Band band2) {
                String regex = "\\d";
                return band1.getName().replaceAll(regex, "").compareTo(band2.getName().replaceAll(regex, ""));
            }
        });
        // second, sort by wavelength
        final List<Integer> comparationResult = new ArrayList<>();
        Arrays.sort(allBands, new Comparator<Band>() {
            @Override
            public int compare(Band band1, Band band2) {
                int n = (int) (band1.getSpectralWavelength() - band2.getSpectralWavelength());
                comparationResult.add(n);
                return n;
            }
        });

        boolean noWavelengthsDifferences = Collections.frequency(comparationResult, 0) == allBands.length - 1;
        if (noWavelengthsDifferences) {
            // third, sort by name WITH digits if all wavelength are equal or don't exist
            Arrays.sort(allBands, new Comparator<Band>() {
                @Override
                public int compare(Band band1, Band band2) {
                    return band1.getName().compareTo(band2.getName());
                }
            });
        }
    }

    private boolean bandsAndGridsFromMoreThanOneProduct(Band[] allBands, TiePointGrid[] allTiePointGrids) {
        Set<Product> productSet = new HashSet<>();
        if (allBands != null) {
            for (Band allBand : allBands) {
                productSet.add(allBand.getProduct());
            }
        }
        if (allTiePointGrids != null) {
            for (TiePointGrid allTiePointGrid : allTiePointGrids) {
                productSet.add(allTiePointGrid.getProduct());
            }
        }
        return productSet.size() > 1;
    }

    @Override
    public int show() {
        strategy.updateCheckBoxStates();
        return super.show();
    }

    private void initUI() {
        JPanel checkersPane = strategy.createCheckersPane();

        JCheckBox selectAllCheckBox = new JCheckBox("Select all");
        selectAllCheckBox.setMnemonic('a');
        selectAllCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                strategy.selectAll();
            }
        });

        JCheckBox selectNoneCheckBox = new JCheckBox("Select none");
        selectNoneCheckBox.setMnemonic('n');
        selectNoneCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                strategy.selectNone();
            }
        });

        strategy.setCheckBoxes(selectAllCheckBox, selectNoneCheckBox);

        final JPanel checkPane = new JPanel(new BorderLayout());
        checkPane.add(selectAllCheckBox, BorderLayout.WEST);
        checkPane.add(selectNoneCheckBox, BorderLayout.CENTER);
        final JPanel content = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(checkersPane);
        final Dimension preferredSize = checkersPane.getPreferredSize();
        scrollPane.setPreferredSize(new Dimension(Math.min(preferredSize.width + 20, 400),
                                                  Math.min(preferredSize.height + 10, 300)));
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(checkPane, BorderLayout.SOUTH);
        setContent(content);
    }

    @Override
    protected boolean verifyUserInput() {
        if (!strategy.atLeastOneBandSelected() && selectAtLeastOneBand) {
            showInformationDialog("No bands selected.\nPlease select at least one band.");
            return false;
        }
        return true;
    }

    public Band[] getSelectedBands() {
        return strategy.getSelectedBands();
    }

    public TiePointGrid[] getSelectedTiePointGrids() {
        return strategy.getSelectedTiePointGrids();
    }

}
