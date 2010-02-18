
p = visat.getSelectedProduct();

w = p.getSceneRasterWidth();
h = p.getSceneRasterHeight();

pins = new Array(20);
dirs = new Array(20);
locs = new Array(20);
for (i = 0; i < pins.length; i++) {
    locs[i] = new PixelPos(java.lang.Math.random()*w, java.lang.Math.random()*h);
    dirs[i] = new PixelPos(1-2*java.lang.Math.random(), 1-2*java.lang.Math.random());
    pins[i] = new Pin("p" + i, "", locs[i]);
    p.addPin(pins[i]);
}

while (true) {
	for (i = 0; i < pins.length; i++) {
       locs[i].x += dirs[i].x;
       locs[i].y += dirs[i].y;
       if (locs[i].x < 0) {
           locs[i].x = 0;
           dirs[i].x *= -1;
       }
       if (locs[i].x >= w - 1) {
           locs[i].x = w - 1;
           dirs[i].x *= -1;
       }
       if (locs[i].y < 0) {
           locs[i].y = 0;
           dirs[i].y *= -1;
       }
       if (locs[i].y >= h - 1) {
           locs[i].y = h - 1;
           dirs[i].y *= -1;
       }
       pins[i].setPixelPos(locs[i]);
       // java.lang.Thread.sleep(5);
       visat.getSelectedProductSceneView().repaint();
    }
}

