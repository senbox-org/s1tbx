package org.esa.beam.timeseries.ui.assistant;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.ui.assistant.AssistantPage;
import org.esa.beam.timeseries.core.timeseries.datamodel.ProductLocation;
import org.esa.beam.timeseries.ui.ProductLocationsPaneModel;
import org.esa.beam.gpf.operators.reproject.CollocationCrsForm;
import org.esa.beam.timeseries.ui.ProductLocationsPaneModel;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TimeSeriesAssistantPage_ReprojectingSources extends AbstractTimeSeriesAssistantPage {

    private MyCollocationCrsForm collocationCrsForm;
    private JTextArea errorText;

    TimeSeriesAssistantPage_ReprojectingSources(TimeSeriesAssistantModel model) {
        super("Reproject Source Products", model);
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public boolean canHelp() {
        // @todo
        return false;
    }

    @Override
    public boolean validatePage() {
        return collocationCrsForm.getCollocationProduct() != null;
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public AssistantPage getNextPage() {
        final Reprojector reprojector = new Reprojector(this.getPageComponent());
        reprojector.executeWithBlocking();
        return new TimeSeriesAssistantPage_VariableSelection(getAssistantModel());
    }

    @Override
    protected Component createPageComponent() {
        final PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                getContext().updateState();
            }
        };
        collocationCrsForm = new MyCollocationCrsForm(listener, getAssistantModel());
        collocationCrsForm.addMyChangeListener();

        final JPanel pagePanel = new JPanel(new BorderLayout());
        final JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(new JLabel("Use CRS of "), BorderLayout.WEST);
        northPanel.add(collocationCrsForm.getCrsUI());
        pagePanel.add(northPanel, BorderLayout.NORTH);
        final JPanel southPanel = new JPanel(new FlowLayout());
        errorText = new JTextArea();
        errorText.setBackground(southPanel.getBackground());
        final JPanel jPanel = new JPanel();
        jPanel.add(errorText);
        southPanel.add(errorText);
        pagePanel.add(southPanel, BorderLayout.SOUTH);
        return pagePanel;
    }

    private void setErrorMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                errorText.setText(message);
            }
        });
    }

    private class MyCollocationCrsForm extends CollocationCrsForm {

        private SourceProductSelector collocateProductSelector;
        private final PropertyChangeListener listener;
        private final TimeSeriesAssistantModel assistantModel;

        public MyCollocationCrsForm(PropertyChangeListener listener, TimeSeriesAssistantModel assistantModel) {
            super(VisatApp.getApp());
            this.listener = listener;
            this.assistantModel = assistantModel;
        }

        void addMyChangeListener() {
            super.addCrsChangeListener(listener);
        }

        @Override
        public CoordinateReferenceSystem getCRS(GeoPos referencePos) {
            Product collocationProduct = collocateProductSelector.getSelectedProduct();
            if (collocationProduct != null) {
                return collocationProduct.getGeoCoding().getMapCRS();
            }
            return null;
        }

        @Override
        public void prepareShow() {
            collocateProductSelector.initProducts();
        }

        @Override
        public void prepareHide() {
            collocateProductSelector.releaseProducts();
        }

        @Override
        protected JComponent createCrsComponent() {
            collocateProductSelector = new SourceProductSelector(getAppContext(), "Product:");
            List<Product> products = new ArrayList<Product>();
            for (ProductLocation productLocation : assistantModel.getProductLocationsModel().getProductLocations()) {
                for (Product product : productLocation.getProducts().values()) {
                    products.add(product);
                }
            }
            collocateProductSelector.setProductFilter(new CollocateProductFilter(products));
            collocateProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
                @Override
                public void selectionChanged(SelectionChangeEvent event) {
                    fireCrsChanged();
                }
            });

            final JPanel panel = new JPanel(new BorderLayout(2, 2));
            panel.add(collocateProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
            panel.add(collocateProductSelector.getProductFileChooserButton(), BorderLayout.EAST);
            panel.addPropertyChangeListener("enabled", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    collocateProductSelector.getProductNameComboBox().setEnabled(panel.isEnabled());
                    collocateProductSelector.getProductFileChooserButton().setEnabled(panel.isEnabled());
                    final boolean collocate = getRadioButton().isSelected();
                    getCrsUI().firePropertyChange("collocate", !collocate, collocate);
                }
            });
            return panel;
        }

        public Product getCollocationProduct() {
            return collocateProductSelector.getSelectedProduct();
        }

        public void setReferenceProduct(Product referenceProduct) {
        }
    }

    private class CollocateProductFilter implements ProductFilter {

        private final List<Product> products;

        public CollocateProductFilter(List<Product> products) {
            this.products = products;
        }

        @Override
        public boolean accept(Product collocationProduct) {
            for (Product timeSeriesSourceProduct : products) {
                if (productsIntersect(timeSeriesSourceProduct, collocationProduct)) {
                    resetErrorMessage();
                    return true;
                }
            }
            setErrorMessage("You need to specify a projected product as collocation product.\n" +
                            "At least one product within the time series needs to intersect the collocation product.");
            return false;
        }

        private void resetErrorMessage() {
            setErrorMessage("");
        }

        private boolean productsIntersect(Product timeSeriesSourceProduct, Product collocationProduct) {
            if (collocationProduct.getGeoCoding() == null) {
                return false;
            }
            final GeoCoding geoCoding = collocationProduct.getGeoCoding();
            if (geoCoding.canGetGeoPos() && geoCoding.canGetPixelPos()
                && ((geoCoding instanceof CrsGeoCoding)||(geoCoding instanceof MapGeoCoding))) {
                final GeneralPath[] sourcePaths = ProductUtils.createGeoBoundaryPaths(timeSeriesSourceProduct);
                final GeneralPath[] collocationPaths = ProductUtils.createGeoBoundaryPaths(collocationProduct);
                for (GeneralPath sourcePath : sourcePaths) {
                    for (GeneralPath collocationPath : collocationPaths) {
                        final Area sourceArea = new Area(sourcePath);
                        final Area collocationArea = new Area(collocationPath);
                        collocationArea.intersect(sourceArea);
                        if (!collocationArea.isEmpty()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private class Reprojector extends ProgressMonitorSwingWorker<Void, TimeSeriesAssistantModel> {

        protected Reprojector(Component parentComponent) {
            super(parentComponent, "Reprojecting source products ...");
        }

        @Override
        protected Void doInBackground(ProgressMonitor pm) throws Exception {
            reprojectSourceProducts(pm);
            return null;
        }

        private void reprojectSourceProducts(ProgressMonitor pm) {
            final ProductLocationsPaneModel productLocationsModel = getAssistantModel().getProductLocationsModel();
            final List<ProductLocation> productLocations = productLocationsModel.getProductLocations();
            pm.beginTask("Reprojecting...", productLocations.size());
            for (ProductLocation productLocation : productLocations) {
                final Map<String, Product> products = productLocation.getProducts();
                final Product crsReferenceProduct = getCrsReferenceProduct();
                for (Map.Entry<String, Product> productEntry : products.entrySet()) {
                    final Product product = productEntry.getValue();
                    if (!product.isCompatibleProduct(crsReferenceProduct, 0.1E-4f)) {
                        Product reprojectedProduct = createProjectedProduct(product, crsReferenceProduct);
                        productEntry.setValue(reprojectedProduct);
                    }
                }
                pm.worked(1);
            }
            pm.done();
        }

        private Product createProjectedProduct(Product toReproject, Product crsReference) {
            final Map<String, Product> productMap = getProductMap(toReproject, crsReference);
            final Map<String, Object> parameterMap = new HashMap<String, Object>();
            parameterMap.put("resamplingName", "Nearest");
            parameterMap.put("includeTiePointGrids", false);
            parameterMap.put("addDeltaBands", false);
            // @todo - generalise
            final Product reprojectedProduct = GPF.createProduct("Reproject", parameterMap, productMap);
            reprojectedProduct.setStartTime(toReproject.getStartTime());
            reprojectedProduct.setEndTime(toReproject.getEndTime());
            return reprojectedProduct;
        }


        private Map<String, Product> getProductMap(Product product, Product crsReference) {
            final Map<String, Product> productMap = new HashMap<String, Product>(2);
            productMap.put("source", product);
            productMap.put("collocateWith", crsReference);
            return productMap;
        }

        private Product getCrsReferenceProduct() {
            return collocationCrsForm.getCollocationProduct();
        }
    }
}
