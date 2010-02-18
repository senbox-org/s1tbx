var w = 512;
var h = 512;
var p = new Product("P1", "T1", w, h);
var b1 = new VirtualBand("B1", ProductData.TYPE_FLOAT32, w, h, "sin(20*PI*(X*Y/(512*512)))");
var b2 = new Band("B2", ProductData.TYPE_FLOAT32, w, h);
b2.ensureRasterData();
p.addBand(b1);
p.addBand(b2);
var x, y, v; 
for (y = 0; y < h; y++) {
    for (x = 0; x < w; x++) {
        v = 0.1 * x * y;
        b2.setPixelFloat(x, y, v);
    }
}
visat.addProduct(p);
