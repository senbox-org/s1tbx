w = 512
h = 512

p = Product("P3", "T1", w, h)
b1 = VirtualBand("B1", ProductData.TYPE_FLOAT32, w, h, "sin(20*PI*(X*Y/(512*512)))")
b2 = Band("B2", ProductData.TYPE_FLOAT32, w, h)
b2.ensureRasterData()

p.addBand(b1)
p.addBand(b2)

for y in range(0, h):
    for x in range(0, w):
         v = 0.1 * x * y
         b2.setPixelFloat(x, y, v)

visat.addProduct(p)