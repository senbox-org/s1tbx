package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductTree;
import org.esa.beam.framework.ui.product.ProductTreeListener;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.geom.Rectangle2D;

import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.glayer.Layer;

/**
 * The window containing all statistics.
 *
 * @author Marco Peters
 */
public class StatisticsToolView extends AbstractToolView {

    public static final String ID = StatisticsToolView.class.getName();

    public static final int INFORMATION_TAB_INDEX = 0;
    public static final int GEOCODING_TAB_INDEX = 1;
    public static final int STATISTICS_TAB_INDEX = 2;
    public static final int HISTOGRAM_TAB_INDEX = 3;
    public static final int SCATTERPLOT_TAB_INDEX = 4;
    public static final int PROFILEPLOT_TAB_INDEX = 5;
    public static final int COORDLIST_TAB_INDEX = 6;

    private static String[] helpIDs = {
            "informationDialog",
            "geoCodingInfoDialog",
            "statisticsDialog",
            "histogramDialog",
            "scatterplotDialog",
            "profilePlotDialog",
            "coordinateListDialog"
    };

    public static final Color DIAGRAM_BG_COLOR = new Color(200, 200, 255);
    public static final Color DIAGRAM_FG_COLOR = new Color(0, 0, 100);
    public static final Color DIAGRAM_TEXT_COLOR = Color.black;
    public static final int DIAGRAM_MIN_INSETS = 5;

    private int currTabIndex;

    private JTabbedPane tabbedPane;
    private PagePanel[] pagePanels;
    private Product product;

    private final PagePanelPTL pagePanelPTL;
    private final PagePanelIFL pagePanelIFL;
    private final PagePanelLL pagePanelLL;

    public StatisticsToolView() {
        pagePanelPTL = new PagePanelPTL();
        pagePanelIFL = new PagePanelIFL();
        pagePanelLL = new PagePanelLL();
    }

    public void show(final int tabIndex) {
        VisatApp.getApp().getPage().showToolView(StatisticsToolView.ID);
        if (!isValidTabIndex(tabIndex)) {
            throw new IllegalArgumentException("illegal tab-index");
        }
        currTabIndex = tabIndex;
        tabbedPane.setSelectedIndex(tabIndex);
    }

    @Override
    public JComponent createControl() {

        tabbedPane = new JTabbedPane();
        final InformationPanel informationPanel = new InformationPanel(this, helpIDs[0]);
        final GeoCodingPanel codingPanel = new GeoCodingPanel(this, helpIDs[1]);
        final StatisticsPanel statisticsPanel = new StatisticsPanel(this, helpIDs[2]);
        final HistogramPanel histogramPanel = new HistogramPanel(this, helpIDs[3]);
        final ScatterPlotPanel scatterPlotPanel = new ScatterPlotPanel(this, helpIDs[4]);
        final ProfilePlotPanel profilePlotPanel = new ProfilePlotPanel(this, helpIDs[5]);
        final CoordListPanel coordListPanel = new CoordListPanel(this, helpIDs[6]);
        pagePanels = new PagePanel[]{informationPanel, codingPanel, statisticsPanel,
                histogramPanel, scatterPlotPanel, profilePlotPanel, coordListPanel};
        tabbedPane.add("Information", informationPanel); /*I18N*/
        tabbedPane.add("Geo-Coding", codingPanel);/*I18N*/
        tabbedPane.add("Statistics", statisticsPanel); /*I18N*/
        tabbedPane.add("Histogram", histogramPanel);  /*I18N*/
        tabbedPane.add("Scatter Plot", scatterPlotPanel); /*I18N*/
        tabbedPane.add("Profile Plot", profilePlotPanel);  /*I18N*/
        tabbedPane.add("Coordinate List", coordListPanel);  /*I18N*/

        tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (tabbedPane.getSelectedIndex() != currTabIndex) {
                    currTabIndex = tabbedPane.getSelectedIndex();
                    updateUIState();
                    updateHelpBroker();
                }
            }
        });

        updateUIState();
        updateHelpBroker();
        return tabbedPane;
    }

    @Override
    public void componentShown() {
        updateCurrentSelection();
        updateUI();
    }

    @Override
    public void componentOpened() {
        final ProductTree productTree = VisatApp.getApp().getProductTree();
        productTree.addProductTreeListener(pagePanelPTL);
        transferProductNodeListener(product, null);
        VisatApp.getApp().addInternalFrameListener(pagePanelIFL);
        updateCurrentSelection();
        transferProductNodeListener(null, product);
        updateUI();
    }

    private void updateUI() {
        for (PagePanel pagePanel : pagePanels) {
            pagePanel.updateUI();
        }
    }

    private void updateCurrentSelection() {
        for (PagePanel pagePanel : pagePanels) {
            pagePanel.updateCurrentSelection();
        }
    }

    private void transferProductNodeListener(Product oldProduct, Product newProduct) {
        if (oldProduct != newProduct) {
            for (PagePanel pagePanel : pagePanels) {
                if (oldProduct != null) {
                    oldProduct.removeProductNodeListener(pagePanel);
                }
                if (newProduct != null) {
                    newProduct.addProductNodeListener(pagePanel);
                }
            }
        }
    }


    @Override
    public void componentClosed() {
        final ProductTree productTree = VisatApp.getApp().getProductTree();
        productTree.removeProductTreeListener(pagePanelPTL);
        transferProductNodeListener(product, null);
        VisatApp.getApp().removeInternalFrameListener(pagePanelIFL);

    }

    private static boolean isValidTabIndex(final int tabIndex) {
        return tabIndex == INFORMATION_TAB_INDEX ||
               tabIndex == STATISTICS_TAB_INDEX ||
               tabIndex == HISTOGRAM_TAB_INDEX ||
               tabIndex == SCATTERPLOT_TAB_INDEX ||
               tabIndex == PROFILEPLOT_TAB_INDEX ||
               tabIndex == COORDLIST_TAB_INDEX ||
               tabIndex == GEOCODING_TAB_INDEX;
    }

    private void updateHelpBroker() {
        Debug.assertTrue(currTabIndex >= 0 && currTabIndex < helpIDs.length);
        setCurrentHelpID(helpIDs[currTabIndex]);
    }

    private void setCurrentHelpID(String helpID) {
        HelpSys.enableHelpKey(getPaneControl(), helpID);
        HelpSys.enableHelpKey(tabbedPane, helpID);
        HelpSys.getHelpBroker().setCurrentID(helpID);
    }

    private void updateUIState() {
        if (tabbedPane != null) {
            final Component selectedComponent = tabbedPane.getSelectedComponent();
            if (selectedComponent instanceof PagePanel) {
                final PagePanel pagePanel = (PagePanel) selectedComponent;
                pagePanel.getParentDialog().getDescriptor().setTitle(pagePanel.getTitle());
            } else {
                setTitle("");
            }
        }
    }

    private void selectionChanged(Product product, RasterDataNode node) {
        this.product = product;
        final PagePanel[] panels = StatisticsToolView.this.pagePanels;
        for (PagePanel panel : panels) {
            panel.selectionChanged(product, node);
        }
    }


    private class PagePanelPTL implements ProductTreeListener {

        public void tiePointGridSelected(TiePointGrid tiePointGrid, int clickCount) {
            selectionChanged(tiePointGrid.getProduct(), tiePointGrid);
        }

        public void bandSelected(Band band, int clickCount) {
            selectionChanged(band.getProduct(), band);
        }

        public void productSelected(Product product, int clickCount) {
            selectionChanged(product, null);
        }

        public void metadataElementSelected(MetadataElement group, int clickCount) {
            selectionChanged(group.getProduct(), null);
        }

        public void productRemoved(Product product) {
            selectionChanged(null, null);
        }

        public void productAdded(Product product) {
        }

    }

    private class PagePanelIFL extends InternalFrameAdapter {

        public PagePanelIFL() {
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                view.getRootLayer().addListener(pagePanelLL);
                selectionChanged(view.getRaster().getProduct(), view.getRaster());
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                view.getRootLayer().removeListener(pagePanelLL);
                selectionChanged(view.getRaster().getProduct(), null);
            }
        }

    }

    private class PagePanelLL extends AbstractLayerListener {
        @Override
        public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
            final PagePanel[] panels = StatisticsToolView.this.pagePanels;
            for (PagePanel panel : panels) {
                panel.handleLayerContentChanged();

            }
        }
    }
}
