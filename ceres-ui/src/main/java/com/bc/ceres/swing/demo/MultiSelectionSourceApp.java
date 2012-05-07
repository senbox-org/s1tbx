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

package com.bc.ceres.swing.demo;

import com.bc.ceres.swing.actions.CopyAction;
import com.bc.ceres.swing.actions.CutAction;
import com.bc.ceres.swing.actions.DeleteAction;
import com.bc.ceres.swing.actions.PasteAction;
import com.bc.ceres.swing.actions.RedoAction;
import com.bc.ceres.swing.actions.SelectAllAction;
import com.bc.ceres.swing.actions.UndoAction;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionManager;
import com.bc.ceres.swing.selection.support.DefaultSelectionManager;
import com.bc.ceres.swing.selection.support.ListSelectionContext;
import com.bc.ceres.swing.undo.UndoContext;
import com.bc.ceres.swing.undo.support.DefaultUndoContext;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.Locale;

public class MultiSelectionSourceApp {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // ok
        }
        Locale.setDefault(Locale.ENGLISH);
        MultiSelectionSourceApp drawingApp = new MultiSelectionSourceApp();
        drawingApp.startUp();
    }


    private final DefaultSelectionManager selectionManager;


    private JFrame frame;
    private JDialog dialog1;
    private JDialog dialog2;
    private JDialog dialog3;

    private UndoAction undoAction;
    private RedoAction redoAction;
    private DeleteAction deleteAction;
    private SelectAllAction selectAllAction;
    private CutAction cutAction;
    private CopyAction copyAction;
    private PasteAction pasteAction;
    private Action[] actions;

    public MultiSelectionSourceApp() {


        UndoContext undoContext = new DefaultUndoContext(this);
        undoAction = new UndoAction(undoContext);

        selectionManager = new DefaultSelectionManager(this);
        redoAction = new RedoAction(undoContext);
        cutAction = new CutAction(selectionManager);
        copyAction = new CopyAction(selectionManager);
        pasteAction = new PasteAction(selectionManager);
        selectAllAction = new SelectAllAction(selectionManager);
        deleteAction = new DeleteAction(selectionManager);

        actions = new Action[]{
                undoAction,
                redoAction,
                cutAction,
                copyAction,
                pasteAction,
                selectAllAction,
                deleteAction,
        };

        frame = new MyFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        dialog1 = createDialog("Context 1", new String[]{
                "die Gegenauslese",
                "die Abgrenzung",
                "die Anwahl",
                "die Auslese",
                "die Aussonderung",
                "das Aussortieren",
                "die Auswahl",
                "die Kollektion",
                "die Markierung",
                "die Selektierung",
                "die Selektion",
                "das Sortiment",
                "die Trennwirkung",
                "die Wahl",
                "der Wahlschalter",
        });
        dialog2 = createDialog("Context 2", new String[]{
                "die Gefahr",
                "das Risiko  Pl.: die Risiken",
                "das Versicherungsrisiko",
                "das Wagnis",
                "das Maximalblatt",
                "die Gesamtversicherung",
                "Versicherung gegen alle Risiken",
                "hochgefährlich",
        });
        dialog3 = createDialog("Context 3", new String[]{
                "abzetteln",
                "annullieren",
                "aufbinden",
                "aufknoten",
                "aufmachen",
                "löschen",
                "lösen",
                "rücksetzen",
                "trennen",
        });


        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());
        menuBar.add(createViewMenu());
        menuBar.setFocusable(false);
        frame.setJMenuBar(menuBar);

        final String[] listContent = {
                "roster die Dienstliste",
                "roster der Dienstplan",
                "roster die Liste",
                "roster das Mitgliedsverzeichnis",
                "duty-roster der Dienstplan",
                "duty roster der Dienstplan",
                "membership roster die Mitgliederliste",
                "membership roster das Mitgliedsbuch",
                "personnel roster die Stammrolle",
                "staff roster die Diensteinteilung",
                "staff roster [tech.] der Dienstplan",
                "duty roster turn [tech.] die Diensteinteilung",
                "rolling stock roster [tech.] der Dienstplan",
        };

        PageComponent pageComponent = new MyPageComponent(frame, listContent);
        JComponent control = pageComponent.createControl(this);
        frame.add(control, BorderLayout.CENTER);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                String message = MessageFormat.format("" +
                        "An internal error occurred!\n" +
                        "Type: {0}\n" +
                        "Message: {1}", e.getClass(), e.getMessage());
                JOptionPane.showMessageDialog(frame, message,
                                              "Internal Error",
                                              JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });

        getSelectionManager().addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                System.out.println("appContext: selection change: " + event.getSelection());
            }

            @Override
            public void selectionContextChanged(SelectionChangeEvent event) {
                System.out.println("appContext: selection context change: " + event.getSelection());
            }
        });

        undoContext.addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent event) {
                System.out.println("appContext: edit happened: " + event.getEdit());
            }
        });
    }


    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public void bindEditKeys(JComponent component) {
        for (Action action : actions) {
            Object value = action.getValue(Action.ACCELERATOR_KEY);
            if (value instanceof KeyStroke) {
                Object actionId = action.getValue(Action.ACTION_COMMAND_KEY);
                component.getInputMap().put((KeyStroke) value, actionId);
                component.getActionMap().put(actionId, action);
            }
        }
    }

    private void startUp() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.pack();
                frame.setLocation(50, 50);
                frame.setVisible(true);

                dialog1.pack();
                dialog1.setLocation(frame.getX() + frame.getWidth(), frame.getY());
                dialog1.setVisible(true);

                dialog2.pack();
                dialog2.setLocation(dialog1.getX() + dialog1.getWidth(), dialog1.getY());
                dialog2.setVisible(true);

                dialog3.pack();
                dialog3.setLocation(dialog2.getX() + dialog2.getWidth(), dialog2.getY());
                dialog3.setVisible(true);
            }
        });
    }

    private JMenu createFileMenu() {
        JMenu menu = new JMenu("File");
        menu.add(new ExitAction());
        return menu;
    }

    private JMenu createEditMenu() {
        JMenu menu = new JMenu("Edit");
        menu.add(undoAction);
        menu.add(redoAction);
        menu.addSeparator();
        menu.add(cutAction);
        menu.add(copyAction);
        menu.add(pasteAction);
        menu.addSeparator();
        menu.add(selectAllAction);
        menu.addSeparator();
        menu.add(deleteAction);
        return menu;
    }

    private JMenu createViewMenu() {
        JMenu menu = new JMenu("View");
        menu.add(new ViewAction(dialog1));
        menu.add(new ViewAction(dialog2));
        menu.add(new ViewAction(dialog3));
        return menu;
    }

    private JDialog createDialog(String title, String[] listContent) {
        JDialog dialog = new JDialog(frame, title, false);
        MyPageComponent myPageComponent = new MyPageComponent(dialog, listContent);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(myPageComponent.createControl(this));
        return dialog;
    }

    public interface PageComponent {
        JComponent createControl(MultiSelectionSourceApp context);

        void windowActivated();

        void windowDeactivated();
    }

    private class ExitAction extends AbstractAction {
        private ExitAction() {
            putValue(Action.NAME, "Exit");
            putValue(Action.ACTION_COMMAND_KEY, getClass().getName());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            frame.dispose();
        }
    }

    private static class ViewAction extends AbstractAction {
        private final JDialog dialog;

        public ViewAction(JDialog dialog) {
            this.dialog = dialog;
            putValue(Action.NAME, dialog.getTitle());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dialog.setVisible(true);
        }
    }

    private class MyFrame extends JFrame {
        public MyFrame() throws java.awt.HeadlessException {
            super(MultiSelectionSourceApp.this.getClass().getSimpleName());
        }
    }


    private static class MyPageComponent implements PageComponent {
        private MultiSelectionSourceApp appContext;
        private final String[] listContent;
        private ListSelectionContext selectionContext;

        public MyPageComponent(Window window, String[] listContent) {
            this.listContent = listContent;
            window.setFocusable(true);
            window.addWindowListener(new WindowAdapter() {

                @Override
                public void windowActivated(WindowEvent e) {
                    MyPageComponent.this.windowActivated();
                }

                @Override
                public void windowDeactivated(WindowEvent e) {
                    MyPageComponent.this.windowDeactivated();
                }


                @Override
                public void windowGainedFocus(WindowEvent e) {
                    System.out.println("e = " + e);
                    MyPageComponent.this.windowActivated();
                }

                @Override
                public void windowLostFocus(WindowEvent e) {
                    System.out.println("e = " + e);
                    MyPageComponent.this.windowDeactivated();
                }
            });
        }

        @Override
        public JComponent createControl(MultiSelectionSourceApp appContext) {
            this.appContext = appContext;

            DefaultListModel listModel = new DefaultListModel();
            for (String item : listContent) {
                listModel.addElement(item);
            }

            final JList list = new JList(listModel);
            selectionContext = new ListSelectionContext(list);

            appContext.bindEditKeys(list);

            JScrollPane scrollPane = new JScrollPane(list);
            JPanel panel = new JPanel(new BorderLayout(3, 3));
            panel.setBorder(new EmptyBorder(3, 3, 3, 3));
            panel.add(scrollPane, BorderLayout.CENTER);
            return panel;
        }

        @Override
        public void windowActivated() {
            appContext.getSelectionManager().setSelectionContext(selectionContext);
        }

        @Override
        public void windowDeactivated() {
        }
    }
}