package org.esa.s1tbx.sar.gpf;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;

import java.io.File;

public class PyRateGammaHeaderWriter {

    private final Product srcProduct;
    final MetadataElement[] roots;
    public PyRateGammaHeaderWriter(Product product){
        this.srcProduct = product;
        final MetadataElement[] secondaryS = srcProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements();
        roots = new MetadataElement[secondaryS.length + 1];
        roots[0] = srcProduct.getMetadataRoot().getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);
        for (int x = 0; x < secondaryS.length; x++){
            roots[x + 1] = secondaryS[x];
        }
    }

    protected String toSentenceCase(String word){
        char [] chars = word.toUpperCase().toCharArray();
        for (int x = 1; x < chars.length; x++){
            chars[x] = Character.toLowerCase(chars[x]);
        }
        return String.valueOf(chars);
    }
    protected String convertDate(String date){
        date = date.split(" ")[0];
        String month = date.split("-")[1];
        String day = date.split("-")[0];
        String year = date.split("-")[2];
        month = toSentenceCase(month);
        return day + month + year;
    }

    // Write out the individual image metadata files as PyRate compatible metadata files,
    // store the file location in
    public File writeHeaderFiles(File destinationFolder, File headerListFile){


        if (headerListFile == null){
            return new File(destinationFolder.getParentFile(), "headers.txt");
        }else{
            return headerListFile;
        }


    }

    // Convert abstracted metadata into file contents for .PAR header.
    private String convertMetadataRootToPyRateGamma(MetadataElement root){
        String contents = "Gamma Interferometric SAR Processor (ISP) - Image Parameter File\n\n";
        contents += "title:\t" + root.getAttributeString("PRODUCT");
        contents += "sensor:\t" + root.getAttributeString("ACQUISITION_MODE") + " " +
                root.getAttributeString("SWATH") + " " + root.getAttributeString("mds1_tx_rx_polar");
        contents += "date:\t";

        return contents;
    }


}
