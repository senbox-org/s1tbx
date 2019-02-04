package org.esa.s1tbx.fex.gpf.ui.decisiontree;

import org.esa.s1tbx.fex.gpf.decisiontree.DecisionTreeNode;
import org.esa.s1tbx.fex.gpf.decisiontree.DecisionTreeOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.product.ProductExpressionPane;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Edits tree nodes
 */
class EditNodePanel extends JPanel {

    private JButton editExpressionButton = new JButton("Edit Expression...");
    private JTextArea expressionArea = new JTextArea();

    private final List<EditNodeListener> listenerList = new ArrayList<>(1);
    private DecisionTreeNode selectednode = null;
    private Product[] sourceProducts;

    EditNodePanel() {
        final BoxLayout layout = new BoxLayout(this, BoxLayout.LINE_AXIS);
        setLayout(layout);

        this.add(expressionArea);
        expressionArea.addKeyListener(createTextAreaKeyListener());

        this.add(editExpressionButton);
        editExpressionButton.addActionListener(createEditExpressionButtonListener(this));

        makeVisible(false);
    }

    private void makeVisible(final boolean flag) {
        expressionArea.setEnabled(flag);
        editExpressionButton.setEnabled(flag);
    }

    private void update() {
        if (selectednode != null) {
            expressionArea.setText(selectednode.getExpression());
        }
    }

    public void setSelected(final DecisionTreeNode node) {
        makeVisible(node != null);
        selectednode = node;
        update();
    }

    public void setProducts(final Product[] sourceProducts) {
        this.sourceProducts = sourceProducts;
    }

    private KeyListener createTextAreaKeyListener() {

        return new KeyListener() {
            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                selectednode.setExpression(expressionArea.getText());
                notifyMSG();
            }

            public void keyTyped(KeyEvent e) {
            }
        };
    }

    private ActionListener createEditExpressionButtonListener(final EditNodePanel panel) {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                openExpressionEditor();
            }
        };
    }

    public void openExpressionEditor() {
        if (sourceProducts == null) {
            Dialogs.showError("Please first open a source product");
            return;
        }

        final Product[] availableProducts = DecisionTreeOp.getAvailableProducts(sourceProducts);

        final ProductExpressionPane pep = new ProductSetExpressionPane(true, availableProducts,
                sourceProducts[0], new DefaultPropertyMap());
        pep.setCode(selectednode.getExpression());
        int status = pep.showModalDialog(SwingUtilities.getWindowAncestor(this), "Arithmetic Expression Editor");
        if (status == ModalDialog.ID_OK) {
            selectednode.setExpression(pep.getCode());
        }
        pep.dispose();
        update();
        notifyMSG();
    }

    public void addListener(final EditNodeListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    private void notifyMSG() {
        for (final EditNodeListener listener : listenerList) {
            listener.notifyMSG();
        }
    }

    public interface EditNodeListener {
        void notifyMSG();
    }
}
