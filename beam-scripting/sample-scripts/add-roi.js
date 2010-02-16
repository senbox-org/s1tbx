importPackage(org.esa.beam.framework.datamodel);
importPackage(org.esa.beam.framework.dataio);
importPackage(org.esa.beam.visat);
importPackage(org.esa.beam.visat.actions);


var visat = VisatApp.getApp();
var fc; // file-chooser




function main() {
    var productFile = promptForFile("Select Product");
    var shapefileFile = promptForFile("Select Shapefile");

    var stx = computeStx(productFile, "", shapefileFile);

    visat.showInfoDialog("mean = " + stx.getMean() + "\n" +
                         "var = " + stx.getVar(), null);
}

function promptForFile(title) {
    if (fc == null) {
        fc = new javax.swing.JFileChooser(title);
    }
    fc.showOpenDialog(null);
    var file = fc.getSelectedFile();
    if (file != null) {
        fc.setCurrentDirectory(file.getParentFile());
    }
    return file;
}


function computeStx(productFile, bandName, shapefileFile) {
   var product = ProductIO.readProduct(file, null);
   if (product == null) {
       return null;
   }
   var band = product.getBand(bandName);
   if (band == null) {
       return null;
   }
   var shape = ImportShapeAction.readShapeFromTextFile(shapefileFile, product.getGeoCoding());
   if (shape == null) {
       return null;
   }

   var roiDef = new ROIDefinition();
   roiDef.setShapeEnabled(true);
   roiDef.setShapeFigure(new ShapeFigure(shape));
   band.setROIDefinition(roiDef);
   var roi = band.createROI(ProgressMonitor.NULL);

   return band.computeStatistics(roi, ProgressMonitor.NULL);
}