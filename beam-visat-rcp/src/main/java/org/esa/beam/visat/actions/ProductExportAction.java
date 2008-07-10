/*
 * $Id: ProductExportAction.java,v 1.3 2007/04/18 13:01:13 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.actions;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurationElement;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductFileChooser;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.visat.VisatApp;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;

/**
 * This action exports a product of the associated format.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ProductExportAction extends ExecCommand {

    private String formatName;
    private ProductFileChooser fileChooser;
    private ProductWriterPlugIn writerPlugin;
    private String lastDirKey;

    @Override
    public void actionPerformed(CommandEvent event) {
        exportProduct();
    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(writerPlugin != null && getVisatApp().getSelectedProduct() != null);
    }

    @Override
    public void updateComponentTreeUI() {
        if (fileChooser != null) {
            SwingUtilities.updateComponentTreeUI(fileChooser);
        }
    }

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        super.configure(config);

        String configString = getConfigString(config, "formatName");
        if (configString != null) {
            setFormatName(configString);
        }

        configureWriterPlugin(config);

        // if properties are not yet set, set the defaults

        String text = getText();
        if (text == null) {
            setText("Export " + getFormatName() + " Product...");
        }

        String parent = getParent();
        if (parent == null) {
            setParent("export");
        }
    }

    private void configureWriterPlugin(ConfigurationElement config) throws CoreException {
        String writerPluginClassName = getConfigString(config, "writerPlugin");
        if (writerPluginClassName != null) {
            Class<?> aClass;
            try {
                aClass = config.getDeclaringExtension().getDeclaringModule().loadClass(writerPluginClassName);
            } catch (ClassNotFoundException e) {
                String msg = MessageFormat.format("[{0}]: Not able to load class [{1}]",
                                                  config.getDeclaringExtension().getDeclaringModule().getName(),
                                                  writerPluginClassName);
                throw new CoreException(msg, e);
            }
            Class<ProductWriterPlugIn> writerPluginClass;
            if (ProductWriterPlugIn.class.isAssignableFrom(aClass)) {
                writerPluginClass = (Class<ProductWriterPlugIn>) aClass;
            } else {
                String msg = MessageFormat.format("[{0}]: Specified class [{1}] must be derived from [{2}]",
                                                  config.getDeclaringExtension().getDeclaringModule().getName(),
                                                  writerPluginClassName,
                                                  ProductWriterPlugIn.class.getName());
                throw new CoreException(msg);
            }

            try {
                writerPlugin = writerPluginClass.newInstance();
            } catch (Exception e) {
                String msg = MessageFormat.format("[{0}]: Specified class [{1}] could not be instantiated",
                                                  config.getDeclaringExtension().getDeclaringModule().getName(),
                                                  writerPluginClass.getName());
                throw new CoreException(msg);

            }
        } else {
            // if writerPlugin not specified, try to find it by formatName
            Iterator iter = ProductIOPlugInManager.getInstance().getWriterPlugIns(getFormatName());
            if (iter.hasNext()) {
                writerPlugin = (ProductWriterPlugIn) iter.next();
            } else {
                writerPlugin = null;
                BeamLogManager.getSystemLogger().severe(
                        "no writer plug-in installed for products of type '" + getFormatName() + "'");
            }
        }
    }


    private void setFormatName(String name) {
        formatName = name;
        lastDirKey = "user." + formatName.toLowerCase() + ".export.dir";
    }

    private String getFormatName() {
        return formatName;
    }


    private void exportProduct() {
        if (writerPlugin == null) {
            // Should not come here...
            return;
        }
        Product product = getVisatApp().getSelectedProduct();
        if (product == null) {
            // Should not come here...
            return;
        }
        File currentDir = null;
        String currentDirPath = getVisatApp().getPreferences().getPropertyString(lastDirKey,
                                                                                 SystemUtils.getUserHomeDir().getPath());
        if (currentDirPath != null) {
            currentDir = new File(currentDirPath);
        }
        if (fileChooser == null) {
            final String title = getVisatApp().getAppName() + " - Export " + getFormatName() + " Product";
            fileChooser = new ProductFileChooser(title, product);
            fileChooser.addChoosableFileFilter(writerPlugin.getProductFileFilter());
            fileChooser.setFileFilter(writerPlugin.getProductFileFilter());
            HelpSys.enableHelpKey(fileChooser, getHelpId());
        } else {
            fileChooser.setProduct(product);
            // clear stored subset definition
            fileChooser.clearProductSubsetDef();
        }
        fileChooser.setCurrentDirectory(currentDir);

        File selectedFile = promptForFile(product);
        if (selectedFile == null) {
            return;
        }

        selectedFile = createValidProductFileName(selectedFile);

        if (selectedFile == null) {
            return;
        }
        if (!getVisatApp().promptForOverwrite(selectedFile)) {
            return;
        }
        final ProductSubsetDef productSubsetDef;
        if (fileChooser.getProductSubsetDef() != null) {
            productSubsetDef = fileChooser.getProductSubsetDef();
        } else {
            productSubsetDef = new ProductSubsetDef();
            productSubsetDef.setSubsetName(selectedFile.getName());
        }
        final String subsetName = productSubsetDef.getSubsetName();
        try {
            product = ProductSubsetBuilder.createProductSubset(product, productSubsetDef, subsetName, null);
        } catch (IOException e) {
            getVisatApp().showErrorDialog("An I/O error occured while creating the product subset:\n" +
                                          e.getMessage());
        } finally {
            // finally implementieren?
        }
        getVisatApp().writeProduct(product, selectedFile, getFormatName());
    }

    /**
     * Creates a valid product file name. Currently it only returns the given file.
     * <p/>
     * <p>Override this method if you want a different behaviour.
     *
     * @param file the file from which the valid product file name must be created.
     *
     * @return a <code>File</code> whth a valid product file name or <code>null</code>
     */
    protected File createValidProductFileName(final File file) {
        return file;
    }

    protected File promptForFile(final Product product) {

        File file = null;
        boolean canceled = false;
        while (file == null && !canceled) {
            // need to call showDialog() instead of showSaveDialog() to ensure correct text for approve button
            final int result = fileChooser.showDialog(getVisatApp().getMainFrame(), null);
            file = fileChooser.getSelectedFile();
            if (file != null && file.getParent() != null) {
                getVisatApp().getPreferences().setPropertyString(lastDirKey, file.getParent());
            }

            if (result != JFileChooser.APPROVE_OPTION) {
                canceled = true;
            }
        }

        return canceled ? null : file;
    }


    private static VisatApp getVisatApp() {
        return VisatApp.getApp();
    }


}
