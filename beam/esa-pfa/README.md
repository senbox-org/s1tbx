ESA PFA - EO Product Feature Extraction
=======

As the acquisition and use of Earth Observation (EO) data is increasing, it becomes more and more difficult to fully exploit the full mission satellite data archives. In particular, current data selection queries are essentially limited to time and geographic location and leave to the “user” the burden of searching for more specific features. 
Millions of Earth Observation scenes from optical and SAR sensors are being collected in Earth Observation data archives, and due to the variety of spatial, spectral, and temporal properties, mining and retrieving useful context-specific information from the archives is a big challenge. The situation will soon become even worse when forthcoming Sentinels data will be available. Therefore, EO scientific communities and data users urgently need new performing methodologies to effectively select data of their interest, among all available ones.
ESA’s Product Feature Extraction and Analysis (PFA) project, an activity within the Long Term Data Preservation (LTDP) programme, tackles this challenge by defining and demonstrating feature extraction and analysis methods for deducing the semantics from features in the context of different applications.
The project is being developed for ESA by a consortium led by Brockmann Consult and including the University of Trento and Array Systems Computing.

The project focuses on state-of-the-art methods that can be applied to SAR and optical images as well as to the multi-scale time variability of temporal signatures in time-series. Extracted features will be employed in three main scenarios: 
1.  content based image retrieval, permitting to search and retrieve images according to the content identified/extracted from them;
2.	content based time-series retrieval, permitting to search and retrieve couples (or larger series) of images showing similar temporal patterns and evolution;
3.	and unsupervised classification with kernel methods, grouping images (or portions of images) into some classes on the basis of attributes directly discovered from data.

For demonstrating the usefulness of the defined feature extraction and analysis algorithms, the PFA project can make use of the full-mission datasets of ASAR, ERS, Landsat TM, MERIS and AATSR. The algorithms will be implemented by using ESA's BEAM and NEST toolboxes and a high-performance EO data processing system based on Apache Hadoop. The demonstration system will be developed with a later integration into existing ESA user services for data delivery and processing in mind.
The “user” is considered here as a broad category of professionals, including environmental scientist, downstream service providers, and analysts working in a county. Last but not least the scenarios outlined above can be beneficial in supporting the interaction of citizens seeking information through the internet. Therefore EO data products must be exploitable in a user-friendly way, through a clear understanding and description of the information, which is associated with the product features. Most end users work with thematic features that change over time, and the growing archives of remote sensing data further encourage and facilitate the analysis of time series. Change detection and trend analysis are important techniques that are able to match the user requirements within global change and environmental monitoring.

Web information page: http://wiki.services.eoportal.org/tiki-index.php?page=PFA


Contact information
===================

Send mail to pfa@brockmann-consult.de
