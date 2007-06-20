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
abstract class TextPagePane extends PagePane {

    private JTextArea _textArea;

    public TextPagePane(final ToolView parentDialog, final String defaultText) {
        super(parentDialog);
        _textArea.setText(defaultText);
    }

    public JTextArea getTextArea() {
        return _textArea;
    }

    @Override
    protected void initContent() {
        _textArea = new JTextArea();
        _textArea.setEditable(false);
        _textArea.addMouseListener(new PopupHandler());
        add(new JScrollPane(_textArea), BorderLayout.CENTER);
    }

    @Override
    protected void updateContent() {
        ensureValidData();
        _textArea.setText(createText());
        _textArea.setCaretPosition(0);
    }

    protected void ensureValidData() {
    }

    protected abstract String createText();

    @Override
    protected String getDataAsText() {
        return _textArea.getText();
    }

    @Override
    protected void handlePopupCreated(final JPopupMenu popupMenu) {
        final JMenuItem menuItem = new JMenuItem("Select All");     /*I18N*/
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                _textArea.selectAll();
                _textArea.requestFocus();
            }
        });
        popupMenu.add(menuItem);
    }
}
