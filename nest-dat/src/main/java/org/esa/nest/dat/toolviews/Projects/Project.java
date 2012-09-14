/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.toolviews.Projects;

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.NewProductDialog;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSubsetDialog;
import org.esa.beam.framework.ui.product.ProductTreeListener;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.dialogs.PromptDialog;
import org.esa.nest.dat.dialogs.ProductSetDialog;
import org.esa.nest.dat.plugins.graphbuilder.GraphBuilderDialog;
import org.esa.nest.util.ProductFunctions;
import org.esa.nest.util.ResourceUtils;
import org.esa.nest.util.XMLSupport;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Timer;

/**
 * A Project helps to organize your data by storing all your work in one folder.
 * User: lveci
 * Date: Jan 23, 2008
 */
public class Project extends Observable {

    private final static Project _instance = new Project();

    private File projectFolder = null;
    private File projectFile = null;
    private ProjectPTL productTreeListener = null;
    private ProjectSubFolder projectSubFolders = null;
    private final static boolean SAVE_PROJECT = true;
    private final Timer timer = new Timer();

    /**
    * @return The unique instance of this class.
    */
    public static Project instance() {
        return _instance;
    }

    @Override
    public void finalize() throws Throwable {
        timer.cancel();
        super.finalize();
    }

    void notifyEvent(boolean saveProject) {
        setChanged();
        notifyObservers();
        clearChanged();
        if(saveProject)
            SaveProject();
    }

    private static void showProjectsView() {
        final ExecCommand command = VisatApp.getApp().getCommandManager().
                getExecCommand("org.esa.nest.dat.toolviews.Projects.ProjectsToolView.showCmd");
        command.execute();
    }

    public void CreateNewProject() {
        final File file = ResourceUtils.GetFilePath("Create Project", "xml", "xml", "myProject", "Project File", true);

        if(file != null) {
            final String prjName = file.getName();
            final String folderName = prjName.substring(0, prjName.lastIndexOf('.'));
            final File prjFolder = new File(file.getParentFile(), folderName);
            if(!prjFolder.exists())
                prjFolder.mkdir();
            final File newProjectFile = new File(prjFolder, prjName);

            initProject(newProjectFile);
            addExistingOpenedProducts();
            notifyEvent(SAVE_PROJECT);
            showProjectsView();
        }
    }

    private void addExistingOpenedProducts() {
        final ProductManager prodman = VisatApp.getApp().getProductManager();
        final int numProducts = prodman.getProductCount();
        for(int i=0; i < numProducts; ++i) {
            addProductLink(prodman.getProduct(i));
        }
    }

    private static boolean findSubFolders(final File currentFolder, final ProjectSubFolder projSubFolder) {
        final File[] files = currentFolder.listFiles();
        boolean hasProducts = false;
        if(files == null) return false;

        for(File f : files) {
            if(f.isDirectory()) {
                if(!f.getName().endsWith(DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION)) {
                    final ProjectSubFolder newProjFolder = projSubFolder.addSubFolder(f.getName());

                    if(findSubFolders(f, newProjFolder))
                        hasProducts = true;
                    //else if(!newProjFolder.isCreatedByUser())
                        //projSubFolder.removeSubFolder(newProjFolder);
                }
            } else if(!projSubFolder.containsFile(f)) {
                boolean found = false;
                final ProjectSubFolder.FolderType folderType = projSubFolder.getFolderType();
                if(folderType == ProjectSubFolder.FolderType.PRODUCT) {
                    found = ProductFunctions.isValidProduct(f);
                } else if(folderType == ProjectSubFolder.FolderType.PRODUCTSET ||
                        folderType == ProjectSubFolder.FolderType.GRAPH) {
                    found = f.getName().toLowerCase().endsWith(".xml");
                }

                if(found) {
                    hasProducts = true;
                    final ProjectFile newFile = new ProjectFile(f, f.getName());
                    boolean added = projSubFolder.addFile(newFile);

                    if (added) {
                        newFile.setFolderType(folderType);
                    }
                }
            }
        }
        return hasProducts;
    }

    public File getProjectFolder() {
        return projectFolder;
    }

    String getProjectName() {
        final String name = projectFile.getName();
        if(name.endsWith(".xml"))
            return name.substring(0, name.length()-4);
        return name;
    }

    protected void initProject(final File file) {
        if(productTreeListener == null && VisatApp.getApp() != null) {
            productTreeListener = new Project.ProjectPTL();
            VisatApp.getApp().addProductTreeListener(productTreeListener);
        }

        projectFile = file;
        projectFolder = file.getParentFile();

        projectSubFolders = new ProjectSubFolder(projectFolder, getProjectName(), false,
                                                ProjectSubFolder.FolderType.ROOT);
        projectSubFolders.setRemoveable(false);

        final ProjectSubFolder productSetsFolder = new ProjectSubFolder(
                new File(projectFolder, "ProductSets"), "ProductSets", true, ProjectSubFolder.FolderType.PRODUCTSET);
        projectSubFolders.addSubFolder(productSetsFolder);
        productSetsFolder.setRemoveable(false);

        final ProjectSubFolder graphsFolder = new ProjectSubFolder(
                new File(projectFolder, "Graphs"), "Graphs", true, ProjectSubFolder.FolderType.GRAPH);
        projectSubFolders.addSubFolder(graphsFolder);
        graphsFolder.setRemoveable(false);

        final ProjectSubFolder productLinksFolder = projectSubFolders.addSubFolder("External Product Links");
        productLinksFolder.setRemoveable(false);
        productLinksFolder.setFolderType(ProjectSubFolder.FolderType.PRODUCT);

        final ProjectSubFolder importedFolder = new ProjectSubFolder(
                new File(projectFolder, "Imported Products"), "Imported Products", true,
                ProjectSubFolder.FolderType.PRODUCT);
        projectSubFolders.addSubFolder(importedFolder);
        importedFolder.setRemoveable(false);

        final ProjectSubFolder processedFolder = new ProjectSubFolder(
                new File(projectFolder, "Processed Products"), "Processed Products", true,
                ProjectSubFolder.FolderType.PRODUCT);
        projectSubFolders.addSubFolder(processedFolder);
        processedFolder.setRemoveable(false);

        refreshProjectTree();

        final String[] defaultFolders = getDefaultProjectFolders();
        for(String folderName : defaultFolders) {
            final ProjectSubFolder newFolder = processedFolder.addSubFolder(folderName);
            newFolder.setCreatedByUser(true);
        }

        if(VisatApp.getApp() != null) {
            VisatApp.getApp().getPreferences().setPropertyString(
                BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR, processedFolder.getPath().getAbsolutePath());
            VisatApp.getApp().updateState();
        }

        // start refresh timer for any outside changes to project folder
        startUpdateTimer();
    }

    private String[] getDefaultProjectFolders() {
        String defaultProjectFolders = System.getProperty("defaultProjectFolders");
        if(defaultProjectFolders == null) {
            defaultProjectFolders = "Calibrated Products, Coregistered Products, Orthorectified Products";
        }

        final List<String> folderNames = new ArrayList<String>(5);
        final StringTokenizer st = new StringTokenizer(defaultProjectFolders, ",");
        int length = st.countTokens();
        for (int i = 0; i < length; i++) {
            folderNames.add(st.nextToken().trim());
        }
        return folderNames.toArray(new String[folderNames.size()]);
    }

    private void startUpdateTimer() {

        timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    if(IsProjectOpen()) {
                        if(refreshProjectTree())
                            notifyEvent(SAVE_PROJECT);
                    }
                }
            }, 2000, 1000*5);
    }

    private void addProductLink(final Product product) {
        final File productFile = product.getFileLocation();
        if(productFile == null)
            return;
        if(projectSubFolders.containsFile(productFile))
            return;
        
        refreshProjectTree();
        if(projectSubFolders.containsFile(productFile))
            return;

        final ProjectSubFolder productLinksFolder = projectSubFolders.addSubFolder("External Product Links");
        ProjectSubFolder destFolder = productLinksFolder;
        final String[] formats = product.getProductReader().getReaderPlugIn().getFormatNames();
        if(formats.length > 0)
            destFolder = productLinksFolder.addSubFolder(formats[0]);

        final ProjectFile newFile = new ProjectFile(productFile, product.getName());
        destFolder.addFile(newFile);
        newFile.setFolderType(ProjectSubFolder.FolderType.PRODUCT);
    }

    public boolean refreshProjectTree() {
        boolean found = false;
        final ProjectSubFolder productSetsFolder = projectSubFolders.findFolder("ProductSets");
        if(findSubFolders(productSetsFolder.getPath(), productSetsFolder))
            found = true;
        pruneNonExistantFiles(productSetsFolder);
        final ProjectSubFolder graphsFolder = projectSubFolders.findFolder("Graphs");
        if(findSubFolders(graphsFolder.getPath(), graphsFolder))
            found = true;
        pruneNonExistantFiles(graphsFolder);
        final ProjectSubFolder importedFolder = projectSubFolders.findFolder("Imported Products");
        if(findSubFolders(importedFolder.getPath(), importedFolder))
            found = true;
        pruneNonExistantFiles(importedFolder);
        final ProjectSubFolder processedFolder = projectSubFolders.findFolder("Processed Products");
        if(findSubFolders(processedFolder.getPath(), processedFolder))
                found = true;
        pruneNonExistantFiles(processedFolder);
        return found;
    }

    private static void pruneNonExistantFiles(final ProjectSubFolder projSubFolder) {
        // check for files to remove
        final ProjectFile[] fileList = projSubFolder.getFileList().toArray(new ProjectFile[projSubFolder.getFileList().size()]);
        for(ProjectFile projFile : fileList) {
            final File f = projFile.getFile();
            if(!f.exists() || f.getName().endsWith(DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION)) {
                projSubFolder.removeFile(f);
            }
        }
        for(ProjectSubFolder subFolder : projSubFolder.getSubFolders()) {
            pruneNonExistantFiles(subFolder);
        }
    }

    public void createNewFolder(final ProjectSubFolder subFolder) {
        final PromptDialog dlg = new PromptDialog("New Folder", "Name", "", false);
        dlg.show();
        if(dlg.IsOK()) {
            final ProjectSubFolder newFolder = subFolder.addSubFolder(dlg.getValue());
            newFolder.setCreatedByUser(true);
            if(subFolder == projectSubFolders || subFolder.isPhysical())
                newFolder.setPhysical(true);
            notifyEvent(SAVE_PROJECT);
        }
    }

    public void createNewProductSet(final ProjectSubFolder subFolder) {
        final String name = "ProductSet"+(subFolder.getFileList().size()+1);
        final ProductSet prodSet = new ProductSet(new File(subFolder.getPath(), name));
        final ProductSetDialog dlg = new ProductSetDialog("New ProductSet", prodSet);
        dlg.show();
        if(dlg.IsOK()) {
            final ProjectFile newFile = new ProjectFile(prodSet.getFile(), prodSet.getName());
            newFile.setFolderType(ProjectSubFolder.FolderType.PRODUCTSET);
            subFolder.addFile(newFile);
            notifyEvent(SAVE_PROJECT);
            refreshProjectTree();
        }
    }

    public static void createNewGraph(final ProjectSubFolder subFolder) {
        final ModelessDialog dialog = new GraphBuilderDialog(VisatApp.getApp(), "Graph Builder", "graph_builder");
        dialog.show();
    }

    public static void openFile(final ProjectSubFolder parentFolder, final File file) {
        if(parentFolder.getFolderType() == ProjectSubFolder.FolderType.PRODUCTSET) {
            ProductSet.OpenProductSet(file);
        } else if(parentFolder.getFolderType() == ProjectSubFolder.FolderType.GRAPH) {
            final GraphBuilderDialog dialog = new GraphBuilderDialog(VisatApp.getApp(), "Graph Builder", "graph_builder");
            dialog.show();
            dialog.LoadGraph(file);
        } else if(parentFolder.getFolderType() == ProjectSubFolder.FolderType.PRODUCT) {
            VisatApp.getApp().openProduct(file);
        }
    }

    public static void openSubset(final ProjectSubFolder parentFolder, final File prodFile) {

        try {
            final Product product = ProductIO.readProduct(prodFile);
            if(product != null) {
                final Product subsetProduct = getProductSubset(product);
                if(subsetProduct != null)
                    VisatApp.getApp().getProductManager().addProduct(subsetProduct);
            }
        } catch(Exception e) {
            VisatApp.getApp().showErrorDialog(e.getMessage());
        }
    }

    public void importSubset(final ProjectSubFolder parentFolder, final File prodFile) {
        final ProductReader reader = ProductIO.getProductReaderForFile(prodFile);
        if (reader != null) {
                final ProjectSubFolder importedFolder = projectSubFolders.findFolder("Imported Products");
                try {
                    final Product product = reader.readProductNodes(prodFile, null);

                    final Product subsetProduct = getProductSubset(product);
                    if(subsetProduct != null) {
                        final File destFile = new File(importedFolder.getPath(), subsetProduct.getName());
                        writeProduct(subsetProduct, destFile);
                    }
                } catch(Exception e) {
                    VisatApp.getApp().showErrorDialog(e.getMessage());
                }
        }
    }

    private static Product getProductSubset(final Product product) {
        if (product != null) {
            final JFrame mainFrame = VisatApp.getApp().getMainFrame();
            final ProductSubsetDialog productSubsetDialog = new ProductSubsetDialog(mainFrame, product);
            if (productSubsetDialog.show() == ProductSubsetDialog.ID_OK) {
                final ProductNodeList<Product> products = new ProductNodeList<Product>();
                products.add(product);
                final NewProductDialog newProductDialog = new NewProductDialog(mainFrame, products, 0, true);
                newProductDialog.setSubsetDef(productSubsetDialog.getProductSubsetDef());
                if (newProductDialog.show() == NewProductDialog.ID_OK) {
                    final Product subsetProduct = newProductDialog.getResultProduct();
                    if (subsetProduct == null || newProductDialog.getException() != null) {
                        VisatApp.getApp().showErrorDialog("The product subset could not be created:\n" +
                                newProductDialog.getException().getMessage());
                    } else {
                        return subsetProduct;
                    }
                }
            }
        }
        return null;
    }

    public void importFile(final ProjectSubFolder parentFolder, final File prodFile) {
        if(parentFolder.getFolderType() == ProjectSubFolder.FolderType.PRODUCT) {

            final ProductReader reader = ProductIO.getProductReaderForFile(prodFile);
            if (reader != null) {
                final ProjectSubFolder importedFolder = projectSubFolders.findFolder("Imported Products");

                final SwingWorker worker = new SwingWorker() {

                    @Override
                    protected Object doInBackground() throws Exception {
                        try {
                            final Product product = reader.readProductNodes(prodFile, null);
                            if(product != null) {
                                // special case for WSS products
                                if(product.getProductType().equals("ASA_WSS_1P")) {
                                    throw new Exception("WSS products need to be debursted before saving as DIMAP");
                                }
                                final File destFile = new File(importedFolder.getPath(), product.getName());

                                VisatApp.getApp().writeProduct(product, destFile, "BEAM-DIMAP");
                            }
                        } catch(Exception e) {
                            VisatApp.getApp().showErrorDialog(e.getMessage());
                        }                                   
                        return null;
                    }

                    @Override
                    public void done() {
                        refreshProjectTree();
                        notifyEvent(SAVE_PROJECT);
                    }
                };
                worker.execute();

            }
        }
    }

    private void writeProduct(final Product product, final File destFile) {

        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                VisatApp.getApp().writeProduct(product, destFile, "BEAM-DIMAP");
                return null;
            }

            @Override
            public void done() {
                refreshProjectTree();
                notifyEvent(SAVE_PROJECT);
            }
        };
        worker.execute();
    }

    public void ImportFileList(final File[] productFilesToOpen) {
        if(!IsProjectOpen()) {
            CreateNewProject();
        }

        final ProjectSubFolder importedFolder = projectSubFolders.findFolder("Imported Products");
        for(File f : productFilesToOpen) {
            importFile(importedFolder, f);
        }
    }

    public void deleteFolder(final ProjectSubFolder parentFolder, final ProjectSubFolder subFolder) {
        parentFolder.removeSubFolder(subFolder);
        notifyEvent(SAVE_PROJECT);
    }

    public void clearFolder(final ProjectSubFolder subFolder) {
        subFolder.clear();
        notifyEvent(SAVE_PROJECT);
    }

    public void renameFolder(final ProjectSubFolder subFolder) {
        final PromptDialog dlg = new PromptDialog("Rename Folder", "Name", "", false);
        dlg.show();
        if(dlg.IsOK()) {
            subFolder.renameTo(dlg.getValue());
            notifyEvent(SAVE_PROJECT);
        }
    }

    public void removeFile(final ProjectSubFolder parentFolder, final File file) {
        parentFolder.removeFile(file);
        if(parentFolder.getFolderType() == ProjectSubFolder.FolderType.PRODUCTSET ||
           parentFolder.getFolderType() == ProjectSubFolder.FolderType.GRAPH)
            file.delete();
        else if(parentFolder.getFolderType() == ProjectSubFolder.FolderType.PRODUCT) {
            if(file.getName().endsWith(DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION)) {
                final String pathStr = file.getAbsolutePath();
                final File dataDir = new File(pathStr.substring(0, pathStr.length()-4) + DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION);
                if(dataDir.exists()) {
                    ResourceUtils.deleteFile(dataDir);
                    file.delete();
                }
            } else {
                file.delete();
            }
        }
        notifyEvent(SAVE_PROJECT);
    }

    public ProjectSubFolder getProjectSubFolders() {
        return projectSubFolders;
    }

    public void CloseProject() {
        projectSubFolders = null;
        notifyEvent(false);

        if(VisatApp.getApp() != null) {
            VisatApp.getApp().updateState();
        }
    }

    public boolean IsProjectOpen() {
        return projectSubFolders != null;
    }

    public void SaveWorkSpace() {

    }

    public void SaveProjectAs() {
        final File file = ResourceUtils.GetFilePath("Save Project", "XML", "xml", getProjectName(), "Project File", true);
        if(file == null) return;
        
        projectFile = file;
        projectFolder = file.getParentFile();

        SaveProject();
    }

    public void SaveProject() {
        if(projectSubFolders == null)
            return;

        final Element root = new Element("Project");
        root.setAttribute("name", getProjectName());
        final Document doc = new Document(root);

        final Vector subFolders = projectSubFolders.getSubFolders();
        for (Enumeration e = subFolders.elements(); e.hasMoreElements();)
        {
            final ProjectSubFolder folder = (ProjectSubFolder)e.nextElement();
            final Element elem = folder.toXML();
            root.addContent(elem);
        }

        XMLSupport.SaveXML(doc, projectFile.getAbsolutePath());
    }

    public void LoadProject() {

        final File file = ResourceUtils.GetFilePath("Load Project", "XML", "xml", "", "Project File", false);
        if(file == null) return;

        initProject(file);

        org.jdom.Document doc;
        try {
            doc = XMLSupport.LoadXML(file.getAbsolutePath());
        } catch(IOException e) {
            VisatApp.getApp().showErrorDialog(e.getMessage());
            return;
        }

        final Vector<ProjectSubFolder> folderList = new Vector<ProjectSubFolder>(30);
        final Vector<ProjectFile> prodList = new Vector<ProjectFile>(50);

        final Element root = doc.getRootElement();

        final List children = root.getContent();
        for (Object aChild : children) {
            if (aChild instanceof Element) {
                final Element child = (Element) aChild;
                if(child.getName().equals("subFolder")) {
                    final Attribute attrib = child.getAttribute("name");
                    final ProjectSubFolder subFolder = projectSubFolders.addSubFolder(attrib.getValue());
                    subFolder.fromXML(child, folderList, prodList);
                }
            }
        }

        loadProducts(folderList, prodList);

        notifyEvent(false);
        showProjectsView();
    }

    private static void loadProducts(final Vector<ProjectSubFolder> folderList,
                                     final Vector<ProjectFile> prodList) {

        final ProgressMonitorSwingWorker worker = new ProgressMonitorSwingWorker(VisatApp.getApp().getMainFrame(), "Opening Project") {
            @Override
            protected Object doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
                pm.beginTask("Opening Project...", prodList.size());
                try {
                    for(int i=0; i < prodList.size(); ++i) {

                        final ProjectSubFolder subFolder = folderList.get(i);
                        final ProjectFile projFile = prodList.get(i);
                        final File prodFile = projFile.getFile();

                        if (ProductFunctions.isValidProduct(prodFile)) {
                            subFolder.addFile(projFile);
                        }
                        pm.worked(1);
                    }
                } finally {
                    pm.done();
                }
                return null;
            }
        };
        worker.executeWithBlocking();
    }

    private class ProjectPTL implements ProductTreeListener {

        public ProjectPTL() {
        }

        public void productAdded(final Product product) {
            if(projectSubFolders == null) return;
            addProductLink(product);
            notifyEvent(SAVE_PROJECT);
        }

        public void productRemoved(final Product product) {
            //if (getSelectedProduct() == product) {
            //    setSelectedProduct(product);
            //}
           // setProducts(VisatApp.getApp());
        }

        public void productSelected(final Product product, final int clickCount) {
            //setSelectedProduct(product);
        }

        public void metadataElementSelected(final MetadataElement group, final int clickCount) {
            //final Product product = group.getProduct();
            //setSelectedProduct(product);
        }

        public void tiePointGridSelected(final TiePointGrid tiePointGrid, final int clickCount) {
            //final Product product = tiePointGrid.getProduct();
            //setSelectedProduct(product);
        }

        public void bandSelected(final Band band, final int clickCount) {
            //final Product product = band.getProduct();
            //setSelectedProduct(product);
        }
    }
}
