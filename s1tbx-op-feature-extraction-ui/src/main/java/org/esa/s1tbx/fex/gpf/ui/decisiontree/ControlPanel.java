
package org.esa.s1tbx.fex.gpf.ui.decisiontree;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Edits tree nodes
 */
class ControlPanel extends JPanel {

    //private JButton loadButton = new JButton("Load");
    //private JButton saveButton = new JButton("Save");
    private JButton clearButton = new JButton("Clear");

    private final List<EditNodeListener> listenerList = new ArrayList<>(1);
    private final DecisionTreePanel treePanel;

    ControlPanel(final DecisionTreePanel treePanel) {
        final BoxLayout layout = new BoxLayout(this, BoxLayout.PAGE_AXIS);
        setLayout(layout);
        this.treePanel = treePanel;

        //this.add(loadButton);
        //this.add(saveButton);
        this.add(clearButton);

        //loadButton.addActionListener(new ActionListener() {
        //    @Override
        //    public void actionPerformed(ActionEvent e) {

        //    }
        //});
        //saveButton.addActionListener(new ActionListener() {
        //    @Override
        //    public void actionPerformed(ActionEvent e) {

        //    }
        //});
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                treePanel.createNewTree(null);
            }
        });
    }

    private void update() {

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
