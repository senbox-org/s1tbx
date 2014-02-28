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

import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphIO;
import org.esa.beam.framework.gpf.graph.GraphProcessor;
import org.esa.beam.framework.gpf.graph.Node;
import org.esa.beam.gpf.operators.standard.ReadOp;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.pfa.fe.op.FeatureWriter;
import org.esa.pfa.fe.op.Patch;
import org.esa.pfa.search.CBIRSession;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.CropDescriptor;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;

/**
 * Feature extraction
 */
public class PatchProcessor extends ProgressMonitorSwingWorker<Patch, Void> {

    private final Product product;
    private final RenderedImage parentImage;
    private final Rectangle region;
    private final CBIRSession session;

    public PatchProcessor(Component component, Product product, RenderedImage parentImage, Rectangle region, CBIRSession session) {
        super(component, "Processing patch");
        this.product = product;
        this.parentImage = parentImage;
        this.region = region;
        this.session = session;
    }

    @Override
    protected Patch doInBackground(ProgressMonitor pm) throws Exception {
        pm.beginTask("Processing...", 100);
        try {
            final Rectangle productRegion = new Rectangle(parentImage.getWidth(), parentImage.getHeight()).
                    intersection(region);

            final Product subset = FeatureWriter.createSubset(product, productRegion);
            final int patchX = region.x / region.width;
            final int patchY = region.y / region.height;

            final BufferedImage patchImage;
            if (parentImage != null) {
                RenderedOp renderedOp = CropDescriptor.create(parentImage,
                                                              (float) productRegion.getX(),
                                                              (float) productRegion.getY(),
                                                              (float) productRegion.getWidth(),
                                                              (float) productRegion.getHeight(), null);
                patchImage = renderedOp.getAsBufferedImage();
            } else {
                patchImage = ProductUtils.createColorIndexedImage(
                        subset.getBand(ProductUtils.findSuitableQuicklookBandName(subset)),
                        com.bc.ceres.core.ProgressMonitor.NULL);
            }

            final Patch patch = new Patch(patchX, patchY, null, subset);
            patch.setImage(patchImage);
            patch.setLabel(Patch.LABEL_RELEVANT);

            final File tmpInFolder = new File(SystemUtils.getApplicationDataDir(),
                                              "tmp" + File.separator + "in" + File.separator + patch.getPatchProduct().getName() + ".fex");
            final File tmpOutFolder = new File(SystemUtils.getApplicationDataDir(),
                                               "tmp" + File.separator + "out" + File.separator + patch.getPatchProduct().getName() + ".fex");
            final File subsetFile = new File(tmpInFolder, patch.getPatchName() + ".dim");
            final WriteOp writeOp = new WriteOp(patch.getPatchProduct(), subsetFile, "BEAM-DIMAP");
            writeOp.setDeleteOutputOnFailure(true);
            writeOp.setWriteEntireTileRows(true);
            writeOp.writeProduct(ProgressMonitor.NULL);

            final File graphFile = session.getApplicationDescriptor().getGraphFile();
            final Graph graph = GraphIO.read(new FileReader(graphFile), null);
            setIO(graph, subsetFile, tmpOutFolder);

            final GraphProcessor processor = new GraphProcessor();
            processor.executeGraph(graph, pm);

            loadFeatures(patch, tmpOutFolder);
            return patch;
        } finally {
            pm.done();
        }
    }

    private void loadFeatures(final Patch patch, final File datasetDir) {
        try {
            final File[] fexDirs = datasetDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory() && file.getName().startsWith(patch.getPatchName());
                }
            });
            if (fexDirs.length == 0)
                return;

            final File[] patchDirs = fexDirs[0].listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory() && file.getName().startsWith("x");
                }
            });
            if (patchDirs.length == 0)
                return;

            patch.setPathOnServer(patchDirs[0].getAbsolutePath());

            final File featureFile = new File(patchDirs[0], "features.txt");
            patch.readFeatureFile(featureFile, session.getEffectiveFeatureTypes());

        } catch (Exception e) {
            final String msg = "Error reading features " + patch.getPatchName() + "\n" + e.getMessage();
            VisatApp.getApp().showErrorDialog(msg);
        }
    }

    private void setIO(final Graph graph, final File srcFile, final File targetFolder) {
        final String readOperatorAlias = OperatorSpi.getOperatorAlias(ReadOp.class);
        final Node readerNode = findNode(graph, readOperatorAlias);
        if (readerNode != null) {
            final DomElement param = new DefaultDomElement("parameters");
            param.createChild("file").setValue(srcFile.getAbsolutePath());
            readerNode.setConfiguration(param);
        }

        Node[] nodes = graph.getNodes();
        if (nodes.length > 0) {
            Node lastNode = nodes[nodes.length - 1];
            DomElement configuration = lastNode.getConfiguration();
            configuration.getChild("targetPath").setValue(targetFolder.getAbsolutePath());
        }

    }

    private Node findNode(final Graph graph, final String alias) {
        for (Node n : graph.getNodes()) {
            if (n.getOperatorName().equals(alias))
                return n;
        }
        return null;
    }
}