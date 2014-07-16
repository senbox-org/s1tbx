import sys

import beampy
from beampy import ProductIO

SubsetOp = beampy.jpy.get_type('org.esa.beam.gpf.operators.standard.SubsetOp')
WKTReader = beampy.jpy.get_type('com.vividsolutions.jts.io.WKTReader')

if len(sys.argv) != 3:
    print("usage: %s <file> <geometry-wkt>" % sys.argv[0])
    print("       %s ./TEST.N1 \"POLYGON((15.786082 45.30223, 11.798364 46.118263, 10.878688 43.61961, 14.722727"
          "42.85818, 15.786082 45.30223))\"" % sys.argv[0])
    sys.exit(1)

file = sys.argv[1]
wkt = sys.argv[2]

# Uncomment if you receive errors of type com.sun.media.jai.util.ServiceConfigurationError, see
# http://www.brockmann-consult.de/beam-jira/browse/BEAM-1699
#beampy.SystemUtils.init3rdPartyLibs(None)  # Initialise BEAM's third party Java libraries JAI and GeoTools.

geom = WKTReader().read(wkt)

print("Reading...")
product = ProductIO.readProduct(file)

op = SubsetOp()
op.setSourceProduct(product)
op.setGeoRegion(geom)

sub_product = op.getTargetProduct()

print("Writing...")
ProductIO.writeProduct(sub_product, "beampy_subset_output.dim", "BEAM-DIMAP")

print("Done.")
