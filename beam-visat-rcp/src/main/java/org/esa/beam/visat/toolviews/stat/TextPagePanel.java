package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.ui.application.ToolView;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A pane within the statistics window which contains text only.
 *
 * @author Marco Peters
 */
abstract class TextPagePanel extends PagePanel {

    private JTextArea textArea;

    protected TextPagePanel(final ToolView parentDialog, final String defaultText, String helpId) {
        super(parentDialog, helpId);
        textArea.setText(defaultText);
    }

    public JTextArea getTextArea() {
        return textArea;
    }

    @Override
    protected void initContent() {
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.addMouseListener(new PopupHandler());
        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    @Override
    protected void updateContent() {
        ensureValidData();
        textArea.setText(createText());
        textArea.setCaretPosition(0);
    }

    protected void ensureValidData() {
    }

    protected abstract String createText();

    @Override
    protected String getDataAsText() {
        return textArea.getText();
    }

    @Override
    protected void handlePopupCreated(final JPopupMenu popupMenu) {
        final JMenuItem menuItem = new JMenuItem("Select All");     /*I18N*/
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                textArea.selectAll();
                textArea.requestFocus();
            }
        });
        popupMenu.add(menuItem);
    }
}
