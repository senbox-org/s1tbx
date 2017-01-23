S1TBX Release Notes
==================

Update S1TBX 5.0.1 and 5.0.2
https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10202&version=12203

#New in S1TBX 5.0

###Bugs fixed 

https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10202&version=11505

###New Features and Important Changes
* Backgeocoding can handle multiple slave images
* Kompsat-5 reader
* StaMPS export
* S1B orbit files handled
* Fast individual downloads of orbit files from archive and QC website
* Ship detection, windfields, oilspill detection export as shape vectors


#New in S1TBX 4.0

###Bugs fixed 

https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10202&version=11504

###New Features and Important Changes
* Support for S1B
* Supervised Classification - Random Forest, KNN, Maximum Likelihood, Minimum Distance
* Correction to precise orbits which caused phase jumps
* Integer interferogram combination
* Double different interferogram within the burst overlap
* Improved offset tracking interpolation
* SeaSat reader


#New in S1TBX 3.0

###Bugs fixed 

https://senbox.atlassian.net/issues/?filter=11200

###New Features and Important Changes

* Burst selection inTOPSAR Split
* IDAN and Lee Sigma Speckle Filters
* Multitemporal Speckle Filter using all filters
* TOPS RangeShift and AzimthShift merged into Enhance-Spectral-Diversity
* InSAR Optimized Coregistration and Automatic Coregistration merged into Coregistration
* GCPSelection renamed to CrossCorrelation
* DEMGeneration renamed to PhaseToElevation
* Square pixel calculations for improved Coherence
* Added Snaphu tiling options
* Generalized Backgeocoding in DEMAssistedCoregistration
* Offset/Speckle Tracking
* S1TBX operators added to performance benchmarks


#New in S1TBX 2.0

###Bugs fixed 

https://senbox.atlassian.net/issues/?filter=11201

###New Features

* S1 Level-2 visualization in WorldWind Analysis View
* S1 AEP phase correction
* S1 GRD border noise removal
* Import/Export to Gamma/Stamps format
* ALOS-2 L1.1 CEOS reading
* ALOS/ALOS-2 reading from directly from zip files
* Several fixes to InSAR processing chain
* Geocoding position error fix
* Fix for very large tifs in S1 Stripmap, RS2, TSX

#New in S1TBX 1.1.1

* Automatic download of S-1 POE and RES orbits
* Fix TOPS Coregistration for products starting at different bursts
* Fix TOPS Coregistration to extend DEM for high elevation areas
* Fix TOPS Coregistration to compute burst offset from middle of burst
* Burst to remove invalid pixel areas
* Fix SLC slice assembly
* Flat-Earth phase to use average terrain height
* Handle bistatic interferogram for TanDEM-X
* Sentinel-1 precise orbit application
* TOPSAR Iterferometry
  * Backgeocoding
  * Enhanced Spectral Diversity
  * Burst coherence and interferogram
  * Deburst of insar products
* Support for TanDEM-X CoSSC
* Support for ALOS-2

