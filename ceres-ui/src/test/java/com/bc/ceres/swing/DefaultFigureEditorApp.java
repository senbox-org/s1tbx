package com.bc.ceres.swing;

import com.bc.ceres.glayer.swing.AdjustableViewScrollPane;
import com.bc.ceres.swing.actions.CopyAction;
import com.bc.ceres.swing.actions.CutAction;
import com.bc.ceres.swing.actions.DeleteAction;
import com.bc.ceres.swing.actions.PasteAction;
import com.bc.ceres.swing.actions.RedoAction;
import com.bc.ceres.swing.actions.SelectAllAction;
import com.bc.ceres.swing.actions.UndoAction;
import com.bc.ceres.swing.figure.AbstractInteractorListener;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.interactions.NewEllipseShapeInteractor;
import com.bc.ceres.swing.figure.interactions.NewLineShapeInteractor;
import com.bc.ceres.swing.figure.interactions.NewPolygonShapeInteractor;
import com.bc.ceres.swing.figure.interactions.NewPolylineShapeInteractor;
import com.bc.ceres.swing.figure.interactions.NewRectangleShapeInteractor;
import com.bc.ceres.swing.figure.interactions.NewTextInteractor;
import com.bc.ceres.swing.figure.interactions.PanInteractor;
import com.bc.ceres.swing.figure.interactions.SelectionInteractor;
import com.bc.ceres.swing.figure.interactions.ZoomInteractor;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import com.bc.ceres.swing.figure.support.FigureEditorPanel;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.support.DefaultSelectionManager;
import com.bc.ceres.swing.undo.support.DefaultUndoContext;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.text.MessageFormat;
import java.util.Locale;

public class DefaultFigureEditorApp {
    private static final Interactor SELECTION_INTERACTOR = new SelectionInteractor();
    private static final Interactor ZOOM_INTERACTOR = new ZoomInteractor();
    private static final Interactor PAN_INTERACTOR = new PanInteractor();
    private static final Interactor NEW_LINE_INTERACTOR = new NewLineShapeInteractor();
    private static final Interactor NEW_RECT_INTERACTOR = new NewRectangleShapeInteractor();
    private static final Interactor NEW_ELLI_INTERACTOR = new NewEllipseShapeInteractor();
    private static final Interactor NEW_POLYLINE_INTERACTOR = new NewPolylineShapeInteractor();
    private static final Interactor NEW_POLYGON_INTERACTOR = new NewPolygonShapeInteractor();
    private static final Interactor NEW_TEXT_INTERACTOR = new NewTextInteractor();

    private final JFrame frame;

    private final UndoAction undoAction;
    private final RedoAction redoAction;
    private final DeleteAction deleteAction;
    private final SelectAllAction selectAllAction;
    private final CutAction cutAction;
    private final CopyAction copyAction;
    private final PasteAction pasteAction;

    public DefaultFigureEditorApp() {
        DefaultSelectionManager selectionManager = new DefaultSelectionManager();
        DefaultUndoContext undoContext = new DefaultUndoContext(this);

        FigureEditorPanel figureEditorPanel = new FigureEditorPanel(undoContext);
        selectionManager.setSelectionContext(figureEditorPanel.getFigureEditor().getSelectionContext());

        undoAction = new UndoAction(undoContext) {
            @Override
            public void execute() {
                super.execute();
                redoAction.updateState();
            }
        };
        redoAction = new RedoAction(undoContext) {
            @Override
            public void execute() {
                super.execute();
                undoAction.updateState();
            }
        };
        cutAction = new CutAction(selectionManager);
        copyAction = new CopyAction(selectionManager);
        pasteAction = new PasteAction(selectionManager);
        selectAllAction = new SelectAllAction(selectionManager);
        deleteAction = new DeleteAction(selectionManager);

        AbstractButton selectButton = createInteractorButton(figureEditorPanel, "S", SELECTION_INTERACTOR);
        AbstractButton zoomButton = createInteractorButton(figureEditorPanel, "Z", ZOOM_INTERACTOR);
        AbstractButton panButton = createInteractorButton(figureEditorPanel, "P", PAN_INTERACTOR);
        AbstractButton newLineButton = createInteractorButton(figureEditorPanel, "L", NEW_LINE_INTERACTOR);
        AbstractButton newRectButton = createInteractorButton(figureEditorPanel, "R", NEW_RECT_INTERACTOR);
        AbstractButton newElliButton = createInteractorButton(figureEditorPanel, "E", NEW_ELLI_INTERACTOR);
        AbstractButton newPLButton = createInteractorButton(figureEditorPanel, "PL", NEW_POLYLINE_INTERACTOR);
        AbstractButton newPGButton = createInteractorButton(figureEditorPanel, "PG", NEW_POLYGON_INTERACTOR);
        AbstractButton newTButton = createInteractorButton(figureEditorPanel, "T", NEW_TEXT_INTERACTOR);

        JToolBar toolBar = new JToolBar();
        toolBar.add(selectButton);
        toolBar.add(zoomButton);
        toolBar.add(panButton);
        toolBar.add(newLineButton);
        toolBar.add(newRectButton);
        toolBar.add(newElliButton);
        toolBar.add(newPLButton);
        toolBar.add(newPGButton);
        toolBar.add(newTButton);

        ButtonGroup group = new ButtonGroup();
        group.add(selectButton);
        group.add(zoomButton);
        group.add(panButton);
        group.add(newLineButton);
        group.add(newRectButton);
        group.add(newElliButton);
        group.add(newPLButton);
        group.add(newPGButton);
        group.add(newTButton);

        figureEditorPanel.getFigureEditor().setInteractor(SELECTION_INTERACTOR);

        FigureCollection drawing = figureEditorPanel.getFigureEditor().getFigureCollection();
        drawing.addFigure(new DefaultShapeFigure(new Rectangle(20, 30, 200, 100), true, DefaultFigureStyle.createShapeStyle(Color.BLUE, Color.GREEN)));
        drawing.addFigure(new DefaultShapeFigure(new Rectangle(90, 10, 100, 200), true, DefaultFigureStyle.createShapeStyle(Color.MAGENTA, Color.ORANGE)));
        drawing.addFigure(new DefaultShapeFigure(new Rectangle(110, 60, 70, 140), false, DefaultFigureStyle.createShapeStyle(Color.MAGENTA, Color.BLACK)));
        drawing.addFigure(new DefaultShapeFigure(new Ellipse2D.Double(50, 100, 80, 80), true, DefaultFigureStyle.createShapeStyle(Color.YELLOW, Color.RED)));
        drawing.addFigure(new DefaultShapeFigure(new Ellipse2D.Double(220, 120, 150, 300), true, DefaultFigureStyle.createShapeStyle(Color.GREEN, Color.BLUE)));
        figureEditorPanel.setPreferredSize(new Dimension(1024, 1024));

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());

        JComponent contentPane = new AdjustableViewScrollPane(figureEditorPanel);
        //JComponent contentPane = figureEditorPanel;

        frame = new JFrame("DrawingApp");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setJMenuBar(menuBar);
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);
        frame.getContentPane().add(contentPane, BorderLayout.CENTER);
        frame.setSize(400, 400);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                String message = MessageFormat.format("" +
                        "An internal error occured!\n" +
                        "Type: {0}\n" +
                        "Message: {1}", e.getClass(), e.getMessage());
                JOptionPane.showMessageDialog(frame, message,
                                              "Internal Error",
                                              JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });

        figureEditorPanel.getFigureEditor().getSelectionContext().addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                System.out.println("selection changed: " + event.getSelection());
            }

            @Override
            public void selectionContextChanged(SelectionChangeEvent event) {
                System.out.println("selection context changed: " + event.getSelection());
            }
        });

        undoContext.addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent event) {
                System.out.println("edit happened: " + event.getEdit());
            }
        });

        selectionManager.getClipboard().addFlavorListener(new FlavorListener() {
            @Override
            public void flavorsChanged(FlavorEvent event) {
                System.out.println("flavors changed: " + event);
            }
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // ok
        }
        Locale.setDefault(Locale.ENGLISH);
        DefaultFigureEditorApp drawingApp = new DefaultFigureEditorApp();
        drawingApp.startUp();
    }

    private void startUp() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
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

    private static AbstractButton createInteractorButton(final FigureEditorPanel figureEditorPanel, String name, final Interactor interactor) {
        final AbstractButton selectButton = new JToggleButton(name);
        selectButton.setSelected(false);
        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                figureEditorPanel.getFigureEditor().setInteractor(interactor);
            }
        });
        interactor.addListener(new AbstractInteractorListener() {
            @Override
            public void interactorActivated(Interactor interactor) {
                selectButton.setSelected(true);
            }

            @Override
            public void interactorDeactivated(Interactor interactor) {
                selectButton.setSelected(false);
            }
        });
        return selectButton;
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

}
