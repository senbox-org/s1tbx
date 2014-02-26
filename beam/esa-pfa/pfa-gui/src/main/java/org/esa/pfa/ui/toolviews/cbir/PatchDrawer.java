/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.pfa.ui.toolviews.cbir;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.pfa.fe.op.Patch;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**

 */
public class PatchDrawer extends JPanel {

    private final static int width = 150;
    private final static int height = 150;
    private final static int margin = 4;

    private static final boolean DEBUG = false;
    private static final Font font = new Font("Ariel", Font.BOLD, 18);

    private static final ImageIcon iconTrue = new ImageIcon(PatchDrawer.class.getClassLoader().getResource("images/check_ball.png"));
    private static final ImageIcon iconFalse = new ImageIcon(PatchDrawer.class.getClassLoader().getResource("images/x_ball.png"));
    private static final ImageIcon iconPatch = new ImageIcon(PatchDrawer.class.getClassLoader().getResource("images/patch.png"));

    private static enum SelectionMode {CHECK, RECT}

    private SelectionMode mode = SelectionMode.CHECK;

    private PatchDrawing selection = null;

    public PatchDrawer(final Patch[] imageList) {
        super(new FlowLayout(FlowLayout.LEADING));
        update(imageList);
    }

    public void update(final Patch[] imageList) {
        this.removeAll();

        if (imageList.length == 0) {
            JLabel label = new JLabel();
            label.setIcon(iconPatch);
            this.add(label);
        } else {
            for (Patch patch : imageList) {
                final PatchDrawing label = new PatchDrawing(patch);
                this.add(label);
            }
        }
        updateUI();
    }

    private class PatchDrawing extends JLabel implements MouseListener {
        private final Patch patch;

        public PatchDrawing(final Patch patch) {
            this.patch = patch;

            if (patch.getImage() != null) {
                setIcon(new ImageIcon(patch.getImage().getScaledInstance(width, height, BufferedImage.SCALE_FAST)));
            }
            addMouseListener(this);
        }

        @Override
        public void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            final Graphics2D g = (Graphics2D) graphics;

            if (DEBUG) {
                g.setColor(Color.WHITE);
                g.fillRect(30, 30, 40, 30);
                g.setColor(Color.RED);
                g.setFont(font);
                g.drawString(Integer.toString(patch.getID()), 35, 50);
            }

            final int label = patch.getLabel();
            if (label > Patch.LABEL_NONE) {
                if (label == Patch.LABEL_RELEVANT) {
                    g.drawImage(iconTrue.getImage(), 0, 0, null);
                } else if (label == Patch.LABEL_IRRELEVANT) {
                    g.drawImage(iconFalse.getImage(), 0, 0, null);
                }
            }

            if (this.equals(selection) && mode == SelectionMode.RECT) {
                g.setColor(Color.CYAN);
                g.setStroke(new BasicStroke(5));
                g.drawRoundRect(0, 0, width, height - 5, 25, 25);
            }
        }

        /**
         * Invoked when the mouse button has been clicked (pressed
         * and released) on a component.
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                int currentLabel = patch.getLabel();
                if (currentLabel != Patch.LABEL_IRRELEVANT) {
                    patch.setLabel(Patch.LABEL_IRRELEVANT);
                }
                if (currentLabel != Patch.LABEL_RELEVANT) {
                    patch.setLabel(Patch.LABEL_RELEVANT);
                }
                repaint();
            }
        }

        /**
         * Invoked when a mouse button has been pressed on a component.
         */
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) {
                JPopupMenu popupMenu = new JPopupMenu();

                if (patch.getFeatures().length > 0) {
                    JMenuItem menuItem = new JMenuItem("Info");
                    menuItem.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            showPatchInfo();
                        }
                    });
                    popupMenu.add(menuItem);
                }
                if (patch.getPathOnServer() != null) {
                    JMenuItem menuItem = new JMenuItem("Show patch product");
                    menuItem.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                showPatchProduct();
                            } catch (IOException ioe) {
                                VisatApp.getApp().handleError("Failed to open product.", ioe);
                            }
                        }
                    });
                    popupMenu.add(menuItem);

                    menuItem = new JMenuItem("Show parent product");
//                    menuItem.addActionListener(new ActionListener() {
//
//                        @Override
//                        public void actionPerformed(ActionEvent e) {
//                            try {
//                                showPatchProduct();
//                            } catch (IOException ioe) {
//                                VisatApp.getApp().handleError("Failed to open product.", ioe);
//                            }
//                        }
//                    });
                    menuItem.setEnabled(false);
                    popupMenu.add(menuItem);

                }

                UIUtils.showPopup(popupMenu, e);

            }
        }

        private void showPatchProduct() throws IOException {
            final VisatApp visat = VisatApp.getApp();
            ProgressMonitorSwingWorker<Band, Void> worker = new ProgressMonitorSwingWorker<Band, Void>(getParent(), "Navigate to patch") {
                @Override
                protected Band doInBackground(ProgressMonitor progressMonitor) throws Exception {
                    File productFile = new File(patch.getPathOnServer(), "patch.dim");
                    Product openProduct = visat.getOpenProduct(productFile);
                    if (openProduct == null) {
                        openProduct = ProductIO.readProduct(productFile);
                        visat.addProduct(openProduct);
                    }
                    if (openProduct == null) {
                        throw new IOException("Failed to open patch product");
                    }
                    String bandName = ProductUtils.findSuitableQuicklookBandName(openProduct);
                    return openProduct.getBand(bandName);
                }

                @Override
                protected void done() {
                    try {
                        visat.openProductSceneView(get());
                    } catch (InterruptedException | ExecutionException e) {
                        VisatApp.getApp().handleError("Failed to open product.", e);
                    }
                }
            };
            worker.execute();
        }


        private void showPatchInfo() {
            PatchInfoDialog patchInfoDialog = new PatchInfoDialog(VisatApp.getApp().getApplicationWindow(), patch);
            patchInfoDialog.getJDialog().pack();
            patchInfoDialog.show();
        }

        /**
         * Invoked when a mouse button has been released on a component.
         */
        @Override
        public void mouseReleased(MouseEvent e) {
        }

        /**
         * Invoked when the mouse enters a component.
         */
        @Override
        public void mouseEntered(MouseEvent e) {
        }

        /**
         * Invoked when the mouse exits a component.
         */
        @Override
        public void mouseExited(MouseEvent e) {
        }

    }

    private class PatchInfoDialog extends ModelessDialog {
        public PatchInfoDialog(Window parent, Patch patch) {
            super(parent, "Patch Info " + patch.getPatchName(), ID_CLOSE, null);
            JTextArea textPane = new JTextArea();
            JScrollPane textScroll = new JScrollPane(textPane);
            textPane.setText(patch.writeFeatures());
            textScroll.setMaximumSize(new Dimension(300, 400));
            textScroll.setPreferredSize(new Dimension(300, 400));
            setContent(textScroll);
        }
    }
}
