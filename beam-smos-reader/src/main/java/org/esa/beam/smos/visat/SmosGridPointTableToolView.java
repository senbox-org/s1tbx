package org.esa.beam.smos.visat;

import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.CompoundMember;
import com.jidesoft.swing.CheckBoxList;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;

import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.dataio.smos.SmosFile;
import org.esa.beam.dataio.smos.L1cSmosFile;

public class SmosGridPointTableToolView extends SmosGridPointInfoToolView {
    public static final String ID = SmosGridPointTableToolView.class.getName();

    private JTable table;
    private DefaultTableModel nullModel;
    private JButton columnsButton;

    public SmosGridPointTableToolView() {
        nullModel = new DefaultTableModel();
    }

    @Override
    protected void updateSmosComponent(ProductSceneView oldView, ProductSceneView newView) {
        table.setEnabled(newView != null);
        columnsButton.setEnabled(newView != null);
    }


    @Override
    protected JComponent createGridPointComponent() {
        table = new JTable();
        return new JScrollPane(table);
    }

    @Override
    protected JComponent createGridPointComponentOptionsComponent() {
        columnsButton = new JButton("Columns...");
        columnsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SmosFile smosFile = getSmosProductReader().getSmosFile();

                if (smosFile instanceof L1cSmosFile) {
                    final L1cSmosFile l1cSmosFile = (L1cSmosFile) smosFile;
                    final CompoundType btDataType = l1cSmosFile.getBtDataType();
                    final CompoundMember[] members = btDataType.getMembers();
                    String[] names = new String[members.length];
                    for (int i = 0; i < members.length; i++) {
                        CompoundMember member = members[i];
                        names[i] = member.getName();
                    }
                    CheckBoxList checkBoxList = new CheckBoxList(names);

                    final ModalDialog dialog = new ModalDialog(SwingUtilities.windowForComponent(columnsButton), "Select Columns",
                                                               new JScrollPane(checkBoxList),
                                                               ModalDialog.ID_OK_CANCEL, null);
                    final int i = dialog.show();
                    if (i == ModalDialog.ID_OK) {

                    }
                }
            }
        });
        final JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        optionsPanel.add(columnsButton);
        return optionsPanel;
    }


    @Override
    protected void updateGridPointComponent(GridPointBtDataset ds) {
        table.setModel(new GridPointTableModel(ds));
    }

    @Override
    protected void updateGridPointComponent(IOException e) {
        table.setModel(nullModel);
    }

    @Override
    protected void clearGridPointComponent() {
        table.setModel(nullModel);
    }

}