package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.io.TableModelCsvEncoder;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Enumeration;

public class TableViewPagePanel extends PagePanel {

    private AbstractButton switchToChartButton;
    private JTable table;
    private final Icon iconForSwitchToChartButton;

    public TableViewPagePanel(ToolView toolView, String helpId, String titlePrefix, Icon iconForSwitchToChartButton) {
        super(toolView, helpId, titlePrefix);
        this.iconForSwitchToChartButton = iconForSwitchToChartButton;
    }

    @Override
    protected void initComponents() {
        switchToChartButton = ToolButtonFactory.createButton(iconForSwitchToChartButton, false);
        switchToChartButton.setToolTipText("Switch to Chart View");
        switchToChartButton.setName("switchToChartButton");
        switchToChartButton.setEnabled(hasAlternativeView());
        switchToChartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                showAlternativeView();

            }
        });

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(switchToChartButton, BorderLayout.NORTH);
        buttonPanel.add(getHelpButton(), BorderLayout.SOUTH);

        add(buttonPanel, BorderLayout.EAST);

        table = new JTable();
        table.removeEditor();
        table.setGridColor(Color.LIGHT_GRAY.brighter());
        table.addMouseListener(new PagePanel.PopupHandler());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        final JScrollPane scrollPane = new JScrollPane(table);

        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    protected void updateComponents() {
    }

    @Override
    protected String getDataAsText() {
        final StringWriter writer = new StringWriter();
        try {
            new TableModelCsvEncoder(table.getModel()).encodeCsv(writer);
            writer.close();
        } catch (IOException ignore) {
        }
        return writer.toString();
    }

    @Override
    protected void showAlternativeView() {
        super.showAlternativeView();
        final PagePanel alternativeView = getAlternativeView();
        alternativeView.handleLayerContentChanged();
        final RasterDataNode raster = alternativeView.getRaster();
        alternativeView.setRaster(null);
        alternativeView.setRaster(raster);
        alternativeView.handleNodeSelectionChanged();
    }

    void setModel(TableModel tableModel) {
        table.setModel(tableModel);
        if (table.getColumnCount() > 0) {
            final JTableHeader tableHeader = table.getTableHeader();
            final int margin = tableHeader.getColumnModel().getColumnMargin();
            final TableCellRenderer renderer = tableHeader.getDefaultRenderer();
            final Enumeration<TableColumn> columns = table.getColumnModel().getColumns();
            while (columns.hasMoreElements()) {
                TableColumn tableColumn = columns.nextElement();
                final int width = getColumnMinWith(tableColumn, renderer, margin);
                tableColumn.setMinWidth(width);
            }
        }
    }

    private int getColumnMinWith(TableColumn column, TableCellRenderer renderer, int margin) {
        final Object headerValue = column.getHeaderValue();
        final JLabel label = (JLabel) renderer.getTableCellRendererComponent(table, headerValue, false, false, 0, 0);

        return label.getPreferredSize().width + margin;
    }


}
