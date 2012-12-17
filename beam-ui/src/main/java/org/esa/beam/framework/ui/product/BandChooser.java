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
import java.util.HashSet;
import java.util.Set;

/**
 * A dialog which lets the user select from a product's bands and tie-point grids.
 */
public class BandChooser extends ModalDialog {

    private final boolean _selectAtLeastOneBand;
    private BandChoosingStrategy strategy;

    public BandChooser(Window parent, String title, String helpID,
                       Band[] allBands, Band[] selectedBands, Product.AutoGrouping autoGrouping) {
        super(parent, title, ModalDialog.ID_OK_CANCEL, helpID);
        boolean multipleProducts = bandsAndGridsFromMoreThanOneProduct(allBands, null);
        strategy = new GroupedBandChoosingStrategy(allBands, selectedBands, null, null, autoGrouping, multipleProducts);
        _selectAtLeastOneBand = false;
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
        _selectAtLeastOneBand = selectAtLeastOneBand;
        initUI();
    }

    private boolean bandsAndGridsFromMoreThanOneProduct(Band[] allBands, TiePointGrid[] allTiePointGrids) {
        Set productSet = new HashSet();
        if (allBands != null) {
            for (int i = 0; i < allBands.length; i++) {
                productSet.add(allBands[i].getProduct());
            }
        }
        if (allTiePointGrids != null) {
            for (int i = 0; i < allTiePointGrids.length; i++) {
                productSet.add(allTiePointGrids[i].getProduct());
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
        if (!strategy.atLeastOneBandSelected() && _selectAtLeastOneBand) {
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
