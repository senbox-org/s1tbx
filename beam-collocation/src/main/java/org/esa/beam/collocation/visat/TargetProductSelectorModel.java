package org.esa.beam.collocation.visat;

import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;

import java.io.File;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class TargetProductSelectorModel {

    private String productName;
    private boolean saveToFileSelected;
    private boolean openInVisatSelected;
    private File directory;
    private String formatName;
    private String[] formatNames;

    public TargetProductSelectorModel(boolean saveToFile) {
        formatNames = ProductIOPlugInManager.getInstance().getAllProductWriterFormatStrings();
        setSaveToFileSelected(saveToFile);
        setFormatName(formatNames[0]);
    }

    public String getProductName() {
        return productName;
    }

    public boolean isSaveToFileSelected() {
        return saveToFileSelected;
    }

    public boolean isOpenInVisatSelected() {
        return openInVisatSelected;
    }

    public File getDirectory() {
        return directory;
    }

    public File getFilePath() {
        return new File(directory, getFileName());
    }

    public String getFileName() {
        String productFileName = productName;
        Iterator<ProductWriterPlugIn> iterator = ProductIOPlugInManager.getInstance().getWriterPlugIns(formatName);
        if (iterator.hasNext()) {
            ProductWriterPlugIn writerPlugIn = iterator.next();

            boolean ok = false;
            for (String extension : writerPlugIn.getDefaultFileExtensions()) {
                if (productFileName.endsWith(extension)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                productFileName = productFileName.concat(writerPlugIn.getDefaultFileExtensions()[0]);
            }
        }
        return productFileName;
    }


    public String[] getFormatNames() {
        return formatNames;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setSaveToFileSelected(boolean saveToFileSelected) {
        this.saveToFileSelected = saveToFileSelected;
        if (!saveToFileSelected) {
            openInVisatSelected = true;
        }
    }

    public void setOpenInVisatSelected(boolean openInVisatSelected) {
        this.openInVisatSelected = openInVisatSelected;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }
}
