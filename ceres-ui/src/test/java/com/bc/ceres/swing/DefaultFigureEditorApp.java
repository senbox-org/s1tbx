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
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.interactions.InsertEllipseFigureInteractor;
import com.bc.ceres.swing.figure.interactions.InsertLineFigureInteractor;
import com.bc.ceres.swing.figure.interactions.InsertPolygonFigureInteractor;
import com.bc.ceres.swing.figure.interactions.InsertPolylineFigureInteractor;
import com.bc.ceres.swing.figure.interactions.InsertRectangleFigureInteractor;
import com.bc.ceres.swing.figure.interactions.PanInteractor;
import com.bc.ceres.swing.figure.interactions.SelectionInteractor;
import com.bc.ceres.swing.figure.interactions.ZoomInteractor;
import com.bc.ceres.swing.figure.support.DefaultFigureFactory;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
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
import java.awt.geom.Line2D;
import java.text.MessageFormat;
import java.util.Locale;

public class DefaultFigureEditorApp {
    private static final Interactor SELECTION_INTERACTOR = new SelectionInteractor();
    private static final Interactor ZOOM_INTERACTOR = new ZoomInteractor();
    private static final Interactor PAN_INTERACTOR = new PanInteractor();
    private static final Interactor NEW_LINE_INTERACTOR = new InsertLineFigureInteractor();
    private static final Interactor NEW_RECT_INTERACTOR = new InsertRectangleFigureInteractor();
    private static final Interactor NEW_ELLI_INTERACTOR = new InsertEllipseFigureInteractor();
    private static final Interactor NEW_POLYLINE_INTERACTOR = new InsertPolylineFigureInteractor();
    private static final Interactor NEW_POLYGON_INTERACTOR = new InsertPolygonFigureInteractor();

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

        JToolBar toolBar = new JToolBar();
        toolBar.add(selectButton);
        toolBar.add(zoomButton);
        toolBar.add(panButton);
        toolBar.add(newLineButton);
        toolBar.add(newRectButton);
        toolBar.add(newElliButton);
        toolBar.add(newPLButton);
        toolBar.add(newPGButton);

        ButtonGroup group = new ButtonGroup();
        group.add(selectButton);
        group.add(zoomButton);
        group.add(panButton);
        group.add(newLineButton);
        group.add(newRectButton);
        group.add(newElliButton);
        group.add(newPLButton);
        group.add(newPGButton);

        figureEditorPanel.getFigureEditor().setInteractor(SELECTION_INTERACTOR);
        FigureFactory figureFactory = new DefaultFigureFactory();
        FigureCollection drawing = figureEditorPanel.getFigureEditor().getFigureCollection();
        drawing.addFigure(figureFactory.createPolygonalFigure(new Rectangle(20, 30, 200, 100), DefaultFigureStyle.createShapeStyle(Color.BLUE, Color.GREEN)));
        drawing.addFigure(figureFactory.createPolygonalFigure(new Rectangle(90, 10, 100, 200), DefaultFigureStyle.createShapeStyle(Color.MAGENTA, Color.ORANGE)));
        drawing.addFigure(figureFactory.createLinealFigure(new Rectangle(110, 60, 70, 140), DefaultFigureStyle.createShapeStyle(Color.MAGENTA, Color.BLACK)));
        drawing.addFigure(figureFactory.createLinealFigure(new Line2D.Double(200, 100, 300, 100), DefaultFigureStyle.createShapeStyle(Color.MAGENTA, Color.BLACK)));
        drawing.addFigure(figureFactory.createPolygonalFigure(new Ellipse2D.Double(50, 100, 80, 80), DefaultFigureStyle.createShapeStyle(Color.YELLOW, Color.RED)));
        drawing.addFigure(figureFactory.createPolygonalFigure(new Ellipse2D.Double(220, 120, 150, 300), DefaultFigureStyle.createShapeStyle(Color.GREEN, Color.BLUE)));
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
