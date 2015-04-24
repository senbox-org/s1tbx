DESCRIPTION OF FILES:

   "WW15MGH.EXE" = self extracting file (compressed via "lha" utility), 
                   world-wide 15 minute gridded geoid heights.  The 
                   extracted name is "WW15MGH.GRD".,
 "WW15MGH.GRD.Z" = UNIX compressed version of the worldwide 15 minute gridded
                   geoid heights.  The uncompressed name is "WW15MGH.GRD".,
       "INTPT.F" = FORTRAN program to interpolate values from "WW15MGH.GRD".,
  "OUTINTPT.DAT" = test output data to check "INTPT.F".,
     "INPUT.DAT" = test input data to check "INTPT.F" and "F477.F".,

        "F477.F" = FORTRAN program designed for the calculation of a geoid 
                   undulation at a point whose latitude and longitude is 
                   specified. The program is designed to use the potential 
                   coefficient model EGM96 and a set of spherical harmonic 
                   coefficients of a correction term ("CORRCOEF").,
   "OUTF477.DAT" = test output data to check "F477.F".,

     "EGM96.EXE" = self extracting file (compressed via "lha" utility),
                   NASA/NIMA spherical harmonic potential coefficient set, 
                   complete to degree and order 360.  The extracted 
                   name is "EGM96".,
       "EGM96.Z" = UNIX compressed version of the NASA/NIMA spherical
                   harmonic potential coefficient set, complete to degree 
                   and order 360.  The uncompressed name is "EGM96".,
  "CORRCOEF.EXE" = self extracting file (compressed via "lha" utility),
                   Spherical harmonic coefficients of a correction term,
                   complete to degree and order 360.  The extracted 
                   name is "CORRCOEF".,
    "CORRCOEF.Z" = UNIX compressed version of the spherical harmonic 
                   coefficients of a correction term, complete to degree 
                   order 360.  The uncompressed name is "CORRCOEF".,
    "README.TXT" = the file you are currently reading.

-------------------------------------------------------------------------------

For MS-DOS users, the world-wide file containing a 15 minute grid of point 
geoid heights is:

              "WW15MGH.EXE"

This is a self extracting compressed file.  To uncompress this file after 
downloading to your PC, just type "WW15MGH.EXE" at your command line.  The
resulting "uncompressed" file is:

              "WW15MGH.GRD"

For UNIX users, the world-wide file containing a 15 minute grid of point geoid
heights is:

              "WW15MGH.GRD.Z"

This is a UNIX compressed file. To uncompress this file after downloading to
your PC, just type "uncompress WW15MGH.GRD.Z" at the command line.  The
resulting "uncompressed file is:

              "WW15MGH.GRD"

This file contains 1038961 point values in grid form.  The first row of the file
is the "header" of the file and shows the south, north, west, and east limits of 
the file followed by the grid spacing in n-s and e-w. All values in the "header" 
are in DECIMAL DEGREES.

The geoid undulation grid is computed at 15 arc minute spacings in north/south 
and east/west with the new "EGM96" spherical harmonic potential coefficient set
complete to degree and order 360 and a geoid height correction value computed 
from a set of spherical harmonic coefficients ("CORRCOEF"), also to degree and 
order 360.  The file is arranged from north to south, west to east (i.e., the
data after the header is the north most latitude band and is ordered from west
to east).

The coverage of this file is:

               90.00 N  +------------------+
                        |                  |
                        | 15' spacing N/S  |
                        |                  |
                        |                  |
                        | 15' spacing E/W  |
                        |                  |
              -90.00 N  +------------------+
                       0.00 E           360.00 E
-------------------------------------------------------------------------------

The program to interpolate point values from "WW15MGH.GRD" is:

               "INTPT.F"

This program interpolates point values from a given gridded file using bilinear
or spline interpolation depending on the value of parameter "IWINDO".   Check 
the subroutine "INTERP" within the body of "INTPT.F" for more information 
concerning the setting of "IWINDO".

"INTPT.F" requires two files to run.  The first file is the gridded data file, 
in this case, "WW15MGH.GRD", and is described above.

The second file is a file containing the latitude and longitude (in decimal 
degrees) corresponding to the location of desired interpolation.  The input 
file should be in the following form (cf. "INPUT.DAT"):
 
                   38.628155  269.779155  
                  -14.621217  305.021114  
                   46.874319  102.448729  
                  -23.617446  133.874712  
                   38.625473  359.999500  
                  -00.466744    0.002300  
 
                   LATITUDE    LONGITUDE    
                       decimal degrees        

    NOTE: "INTPT.F" reads this file in free-format.

After running "INTPT.F" a third file is created and is named "OUTINTPT.DAT".
This file contains the interpolated geoid heights from "WW15MGH.GRD" at the
locations in the file "INPUT.DAT".  A sample output (cf. "OUTINTPT.DAT") is 
given as:

              38.6281550   269.7791550     -31.628
             -14.6212170   305.0211140      -2.969
              46.8743190   102.4487290     -43.575
             -23.6174460   133.8747120      15.871
              38.6254730   359.9995000      50.066
               -.4667440      .0023000      17.329

               LATITUDE     LONGITUDE    GEOID HEIGHT
                   decimal degrees          meters 

-------------------------------------------------------------------------------

The program "F477.F" is provided to compute point geoid height values using the
"EGM96" spherical harmonic potential coefficient set and the "CORRCOEF" 
spherical harmonic correction coefficient both complete to degree and order 360.  
Both coefficient files have compressed versions for either MS-DOS or UNIX.  To 
uncompress these files, use the method appropriate to your operating system as 
described for the 15 minute gridded geoid height file "WW15MGH.GRD".

The input files consist of:

                correction coefficient set ("CORRCOEF") => UNIT = 1
                    potential coefficient set ("EGM96") => UNIT = 12
                points at which to compute (INPUT.DAT") => UNIT = 14

The output file is:

                     computed geoid heights ("OUTF477") => UNIT = 20

Files "INPUT.DAT" and "OUTF477.DAT" are in the same format as described above.

-------------------------------------------------------------------------------

Files "INPUT.DAT", "OUTINTPT.DAT", and "OUTF477.DAT" are provided to check that
programs "INTPT.F" and "F477.F" are operating correctly on your machine.  

To run the programs: 

"INTPT.FOR"

   1.  Create an input data set named:  "INPUT.DAT"
       This file should be in the previously mentioned format.
   2.  Compile the program using a FORTRAN compiler.
   3.  Execute the program.
   4.  The results should be in file: "OUTINTPT.OUT".


"F477.F"

   1.  Create an input data set named:  "INPUT.DAT"
       This file should be in the previously mentioned format.
   2.  Compile the program using a FORTRAN compiler.
   3.  Execute the program.
   4.  The results should be in file: "OUTF477.OUT".

Using the provided file named "INPUT.DAT" as input data, your results should 
match those in the provided sample output files.



