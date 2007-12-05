importPackage(org.esa.beam.framework.datamodel);
importPackage(org.esa.beam.framework.dataio);
importPackage(org.esa.beam.visat);

var visat = VisatApp.getApp();

w = 512;
h = 512;
p = new Product("P3", "T1", w, h);
b1 = new VirtualBand("B1", ProductData.TYPE_FLOAT32, w, h, "sin(20*PI*(X*Y/(512*512)))");
b2 = new Band("B2", ProductData.TYPE_FLOAT32, w, h);
b2.ensureRasterData();
p.addBand(b1);
p.addBand(b2);
for (y = 0; y < h; y++) {
    for (x = 0; x < w; x++) {
        v = 0.1 * x * y;
        b2.setPixelFloat(x, y, v);
    }
}
visat.addProduct(p);