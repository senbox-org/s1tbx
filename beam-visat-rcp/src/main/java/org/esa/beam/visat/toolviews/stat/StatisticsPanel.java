package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.util.StringUtils;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.image.RenderedImage;
import java.util.List;

/**
 * A general pane within the statistics window.
 *
 * @author Marco Peters
 */
class StatisticsPanel extends TextPagePanel implements MultipleRoiComputePanel.ComputeMasks {

    private static final String DEFAULT_STATISTICS_TEXT = "No statistics computed yet.";  /*I18N*/
    private static final String TITLE_PREFIX = "Statistics";

    private MultipleRoiComputePanel computePanel;

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
        computePanel = new MultipleRoiComputePanel(this, getRaster());
        final JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(computePanel, BorderLayout.NORTH);
        final JPanel helpPanel = new JPanel(new BorderLayout());
        helpPanel.add(getHelpButton(), BorderLayout.EAST);
        rightPanel.add(helpPanel, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.EAST);
    }

    @Override
    protected void updateContent() {
        super.updateContent();
        if (computePanel != null) {
            final RasterDataNode raster = getRaster();
            computePanel.setRaster(raster);
            if (raster != null && raster.isStxSet() && raster.getStx().getResolutionLevel() == 0) {
                final Stx stx = raster.getStx();
                getTextArea().setText(createText(null, stx));
            } else {
                getTextArea().setText(DEFAULT_STATISTICS_TEXT);
            }
        }
    }

    @Override
    protected String createText() {
        // not used
        return DEFAULT_STATISTICS_TEXT;
    }
    
    private static class ComputeResult {
        final Stx stx;
        final Mask mask;

        ComputeResult(Stx stx, Mask mask) {
            this.stx = stx;
            this.mask = mask;
        }
    }

    @Override
    public void compute(final Mask[] selectedMasks) {
        final String title = "Computing Statistics";
        SwingWorker<Object, ComputeResult> swingWorker = new ProgressMonitorSwingWorker<Object, ComputeResult>(this, title) {

            @Override
            protected Object doInBackground(ProgressMonitor pm) {
                pm.beginTask(title, selectedMasks.length);
                try {
                    for (Mask mask : selectedMasks) {
                        final Stx stx;
                        ProgressMonitor subPm = SubProgressMonitor.create(pm, 1);
                        if (mask == null) {
                            stx = Stx.create(getRaster(), 0, subPm);
                            getRaster().setStx(stx);
                        } else {
                            final RenderedImage maskImage = mask.getSourceImage();
                            stx = Stx.create(getRaster(), maskImage, subPm);
                        }
                        publish(new ComputeResult(stx, mask));
                    }
                } finally {
                    pm.done();
                }
                return null;
            }
            
            @Override
            protected void process(List<ComputeResult> chunks) {
                for (ComputeResult result : chunks) {
                    Stx stx = result.stx;
                    String existingText = getTextArea().getText();
                    if (!existingText.isEmpty()) {
                        existingText += "\n\n";
                    }
                    String text;
                    if (stx.getSampleCount() > 0) {
                        text = createText(result.mask, stx);
                    } else {
                        if (result.mask != null) {
                            text = "The ROI-Mask '" + result.mask.getName() + "' is empty.";
                        } else {
                            text = "The scene contains no valid pixels.";
                        }
                    }
                    getTextArea().setText(existingText + text);
                }
            }
            
            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                                "Failed to compute statistics.\nAn error occured:" + e.getMessage(),
                                                /*I18N*/
                                                "Statistics", /*I18N*/
                                                JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        getTextArea().setText("");
        swingWorker.execute();
        getTextArea().setCaretPosition(0);
    }

    private String createText(final Mask mask, final Stx stat) {

        RasterDataNode raster = getRaster();
        boolean maskUsed = mask != null;
        final String unit = (StringUtils.isNotNullAndNotEmpty(raster.getUnit()) ? raster.getUnit() : "1");
        final long numPixelTotal = (long) raster.getSceneRasterWidth() * (long) raster.getSceneRasterHeight();
        final StringBuffer sb = new StringBuffer(1024);

        sb.append("\n");

        sb.append("Only ROI-Mask pixels considered:  \t");
        sb.append(maskUsed ? "Yes" : "No");
        sb.append("\n");
        
        if (maskUsed) {
            sb.append("ROI-Mask name:  \t");
            sb.append(mask.getName());
            sb.append("\n");
        }

        sb.append("Number of pixels total:      \t");
        sb.append(numPixelTotal);
        sb.append("\n");

        sb.append("Number of considered pixels: \t");
        sb.append(stat.getSampleCount());
        sb.append("\n");

        sb.append("Ratio of considered pixels:  \t");
        sb.append(100.0 * stat.getSampleCount() / numPixelTotal);
        sb.append("\t ");
        sb.append("%");
        sb.append("\n");

        sb.append("\n");

        sb.append("Minimum:  \t");
        sb.append(getMin(stat));
        sb.append("\t ");
        sb.append(unit);
        sb.append("\n");

        sb.append("Maximum:  \t");
        sb.append(getMax(stat));
        sb.append("\t ");
        sb.append(unit);
        sb.append("\n");

        sb.append("\n");

        sb.append("Mean:     \t");
        sb.append(getMean(stat));
        sb.append("\t ");
        sb.append(unit);
        sb.append("\n");

        sb.append("Median:  \t");
        sb.append(getMedian(stat));
        sb.append("\t ");
        sb.append(unit);
        sb.append("\n");

        sb.append("Std-Dev:  \t");
        sb.append(getStandardDeviation(stat));
        sb.append("\t ");
        sb.append(unit);
        sb.append("\n");

        sb.append("Coefficient of variation:  \t");
        sb.append(getCoefficientOfVariation(stat));
        sb.append("\t ");
        sb.append("");
        sb.append("\n");

        return sb.toString();
    }

    private double getCoefficientOfVariation(Stx stat) {
        return getStandardDeviation(stat) / getMean(stat);
    }

    private double getMedian(Stx stat) {
        return getRaster().scale(stat.getMedian());
    }

    private double getMin(Stx stat) {
        return getRaster().scale(stat.getMin());
    }

    private double getMax(Stx stat) {
        return getRaster().scale(stat.getMax());
    }

    private double getMean(Stx stat) {
        return getRaster().scale(stat.getMean());
    }

    /*
     * Use error-propagation to compute stddev for log10-scaled bands. (Ask Ralf for maths details.)
     */
    private double getStandardDeviation(Stx stat) {
        if (getRaster().isLog10Scaled()) {
            return getRaster().getScalingFactor() * Math.log(10.0) * getMean(stat) * stat.getStandardDeviation();
        } else {
            return getRaster().scale(stat.getStandardDeviation());
        }
    }
}
