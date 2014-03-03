package org.esa.pfa.ui.toolviews.cbir;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.pfa.fe.PFAApplicationDescriptor;
import org.esa.pfa.fe.op.Feature;
import org.esa.pfa.fe.op.Patch;
import org.esa.pfa.ordering.ProductOrder;
import org.esa.pfa.ordering.ProductOrderBasket;
import org.esa.pfa.ordering.ProductOrderService;
import org.esa.pfa.search.CBIRSession;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.border.LineBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author Norman Fomferra
 */
public class PatchContextMenuFactory {

    public JPopupMenu createContextMenu(Patch patch) {
        List<Action> actionList = getContextActions(patch);
        if (actionList.isEmpty()) {
            return null;
        }
        JPopupMenu popupMenu = new JPopupMenu();
        for (Action action : actionList) {
            popupMenu.add(action);
        }
        return popupMenu;
    }

    public List<Action> getContextActions(final Patch patch) {
        List<Action> actionList = new ArrayList<>();

        Action showPatchInfoAction = createShowPatchInfoAction(patch);
        if (showPatchInfoAction != null) {
            actionList.add(showPatchInfoAction);
        }

        Action openPatchProductAction = createOpenPatchProductAction(patch);
        if (showPatchInfoAction != null) {
            actionList.add(openPatchProductAction);
        }

        Action openParentProductAction = createOpenParentProductAction(patch);
        if (openParentProductAction != null) {
            actionList.add(openParentProductAction);
        }

        Action orderParentProductAction = createOrderParentProductAction(patch);
        if (orderParentProductAction != null) {
            actionList.add(orderParentProductAction);
        }

        return actionList;
    }

    public Action createOrderParentProductAction(final Patch patch) {
        final String parentProductName = getParentProductName(patch);
        if (parentProductName == null) {
            return null;
        }

        return new AbstractAction("Order Parent Product") {

            @Override
            public void actionPerformed(ActionEvent e) {
                orderParentProduct(patch);
            }

            private void orderParentProduct(Patch patch) {
                ProductOrderBasket productOrderBasket = CBIRSession.Instance().getProductOrderBasket();
                ProductOrder productOrder = productOrderBasket.getProductOrder(parentProductName);
                if (productOrder != null) {
                    if (productOrder.getState() == ProductOrder.State.DOWNLOADED) {
                        int resp = JOptionPane.showConfirmDialog(VisatApp.getApp().getApplicationWindow(),
                                                                 String.format("Data product\n%s\nhas already been downloaded.\nOpen it?",
                                                                               parentProductName),
                                                                 "Order Product", JOptionPane.OK_CANCEL_OPTION,
                                                                 JOptionPane.QUESTION_MESSAGE);
                        if (resp == JOptionPane.OK_OPTION) {
                            createOpenParentProductAction(patch);
                        }
                    } else {
                        JOptionPane.showMessageDialog(VisatApp.getApp().getApplicationWindow(),
                                                      String.format("Data product\n%s\nis already in the basket.",
                                                                    parentProductName));
                    }
                    return;
                }

                int resp = JOptionPane.showConfirmDialog(VisatApp.getApp().getApplicationWindow(),
                                                         String.format("Data product\n%s\nwill be ordered.\nProceed?",
                                                                       parentProductName),
                                                         "Order Product",
                                                         JOptionPane.OK_CANCEL_OPTION,
                                                         JOptionPane.QUESTION_MESSAGE);
                if (resp == JOptionPane.OK_OPTION) {
                    ProductOrderService productOrderService = CBIRSession.Instance().getProductOrderService();
                    productOrderService.submit(new ProductOrder(parentProductName));
                }
            }
        };
    }


    public Action createOpenParentProductAction(final Patch patch) {

        String parentProductName = getParentProductName(patch);
        if (parentProductName == null) {
            return null;
        }

        // O-oh, no good design here...
        CBIRSession session = CBIRSession.Instance();
        PFAApplicationDescriptor applicationDescriptor = session.getApplicationDescriptor();
        if (applicationDescriptor == null) {
            // session not init?
            return null;
        }

        File localProductDir = applicationDescriptor.getLocalProductDir();
        if (localProductDir == null) {
            // config property not set?
            return null;
        }

        final File parentProductFile = new File(localProductDir, parentProductName);
        if (!parentProductFile.exists()) {
            return null;
        }

        return new AbstractAction("Open Parent Product") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    showProduct(patch, parentProductFile);
                } catch (Exception ioe) {
                    VisatApp.getApp().handleError("Failed to open parent product.", ioe);
                }
            }
        };
    }

    public static String getParentProductName(final Patch patch) {
        String pathOnServer = patch.getPathOnServer();
        if (pathOnServer == null) {
            return null;
        }

        File serverPathToPatch = new File(pathOnServer);
//        System.out.println("serverPathToPatch = " + serverPathToPatch);
        String parentProductFexName = serverPathToPatch.getParentFile().getName();
//        System.out.println("parentProductFexName = " + parentProductFexName);
        String parentProductName = parentProductFexName.substring(0, parentProductFexName.length() - 4); // prune ".fex" extension
//        System.out.println("parentProductName = " + parentProductName);
        return parentProductName;
    }

    public Action createOpenPatchProductAction(final Patch patch) {
        if (patch.getPathOnServer() == null) {
            return null;
        }

        final File patchProductFile = new File(patch.getPathOnServer(), "patch.dim");
        if (!patchProductFile.exists()) {
            return null;
        }

        return new AbstractAction("Open Patch Product") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    showProduct(patch, patchProductFile);
                } catch (Exception ioe) {
                    VisatApp.getApp().handleError("Failed to open patch product.", ioe);
                }
            }
        };
    }

    public Action createShowPatchInfoAction(final Patch patch) {
        if (patch.getFeatures().length == 0) {
            return null;
        }

        return new AbstractAction("Show Patch Info") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPatchInfo(patch);
            }

            private void showPatchInfo(Patch patch) {
                PatchInfoDialog patchInfoDialog = new PatchInfoDialog(VisatApp.getApp().getApplicationWindow(), patch, createOtherButtons(patch));
                patchInfoDialog.show();
            }

            private JButton[] createOtherButtons(Patch patch) {
                Action openParentProductAction = createOpenParentProductAction(patch);
                if (openParentProductAction != null) {
                    JButton button = new JButton(openParentProductAction);
                    return new JButton[]{button};
                }
                return null;
            }
        };

    }


    private static void showProduct(final Patch patch, File patchProductFile) throws Exception {
        final Product product = openProduct(patchProductFile);
        if (product == null) {
            return;
        }
        String bandName = ProductUtils.findSuitableQuicklookBandName(product);
        if (bandName == null) {
            return;
        }

        Band band = product.getBand(bandName);
        if (band == null) {
            return;
        }

        final Dimension patchDimension;
        PFAApplicationDescriptor applicationDescriptor = CBIRSession.Instance().getApplicationDescriptor();
        if (applicationDescriptor != null) {
            patchDimension = applicationDescriptor.getPatchDimension();
        } else {
            patchDimension = null;
        }

        VisatApp visatApp = VisatApp.getApp();
        JInternalFrame internalFrame = visatApp.findInternalFrame(band);
        if (internalFrame != null) {
            internalFrame.setSelected(true);
            ProductSceneView sceneView = getProductSceneView(internalFrame);
            if (sceneView != null && patchDimension != null) {
                zoomToPatch(sceneView, product, patch, patchDimension);
            }
        } else {
            if (patchDimension != null) {
                zoomToPatchOnViewSelected(product, patch, patchDimension);
            }
            visatApp.openProductSceneView(band);
        }
    }

    private static void zoomToPatchOnViewSelected(final Product product, final Patch patch, final Dimension patchDimension) {
        VisatApp visatApp = VisatApp.getApp();
        final InternalFrameAdapter sceneViewTracker = new InternalFrameAdapter() {

            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                JInternalFrame internalFrame = e.getInternalFrame();
                ProductSceneView sceneView = getProductSceneView(internalFrame);
                if (sceneView != null) {
                    zoomToPatch(sceneView, product, patch, patchDimension);
                    VisatApp.getApp().removeInternalFrameListener(this);
                }
            }

        };
        visatApp.addInternalFrameListener(sceneViewTracker);
        Timer timer = new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VisatApp.getApp().removeInternalFrameListener(sceneViewTracker);
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static void zoomToPatch(ProductSceneView sceneView, Product product, Patch patch, Dimension patchDimension) {
        if (sceneView != null && sceneView.getProduct() == product) {
            Rectangle modelRect = new Rectangle(patch.getPatchX() * patchDimension.width,
                                                patch.getPatchY() * patchDimension.height,
                                                patchDimension.width, patchDimension.height);
            modelRect.grow(patchDimension.width / 2, patchDimension.height / 2);
//            System.out.println("modelRect = " + modelRect);
            sceneView.getLayerCanvas().setInitiallyZoomingAll(false);
            sceneView.zoom(modelRect);
        }
    }

    private static ProductSceneView getProductSceneView(JInternalFrame internalFrame) {
        return internalFrame != null && internalFrame.getContentPane() instanceof ProductSceneView ? (ProductSceneView) internalFrame.getContentPane() : null;
    }

    public static Product openProduct(final File productFile) throws Exception {
        final VisatApp visat = VisatApp.getApp();
        Product product = visat.getOpenProduct(productFile);
        if (product != null) {
            return product;
        }
        ProgressMonitorSwingWorker<Product, Void> worker = new ProgressMonitorSwingWorker<Product, Void>(VisatApp.getApp().getApplicationWindow(), "Navigate to patch") {
            @Override
            protected Product doInBackground(ProgressMonitor progressMonitor) throws Exception {
                return ProductIO.readProduct(productFile);
            }

            @Override
            protected void done() {
                try {
                    visat.getProductManager().addProduct(get());
                } catch (InterruptedException | ExecutionException e) {
                    VisatApp.getApp().handleError("Failed to open product.", e);
                }
            }
        };
        worker.executeWithBlocking();
        return worker.get();
    }


    private static class PatchInfoDialog extends ModelessDialog {

        public PatchInfoDialog(Window parent, Patch patch, JButton[] buttons) {
            super(parent, "Patch Info - " + patch.getPatchName(), ID_CLOSE, buttons, null);

            Object[][] array = getFeatureTableData(patch);
            JTable table = new JTable(new DefaultTableModel(array, new Object[]{"Name", "Value"}));

            JPanel contentPanel = new JPanel(new BorderLayout(2, 2));
            if (patch.getImage() != null) {

                JLabel imageCanvas = new JLabel(new ImageIcon(patch.getImage()));
                imageCanvas.setBorder(new LineBorder(Color.DARK_GRAY));

                JPanel compRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
                compRow.add(imageCanvas);
                compRow.setPreferredSize(imageCanvas.getPreferredSize());

                contentPanel.add(compRow, BorderLayout.NORTH);
            }
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(200, 80));
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            setContent(contentPanel);
        }

        private Object[][] getFeatureTableData(Patch patch) {
            ArrayList<Object[]> data = new ArrayList<>();
            for (Feature feature : patch.getFeatures()) {
                data.add(new Object[]{feature.getName(), feature.getValue()});
            }
            return data.toArray(new Object[0][]);
        }
    }
}
