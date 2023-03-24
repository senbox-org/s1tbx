
#Update 9.0.3
https://senbox.atlassian.net/projects/SNAP/versions/12822/tab/release-report-all-issues

#Update 9.0.2
https://senbox.atlassian.net/projects/SITBX/versions/12816/tab/release-report-all-issues

#Update 9.0.1
https://senbox.atlassian.net/projects/SITBX/versions/12815/tab/release-report-all-issues

#New in S1TBX 9.0

* Support for updated Sentinel-1 format
* Noise Power Image for ARD
* Ionospheric estimation and correction using a splitbandwidth approach 
* Retrieval of Vertical and E W motion components from a pair of interferograms
* Gamma-to-Sigma ratio image in Terrain Flattening
* InSAR Coherence calculation for image segments 
* Estimate noise equivalent beta0, sigma0 and gamma0 in Thermal Noise Removal
* Sentinel-1 Burst Subsets in SAFE Format
* PyRate workflow and tutorial
* Kennaugh Matrix, Huynen, Krogager, Cameron, Yang decompositions
* Contributed Radar Vegetation Indices
* Support for Cosmo-Skymed SG
* Support for Gaofen-3
* Support for Spacety


#New in S1TBX 8.0

* Updated support for RCM 
* Support for Capella
* Support for SAOCOM
* PyRate export
* Soil Moisture Toolkit for Radarsat-2/RCM 
* Joint Coregistration for Large TOPS Stacks
* Spotlight Interferometry

###Bugs fixed 

https://senbox.atlassian.net/browse/SITBX-765?filter=11944

#New in S1TBX 7.0

* Support for S-1 on AWS
* Support for RCM 
* Support for IcEye 
* Support for PAZ
* Support for RISAT-1
* Support for ALOS-2 in GeoTiff
* Support for Kompsat-5 in GeoTiff
* Terrain Correction of ALOS-1 from ESA On the fly products
* Automatic orbit download via QC Rest API
* Compact polarimetric tools
* TOPS Deramp/Demod operator
* Improved Terrain Flattening
* Improved thermal noise filtering
* Multilook and SLC to PRI moved to SAR utilities menu 

###Bugs fixed 

https://senbox.atlassian.net/browse/SITBX-637?filter=-4&jql=project%20%3D%20SITBX%20AND%20fixVersion%20%3D%207.0

#New in S1TBX 6.0

###Bugs fixed 

https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10202&version=12202

###New Features and Important Changes
* Integrate into the Product Library integration with SciHub
* Add option to terrain correction to use a standard grid across images
* Support for ALOS on demand products from ESA IPF
* Topo Phase Removal handles multiple slave images correctly
* DEM is oversampled in Terrain Flattening 
* Error in Interferogram with TSX/TDX CoSSC data
* RSTB output Polarisation Orientation Angle


#New in S1TBX 5.0

###Bugs fixed 

https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10202&version=11504

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

