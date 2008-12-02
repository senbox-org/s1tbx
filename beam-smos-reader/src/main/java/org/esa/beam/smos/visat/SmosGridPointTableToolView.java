package org.esa.beam.smos.visat;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;

public class SmosGridPointTableToolView extends SmosGridPointInfoToolView {
    public static final String ID = SmosGridPointTableToolView.class.getName();

    private JTable table;
    private DefaultTableModel nullModel;

    public SmosGridPointTableToolView() {
        nullModel = new DefaultTableModel();
    }

    @Override
    protected JComponent createGridPointComponent() {
        table = new JTable();
        return new JScrollPane(table);
    }

    @Override
    protected void updateGridPointComponent(GridPointDataset ds) {
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