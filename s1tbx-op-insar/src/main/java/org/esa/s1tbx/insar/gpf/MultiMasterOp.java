/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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
package org.esa.s1tbx.insar.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.ThreadExecutor;
import org.esa.snap.core.util.ThreadRunnable;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

@OperatorMetadata(alias = "MultiMasterStackGenerator",
        category = "Radar/Interferometric/Products",
        authors = "Alex McVittie",
        version = "1.0",
        copyright = "Copyright (C) 2020 by SkyWatch Space Applications Inc.",
        description = "Generates a set of master-slave pairs from a coregistered stack for use in SBAS processing")
public class MultiMasterOp extends Operator{
    // Things this needs:
    // output folder
    // input coregistered stack product
    @SourceProduct(alias = "source", label="Coregistered stack with single master and multiple (2 or more) slaves")
    private Product sourceProduct;
    @Parameter(description = "Output folder", alias="outputFolder",
            defaultValue = "" , label = "Output folder location for putting coregistered image pairs into.")
    private String outputFolder;


    @TargetProduct
    private Product targetProduct;

    /* Processing steps:
        1) Create a 2D list of band pairs, so you have your A-B B-C C-D interferogram network
        2) Loop through the 2D list and add those two bands to a new product, and set the metadata to have
            master/slave metadata, with the master being the oldest band in the list and the slave being the newest band
                a) Set the band names accordingly so that you have mst as master, and slv as slave.
                b) Update any band name info in the metadata accordingly, as you have in step 2b
        3) With your folder of master/slave coregistered pairs, you should now perform any additional preprocessing
            steps needed (goldstein phase filter, deburst, subset, etc)
        4) Perform snaphu unwrapping on each product - loop through and extract the command from the snaphu.conf file
        5) Loop through and import the bands back in, apply terrain correction
        6) Loop through the output folder and export each to a PyRate GAMMA format, and the last (or first) one, add
            DEM band and export the DEM
     */
    @Override
    public void initialize() throws OperatorException {

        try{
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            targetProduct = sourceProduct;
            validator.checkIfCoregisteredStack();
            //splitSingleMasterProduct();

        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        try {
            splitSingleMasterProduct(pm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void splitSingleMasterProduct(final ProgressMonitor pm) throws Exception {
        pm.beginTask("Splitting stack into multiple products", 100);
        pm.worked(1);

        final Product singleMaster_MultiSlave = sourceProduct; //ProductIO.readProduct(filepath);
        MetadataElement rootMetadata = singleMaster_MultiSlave.getMetadataRoot();
        Band[] bands = singleMaster_MultiSlave.getBands();
        MetadataElement slaves = rootMetadata.getElement(AbstractMetadata.SLAVE_METADATA_ROOT);
        MetadataElement [] slaveAbstractedMetadata = slaves.getElements();
        MetadataElement masterAbstractedMetadata = rootMetadata.getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);

        //MetadataElement masterOrbitData = masterAbstractedMetadata.getElement("Orbit_State_Vectors");
        HashMap<Integer, ArrayList<Band>> date_bandpairs = new HashMap<Integer, ArrayList<Band>>();
        HashMap<Integer, MetadataElement> date_metadatapairs = new HashMap<Integer, MetadataElement>();
        ArrayList<Integer> dates = new ArrayList<Integer>();
        String masterDate = masterAbstractedMetadata.getAttributeString("PROC_TIME").split(" ")[0];
        int masterDateInt = strDatetoInt(masterDate);
        ArrayList<Band> mstBands = new ArrayList<Band>();
        for(Band b: bands){
            if (b.getName().contains("mst")){
                mstBands.add(b);
            }
        }
        date_bandpairs.put(masterDateInt, mstBands);
        date_metadatapairs.put(masterDateInt, masterAbstractedMetadata);
        dates.add(masterDateInt);
        for(MetadataElement s : slaveAbstractedMetadata){
            String date = s.getAttributeString("first_line_time").split(" ")[0];
            int dateInt = strDatetoInt(date);
            date_metadatapairs.put(dateInt, s);
            dates.add(dateInt);
            ArrayList<Band> curBands = new ArrayList<Band>();
            for (Band b: bands){
                String name = b.getName().toUpperCase();
                if (name.contains(date.replace("-", ""))){
                    curBands.add(b);
                }
            }
            date_bandpairs.put(dateInt, curBands);
        }
        Collections.sort(dates);
        ThreadExecutor te = new ThreadExecutor();

        final int worked = 100 / (dates.size() - 1);
        final Product [] toOpenInSnap = new Product[dates.size() - 1];
        ArrayList<String> writtenProductPaths = new ArrayList<>();

        for (int x = 0; x < dates.size() - 1; x++){
            int dateMst = dates.get(x);
            int dateSlv = dates.get(x + 1);
            String productName = singleMaster_MultiSlave.getProductType() + "_" + dateMst + "_" + dateSlv;
            Product tmp = new Product(productName,singleMaster_MultiSlave.getProductType());
            ProductUtils.copyProductNodes(singleMaster_MultiSlave, tmp);
            final Product p = cleanProduct(tmp);
            MetadataElement root = p.getMetadataRoot();
            MetadataElement absMetadata = new MetadataElement("Abstracted_Metadata");// date_metadatapairs.get(dateMst);
            ProductUtils.copyMetadata(masterAbstractedMetadata, absMetadata);
            root.addElement(absMetadata);
            MetadataAttribute multimaster = new MetadataAttribute("multimaster_split", ProductData.TYPE_UINT8);
            absMetadata.addAttribute(multimaster);
            //absMetadata.removeElement(absMetadata.getElement("Orbit_State_Vectors"));
            //absMetadata.addElement(masterOrbitData);
            try{
                MetadataElement original_product_metadata = rootMetadata.getElement("Original_Product_Metadata");
                root.addElement(original_product_metadata);
            } catch(Exception e){
                e.printStackTrace();
            }

            absMetadata.setAttributeInt(AbstractMetadata.coregistered_stack, 1);
            MetadataElement slave_data = new MetadataElement(AbstractMetadata.SLAVE_METADATA_ROOT);

            slave_data.addElement(date_metadatapairs.get(dateMst));
            slave_data.addElement(date_metadatapairs.get(dateSlv));


            root.addElement(slave_data);
            mstBands = date_bandpairs.get(dateMst);
            final ArrayList<Band> slvBands = date_bandpairs.get(dateSlv);
            final ArrayList<Band> finalMstBands = mstBands;
            GeoCoding g = singleMaster_MultiSlave.getSceneGeoCoding();
            //p.setSceneGeoCoding(g);
            final int finalX = x;
            final ThreadRunnable runnable = new ThreadRunnable() {

                @Override
                public void process() {
                    try {
                        for (Band b: finalMstBands){
                            ProductUtils.copyBand(b.getName(), singleMaster_MultiSlave, p, true);
                            p.getBand(b.getName()).setName(b.getName().replace("slv", "mst"));
                        }
                        for (Band b: slvBands){
                            ProductUtils.copyBand(b.getName(), singleMaster_MultiSlave, p, true);
                            p.getBand(b.getName()).setName(b.getName().replace( "mst", "slv"));
                        }

                        ProductIO.writeProduct(p, outputFolder + "/" + p.getName(), "BEAM-DIMAP");
                        toOpenInSnap[finalX] = p;
                        writtenProductPaths.add(outputFolder + "/" + p.getName());



                        pm.worked(worked);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            };
            te.execute(runnable);
        }
        te.complete();

    }
    private int strDatetoInt(String date){
        String [] tmp = date.split("-");
        if ("JAN".equals(tmp[1])) {
            tmp[1] = "01";
        } else if ("FEB".equals(tmp[1])) {
            tmp[1] = "02";
        } else if ("MAR".equals(tmp[1])) {
            tmp[1] = "03";
        } else if ("APR".equals(tmp[1])) {
            tmp[1] = "04";
        } else if ("MAY".equals(tmp[1])) {
            tmp[1] = "05";
        } else if ("JUN".equals(tmp[1])) {
            tmp[1] = "06";
        } else if ("JUL".equals(tmp[1])) {
            tmp[1] = "07";
        } else if ("AUG".equals(tmp[1])) {
            tmp[1] = "08";
        } else if ("SEP".equals(tmp[1])) {
            tmp[1] = "09";
        } else if ("OCT".equals(tmp[1])) {
            tmp[1] = "10";
        } else if ("NOV".equals(tmp[1])) {
            tmp[1] = "11";
        } else {
            tmp[1] = "12";
        }
        date = tmp[2] + tmp[1] + tmp[0];
        return Integer.parseInt(date);
    }
    private Product cleanProduct(Product origProduct){
        for(Band b: origProduct.getBands()){
            origProduct.removeBand(b);
        }
        MetadataElement root = origProduct.getMetadataRoot();
        for (MetadataElement e : root.getElements()){
            root.removeElement(e);
        }
        for (MetadataAttribute a : root.getAttributes()){
            root.removeAttribute(a);
        }
        return origProduct;


    }
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MultiMasterOp.class);
        }
    }

}
