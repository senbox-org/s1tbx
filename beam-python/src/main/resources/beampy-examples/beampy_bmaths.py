import sys

from beampy import ProductIO
from beampy import GPF


if len(sys.argv) != 2:
    print("usage: %s <file>" % sys.argv[0])
    sys.exit(1)

file = sys.argv[1]

product = ProductIO.readProduct(file)
width = product.getSceneRasterWidth()
height = product.getSceneRasterHeight()
name = product.getName()
description = product.getDescription()
band_names = product.getBandNames()

print("Product: %s, %d x %d pixels, %s" % (name, width, height, description))
print("Bands:   %s" % (band_names))

GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis()

parameters = {
    'targetBands': [
        {
            'name': 'band_1',
            'expression': 'radiance_1 - radiance_2'
        },
        {
            'name': 'band_2',
            'expression': 'radiance_6 - radiance_7'
        }
    ]
}



result = GPF.createProductFromSourceProduct('BandMaths', parameters, product)

'''
The above call currently results in:

Exception in thread "main" org.esa.beam.framework.gpf.OperatorException: Operator 'BandMathsOp': Value for 'Target band descriptors' must be of type 'BandDescriptor[]'.
        at org.esa.beam.framework.gpf.internal.OperatorContext.injectParameterValues(OperatorContext.java:1043)
        at org.esa.beam.framework.gpf.internal.OperatorContext.initializeOperator(OperatorContext.java:448)
        at org.esa.beam.framework.gpf.internal.OperatorContext.getTargetProduct(OperatorContext.java:248)
        at org.esa.beam.framework.gpf.Operator.getTargetProduct(Operator.java:317)
        at org.esa.beam.framework.gpf.GPF.createProductNS(GPF.java:311)
        at org.esa.beam.framework.gpf.GPF.createProduct(GPF.java:246)
        at org.esa.beam.framework.gpf.GPF.createProduct(GPF.java:178)
        at org.esa.beam.framework.gpf.GPF.createProduct(GPF.java:157)
Caused by: com.bc.ceres.binding.ValidationException: Value for 'Target band descriptors' must be of type 'BandDescriptor[]'.
        at com.bc.ceres.binding.validators.TypeValidator.validateValue(TypeValidator.java:31)
        at com.bc.ceres.binding.Property.validate(Property.java:207)
        at com.bc.ceres.binding.Property.setValue(Property.java:164)
        at org.esa.beam.framework.gpf.internal.OperatorContext.injectParameterValues(OperatorContext.java:1040)
        ... 7 more
'''

ProductIO.writeProduct(result, 'output.dim', 'BEAM-DIMAP')
