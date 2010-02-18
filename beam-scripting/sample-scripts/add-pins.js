p = visat.getSelectedProduct();
for (i = 0; i < 100; i++) {
    x = java.lang.Math.random() * p.getSceneRasterWidth();
    y = java.lang.Math.random() * p.getSceneRasterHeight();
    pos = new PixelPos(x, y);
    p.addPin(new Pin("p"+i, "p"+i, pos));
}