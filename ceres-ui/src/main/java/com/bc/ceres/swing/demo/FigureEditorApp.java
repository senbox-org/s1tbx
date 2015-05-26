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
import com.bc.ceres.swing.figure.ShapeFigure;
import com.bc.ceres.swing.figure.interactions.InsertEllipseFigureInteractor;
import com.bc.ceres.swing.figure.interactions.InsertLineFigureInteractor;
import com.bc.ceres.swing.figure.interactions.InsertPolygonFigureInteractor;
import com.bc.ceres.swing.figure.interactions.InsertPolylineFigureInteractor;
import com.bc.ceres.swing.figure.interactions.InsertRectangleFigureInteractor;
import com.bc.ceres.swing.figure.interactions.PanInteractor;
import com.bc.ceres.swing.figure.interactions.SelectionInteractor;
import com.bc.ceres.swing.figure.interactions.ZoomInteractor;
import com.bc.ceres.swing.figure.support.DefaultFigureCollection;
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
import javax.swing.JFileChooser;
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
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.prefs.Preferences;

public abstract class FigureEditorApp {
    private static final Interactor SELECTION_INTERACTOR = new SelectionInteractor();
    private static final Interactor ZOOM_INTERACTOR = new ZoomInteractor();
    private static final Interactor PAN_INTERACTOR = new PanInteractor();
    private static final Interactor NEW_LINE_INTERACTOR = new InsertLineFigureInteractor();
    private static final Interactor NEW_RECT_INTERACTOR = new InsertRectangleFigureInteractor();
    private static final Interactor NEW_ELLI_INTERACTOR = new InsertEllipseFigureInteractor();
    private static final Interactor NEW_POLYLINE_INTERACTOR = new InsertPolylineFigureInteractor();
    private static final Interactor NEW_POLYGON_INTERACTOR = new InsertPolygonFigureInteractor();

    private JFrame frame;

    private UndoAction undoAction;
    private RedoAction redoAction;
    private DeleteAction deleteAction;
    private SelectAllAction selectAllAction;
    private CutAction cutAction;
    private CopyAction copyAction;
    private PasteAction pasteAction;
    private FigureEditorPanel figureEditorPanel;

    static {
        Locale.setDefault(Locale.ENGLISH);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // ok
        }
    }

    protected FigureEditorApp() {
    }

    protected abstract FigureFactory getFigureFactory();

    protected abstract void loadFigureCollection(File file, FigureCollection figureCollection) throws IOException;

    protected abstract void storeFigureCollection(FigureCollection figureCollection, File file) throws IOException;

    private void init() {

        DefaultSelectionManager selectionManager = new DefaultSelectionManager();
        DefaultUndoContext undoContext = new DefaultUndoContext(this);

        figureEditorPanel = new FigureEditorPanel(undoContext, new DefaultFigureCollection(), getFigureFactory());
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
        figureEditorPanel.setPreferredSize(new Dimension(1024, 1024));

        FigureCollection drawing = figureEditorPanel.getFigureEditor().getFigureCollection();

        FigureFactory figureFactory = figureEditorPanel.getFigureEditor().getFigureFactory();
        final ShapeFigure polygonFigure1 = figureFactory.createPolygonFigure(new Rectangle(20, 30, 200, 100), DefaultFigureStyle.createPolygonStyle(Color.BLUE, Color.GREEN));
        if (polygonFigure1 != null) {
            drawing.addFigure(polygonFigure1);
        }
        final ShapeFigure polygonFigure2 = figureFactory.createPolygonFigure(new Rectangle(90, 10, 100, 200), DefaultFigureStyle.createPolygonStyle(Color.MAGENTA, Color.ORANGE));
        if (polygonFigure2 != null) {
            drawing.addFigure(polygonFigure2);
        }
        Path2D linePath = rectPath(true, 110, 60, 70, 140);
        final ShapeFigure lineFigure1 = figureFactory.createLineFigure(linePath, DefaultFigureStyle.createLineStyle(Color.GRAY));
        if (lineFigure1 != null) {
            drawing.addFigure(lineFigure1);
        }

        linePath = new Path2D.Double();
        linePath.moveTo(110, 60);
        linePath.lineTo(110 + 70, 60);
        linePath.lineTo(110 + 70, 60 + 140);
        final ShapeFigure lineFigure2 = figureFactory.createLineFigure(linePath, DefaultFigureStyle.createLineStyle(Color.BLACK));
        if (lineFigure2 != null) {
            drawing.addFigure(lineFigure2);
        }

        linePath = new Path2D.Double();
        linePath.moveTo(200, 100);
        linePath.lineTo(300, 200);
        final ShapeFigure lineFigure3 = figureFactory.createLineFigure(linePath, DefaultFigureStyle.createLineStyle(Color.MAGENTA, new BasicStroke(5.0f)));
        if (lineFigure3 != null) {
            drawing.addFigure(lineFigure3);
        }

        final ShapeFigure polygonFigure3 = figureFactory.createPolygonFigure(new Ellipse2D.Double(50, 100, 80, 80), DefaultFigureStyle.createPolygonStyle(Color.YELLOW, Color.RED));
        if (polygonFigure3 != null) {
            drawing.addFigure(polygonFigure3);
        }
        final ShapeFigure polygonFigure4 = figureFactory.createPolygonFigure(new Ellipse2D.Double(220, 120, 150, 300), DefaultFigureStyle.createPolygonStyle(Color.GREEN, Color.BLUE));
        if (polygonFigure4 != null) {
            drawing.addFigure(polygonFigure4);
        }

        Area area = new Area(new Rectangle(0, 0, 100, 100));
        area.subtract(new Area(new Rectangle(25, 25, 50, 50)));
        area.add(new Area(new Rectangle(75, 75, 50, 50)));
        area.subtract(new Area(new Rectangle(87, 87, 25, 25)));
        area.subtract(new Area(new Rectangle(-26, -26, 50, 50)));
//        drawing.addFigure(figureFactory.createPolygonalFigure(area, DefaultFigureStyle.createShapeStyle(Color.RED, Color.ORANGE)));

        Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        path.append(rectPath(true, 0, 0, 100, 100), false);
        path.append(rectPath(false, 12, 12, 25, 25), false);
        path.append(rectPath(false, 65, 65, 25, 25), false);
        DefaultFigureStyle shapeStyle = DefaultFigureStyle.createPolygonStyle(new Color(0, 0, 255, 127), Color.ORANGE);
        final ShapeFigure polygonFigure5 = figureFactory.createPolygonFigure(path, shapeStyle);
        if (polygonFigure5 != null) {
            drawing.addFigure(polygonFigure5);
        }

        for (int i = 0; i < 50; i++) {
            DefaultFigureStyle pointStyle = new DefaultFigureStyle();
            pointStyle.setStrokeColor(new Color(0, 0, 64));
            pointStyle.setStrokeOpacity(0.9);
            pointStyle.setStrokeWidth(1.0);

            int type = i % 4;
            if (type == 0) {
                pointStyle.setSymbolName("pin");
                pointStyle.setFillColor(new Color(128, 128, 255));
                pointStyle.setFillOpacity(0.7);
            } else if (type == 1) {
                pointStyle.setSymbolName("circle");
                pointStyle.setFillColor(new Color(128, 128, 0));
                pointStyle.setFillOpacity(0.7);
            } else if (type == 2) {
                pointStyle.setSymbolName("star");
            } else {
                pointStyle.setSymbolImagePath("/com/bc/ceres/swing/update/icons/list-add.png");
                pointStyle.setSymbolRefX(8.0);
                pointStyle.setSymbolRefY(8.0);
            }
            drawing.addFigure(figureFactory.createPointFigure(new Point2D.Double(i * 10, i * 10), pointStyle));
        }

        /*
        Area a2 = new Area();
        a2.add(new Area(new Rectangle(0, 0, 100, 100)));
        a2.subtract(new Area(new Rectangle(12, 12, 25, 25)));
        a2.subtract(new Area(new Rectangle(65, 65, 25, 25)));
        a2.add(new Area(new Rectangle(200, 200, 100, 100)));
        a2.subtract(new Area(new Rectangle(200 + 12, 200 + 12, 25, 25)));
        a2.subtract(new Area(new Rectangle(200 + 65, 200 + 65, 25, 25)));
        drawing.addFigure(figureFactory.createPolygonalFigure(a2, DefaultFigureStyle.createShapeStyle(new Color(255, 255, 0, 127), Color.ORANGE)));
        */

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());

        JComponent contentPane = new AdjustableViewScrollPane(figureEditorPanel);

        frame = new JFrame(getClass().getSimpleName());
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
                                                              "An internal error occurred!\n" +
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


    private static Path2D rectPath(boolean clockwise, int x, int y, int w, int h) {
        Path2D.Double linePath = new Path2D.Double();
        linePath.moveTo(x, y);
        if (clockwise) {
            linePath.lineTo(x, y + h);
            linePath.lineTo(x + w, y + h);
            linePath.lineTo(x + w, y);
        } else {
            linePath.lineTo(x + w, y);
            linePath.lineTo(x + w, y + h);
            linePath.lineTo(x, y + h);
        }
        linePath.lineTo(x, y);
        linePath.closePath();
        return linePath;
    }

    public static void run(FigureEditorApp drawingApp) {
        drawingApp.init();
        drawingApp.run();
    }

    public JFrame getFrame() {
        return frame;
    }

    private void run() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
            }
        });
    }

    private JMenu createFileMenu() {
        JMenu menu = new JMenu("File");
        menu.add(new OpenAction());
        menu.add(new SaveAsAction());
        menu.addSeparator();
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
                boolean b = selectButton.isSelected();
                System.out.println("b = " + b);
                if (b) {
                    figureEditorPanel.getFigureEditor().setInteractor(interactor);
                }
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

    private class OpenAction extends AbstractAction {
        private OpenAction() {
            putValue(Action.NAME, "Open...");
            putValue(Action.ACTION_COMMAND_KEY, getClass().getName());
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            File lastDir = new File(Preferences.userNodeForPackage(FigureEditorApp.class).get("lastDir", "."));
            JFileChooser chooser = new JFileChooser(lastDir);
            chooser.setAcceptAllFileFilterUsed(true);
            int i = chooser.showOpenDialog(frame);
            if (i == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                Preferences.userNodeForPackage(FigureEditorApp.class).put("lastDir", chooser.getCurrentDirectory().getPath());
                figureEditorPanel.getFigureEditor().getFigureSelection().removeAllFigures();
                figureEditorPanel.getFigureEditor().getFigureCollection().removeAllFigures();
                try {
                    loadFigureCollection(chooser.getSelectedFile(), figureEditorPanel.getFigureEditor().getFigureCollection());
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(getFrame(), "Error: " + e.getMessage());
                }
            }
        }
    }

    private class SaveAsAction extends AbstractAction {
        private SaveAsAction() {
            putValue(Action.NAME, "Save As...");
            putValue(Action.ACTION_COMMAND_KEY, getClass().getName());
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            File lastDir = new File(Preferences.userNodeForPackage(FigureEditorApp.class).get("lastDir", "."));
            JFileChooser chooser = new JFileChooser(lastDir);
            chooser.setAcceptAllFileFilterUsed(true);
            int i = chooser.showSaveDialog(frame);
            if (i == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                Preferences.userNodeForPackage(FigureEditorApp.class).put("lastDir", chooser.getCurrentDirectory().getPath());
                try {
                    storeFigureCollection(figureEditorPanel.getFigureEditor().getFigureCollection(), chooser.getSelectedFile());
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(getFrame(), "Error: " + e.getMessage());
                }
            }
        }
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
