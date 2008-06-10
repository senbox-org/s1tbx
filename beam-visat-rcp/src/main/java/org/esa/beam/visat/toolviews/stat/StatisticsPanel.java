package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import org.esa.beam.framework.datamodel.ROIDefinition;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.math.Statistics;

import javax.media.jai.ROI;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.JPanel;
import javax.swing.AbstractButton;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * A general pane within the statistics window.
 *
 * @author Marco Peters
 */
class StatisticsPanel extends TextPagePanel {

    private static final String DEFAULT_STATISTICS_TEXT = "No statistics computed yet.";  /*I18N*/
    private static final String TITLE_PREFIX = "Statistics";

    private ComputePanel computePanel;
    private ActionListener allPixelsActionListener;
    private ActionListener roiActionListener;

    public StatisticsPanel(final ToolView parentDialog, String helpID) {
        super(parentDialog, DEFAULT_STATISTICS_TEXT, helpID);

    }

    @Override
    protected String getTitlePrefix() {
        return TITLE_PREFIX;
    }

    @Override
    protected void initContent() {
        super.initContent();
        computePanel = ComputePanel.createComputePane(getAllPixelActionListener(), getRoiActionListener(), getRaster());
        final JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(computePanel, BorderLayout.NORTH);
        final JPanel helpPanel = new JPanel(new BorderLayout());
        helpPanel.add(getHelpButton(), BorderLayout.EAST);
        rightPanel.add(helpPanel,BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.EAST);
    }

    private ActionListener getAllPixelActionListener() {
        if (allPixelsActionListener == null) {
            allPixelsActionListener = new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    computeStatistics(false);
                }
            };
        }
        return allPixelsActionListener;
    }

    private ActionListener getRoiActionListener() {
        if (roiActionListener == null) {
            roiActionListener = new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    computeStatistics(true);
                }
            };
        }
        return roiActionListener;
    }

    @Override
    protected void updateContent() {
        super.updateContent();
        if (computePanel != null) {
            computePanel.setRaster(getRaster());
            getTextArea().setText(DEFAULT_STATISTICS_TEXT);
        }
    }

    @Override
    protected String createText() {
        // not used
        return DEFAULT_STATISTICS_TEXT;
    }

    private void computeStatistics(final boolean useROI) {
        final RasterDataNode raster = getRaster();
        final ROI roi;
        try {
            roi = useROI ? raster.createROI(ProgressMonitor.NULL) : null;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                          "Failed to compute statistics.\nAn I/O error occured:" + e.getMessage(),
                                          /*I18N*/
                                          "Statistics", /*I18N*/
                                          JOptionPane.ERROR_MESSAGE);
            getTextArea().setText(DEFAULT_STATISTICS_TEXT);
            return;
        }

        final SwingWorker swingWorker = new SwingWorker() {
            final ProgressMonitor pm = new DialogProgressMonitor(getParentDialogContentPane(), "Compute Statistic",
                                                                 Dialog.ModalityType.APPLICATION_MODAL);

            @Override
            protected Object doInBackground() throws Exception {
                try {
                    final Statistics stat = raster.computeStatistics(roi, pm);
                    return (stat.getNum() > 0) ? stat : null;
                } catch (IOException e) {
                    return e;
                }
            }

            @Override
            public void done() {
                if (pm.isCanceled()) {
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                                  "Failed to compute statistics.\nThe user has cancelled the calculation.",
                                                  /*I18N*/
                                                  "Statistics", /*I18N*/
                                                  JOptionPane.INFORMATION_MESSAGE);
                    getTextArea().setText(DEFAULT_STATISTICS_TEXT);
                } else {
                    Object value = null;
                    try {
                        value = get();
                    } catch (Exception e) {
                        value = e;
                    }
                    if (value instanceof Statistics) {
                        final Statistics stat = (Statistics) value;
                        getTextArea().setText(createText(stat, roi));
                        getTextArea().setCaretPosition(0);
                    } else if (value instanceof Exception) {
                        final Exception e = (Exception) value;
                        JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                                      "Failed to compute statistics.\nAn internal error occured:" + e.getMessage(),
                                                      /*I18N*/
                                                      "Statistics", /*I18N*/
                                                      JOptionPane.ERROR_MESSAGE);
                        getTextArea().setText(DEFAULT_STATISTICS_TEXT);
                    } else if (value == null) {
                        final String msgPrefix;
                        if (useROI) {
                            msgPrefix = "The ROI is empty.";        /*I18N*/
                        } else {
                            msgPrefix = "The scene contains no valid pixels.";  /*I18N*/
                        }
                        JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                                      msgPrefix +
                                                      "\nStatistics have not been computed.", /*I18N*/
                                                                                              "Statistics", /*I18N*/
                                                                                              JOptionPane.WARNING_MESSAGE);
                        getTextArea().setText(DEFAULT_STATISTICS_TEXT);
                    }
                }
            }
        };
        swingWorker.execute();
    }

    private String createText(final Statistics stat, final ROI roi) {

        final String unit = (StringUtils.isNotNullAndNotEmpty(getRaster().getUnit()) ? getRaster().getUnit() : "1");

        final StringBuffer sb = new StringBuffer(1024);

        sb.append("\n");

        sb.append("Only ROI pixels considered:  \t");
        sb.append(roi != null ? "Yes" : "No");
        sb.append("\n");

        sb.append("Number of pixels total:      \t");
        sb.append(stat.getNumTotal());
        sb.append("\n");

        sb.append("Number of considered pixels: \t");
        sb.append(stat.getNum());
        sb.append("\n");

        sb.append("Ratio of considered pixels:  \t");
        sb.append(100 * stat.getRatio());
        sb.append("\t ");
        sb.append("%");
        sb.append("\n");

        sb.append("\n");

        sb.append("Minimum:  \t");
        sb.append(stat.getMin());
        sb.append("\t ");
        sb.append(unit);
        sb.append("\n");

        sb.append("Maximum:  \t");
        sb.append(stat.getMax());
        sb.append("\t ");
        sb.append(unit);
        sb.append("\n");

        sb.append("\n");

        sb.append("Mean:     \t");
        sb.append(stat.getMean());
        sb.append("\t ");
        sb.append(unit);
        sb.append("\n");

        sb.append("Std-Dev:  \t");
        sb.append(stat.getStdDev());
        sb.append("\t ");
        sb.append(unit);
        sb.append("\n");

        sb.append("Variance: \t");
        sb.append(stat.getVar());
        sb.append("\t ");
        sb.append(unit);
        sb.append(" ^ 2");
        sb.append("\n");

        sb.append("Sum:      \t");
        sb.append(stat.getSum());
        sb.append("\t ");
        sb.append(unit);
        sb.append("\n");

        if (roi != null) {
            final ROIDefinition roiDefinition = getRaster().getROIDefinition();

            sb.append("\n");

            sb.append("ROI area shapes used: \t");
            sb.append(roiDefinition.isShapeEnabled() ? "Yes" : "No");
            sb.append("\n");

            sb.append("ROI value range used: \t");
            sb.append(roiDefinition.isValueRangeEnabled() ? "Yes" : "No");
            sb.append("\n");

            if (roiDefinition.isValueRangeEnabled()) {
                sb.append("ROI minimum value:   \t");
                sb.append(roiDefinition.getValueRangeMin());
                sb.append("\t ");
                sb.append(unit);
                sb.append("\n");

                sb.append("ROI maximum value:   \t");
                sb.append(roiDefinition.getValueRangeMax());
                sb.append("\t ");
                sb.append(unit);
                sb.append("\n");
            }

            sb.append("ROI bitmask used: \t");
            sb.append(roiDefinition.isBitmaskEnabled() ? "Yes" : "No");
            sb.append("\n");

            if (roiDefinition.isBitmaskEnabled()) {
                sb.append("ROI bitmask expression: \t");
                sb.append(roiDefinition.getBitmaskExpr());
                sb.append("\n");
            }

            sb.append("ROI combination operator: \t");
            sb.append(roiDefinition.isOrCombined() ? "OR" : "AND");
            sb.append("\n");

            sb.append("ROI inverted: \t");
            sb.append(roiDefinition.isInverted() ? "Yes" : "No");
            sb.append("\n");
        }
        return sb.toString();
    }
}
