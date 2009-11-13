package org.esa.beam.gpf.common.mosaic;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ComponentAdapter;
import static org.junit.Assert.*;
import org.junit.Test;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class MosaicOpBindingTest {

    @Test
    public void variablesProperty() {
        final MosaicOp op = new MosaicOp();
        op.variables = new MosaicOp.Variable[]{
                new MosaicOp.Variable("a", "a"),
                new MosaicOp.Variable("b", "b"),
                new MosaicOp.Variable("c", "c"),
        };
        final PropertyContainer pc = PropertyContainer.createObjectBacked(op);

        final Property property = pc.getProperty("variables");
        assertNotNull(property);
        final Object value = property.getValue();
        assertTrue(value instanceof MosaicOp.Variable[]);

        final BindingContext bc = new BindingContext(pc);
        final JTable table = new JTable();
        bc.bind("variables", new VariableTableAdapter(table));

        assertEquals(3, table.getRowCount());

        final MosaicOp.Variable[] value1 = {
                new MosaicOp.Variable("a", "a")
        };
        pc.setValue("variables", value1);

        assertEquals(1, table.getRowCount());
        assertEquals("a", table.getValueAt(0, 0));

//        op.variables[0] = new MosaicOp.Variable("b", "b");
//        assertEquals("b", table.getValueAt(0, 0));

        table.setValueAt("b", 0, 0);
        assertEquals("b", op.variables[0].name);
    }

    private static class VariableTableAdapter extends ComponentAdapter implements PropertyChangeListener {

        private final JTable table;

        private VariableTableAdapter(JTable table) {
            this.table = table;
        }

        @Override
        public JComponent[] getComponents() {
            return new JComponent[]{table};
        }

        @Override
        public void bindComponents() {
            adjustTableModel();
        }

        @Override
        public void unbindComponents() {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void adjustComponents() {
            adjustTableModel();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        private void adjustTableModel() {
            final Object value = getBinding().getPropertyValue();
            if (value instanceof MosaicOp.Variable[]) {
                final MosaicOp.Variable[] variables = (MosaicOp.Variable[]) value;
                final TableModel tableModel = createTableModel(variables.length);
                for (int i = 0; i < variables.length; i++) {
                    final MosaicOp.Variable variable = variables[i];
                    tableModel.setValueAt(variable.name, i, 0);
                    tableModel.setValueAt(variable.expression, i, 1);
                }
                tableModel.addTableModelListener(new TableModelListener() {
                    @Override
                    public void tableChanged(TableModelEvent e) {
                        final TableModel tableModel = (TableModel) e.getSource();
                        final MosaicOp.Variable[] variables = new MosaicOp.Variable[tableModel.getRowCount()];
                        for (int i = 0; i < variables.length; i++) {
                            variables[i] = new MosaicOp.Variable(tableModel.getValueAt(i, 0).toString(),
                                                                 tableModel.getValueAt(i, 1).toString());
                        }
                        getBinding().setPropertyValue(variables);
                    }
                });
                table.setModel(tableModel);
            }
        }

        private DefaultTableModel createTableModel(int rowCount) {
            return new DefaultTableModel(new String[]{"Name", "Expression"}, rowCount);
        }
    }
}
