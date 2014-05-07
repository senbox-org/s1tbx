import sys

from beampy import ProductIO
from beampy import JtsGeometryConverter
from beampy import SubsetOp

def wkt2geom(wkt):
    return JtsGeometryConverter.newJtsGeometryConverter().parse(wkt)

if len(sys.argv) != 3:
    print("usage: %s <file> <geometry-wkt>" % sys.argv[0])
    print("       %s ./TEST.N1 'POLYGON((15.786082 45.30223, 11.798364 46.118263, 10.878688 43.61961, 14.722727 42.85818, 15.786082 45.30223))'" % sys.argv[0])
    sys.exit(1)

file = sys.argv[1]
wkt = sys.argv[2]

geom = wkt2geom(wkt)

print("Reading...")
product = ProductIO.readProduct(file)

op = SubsetOp.newSubsetOp()
op.setSourceProduct(product)
op.setGeoRegion(geom)

sub_product = op.getTargetProduct()

print("Writing...")
ProductIO.writeProduct(sub_product, "subset.dim", "BEAM-DIMAP")

print("Done.")
