if (fc == null) {
	fc = new javax.swing.JFileChooser();
}
fc.showOpenDialog(null);
f = fc.getSelectedFile();
if (f != null) {
   fc.setCurrentDirectory(f.getParentFile());
   p = ProductIO.readProduct(f, null);
   if (p != null) {
       visat.addProduct(p);
   }
}