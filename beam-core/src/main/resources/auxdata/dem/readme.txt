This is the auxdata folder for digital elevation models (DEM) used by BEAM. Properties
for different DEMs are stored here in related sub-folders. The name of the sub-folder
should be the same as the name of the DEM. Each sub-folder contains a Java properties
file named "dem.properties" which provides BEAM with the actual location of the DEM
files via the property "dem.installDir", for example:

dem.installDir = C\:\\Data\\GETASSE30

