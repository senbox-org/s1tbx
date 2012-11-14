package org.esa.beam.opendap.ui;

import org.esa.beam.opendap.datamodel.OpendapLeaf;
import org.esa.beam.util.StringUtils;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DatasetNameFilter implements FilterComponent {

    final JCheckBox checkBox;
    final JTextField expressionTextField;
    final JButton applyButton;
    List<FilterChangeListener> listeners;

    public DatasetNameFilter(JCheckBox filterCheckBox) {
        checkBox = filterCheckBox;
        checkBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateUIState(false);
                if (StringUtils.isNotNullAndNotEmpty(expressionTextField.getText())) {
                    fireFilterChangedEvent();
                }

            }
        });
        expressionTextField = new JTextField();
        expressionTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateUIState(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateUIState(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateUIState(true);
            }
        });
        listeners = new ArrayList<FilterChangeListener>();
        applyButton = new JButton("Apply");
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireFilterChangedEvent();
                updateUIState(false);
            }
        });
        updateUIState(false);
    }

    @Override
    public JComponent getUI() {
        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(applyButton, BorderLayout.EAST);

        final JPanel filterUI = new JPanel(new BorderLayout(4, 4));
        filterUI.add(expressionTextField, BorderLayout.NORTH);
        filterUI.add(buttonPanel, BorderLayout.SOUTH);
        return filterUI;
    }

    @Override
    public boolean accept(OpendapLeaf leaf) {
        String text = expressionTextField.getText();
        if (StringUtils.isNullOrEmpty(text)) {
            return true;
        }

        text = text.replace("*", ".*").toLowerCase();

        Pattern pattern = Pattern.compile(text);
        final Matcher matcher = pattern.matcher(leaf.getName().toLowerCase());
        return matcher.matches();
    }

    @Override
    public void addFilterChangeListener(FilterChangeListener listener) {
        listeners.add(listener);
    }

    private void fireFilterChangedEvent() {
        for (FilterChangeListener listener : listeners) {
            listener.filterChanged();
        }
    }

    private void updateUIState(boolean enableApplyButton) {
        if (!checkBox.isSelected()) {
            expressionTextField.setEnabled(false);
            applyButton.setEnabled(false);
            return;
        }

        expressionTextField.setEnabled(true);
        applyButton.setEnabled(enableApplyButton);
    }
}
