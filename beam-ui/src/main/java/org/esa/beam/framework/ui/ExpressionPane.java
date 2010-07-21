/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.framework.ui;

import com.bc.jexp.Function;
import com.bc.jexp.Namespace;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Term;
import com.bc.jexp.impl.NamespaceImpl;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.PropertyMap;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * The expression pane is a UI component which is used to edit mathematical expressions. There are four methods which
 * can be used to customize the UI of the expression pane: <code>{@link #setLeftAccessory}</code>, <code>{@link
 * #setRightAccessory}</code>, <code>{@link #setTopAccessory}</code> and <code>{@link #setBottomAccessory}</code>.
 */
public class ExpressionPane extends JPanel {

    public static final String HELP_ID = "expressionEditor";

    /**
     * The prefix used to store the code history in the preferences.
     */
    public static final String CODE_HISTORY_PREFERENCES_PREFIX = "expression.history.";

    private static final int CODE_HISTORY_MAX = 100;

    /**
     * The string used to represent an expression placeholder for text insertion.
     */
    public static final String PLACEHOLDER = "@";

    private static final int PLACEHOLDER_LEN = PLACEHOLDER.length();

    private static final String[] CONSTANT_LITERALS = new String[]{
            "PI",
            "E",
            "NaN",
            "true",
            "false",
            "X",
            "Y",
            "LAT",
            "LON",
            "0.5",
            "0.0",
            "1.0",
            "2.0",
            "0",
            "1",
            "2",
    };

    private static final String[] OPERATOR_PATTERNS = new String[]{
            "@ ? @ : @",
            "@ || @",
            "@ or @",
            "@ && @",
            "@ and @",
            "@ < @",
            "@ <= @",
            "@ > @",
            "@ >= @",
            "@ == @",
            "@ <= @",
            "@ | @",
            "@ ^ @",
            "@ & @",
            "@ + @",
            "@ - @",
            "@ * @",
            "@ / @",
            "@ % @",
            "+@",
            "-@",
            "~@",
            "!@",
            "not @"
    };

    private static final String[] FUNCTION_NAMES = new String[]{
            "sqrt(@)",
            "pow(@,@)",
            "exp(@)",
            "log(@)",
            "min(@,@)",
            "max(@,@)",
            "rad(@)",
            "deg(@)",
            "sign(@)",
            "abs(@)",
            "sin(@)",
            "cos(@)",
            "tan(@)",
            "asin(@)",
            "acos(@)",
            "atan(@)",
            "atan2(@,@)",
            "ampl(@,@)",
            "phase(@,@)"
    };

    private static Font exprTextAreaFont = new Font("Courier", Font.PLAIN, 12);
    private static Font insertCompFont = new Font("Courier", Font.PLAIN, 11);
    private static Color insertCompColor = new Color(0, 0, 128);
    private static Color okMsgColor = new Color(0, 128, 0);
    private static Color warnMsgColor = new Color(128, 0, 0);

    private Parser parser;
    private final Stack<String> undoBuffer;
    private boolean booleanExpressionPreferred;

    private JTextArea codeArea;
    private JLabel messageLabel;
    private ExpressionPane.ActionPane actionPane;
    private String lastErrorMessage;
    private PropertyMap preferences;
    private List<String> history;
    private int historyIndex;
    private boolean emptyExpressionAllowed;

    /**
     * Constructs a new expression pane.
     *
     * @param requiresBoolExpr if <code>true</code> the expressions are checked to return a boolean value.
     * @param parser           the parser used to check expression syntax
     * @param preferences      a property map which stores expression pane related properties such as the code history
     */
    public ExpressionPane(boolean requiresBoolExpr, Parser parser, PropertyMap preferences) {
        super(new BorderLayout(4, 4));
        undoBuffer = new Stack<String>();
        this.parser = parser;
        booleanExpressionPreferred = requiresBoolExpr;
        history = new LinkedList<String>();
        historyIndex = -1;
        emptyExpressionAllowed = true;
        setPreferences(preferences);
        createUI();
    }

    public int showModalDialog(Window parent, String title) {
        ModalDialog dialog = new ExpressionPaneDialog(parent, title);
        return dialog.show();
    }

    public PropertyMap getPreferences() {
        return preferences;
    }

    public void setPreferences(PropertyMap preferences) {
        this.preferences = preferences;
        if (this.preferences != null) {
            loadCodeHistory();
        }
    }

    public void setEmptyExpressionAllowed(boolean allow) {
        this.emptyExpressionAllowed = allow;
    }

    public boolean isEmptyExpressionAllowed() {
        return emptyExpressionAllowed;
    }

    public void updateCodeHistory() {
        String code = getCode();
        if (code != null) {
            code = code.trim();
            if (!code.equals("")) {
                addToCodeHistory(code, true);
                storeCodeHistory();
            }
        }
    }

    private void addToCodeHistory(String code, boolean head) {
        if (code != null) {
            code = code.trim();
            if (!code.equals("")) {
                if (history.contains(code)) {
                    history.remove(code);
                }
                if (head) {
                    history.add(0, code);
                } else {
                    history.add(code);
                }
            }
        }
    }

    public void loadCodeHistory() {
        if (preferences != null) {
            history.clear();
            for (int index = 0; index < CODE_HISTORY_MAX; index++) {
                final String code = preferences.getPropertyString(CODE_HISTORY_PREFERENCES_PREFIX + index);
                addToCodeHistory(code, false);
            }
            historyIndex = -1;
            updateUIState();
        }
    }

    public void storeCodeHistory() {
        if (history != null && preferences != null) {
            final Iterator<String> iterator = history.iterator();
            for (int index = 0; index < CODE_HISTORY_MAX && iterator.hasNext(); index++) {
                String code = iterator.next();
                preferences.setPropertyString(CODE_HISTORY_PREFERENCES_PREFIX + index, code);
            }
        }
    }

    protected void dispose() {
        undoBuffer.clear();
        parser = null;
        codeArea = null;
        messageLabel = null;
        actionPane = null;
    }

    public void setLeftAccessory(Component component) {
        add(component, BorderLayout.WEST);
    }

    public void setRightAccessory(Component component) {
        add(component, BorderLayout.EAST);
    }

    public void setTopAccessory(Component component) {
        add(component, BorderLayout.NORTH);
    }

    public void setBottomAccessory(Component component) {
        add(component, BorderLayout.SOUTH);
    }

    public JTextArea getCodeArea() {
        return codeArea;
    }

    public boolean isBooleanExpressionPreferred() {
        return booleanExpressionPreferred;
    }

    public void setBooleanExpressionPreferred(boolean booleanExpressionPreferred) {
        this.booleanExpressionPreferred = booleanExpressionPreferred;
    }

    public Parser getParser() {
        return parser;
    }

    public void setParser(Parser parser) {
        Parser oldValue = this.parser;
        if (oldValue == parser) {
            return;
        }
        this.parser = parser;
        firePropertyChange("parser", oldValue, parser);
    }

    public String getCode() {
        return codeArea.getText();
    }

    public void setCode(String newCode) {
        setCode(newCode, false, -1);
    }

    public void setCode(String newCode, boolean recordUndo, int caretPos) {
        String oldCode = codeArea.getText();
        if (recordUndo) {
            pushCodeOnUndoStack(oldCode);
        }
        codeArea.setText(newCode == null ? "" : newCode);
        checkCode(newCode);
        updateUIState();
        if (caretPos >= 0) {
            codeArea.setCaretPosition(caretPos);
        }
        codeArea.requestFocus();
        firePropertyChange("code", oldCode, newCode);
    }

    public void clearCode() {
        setCode("");
    }

    public void selectAllCode() {
        codeArea.selectAll();
        codeArea.requestFocus();
    }

    public void undoLastEdit() {
        if (!undoBuffer.isEmpty()) {
            String code = undoBuffer.pop();
            setCode(code);
            updateUIState();
            codeArea.requestFocus();
        }
    }

    public void insertCodePattern(String pattern) {

        String oldCode = getCode();

        int newCaretPos;

        StringBuffer sb = new StringBuffer(oldCode.length() + 2 * pattern.length());

        int selPos1 = codeArea.getSelectionStart();
        int selPos2 = codeArea.getSelectionEnd();
        if (selPos1 >= 0 && selPos2 >= 0 && selPos1 > selPos2) {
            int temp = selPos1;
            selPos1 = selPos2;
            selPos2 = temp;
        }
        int phPatPos = pattern.indexOf(PLACEHOLDER);
        // If code was selected,
        if (selPos2 > selPos1) {
            String selCode = oldCode.substring(selPos1, selPos2);
            // ...look if there is a placeholder in the pattern
            append(sb, oldCode.substring(0, selPos1));
            if (phPatPos >= 0) {
                // replace placeholder in pattern with selected text
                append(sb, pattern.substring(0, phPatPos));
                append(sb, selCode.trim());
                append(sb, pattern.substring(phPatPos + PLACEHOLDER_LEN));
            } else {
                // replace selected text with pattern
                append(sb, pattern);
            }
            newCaretPos = sb.length();
            append(sb, oldCode.substring(selPos2));
        } else {
            // If no code was selected,
            // ...look if there is a placeholder in the code
            int phPos = oldCode.indexOf(PLACEHOLDER);
            if (phPos >= 0 && phPatPos == -1) {
                // replace placeholder in code with pattern
                append(sb, oldCode.substring(0, phPos));
                append(sb, pattern);
                newCaretPos = sb.length();
                append(sb, oldCode.substring(phPos + PLACEHOLDER_LEN));
            } else {
                // ... look for the caret pos
                int caretPos = codeArea.getCaretPosition();
                // ... and divide code in a left and right part
                String lCode = oldCode.substring(0, caretPos).trim();
                String rCode = oldCode.substring(caretPos).trim();
                // if there is text to the left and the pattern starts with a placeholder
                if (lCode.length() > 0 && pattern.startsWith(PLACEHOLDER)) {
                    // if there is text to the right and the pattern ends with a placeholder
                    if (rCode.length() > 0 && pattern.endsWith(PLACEHOLDER)) {
                        // ...replace both placeholder in the pattern
                        append(sb, lCode);
                        append(sb, pattern.substring(PLACEHOLDER_LEN, pattern.length() - PLACEHOLDER_LEN));
                        newCaretPos = sb.length();
                        append(sb, rCode);
                    } else {
                        // ...replace left placeholder in the pattern
                        append(sb, lCode);
                        append(sb, pattern.substring(PLACEHOLDER_LEN));
                        newCaretPos = sb.length();
                        append(sb, rCode);
                    }
                    // if there is text to the right and the pattern ends with a placeholder
                } else if (rCode.length() > 0 && pattern.endsWith(PLACEHOLDER)) {
                    // ...replace right placeholder in the pattern
                    append(sb, lCode);
                    append(sb, pattern.substring(0, pattern.length() - PLACEHOLDER_LEN));
                    newCaretPos = sb.length();
                    append(sb, rCode);
                } else {
                    // ...insert pattern at caret position
                    append(sb, lCode);
                    append(sb, pattern);
                    newCaretPos = sb.length();
                    append(sb, rCode);
                }
            }
        }

        setCode(sb.toString(), true, newCaretPos);
    }

    private static void append(StringBuffer sb, String s) {
        int n1 = sb.length();
        int n2 = s.length();
        if (n1 > 0 && n2 > 0) {
            char ch1 = sb.charAt(n1 - 1);
            char ch2 = s.charAt(0);
            if (ch1 != ' ' && ch2 != ' ' && ch1 != ',' && ch1 != '(' && ch2 != ')') {
                sb.append(' ');
            }
        }
        sb.append(s);
    }

    public ActionPane createActionPane() {
        return new ActionPane();
    }

    public JButton createInsertButton(final String pattern) {
        JButton button = new JButton(pattern);
        button.setFont(insertCompFont);
        button.setForeground(insertCompColor);
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                insertCodePattern(pattern);
            }
        });
        return button;
    }

    private JComboBox createInsertComboBox(final String title, final String[] patterns) {
        ArrayList<String> itemList = new ArrayList<String>();
        itemList.add(title);
        itemList.addAll(Arrays.asList(patterns));
        final JComboBox comboBox = new JComboBox(itemList.toArray());
        comboBox.setFont(insertCompFont);
        comboBox.setEditable(false);
        comboBox.setForeground(insertCompColor);
        comboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (comboBox.getSelectedIndex() != 0) {
                    insertCodePattern((String) comboBox.getSelectedItem());
                    comboBox.setSelectedIndex(0);
                }
            }
        });
        return comboBox;
    }

    public JList createPatternList() {
        return createPatternList(null);
    }

    public JList createPatternList(final String[] patterns) {
        final JList patternList = new JList(patterns);
        patternList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final ListCellRenderer cellRenderer = patternList.getCellRenderer();
        final Border cellBorder = BorderFactory.createEtchedBorder();
        patternList.setCellRenderer(new ListCellRenderer() {

            public Component getListCellRendererComponent(JList list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                final Component component = cellRenderer.getListCellRendererComponent(list, value, index, isSelected,
                                                                                      cellHasFocus);
                if (component instanceof JComponent) {
                    ((JComponent) component).setBorder(cellBorder);
                }
                return component;
            }
        });
        patternList.setFont(insertCompFont);
        patternList.setBackground(getBackground());
        patternList.setForeground(insertCompColor);
        patternList.addMouseListener(new MouseAdapter() {

            /**
             * Invoked when the mouse has been clicked on a component.
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                final int index = patternList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    final String value = (String) patternList.getModel().getElementAt(index);
                    final String pattern = BandArithmetic.createExternalName(value);
                    insertCodePattern(pattern);
                    patternList.clearSelection();
                }
            }
        });
        return patternList;
    }

    protected JPanel createPatternListPane(final String labelText, final String[] patterns) {
        JList list = createPatternList(patterns);
        JScrollPane scrollableList = new JScrollPane(list);
        JPanel pane = new JPanel(new BorderLayout());
        pane.add(BorderLayout.NORTH, new JLabel(labelText));
        pane.add(BorderLayout.CENTER, scrollableList);
        return pane;
    }


    protected void createUI() {

        codeArea = new JTextArea(10, 40);
        codeArea.setName("codeArea");
        codeArea.setLineWrap(true);
        codeArea.setWrapStyleWord(true);
        codeArea.setFont(exprTextAreaFont);
        codeArea.getDocument().addDocumentListener(new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                checkCode();
            }

            public void removeUpdate(DocumentEvent e) {
                checkCode();
            }

            public void changedUpdate(DocumentEvent e) {
                checkCode();
            }
        });

        actionPane = createActionPane();
        actionPane.setName("actionPane");

        messageLabel = new JLabel();
        messageLabel.setFont(getFont().deriveFont(10.0F));
        messageLabel.setHorizontalAlignment(JLabel.RIGHT);

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(actionPane, BorderLayout.WEST);
        panel.add(messageLabel, BorderLayout.EAST);

        JScrollPane scrollableTextArea = new JScrollPane(codeArea);
        JPanel codePane = new JPanel(new BorderLayout());
        codePane.add(new JLabel("Expression:"), BorderLayout.NORTH);  /*I18N*/
        codePane.add(scrollableTextArea, BorderLayout.CENTER);

        codePane.add(panel, BorderLayout.SOUTH);

        add(codePane, BorderLayout.CENTER);

        setCode("");
    }

    protected JPanel createPatternInsertionPane() {
        final GridBagLayout gbl = new GridBagLayout();
        JPanel patternPane = new JPanel(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.ipadx = 1;
        gbc.ipady = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridy = 0;

        if (booleanExpressionPreferred) {
            final JButton andButton = createInsertButton("@ and @");
            final JButton orButton = createInsertButton("@ or @");
            final JButton notButton = createInsertButton("not @");
            andButton.setName("andButton");
            orButton.setName("orButton");
            notButton.setName("notButton");

            add(patternPane, andButton, gbc);
            gbc.gridy++;
            add(patternPane, orButton, gbc);
            gbc.gridy++;
            add(patternPane, notButton, gbc);
            gbc.gridy++;
        } else {
            final JButton plusButton = createInsertButton("@ + @");
            final JButton minusButton = createInsertButton("@ - @");
            final JButton mulButton = createInsertButton("@ * @");
            final JButton divButton = createInsertButton("@ / @");
            plusButton.setName("plusButton");
            minusButton.setName("minusButton");
            mulButton.setName("mulButton");
            divButton.setName("divButton");

            add(patternPane, plusButton, gbc);
            gbc.gridy++;
            add(patternPane, minusButton, gbc);
            gbc.gridy++;
            add(patternPane, mulButton, gbc);
            gbc.gridy++;
            add(patternPane, divButton, gbc);
            gbc.gridy++;
        }

        final String[] functionNames = getFunctionTemplates();

        final JButton parenButton = createInsertButton("(@)");
        parenButton.setName("parenButton");
        final JComboBox functBox = createInsertComboBox("Functions...", functionNames);
        final JComboBox operBox = createInsertComboBox("Operators...", OPERATOR_PATTERNS);
        final JComboBox constBox = createInsertComboBox("Constants...", CONSTANT_LITERALS);
        functBox.setName("functBox");
        operBox.setName("operBox");
        constBox.setName("constBox");

        add(patternPane, parenButton, gbc);
        gbc.gridy++;
        add(patternPane, constBox, gbc);
        gbc.gridy++;
        add(patternPane, operBox, gbc);
        gbc.gridy++;
        add(patternPane, functBox, gbc);
        gbc.gridy++;

        return patternPane;
    }

    private String[] getFunctionTemplates() {
        final Namespace defaultNamespace = parser.getDefaultNamespace();
        // collect names
        String[] functionNames;
        if (defaultNamespace instanceof NamespaceImpl) {
            final NamespaceImpl namespace = (NamespaceImpl) defaultNamespace;
            final Function[] functions = namespace.getAllFunctions();
            functionNames = new String[functions.length];
            for (int i = 0; i < functions.length; i++) {
                functionNames[i] = createFunctionTemplate(functions[i]);
            }
        } else {
            functionNames = FUNCTION_NAMES;
        }
        // remove double values
        Set<String> set = new HashSet<String>();
        for (String functionName : functionNames) {
            set.add(functionName);
        }
        functionNames = set.toArray(new String[set.size()]);
        Arrays.sort(functionNames);
        return functionNames;
    }

    private static String createFunctionTemplate(Function function) {
        StringBuffer sb = new StringBuffer(16);
        sb.append(function.getName());
        sb.append("(");
        for (int i = 0; i < function.getNumArgs(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("@");
        }
        sb.append(")");
        return sb.toString();
    }

    protected JPanel createDefaultAccessoryPane(Component subAssessory) {

        JPanel patternPane = createPatternInsertionPane();
//        JPanel historyPane = createHistoryPane();
//        _actionPane = createActionPane();

        JPanel p1 = new JPanel(new BorderLayout());
        p1.add(new JLabel(" "), BorderLayout.NORTH);
        p1.add(patternPane, BorderLayout.CENTER);

        JPanel p2 = new JPanel(new BorderLayout(4, 4));
        p2.add(p1, BorderLayout.NORTH);
//        p2.add(historyPane, BorderLayout.CENTER);
//        p2.add(_actionPane, BorderLayout.SOUTH);

        if (subAssessory != null) {
            JPanel p3 = new JPanel(new BorderLayout(4, 4));
            p3.add(subAssessory, BorderLayout.WEST);
            p3.add(p2, BorderLayout.EAST);
            return p3;
        } else {
            return p1;
        }
    }

    private static void add(JPanel panel, Component comp, GridBagConstraints gbc) {
        final GridBagLayout gbl = (GridBagLayout) panel.getLayout();
        gbl.setConstraints(comp, gbc);
        panel.add(comp, gbc);
    }

    protected void checkCode() {
        checkCode(getCode());
    }

    protected void checkCode(String code) {
        lastErrorMessage = null;

        String message;
        Color foreground;
        if ((code == null || code.trim().isEmpty())) {
            if (emptyExpressionAllowed) {
                return;
            } else {
                lastErrorMessage = "Empty expression not allowed.";   /*I18N*/
                message = lastErrorMessage;
                foreground = warnMsgColor;
            }
        } else if (code.contains(PLACEHOLDER)) {
            lastErrorMessage = "Replace '@' by inserting an element.";   /*I18N*/
            message = lastErrorMessage;
            foreground = warnMsgColor;
        } else {
            if (parser != null) {
                try {
                    Term term = parser.parse(code);
                    if (term == null || !booleanExpressionPreferred || term.isB()) {
                        message = "Ok, no errors.";  /*I18N*/
                        foreground = okMsgColor;
                    } else {
                        message = "Ok, but not a boolean expression.";  /*I18N*/
                        foreground = warnMsgColor;
                    }
                } catch (ParseException e) {
                    lastErrorMessage = e.getMessage();
                    message = lastErrorMessage;
                    foreground = warnMsgColor;
                }
            } else {
                message = "Ok, no errors.";  /*I18N*/
                foreground = okMsgColor;
            }
        }
        messageLabel.setText(message);
        messageLabel.setToolTipText(message);
        messageLabel.setForeground(foreground);
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    protected void updateUIState() {
        if (actionPane != null) {
            actionPane.updateUIState();
        }
    }

    private void pushCodeOnUndoStack(String code) {
        if (undoBuffer.isEmpty() || !code.equals(undoBuffer.peek())) {
            undoBuffer.push(code);
        }
    }

    private static String getTypeString(int type) {
        if (type == Term.TYPE_B) {
            return "boolean";
        } else if (type == Term.TYPE_I) {
            return "int";
        } else if (type == Term.TYPE_D) {
            return "double";
        } else {
            return "?";
        }
    }

    public static String getParamTypeString(String name, Term[] args) {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append('(');
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(getTypeString(args[i].getRetType()));
        }
        sb.append(')');
        return sb.toString();
    }

    class ActionPane extends JPanel {

        private AbstractButton selAllButton;
        private AbstractButton undoButton;
        private AbstractButton clearButton;
        private AbstractButton historyUpButton;
        private AbstractButton historyDownButton;

        public ActionPane() {
            createUI();
        }

        protected void createUI() {
            selAllButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/SelectAll24.gif"), false);
            selAllButton.setName("selAllButton");
            selAllButton.setToolTipText("Select all"); /*I18N*/
            selAllButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    selectAllCode();
                }
            });

            clearButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Remove24.gif"), false);
            clearButton.setName("clearButton");
            clearButton.setToolTipText("Clear");   /*I18N*/
            clearButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    clearCode();
                }
            });


            undoButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Undo24.gif"), false);
            undoButton.setName("undoButton");
            undoButton.setToolTipText("Undo");   /*I18N*/
            undoButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    undoLastEdit();
                }
            });

            historyUpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/HistoryUp24.gif"), false);
            historyUpButton.setName("historyUpButton");
            historyUpButton.setToolTipText("Scroll history up");   /*I18N*/
            historyUpButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (history.size() > 0 && historyIndex < history.size()) {
                        historyIndex++;
                        setCode(history.get(historyIndex));
                    }
                }
            });

            historyDownButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/HistoryDown24.gif"),
                                                               false);
            historyDownButton.setName("historyDownButton");
            historyDownButton.setToolTipText("Scroll history down");  /*I18N*/
            historyDownButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (history.size() > 0 && historyIndex >= 0) {
                        final int oldHistoryIndex = historyIndex;
                        historyIndex--;
                        setCode(history.get(oldHistoryIndex));
                    }
                }
            });


            add(selAllButton, BorderLayout.WEST);
            add(clearButton, BorderLayout.CENTER);
            add(undoButton, BorderLayout.EAST);
            add(historyUpButton);
            add(historyDownButton);
        }

        protected void updateUIState() {
            String text = codeArea.getText();
            final boolean hasText = text.length() > 0;
            final boolean canUndo = !undoBuffer.isEmpty();
            final boolean hasHistory = history != null && !history.isEmpty();

            selAllButton.setEnabled(hasText);
            clearButton.setEnabled(hasText);
            undoButton.setEnabled(canUndo);
            historyUpButton.setEnabled(hasHistory && historyIndex < history.size() - 1);
            historyDownButton.setEnabled(hasHistory && historyIndex >= 0);
        }
    }

    class ExpressionPaneDialog extends ModalDialog {

        public ExpressionPaneDialog(Window parent, String title) {
            super(parent, title, ExpressionPane.this, ModalDialog.ID_OK_CANCEL | ModalDialog.ID_HELP,
                  ExpressionPane.HELP_ID);
        }

        @Override
        protected void onOK() {
            updateCodeHistory();
            super.onOK();
        }

        @Override
        protected boolean verifyUserInput() {
            checkCode();
            String lastErrorMessage = getLastErrorMessage();
            if (lastErrorMessage != null) {
                JOptionPane.showMessageDialog(getJDialog(), lastErrorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }
    }
}
