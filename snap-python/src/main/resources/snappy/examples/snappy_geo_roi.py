#!/usr/bin/env python3

import sys

from snappy import (ProgressMonitor, VectorDataNode,
                    WKTReader, ProductIO, PlainFeatureFactory,
                    SimpleFeatureBuilder, DefaultGeographicCRS,
                    ListFeatureCollection, FeatureUtils)

if len(sys.argv) != 3:
    print("usage: %s <inputProduct> <wkt>" % sys.argv[0])
    sys.exit(1)

input = sys.argv[1]
inputFileName = input[input.rfind('/')+1:]

wellKnownText = sys.argv[2]

try:
    geometry = WKTReader().read(wellKnownText)
except:
    geometry = None
    print('Failed to convert WKT into geometry')
    sys.exit(2)

product = ProductIO.readProduct(input)

wktFeatureType = PlainFeatureFactory.createDefaultFeatureType(DefaultGeographicCRS.WGS84)
featureBuilder = SimpleFeatureBuilder(wktFeatureType)
wktFeature = featureBuilder.buildFeature('shape')
wktFeature.setDefaultGeometry(geometry)

newCollection = ListFeatureCollection(wktFeatureType)
newCollection.add(wktFeature)

productFeatures = FeatureUtils.clipFeatureCollectionToProductBounds(newCollection, product, None, ProgressMonitor.NULL)

node = VectorDataNode('shape', productFeatures)
print ('Num features = ', node.getFeatureCollection().size())

product.getVectorDataGroup().add(node)

vdGroup = product.getVectorDataGroup()
for i in range(vdGroup.getNodeCount()):
    print('Vector data = ', vdGroup.get(i).getName())

maskGroup = product.getMaskGroup()
for i in range(maskGroup.getNodeCount()):
    print('Mask = ', maskGroup.get(i).getName())




