// /disk01/web/ocssw/build/src/l2bin/l2bin.c (r8080/r7196)
 #include <stdlib.h>
 #include <stdint.h>
 #include <libgen.h>
 #include <math.h>
 #include <stdio.h>
 #include <string.h>
 #include <sys/types.h>
 #include <time.h>
 #include "hdf.h"

 #include "seabin.h"
 #include "readL2scan.h"
 #include "l2bin_input.h"
 #include "time_utils.h"
 #include <setupflags.h>

 #ifdef GSL
 #include <gsl/gsl_sort.h>
 #include <gsl/gsl_statistics.h>
 #endif

 #define PI      3.141592653589793
 #define MTILT_DIMS_2    20
 #define LTILT_DIMS_2    2
 #define MAX32BITVALUE 4294967295
 #define MAXALLOCPERBIN 20
 //#define BINCHECK -1
 //#define BINCHECK -2
 #define EARTH_RADIUS 6378.14
 #define BINCHECK 1831598

 /* Global variables */
 static instr input;
 static l2_prod l2_str[MAXNFILES];
 static int32 *numbin;
 static int32 *basebin;
 static int32 nrows=-1;
 static float32 *scan_frac;
 static int16 *nobs;
 static float32 **data_values;
 static int16 **file_index;
 static uint8 **data_quality;
 static char prod_avg[64];


 #define VERSION "2.4.9"
 #define PROGRAM "L2BIN"

 /*
   Revision 2.4.8 10/14/12
   Put MERIS p1hr dataday parameter back to 19
   J. Gales

   Revision 2.4.8 10/14/12
   Adjust MERIS dataday parameters
   J. Gales

   Revision 2.4.7 10/10/12
   Switch MERIS from TERRA-like to SEAWIFS-like
   J. Gales

   Revision 2.4.6 07/30/12
   Incorporate cache allocation fix in readL2scan
   J. Gales

   Revision 2.4.5 09/12/11
   Put back "Mission Characteristics" & "Sensor Characteristics" metadata
   J. Gales

   Revision 2.4.4 06/27/11
   Add MERIS to "TERRA-like" dataday
   J. Gales

   Revision 2.4.3 08/05/10
   Exit on non-existent parm/suite file
   J. Gales

   Revision 2.4.2 02/04/10
   Change parameter for cde=5 (dataday) to 0.92
   J. Gales

   Revision 2.4.0 02/04/10
   Add caching for better i/o performance.
   D. Shea

   Revision 2.3.2 08/31/09
   Determine bad value by testing for nan set in readL2 function.
   J. Gales

   Revision 2.3.2 08/25/09
   Add suite parameter
   Modify l2bin_input code to handle suite defaults
   J. Gales

   Revision 2.3.1 08/11/09
   Add pversion parameter
   Remove "Replacement Flag" metadata
   J. Gales

   Revision 2.3.0 11/15/07
   Add ability to "flag" on bad value defined in SDS metadata
   J. Gales

   Revision 2.2.3 05/02/07
   Check that no more than one delimiter is specified.
   J. Gales

   Revision 2.2.2 04/10/07
   Tweak ssec limit for TERRA night
   previous day/no scancross granules to 10.1*60*60
   (T2000138100000.L2_LAC_SST)
   J. Gales

   Revision 2.2.1 04/06/07
   Set bins with -32767 l2 pixel values to data_quality = 4
   J. Gales

   Revision 2.2.0 04/03/07
   Only throw out bad geolocated scans, not entire granules
   J. Gales

   Revision 2.1.9 04/02/07
   Fix memory leak when no good bins are left after qual check.
   J. Gales

   Revision 2.1.8 03/27/07
   Add check for non-navigatable file
   J. Gales

   Revision 2.1.7 03/02/07
   Remove previous revision (2.1.6)
   Use spline check in libl2 to catch bad lon/lat
   Skip pixels with bad lon/lat
   J. Gales

   Revision 2.1.6 03/01/07
   Skip swath scan lines with bad lon/lat
   J. Gales

   Revision 2.1.5 02/09/07
   Disable INTERP parameter
   Fix missing and incorrect entries in Input Parameters Attribute
   J. Gales

   Revision 2.1.4 12/12/06
   Add ',' and ' ' as product delimiters.
   Add '=' as min value delimiters.
   Trap bad minimum values.
   J. Gales

   Revision 2.1.3 12/01/06
   Add VERBOSE input parameter
   J. Gales

   Revision 2.1.2 11/10/06
   Fix interp_distance value definition
   J. Gales

   Revision 2.1.1 09/25/06
   Set MAXNFILES to 544 (Fix made in readL2scan.h)
   J. Gales

   Revision 2.1.0 09/05/06
   Fix problem with prodtype=regional and fileuse
   J. Gales

   Revision 2.0.9 05/12/06
   Fix problem when escan_row > bscan_row.
   J. Gales

   Revision 2.0.8 04/27/06
   Fix dataday problems for day granules
   J. Gales

   Revision 2.0.7 04/10/06
   Support for TERRA SST
   J. Gales

   Revision 2.0.6 03/31/06
   Add longitude boundary check
   (input.lonwest & input.loneast input parameters)
   J. Gales

   Revision 2.0.5 03/16/06
   Added support for HMODIST
   J. Gales
 */


 void usage (char *progname)
 {

   printf("This is version %s of %s (compiled on %s %s)\n",
      VERSION,progname,__DATE__,__TIME__);

   printf("\nUsage: %s parfile=parfile or\n",progname);
   printf("            infile=infile ofile=ofile [sday=sday] [eday=eday]\n");
   printf("            resolve=resolve [flaguse=flaguse] [l3bprod=l3bprod]\n");
   /*  printf("            [prodtype=prodtype] [interp=interp] [noext=noext]\n");*/
   printf("            [prodtype=prodtype] [noext=noext] [verbose=verbose\n");
   printf("            [rowgroup=rowgroup] [night=night] [pversion=pversion]\n");
   printf("\n");
   printf("   parfile   = parameter filename\n");
   printf("   infile    = input filename/filelist\n");
   printf("   ofile     = output bin filename\n");
   printf("   sday      = start datadate (YYYYDDD) [ignored for \"regional\" prodtype]\n");
   printf("   eday      = end datadate   (YYYYDDD) [ignored for \"regional\" prodtype]\n");
   printf("   resolve   = bin resolution (H,1,2,4,9,36)\n");
   printf("   flaguse   = flags masked [see /SENSOR/l2bin_defaults.par]\n");
   printf("   l3bprod   = bin products [default=all products]\n");
   printf("               Set to \"ALL\" or \"all\" for all L2 products in 1st input file.\n");
   printf("               Use ':' or ',' or ' ' as delimiters.\n");
   printf("               Use ';' or '=' to delineate minimum values.\n");
   printf("   prodtype  = product type (Set to \"regional\" to bin all scans.) [default=day]\n");
   printf("   pversion  = production version [default=Unspecified]\n");
   /*
   printf("   interp    = interpolation flag (0=off,1=on) [default=0]\n");
   printf("               Interpolates between widely spaced pixels at ends of scan.\n");
   printf("               Useful only for GAC resolution L2 granules.\n");
   */
   printf("   noext     = set to 1 to suppress generation of external files\n");
   printf("               [default=0, (1 for \"regional\" prodtype)]\n");
   printf("   rowgroup  = # of bin rows to process at once.\n");
   printf("   night     = set to 1 for SST night processing [default=0]\n");
   printf("   qual_prod = quality product field name\n");
   printf("   qual_max  = maximum acceptable quality [default=2]\n");
   printf("   verbose   = Allow more verbose screen messages [default=0]\n");
   exit(0);
 }


 int main(int argc, char **argv)
 {
   int i,j,k,ii;
   int status;
   intn ret_status=0;

   int32 ifile, jsrow, ipixl, iprod, jprod, kprod, krow;
   int32 bin;
   int32 ibin;
   int32 nfiles;
   int32 n_active_files;
   int32 nsamp;
   int32 isamp;
   int32 ncols;

   int32 within_flag;
   int16 *allocated_space;

   int32 n_filled_bins;
   int32 total_filled_bins=0;
   int32 noext=0;
   int32 date;
   int32 total_alloc_space;
   int32 flag_9999 = 0;
   int32 bad_lonlat;

   int16 brk_scan[MAXNFILES];
   int8  snode[MAXNFILES];
   int8  enode[MAXNFILES];

   float32 *slat=NULL;
   float32 *elat=NULL;
   float32 *clat=NULL;
   float32 *slon=NULL;
   float32 *elon=NULL;
   float32 *clon=NULL;

   float32 dlat;
   float32 latbin=0.0;
   float32 lonbin=0.0;


   static int32 *bscan_row[MAXNFILES];
   static int32 *escan_row[MAXNFILES];
   static unsigned char *scan_in_rowgroup[MAXNFILES];
   int32 row_group=-1;
   int32 last_group;

   int32 n_allocperbin;

   char **prodname;


   int32 fileid_w;
   int32 sd_id_w;
   int32 vgid_w;
   int32 index;

   int32 zero=0;
   int32 five=5;
   int32 type[16];
   int32 start[3]={0,0,0};
   int32 edges[3];
   int32 *beg;
   int32 *ext;
   int32 *binnum_data;
   int32 i32;
   int32 n_bins_in_group;
   int32 len;
   int32 l3b_nprod;
   int32 first_fileuse = 1;

   int32 diffday_beg, diffday_end, ssec, sday;

   int32 tiltstate=0;
   int32 foundtiltstate;
   int32 ntilts;
   int16 tilt_flags[MTILT_DIMS_2];
   int16 tilt_ranges[LTILT_DIMS_2][MTILT_DIMS_2];

   uint32 flagusemask;
   uint32 required;
   uint32 flagcheck;

   int32 proc_day_beg, proc_day_end;
   int32 sd_id, sds_id;

   int16 i16;
   int16 time_rec=0;
   int16 cde;

   int8  scancross;
   uint8 *a, *bin_indx;
   uint8 selcat;
   uint8 *best_qual, qual_max_allowed;

   int16 *numer[MAXNFILES], *denom[MAXNFILES];
   int16 qual_prod_index[MAXNFILES];

   float32 p1hr, m1hr;

   float32 *sum_data;
   float32 *sum2_data;
   float32 f32, wgt, sum, sum2;
   float32 *min_value;
   float32 northmost=-90.0, southmost=90.0, eastmost=-180.0, westmost=180.0;

   int32   *bin_flag;
   int16   *tilt, *qual, *nscenes, *lastfile;

   float64 radius=6378.137000;
   float64 north=90.0;
   float64 south=-90.0;
   float64 seam_lon=-180.0;
   float64 vsize;
   float64 hsize;

   int32 bad_value;

   div_t quot1, quot2, quot3;
   time_t tnow;
   struct tm *tmnow;

   static meta_l2Type     meta_l2;
   static meta_l3bType    meta_l3b;

   int32 off[MAXNFILES][100];    /* Byte offsets to each of the data channels */

   static char buf[65535];
   char small_buf[1024];
   char units[1024];
   char *tmp_str;

   char *char_ptr1, *char_ptr2;

   char* fldname1[]={"registration","straddle","bins","radius", \
             "max_north","max_south","seam_lon"};

   char* fldname2[]={"bin_num","nobs","nscenes","time_rec","weights", \
             "sel_cat","flags_set"};

   char* fldname3[2];

   char* fldname4[]={"row_num","vsize","hsize","start_num","begin","extent","max"};

   char* fldname5[]={"qual_l3"};

   /* Function Prototypes */
   int32 isleap(int32);
   int32 diffday(int32, int32);
   int32 getbinnum(int32, int32, int32);
   int32 compute_scanfrac(int32, int32, uint32, uint32);
   int32 midaverage(int32, int32, int32);
   int32 median(int32, int32, int32);

   FILE *fp=NULL, *fp2=NULL;

   char delim;


 #ifdef MALLINFO
   struct mallinfo minfo;
 #endif

   init_rowgroup_cache();

   /* From Fred Patt

         sum(data)    sum(data)*sqrt(n)
    s =  --------- =  -----------------  =  avg(data)*sqrt(n)
          sqrt(n)            n

   */


   setlinebuf(stdout);

   printf("%s %s (%s %s)\n", PROGRAM, VERSION, __DATE__, __TIME__);

   if (l2bin_input(argc, argv, &input) != 0) {
     usage(argv[0]);
     exit(1);
   }


   /* Single HDF input */
   /* ---------------- */
   int ncid;
   if (Hishdf(input.infile) == TRUE || nc_open(input.infile, 0, &ncid) == 0) {
     nfiles = 1;
     status = openL2(input.infile, 0x0, &l2_str[0]);

     status = readL2meta(&meta_l2, 0);
     if (meta_l2.snode[0] == 'A') snode[0] = +1;
     if (meta_l2.snode[0] == 'D') snode[0] = -1;
     if (meta_l2.enode[0] == 'A') enode[0] = +1;
     if (meta_l2.enode[0] == 'D') enode[0] = -1;

     closeL2(&l2_str[0], 0);
     input.noext = 1;
     printf("Single HDF input\n");
   }
   else {

     /* Filelist input - Determine number of input files */
     /* ------------------------------------------------ */
     nfiles = 0;
     fp = fopen(input.infile, "r");
     if (fp == NULL) {
       printf("Input listing file: \"%s\" not found.\n", input.infile);
       return -1;
     }
     while(fgets(buf, 256, fp) != NULL) {
       nfiles++;
     }
     fclose(fp);
     printf("%d input files\n", nfiles);


     /* Open L2 input files */
     /* ------------------- */
     fp = fopen(input.infile, "r");
     for (ifile=0; ifile<nfiles; ifile++) {

       fgets(buf, 256, fp);
       buf[strlen(buf)-1] = 0;

       status = openL2(buf, 0x0, &l2_str[ifile]);

       status = readL2meta(&meta_l2, ifile);
       if (meta_l2.snode[0] == 'A') snode[ifile] = +1;
       if (meta_l2.snode[0] == 'D') snode[ifile] = -1;
       if (meta_l2.enode[0] == 'A') enode[ifile] = +1;
       if (meta_l2.enode[0] == 'D') enode[ifile] = -1;

       closeL2(&l2_str[ifile], ifile);
       /*
       printf("%s %d %d %d\n", buf,ifile,nfiles,buf[strlen(buf)-1]);

       printf("%s: %3d products  samples %6d\n",
          buf, l2_str[ifile].nprod, l2_str[ifile].nsamp);
       */

     } /* ifile loop */
     fclose(fp);
   }


   proc_day_beg  = input.sday;
   proc_day_end  = input.eday;

   if (strcmp(input.resolve, "36") == 0) nrows = 2160/4;
   if (strcmp(input.resolve, "9") == 0)  nrows = 2160;
   if (strcmp(input.resolve, "4") == 0)  nrows = 2160*2;
   if (strcmp(input.resolve, "2") == 0)  nrows = 2160*4;
   if (strcmp(input.resolve, "1") == 0)  nrows = 2160*8;
   if (strcmp(input.resolve, "H") == 0)  nrows = 2160*16;
   if (strcmp(input.resolve, "Q") == 0)  nrows = 2160*32;

   if (nrows == -1) {
     printf("Grid resolution not defined.\n");
     exit(-1);
   }

   dlat = 180. / nrows;

   noext = input.noext;
   qual_max_allowed = input.qual_max;

   row_group = input.rowgroup;

   if (row_group <= 0) {
     printf("row_group not defined.\n");
     exit(-1);
   }
   printf("%d %d %d\n", proc_day_beg, proc_day_end, row_group);
   printf("Averaging: %s\n", input.average);
   /*printf("Interpolation: %d\n", input.interp);*/
   printf("Resolution: %s\n", input.resolve);
   printf("Max Qual Allowed: %d\n", input.qual_max);

   if (strchr(input.average, ':') != NULL)
     strcpy(prod_avg, strchr(input.average, ':')+1);
   printf("prod_avg: %s\n", prod_avg);


   /* Find row_group that divides nrows */
   /* --------------------------------- */
   for (i=nrows; i>0; i--) {
     if ((nrows % i) == 0) {
       if (i <= row_group) {
     row_group = i;
     break;
       }
     }
   }
   if (input.rowgroup != row_group) {
     printf("Input row_group: %d   Actual row_group: %d\n",
        input.rowgroup, row_group);
   }


 #if 0
   /* Make sure number of L2 products are identical for every input L2 file */
   /* --------------------------------------------------------------------- */
   status = 0;
   for (ifile=1; ifile<nfiles; ifile++) {
     if (l2_str[ifile-1].nprod != l2_str[ifile].nprod) {
     printf("Number of products for %s (%d) differs from %s (%d)\n",
            l2_str[ifile-1].filename, l2_str[ifile-1].nprod,
            l2_str[ifile].filename, l2_str[ifile].nprod);
     status = -1;
     }
   }
   if (status == -1) exit(-1);


   /* Make sure L2 product names are identical for every input L2 file */
   /* ---------------------------------------------------------------- */
   status = 0;
   for (ifile=1; ifile<nfiles; ifile++) {
     for (i=0; i<l2_str[ifile].nprod; i++) {
       if (strcmp(l2_str[ifile-1].prodname[i], l2_str[ifile].prodname[i]) != 0) {
     printf("Product %d for %s (%s) differs from %s (%s)\n",
            i, l2_str[ifile-1].filename, l2_str[ifile-1].prodname[i],
            l2_str[ifile].filename, l2_str[ifile].prodname[i]);
     status = -1;
       }
     }
   }
   if (status == -1) exit(-1);
 #endif


   /* Fill offset array */
   /* ----------------- */
   for (ifile=0; ifile<nfiles; ifile++) {
     for (ii=0; ii<100; ii++) {
       off[ifile][ii] = ii * l2_str[ifile].nsamp;
       if (off[ifile][ii] < 0) {
     fprintf(stderr,"Error getting band offset\n");
     exit(-1);
       }
     }
   }


   /* Setup flag mask */
   /* --------------- */
   strcpy(buf, l2_str[0].flagnames);
   setupflags(buf, input.flaguse, &flagusemask, &required, &status );
   printf("flagusemask: %d\n", flagusemask);
   printf("required: %d\n", required);


   /* Determine delimiter */
   /* ------------------- */
   if (strchr(input.l3bprod, ':') != NULL) delim = ':';
   if (strchr(input.l3bprod, ',') != NULL) delim = ',';
   if (strchr(input.l3bprod, ' ') != NULL) delim = ' ';

   if (strchr(input.l3bprod, ':') != NULL &&
       strchr(input.l3bprod, ',') != NULL) {
     printf("Both ':' and ',' used as delimiters.\n");
     exit(1);
   }

   if (strchr(input.l3bprod, ':') != NULL &&
       strchr(input.l3bprod, ' ') != NULL) {
     printf("Both ':' and ' ' used as delimiters.\n");
     exit(1);
   }

   if (strchr(input.l3bprod, ',') != NULL &&
       strchr(input.l3bprod, ' ') != NULL) {
     printf("Both ',' and ' ' used as delimiters.\n");
     exit(1);
   }


   /* L3 Product List (ALL/all) */
   /* ------------------------- */
   /*  printf("%s\n", input.l3bprod);*/
   if (strcmp(input.l3bprod, ":ALL:") == 0 ||
       strcmp(input.l3bprod, ":all:") == 0) {
     strcpy(input.l3bprod, ":");
     strcat(input.l3bprod, l2_str[0].prodname[0]);
     strcat(input.l3bprod, ":");

     for (i=1; i<l2_str[0].nprod; i++) {
       strcat(input.l3bprod, l2_str[0].prodname[i]);
       strcat(input.l3bprod, ":");
     }

     /* Set L3BPROD entry in Input Parameters Attribute */
     char_ptr1 = strstr(input.parms, "L3BPROD");
     strcpy(small_buf, char_ptr1 + strlen("L3BPROD = ALL"));
     sprintf(char_ptr1, "L3BPROD = %s", &input.l3bprod[1]);
     strcat(input.parms, small_buf);
   }
   /*  printf("%s\n", input.l3bprod);*/


   /* Parse L3 Product list */
   /* --------------------- */
   len = strlen(input.l3bprod);
   l3b_nprod = 0;
   for (i=1; i<len; i++) if (input.l3bprod[i] == delim) l3b_nprod++;

   prodname = (char **) calloc(l3b_nprod+1, sizeof(char *));

   j = 0;
   for (i=0; i<len; i++) {
     if (input.l3bprod[i] == delim) {
       prodname[j] = input.l3bprod + i + 1;
       input.l3bprod[i] = 0;
       j++;
     }
   }


   /* Get minimum value */
   /* ----------------- */
   min_value = (float32 *) calloc(l3b_nprod, sizeof(float32));
   for (i=0; i<l3b_nprod; i++) {

     char_ptr1 = strchr(prodname[i], '/');
     char_ptr2 = strchr(prodname[i], ';');
     if (char_ptr2 == NULL) char_ptr2 = strchr(prodname[i], '=');
     if (char_ptr2 != NULL) {
       *char_ptr2 = 0;
       min_value[i] = (float32) strtod(char_ptr2+1, &tmp_str);
       if (strcmp(char_ptr2+1, tmp_str) == 0) {
     printf("Unable to convert min value: \"%s\"\n", char_ptr2+1);
     exit(-1);
       }
     } else {
       min_value[i] = 0;
     }
   }


   /* Initialize bscan_row, escan_row, numer, denom */
   /* --------------------------------------------- */
   for (i=0; i<MAXNFILES; i++) {
     bscan_row[i] = NULL;
     escan_row[i] = NULL;
     numer[i] = NULL;
     denom[i] = NULL;
     scan_in_rowgroup[i] = NULL;
   }


   /* Check whether L3 products exist in L2 */
   /* ------------------------------------- */
   for (ifile=0; ifile<nfiles; ifile++) {

     numer[ifile] = (int16 *) calloc(l3b_nprod, sizeof(int16));
     denom[ifile] = (int16 *) calloc(l3b_nprod, sizeof(int16));

     for (jprod=0; jprod<l3b_nprod; jprod++) {

       char_ptr1 = strchr(prodname[jprod], '/');
       if (char_ptr1 != NULL) *char_ptr1 = 0;

       for (i=0; i<l2_str[ifile].nprod; i++)
     if (strcmp(prodname[jprod], l2_str[ifile].prodname[i]) == 0) break;

       numer[ifile][jprod] = i;
       denom[ifile][jprod] = -1;

       if (i == l2_str[ifile].nprod) {

     /* Check if FLAG product */
     /* --------------------- */
     if (strncmp(prodname[jprod], "FLAG_", 5) == 0) {

       strcpy(small_buf, ",");
       strcat(small_buf, prodname[jprod]+5);
       strcat(small_buf, ",");

       strcpy(buf, ",");
       strcat(buf, l2_str[ifile].flagnames);
       strcat(buf, ",");

       char_ptr2 = strstr(buf, small_buf);

       if (char_ptr2 != NULL) {
         numer[ifile][jprod] = 0;
         while(char_ptr2 > buf) {
           if (*char_ptr2 == ',') numer[ifile][jprod]++;
           char_ptr2--;
         }
         denom[ifile][jprod] = -2;

       } else {

         printf("L3 product: \"%s\" not found in L2 flagnames.\n",
            prodname[jprod]);
         exit(-1);
       }

     } else {

       printf("L3 product: \"%s\" not found in L2 dataset \"%s\".\n",
          prodname[jprod], l2_str[ifile].filename);
       exit(-1);
     }
       }

       if (char_ptr1 != NULL) *char_ptr1 = '/'; else continue;

       for (i=0; i<l2_str[ifile].nprod; i++)
     if (strcmp(char_ptr1+1, l2_str[ifile].prodname[i]) == 0) break;
       denom[ifile][jprod] = i;

       if (i == l2_str[ifile].nprod) {
     printf("L3 product: \"%s\" not found in L2 dataset \"%s\".\n",
            char_ptr1+1, l2_str[ifile].filename);
     exit(-1);
       }

     } /* jprod loop */

 #if 0
     /* Print L3B product info */
     for (jprod=0; jprod<l3b_nprod; jprod++)
       printf("%3d %-25s %3d %3d %8.3e\n",
          jprod, prodname[jprod], numer[ifile][jprod], denom[ifile][jprod],
          min_value[jprod]);
 #endif
   } /* ifile loop */



   /* Check whether Quality product exists in L2 */
   /* ------------------------------------------ */
   if (input.qual_prod[0] != 0) {
     for (ifile=0; ifile<nfiles; ifile++) {

       for (i=0; i<l2_str[ifile].nprod; i++)
     if (strcmp(input.qual_prod, l2_str[ifile].prodname[i]) == 0) break;

       qual_prod_index[ifile] = i;

       if (i == l2_str[ifile].nprod) {
     printf("Quality product: \"%s\" not found in L2 dataset \"%s\".\n",
            input.qual_prod, l2_str[ifile].filename);
     exit(-1);
       }
     }
   }


   /* Find begin and end scan latitudes for each swath row */
   /* ---------------------------------------------------- */
   if (Hishdf(input.infile) == FALSE) fp = fopen(input.infile, "r");

   for (ifile=0; ifile<nfiles; ifile++) {

     slat = (float32 *) calloc(l2_str[ifile].nrec, sizeof(float32));
     elat = (float32 *) calloc(l2_str[ifile].nrec, sizeof(float32));

     bscan_row[ifile] = (int32 *) calloc(l2_str[ifile].nrec, sizeof(int32));
     escan_row[ifile] = (int32 *) calloc(l2_str[ifile].nrec, sizeof(int32));

     if (Hishdf(input.infile) == FALSE) {
       fgets(buf, 256, fp);
       buf[strlen(buf)-1] = 0;
     } else strcpy(buf, input.infile);

     sd_id = SDstart(buf, DFACC_RDONLY);

     start[0] = 0;
     edges[0] = l2_str[ifile].nrec;

     index = SDnametoindex(sd_id, "slat");
     sds_id = SDselect(sd_id, index);

     status = SDreaddata(sds_id, start, NULL, edges, (VOIDP) slat);
     SDendaccess(sds_id);

     index = SDnametoindex(sd_id, "elat");
     sds_id = SDselect(sd_id, index);

     status = SDreaddata(sds_id, start, NULL, edges, (VOIDP) elat);
     SDendaccess(sds_id);

     /* Note: bscan > escan */

     for (jsrow=0; jsrow<l2_str[ifile].nrec; jsrow++) {
       escan_row[ifile][jsrow] = (int32) ((90 + elat[jsrow]) / dlat);
       bscan_row[ifile][jsrow] = (int32) ((90 + slat[jsrow]) / dlat);

       if (escan_row[ifile][jsrow] > bscan_row[ifile][jsrow]) {
     k = escan_row[ifile][jsrow];
     escan_row[ifile][jsrow] = bscan_row[ifile][jsrow];
     bscan_row[ifile][jsrow] = k;
       }
       escan_row[ifile][jsrow] -= 10;
       bscan_row[ifile][jsrow] += 10;
     }

     SDreadattr(sd_id, SDfindattr(sd_id, "Sensor Name"), small_buf);
     SDend(sd_id);

     free(slat);
     free(elat);

   } /* ifile loop */
   if (Hishdf(input.infile) == FALSE) fclose(fp);


   /* Find begin & end scans for each input file */
   /* ------------------------------------------ */
   if (Hishdf(input.infile) == FALSE) fp = fopen(input.infile, "r");

   n_active_files = nfiles;
   for (ifile=0; ifile<nfiles; ifile++) {

     slon = (float32 *) calloc(l2_str[ifile].nrec, sizeof(float32));
     elon = (float32 *) calloc(l2_str[ifile].nrec, sizeof(float32));
     clon = (float32 *) calloc(l2_str[ifile].nrec, sizeof(float32));
     elat = (float32 *) calloc(l2_str[ifile].nrec, sizeof(float32));
     slat = (float32 *) calloc(l2_str[ifile].nrec, sizeof(float32));
     clat = (float32 *) calloc(l2_str[ifile].nrec, sizeof(float32));

     if (Hishdf(input.infile) == FALSE) {
       fgets(buf, 256, fp);
       buf[strlen(buf)-1] = 0;
     } else strcpy(buf, input.infile);

     sd_id = SDstart(buf, DFACC_RDONLY);

     start[0] = 0;
     edges[0] = l2_str[ifile].nrec;

     index = SDnametoindex(sd_id, "slon");
     sds_id = SDselect(sd_id, index);
     status = SDreaddata(sds_id, start, NULL, edges, (VOIDP) slon);
     SDendaccess(sds_id);

     index = SDnametoindex(sd_id, "elon");
     sds_id = SDselect(sd_id, index);
     status = SDreaddata(sds_id, start, NULL, edges, (VOIDP) elon);
     SDendaccess(sds_id);

     index = SDnametoindex(sd_id, "clon");
     sds_id = SDselect(sd_id, index);
     status = SDreaddata(sds_id, start, NULL, edges, (VOIDP) clon);
     SDendaccess(sds_id);

     index = SDnametoindex(sd_id, "elat");
     sds_id = SDselect(sd_id, index);
     status = SDreaddata(sds_id, start, NULL, edges, (VOIDP) elat);
     SDendaccess(sds_id);

     index = SDnametoindex(sd_id, "slat");
     sds_id = SDselect(sd_id, index);
     status = SDreaddata(sds_id, start, NULL, edges, (VOIDP) slat);
     SDendaccess(sds_id);

     index = SDnametoindex(sd_id, "clat");
     sds_id = SDselect(sd_id, index);
     status = SDreaddata(sds_id, start, NULL, edges, (VOIDP) clat);
     SDendaccess(sds_id);
     SDend(sd_id);


     /* Determine brk_scan value */
     /* ------------------------ */
     brk_scan[ifile] = 0;
     cde = 0;

     /* Regional Product */
     /* ---------------- */
     if (strcmp(input.prodtype, "regional") == 0) {
       printf("%s   brk:%5d  %5d %3d %6d\n",
          buf, brk_scan[ifile],
          l2_str[ifile].nrec,l2_str[ifile].sday,
          l2_str[ifile].smsec/1000);

       /*
       if (input.fileuse[0] != 0) {
     if (first_fileuse == 1) {
       fp2 = fopen(input.fileuse, "w");
     }
     if (brk_scan[ifile] != -9999) {
       fprintf(fp2,"%s\n", buf);
     }
     first_fileuse = 0;
       }
       */

       free(slon);
       free(elon);
       free(clon);
       free(elat);
       free(slat);
       free(clat);
       continue;
     }


     date = l2_str[ifile].syear*1000+l2_str[ifile].sday;
     diffday_beg = diffday(date, proc_day_beg);
     diffday_end = diffday(date, proc_day_end);
     sday = l2_str[ifile].sday;
     ssec = l2_str[ifile].smsec/1000;


     /* MODIS (AQUA) */
     /* ------------ */
     if (strcmp(small_buf, "MODISA")  == 0 ||
         strcmp(small_buf, "VIIRSN")  == 0 ||
     strcmp(small_buf, "HMODISA") == 0) {


       /* Determine if swath crossed dateline */
       /* ----------------------------------- */
       scancross = 0;
       for (jsrow=l2_str[ifile].nrec-1; jsrow>=0; jsrow--) {
     scancross = (elon[jsrow]*snode[ifile] > 0 &&
              slon[jsrow]*snode[ifile] < 0 &&
              0.5 * (fabs(elat[jsrow]) + fabs(slat[jsrow])) < 70);
     if (scancross != 0) {
       break;
     }
       }


       /* If non-polar granule ... */
       /* ------------------------ */
       if (abs(clat[0]) < 75 && abs(clat[l2_str[ifile].nrec-1]) < 75) {

     /* Skip ascending granules if night SST */
     if (input.night == 1) {
       if (snode[ifile] == +1) {
         cde = 1;
         brk_scan[ifile] = -9999;
         goto nxtfile;
       }
     }

     /* Skip descending granules if day SST */
     if (input.night == 0) {
       if (snode[ifile] == -1) {
         cde = 2;
         brk_scan[ifile] = -9999;
         goto nxtfile;
       }
     }
       }

       if (input.night == 1) {

     if (diffday_beg <= -2) {
       cde = 3;
       brk_scan[ifile] = -9999;
     }

     if (diffday_end >= +2) {
       cde = 4;
       brk_scan[ifile] = -9999;
     }

     if (diffday_beg == -1 && (ssec < 12*60*60)) {
       cde = 5;
       brk_scan[ifile] = -9999;
     }

     if (diffday_end == 1 && (ssec >= 12*60*60)) {
       cde = 6;
       brk_scan[ifile] = -9999;
     }

     if (diffday_beg == -1 && (ssec < 12.76*60*60) &&
         (scancross == 0)) {
       cde = 7;
       brk_scan[ifile] = -9999;
     }

     if (diffday_beg == -1 && (scancross == 1)) {
       brk_scan[ifile] = -1;
     }

     if (diffday_beg == 0 && (scancross == 1) &&
         ssec <= 12*60*60) {
       brk_scan[ifile] = +1;
     }

     if (diffday_end == 0 && (ssec >= 12*60*60) && (scancross == 0) &&
         brk_scan[ifile-1] == 1) {
       cde = 8;
       brk_scan[ifile] = -9999;
     }

     if (diffday_end == 0 && (ssec >= 12*60*60) && (scancross == 1)) {
       brk_scan[ifile] = +1;
     }

     if (diffday_end == 0 && (ssec >= 14.42*60*60)) {
       cde = 9;
       brk_scan[ifile] = -9999;
     }

     if (diffday_end == 0 && (scancross == 0) &&
         ssec > 14.2*60*60) {
       flag_9999 = 1;
     }

     if (flag_9999 == 1) {
       cde = 10;
       brk_scan[ifile] = -9999;
     }

       } else {

     if (diffday_beg <= -1) {
       cde = 3;
       brk_scan[ifile] = -9999;
     }

     if (diffday_end >= +2) {
       cde = 4;
       brk_scan[ifile] = -9999;
     }

     if (diffday_beg == 0 && (scancross == 0) &&
         ssec < 0.92*60*60) { // 0.76
       cde = 5;
       brk_scan[ifile] = -9999;
     }

     if (diffday_beg == 0 && (scancross == 1) &&
         ssec <= 12*60*60) {
       brk_scan[ifile] = -1;
     }

     if (diffday_beg == 0 && (scancross == 1) &&
         ssec > 12*60*60) {
       brk_scan[ifile] = +1;
     }

     if (diffday_end == +1 && (scancross == 0) &&
         brk_scan[ifile-1] == 1) {
       cde = 6;
       brk_scan[ifile] = -9999;
     }

     if (diffday_end == +1 && (scancross == 1)) {
       brk_scan[ifile] = +1;
     }

     if (diffday_end == +1 &&
         ssec > 2.42*60*60) {
       cde = 7;
       brk_scan[ifile] = -9999;
     }

     if (diffday_end == +1 && (scancross == 0) &&
         ssec > 2.2*60*60) {
       flag_9999 = 1;
     }

     if (flag_9999 == 1) {
       cde = 8;
       brk_scan[ifile] = -9999;
     }
       }


       /* MODIS (TERRA) */
       /* ------------- */
     } else if (strcmp(small_buf, "MODIST")  == 0 ||
            strcmp(small_buf, "HMODIST") == 0) {

       /* Determine if swath crossed dateline */
       /* ----------------------------------- */
     scancross = 0;
     for (jsrow=l2_str[ifile].nrec-1; jsrow>=0; jsrow--) {
     scancross = (elon[jsrow]*snode[ifile] > 0 &&
              slon[jsrow]*snode[ifile] < 0 &&
              0.5 * (fabs(elat[jsrow]) + fabs(slat[jsrow])) < 70);
     if (scancross == 1) {
       //      printf("elon: %f  elat: %f\n", elon[jsrow],elat[jsrow]);
       break;
     }
       }

       /* If non-polar granule ... */
       /* ------------------------ */
       if (abs(clat[0]) < 75 && abs(clat[l2_str[ifile].nrec-1]) < 75) {

     /* Skip descending granules if night SST */
     if (input.night == 1) {
       if (snode[ifile] == -1) {
         cde = 1;
         brk_scan[ifile] = -9999;
         goto nxtfile;
       }
     }

     /* Skip ascending granules if day SST */
     if (input.night == 0) {
       if (snode[ifile] == +1) {
         cde = 2;
         brk_scan[ifile] = -9999;
         goto nxtfile;
       }
     }
       }

       if (input.night == 1) { /* NIGHT (TERRA) */

     if (diffday_beg <= -2) {
       cde = 3;
       brk_scan[ifile] = -9999;
     }

     if (diffday_end >= +1) {
       cde = 4;
       brk_scan[ifile] = -9999;
     }

     if (diffday_beg == -1 && (scancross == 1) &&
         ssec <= 24*60*60) {
       brk_scan[ifile] = -1;
     }

     if (diffday_beg == 0 && (scancross == 1) &&
         ssec > 0*60*60) {
       brk_scan[ifile] = +1;
     }

     if (diffday_end == -1 && (scancross == 0) &&
         ssec < 10.1*60*60) {
       //        ssec < 10*60*60) {
       cde = 5;
       brk_scan[ifile] = -9999;
     }

     if (diffday_end == -1 && (scancross == 1) &&
         ssec < 11*60*60) {
       brk_scan[ifile] = -1;
     }

     if (diffday_beg == 0 && (scancross == 0) &&
         ssec > 11.0*60*60) {
       cde = 6;
       brk_scan[ifile] = -9999;
     }

     if (diffday_end == -1 &&
         ssec < 9.25*60*60) {
       cde = 7;
       brk_scan[ifile] = -9999;
     }

       } else { /* DAY (TERRA) */

     if (diffday_beg <= -2) {
       cde = 3;
       brk_scan[ifile] = -9999;
     }

     if (diffday_end >= +1) {
       cde = 4;
       brk_scan[ifile] = -9999;
     }

     if (diffday_beg == 0 && (scancross == 1) &&
         ssec <= 12*60*60) {
       brk_scan[ifile] = -1;
     }

     if (diffday_beg == 0 && (scancross == 1) &&
         ssec > 12*60*60) {
       brk_scan[ifile] = +1;
     }

     if (diffday_end == -1 && (scancross == 0) &&
         ssec < 22.0*60*60) {
       cde = 5;
       brk_scan[ifile] = -9999;
     }

     if (diffday_end == -1 && (scancross == 1)) {
       brk_scan[ifile] = -1;
     }

     if (diffday_beg == 0 && (scancross == 0) &&
         ssec > 23.0*60*60) {
       cde = 6;
       brk_scan[ifile] = -9999;
     }

     if (diffday_end == -1 && (scancross == 1) &&
         ssec < 21.0*60*60) {
       cde = 7;
       brk_scan[ifile] = -9999;
     }

       }

     } else if (strcmp(small_buf, "SeaWiFS") == 0 ||
            strcmp(small_buf, "CZCS"   ) == 0 ||
                strcmp(small_buf, "OCM2"   ) == 0) {

       /* SeaWiFS */
       /* ------- */

       /* Determine if swath crossed dateline */
       /* ----------------------------------- */
       scancross = 0;
       for (jsrow=l2_str[ifile].nrec-1; jsrow>=1; jsrow--) {
     scancross = slon[jsrow] >= 0 && slon[jsrow-1] < 0;
     if (scancross == 1) break;
       }

       /* If no crossing then filedate must equal processdate (Single Swath) */
       /* ------------------------------------------------------------------ */
       if (nfiles == 1 && scancross == 0 && (proc_day_beg == proc_day_end) &&
       l2_str[ifile].syear*1000+l2_str[ifile].sday != proc_day_beg) {
     printf("No output file generated\n");

     /* Close L2 files */
     /* -------------- */
     printf("Closing L2 files\n");
     for (ifile=0; ifile<nfiles; ifile++) {
       freeL2(&l2_str[ifile]);
     }
     freeL2(NULL);

     if (Hishdf(input.infile) == FALSE) fclose(fp);

     for (ifile=0; ifile<nfiles; ifile++) {
       if (bscan_row[ifile] != NULL) free(bscan_row[ifile]);
       if (escan_row[ifile] != NULL) free(escan_row[ifile]);
     }

     exit(110);
       }


       p1hr=18;
       m1hr=06;

       if (diffday_beg <= -2)
     brk_scan[ifile] = -9999;
       else if (diffday_end >= +2)
     brk_scan[ifile] = -9999;

       else if (diffday_beg == -1) {
     if (ssec > p1hr*60*60 && scancross == 1)
       brk_scan[ifile] = -1;
     else brk_scan[ifile] = -9999;
       }

       else if (diffday_end == +1) {
     if (ssec < m1hr*60*60 && scancross == 1)
       brk_scan[ifile] = +1;
     else brk_scan[ifile] = -9999;
       }

       else if (date == proc_day_beg && date == proc_day_end) {
     if (ssec > p1hr*60*60 && scancross == 1)
       brk_scan[ifile] = +1;

     if (ssec < m1hr*60*60 && scancross == 1)
       brk_scan[ifile] = -1;
       }

     } else if (strcmp(small_buf, "MERIS") == 0) {

       /* MERIS */
       /* ----- */

       /* Determine if swath crossed dateline */
       /* ----------------------------------- */
       scancross = 0;
       for (jsrow=l2_str[ifile].nrec-1; jsrow>=1; jsrow--) {
            scancross = slon[jsrow] >= 0 && slon[jsrow-1] < 0;
            if (scancross == 1) break;
            scancross = slon[jsrow] >= 0 && elon[jsrow] < 0;
            if (scancross == 1) break;
       }

       /* If no crossing then filedate must equal processdate (Single Swath) */
       /* ------------------------------------------------------------------ */
       if (nfiles == 1 && scancross == 0 && (proc_day_beg == proc_day_end) &&
       l2_str[ifile].syear*1000+l2_str[ifile].sday != proc_day_beg) {
     printf("No output file generated\n");

     /* Close L2 files */
     /* -------------- */
     printf("Closing L2 files\n");
     for (ifile=0; ifile<nfiles; ifile++) {
       freeL2(&l2_str[ifile]);
     }
     freeL2(NULL);

     if (Hishdf(input.infile) == FALSE) fclose(fp);

     for (ifile=0; ifile<nfiles; ifile++) {
       if (bscan_row[ifile] != NULL) free(bscan_row[ifile]);
       if (escan_row[ifile] != NULL) free(escan_row[ifile]);
     }

     exit(110);
       }

       p1hr=19;

       if (diffday_beg <= -2)
     brk_scan[ifile] = -9999;
       else if (diffday_end >= +2)
     brk_scan[ifile] = -9999;

       if (diffday_beg == -1) {
     if (ssec > p1hr*60*60 && scancross == 1) {
       brk_scan[ifile] = -1;
     } else if ((ssec > p1hr*60*60) && (scancross == 0)) {
       brk_scan[ifile] = 0;
     } else brk_scan[ifile] = -9999;
       }

       if (diffday_end == +1) {
     brk_scan[ifile] = -9999;
       }

       else if (date == proc_day_beg && date == proc_day_end) {
     if (ssec > p1hr*60*60) {
       if (scancross == 1)
         brk_scan[ifile] = +1;
       else
         brk_scan[ifile] = -9999;
     }
       }

     } else if (strcmp(small_buf, "OCTS") == 0) {

       /* OCTS */
       /* ---- */

       /* Determine if swath crossed dateline */
       /* ----------------------------------- */
       scancross = 0;
       for (jsrow=l2_str[ifile].nrec-1; jsrow>=1; jsrow--) {
     scancross = slon[jsrow] >= 0 && slon[jsrow-1] < 0;
     if (scancross == 1) break;
       }

       /* If no crossing then filedate must equal processdate (Single Swath) */
       /* ------------------------------------------------------------------ */
       if (nfiles == 1 && scancross == 0 &&
       l2_str[ifile].syear*1000+l2_str[ifile].sday != proc_day_beg) {
     printf("No output file generated\n");

     /* Close L2 files */
     /* -------------- */
     printf("Closing L2 files\n");
     for (ifile=0; ifile<nfiles; ifile++) {
       freeL2(&l2_str[ifile]);
     }
     freeL2(NULL);

     if (Hishdf(input.infile) == FALSE) fclose(fp);

     for (ifile=0; ifile<nfiles; ifile++) {
       if (bscan_row[ifile] != NULL) free(bscan_row[ifile]);
       if (escan_row[ifile] != NULL) free(escan_row[ifile]);
     }

     exit(110);
       }

       p1hr=18-1.333;
       m1hr=06-1.333;

       if (diffday_beg <= -2)
     brk_scan[ifile] = -9999;
       else if (diffday_end >= +2)
     brk_scan[ifile] = -9999;

       else if (diffday_beg == -1) {
     if (ssec > p1hr*60*60 && scancross == 1)
       brk_scan[ifile] = -1;
     else brk_scan[ifile] = -9999;
       }

       else if (diffday_end == +1) {
     if (ssec < m1hr*60*60 && scancross == 1)
       brk_scan[ifile] = +1;
     else brk_scan[ifile] = -9999;
       }

       else if (date == proc_day_beg && date == proc_day_end) {
     if (ssec > p1hr*60*60 && scancross == 1)
       brk_scan[ifile] = +1;

     if (ssec < m1hr*60*60 && scancross == 1)
       brk_scan[ifile] = -1;
       }

     } else {
       printf("Unsupported Sensor: %s\n", small_buf);
       exit(-1);
     }


   nxtfile:
     if (input.verbose == 1) {
       printf("scancross:%d  %s  brk:%5d  %3d %6d  cde:%2d\n",
          scancross, buf, brk_scan[ifile], sday, ssec, cde);
     }

     /*
     if (input.fileuse[0] != 0) {
       if (first_fileuse == 1) {
     fp2 = fopen(input.fileuse, "w");
       }
       if (brk_scan[ifile] != -9999) {
     fprintf(fp2,"%s\n", buf);
       }
     first_fileuse = 0;
     }
     */

     if (brk_scan[ifile] == -9999) n_active_files--;

     free(slon);
     free(elon);
     free(clon);
     free(elat);
     free(slat);
     free(clat);

   } /* ifile loop */

   // exit(-3);

   if (Hishdf(input.infile) == FALSE) fclose(fp);
   //  if (input.fileuse[0] != 0) fclose(fp2);

   /* Compute numbin array (Number of bins in each row) */
   /* ------------------------------------------------- */
   numbin = (int32 *) calloc(nrows, sizeof(int32));
   for (i=0; i<nrows; i++) {
     latbin = (i + 0.5) * (180.0 / nrows) - 90.0;
     numbin[i] = (int32) (cos(latbin * PI/180.0) * (2.0*nrows) + 0.5);
   }

   /* Compute basebin array (Starting bin of each row [1-based]) */
   /* ---------------------------------------------------------- */
   basebin = (int32 *) calloc(nrows+1, sizeof(int32));
   basebin[0] = 1;
   for (i=1; i<=nrows; i++) {
     basebin[i] = basebin[i-1] + numbin[i-1];
   }
   printf("total number of bins: %d\n", basebin[nrows]-1);



   /* Create output file */
   /* ------------------ */
   strcpy(buf, input.ofile);
   if (noext == 0) strcat(buf, ".main");
   fileid_w = Hopen(buf, DFACC_CREATE, 0);
   sd_id_w = SDstart(buf, DFACC_RDWR);

   Vstart(fileid_w);

   vgid_w = Vattach(fileid_w, -1, "w");

   Vsetname(vgid_w, "Level-3 Binned Data");
   Vsetclass(vgid_w, "PlanetaryGrid");



   /* Write "SEAGrid" */
   /* --------------- */
   a = (uint8 *) malloc(44);

   ncols = 2 * nrows;

   memcpy(&a[0],  &five, 4);
   memcpy(&a[4],  &zero, 4);
   memcpy(&a[8],  &ncols, 4);
   memcpy(&a[12], &radius, 8);
   memcpy(&a[20], &north, 8);
   memcpy(&a[28], &south, 8);
   memcpy(&a[36], &seam_lon, 8);

   type[0] = DFNT_INT32;
   type[1] = DFNT_INT32;
   type[2] = DFNT_INT32;
   type[3] = DFNT_FLOAT64;
   type[4] = DFNT_FLOAT64;
   type[5] = DFNT_FLOAT64;
   type[6] = DFNT_FLOAT64;

   wr_vdata(input.ofile, fileid_w, vgid_w, "SEAGrid", "Geometry", 7, 1,
        fldname1, type, 0, a, input.verbose);
   wr_vdata(input.ofile, fileid_w, vgid_w, "SEAGrid", "Geometry", 7, 0,
        NULL, NULL, 0, NULL, input.verbose);

   free(a);


   /* Allocate Arrays for Bin Index */
   /* ----------------------------- */
   beg      = (int32 *) calloc(nrows, sizeof(int32));
   ext      = (int32 *) calloc(nrows, sizeof(int32));
   bin_indx = (uint8 *) calloc(36 * nrows, 1);


   /* Initialize bin_indx array */
   /* ------------------------- */
   for (i=0; i<nrows; i++) {

     i32 = i;

     if (i32 < 0 || i32 >= nrows) {
       printf("%d %d\n", i, nrows);
       exit (-1);
     }

     vsize = 180.0 / nrows;
     hsize = 360.0 / numbin[i32];

     memcpy(&bin_indx[i*36], &i32, 4);
     memcpy(&bin_indx[i*36+4],  &vsize, 8);
     memcpy(&bin_indx[i*36+12], &hsize, 8);
     // JMG    memcpy(&bin_indx[i*36+20], &basebin[i32], 4);
     memcpy(&bin_indx[i*36+24], &beg[i32], 4);
     memcpy(&bin_indx[i*36+28], &ext[i32], 4);
     memcpy(&bin_indx[i*36+32], &numbin[i32], 4);
   } /* row_group loop */




   /* Process each group of bin rows (Main Loop) */
   /* ========================================== */
   for (krow=0; krow<nrows; krow+=row_group) {

     if (((float32) (krow+row_group) / nrows) * 180 - 90 < input.latsouth)
       continue;

     if ((float32) krow / nrows * 180 - 90 > input.latnorth)
       continue;

     n_bins_in_group = basebin[krow+row_group] - basebin[krow];
     within_flag = 0;


     /* Determine relevant swath rows for this bin row group for each file */
     /* ------------------------------------------------------------------ */
     for (ifile=0; ifile<nfiles; ifile++) {

       /* add an extra 0 to the end of scan_in_rowgroup so the caching
        * code never reads past the end of the file */
       scan_in_rowgroup[ifile] = (unsigned char *)
     calloc(l2_str[ifile].nrec+1, sizeof(unsigned char));

       for (jsrow=0; jsrow<l2_str[ifile].nrec; jsrow++) {
       scan_in_rowgroup[ifile][jsrow] = 1;
     if (bscan_row[ifile][jsrow] < krow ||
         escan_row[ifile][jsrow] >= (krow+row_group-1)) {
       scan_in_rowgroup[ifile][jsrow] = 255;
     }
       } /* jsrow loop */


       /* Determine if within bin row group */
       /* --------------------------------- */
       for (jsrow=0; jsrow<l2_str[ifile].nrec; jsrow++) {
     if (scan_in_rowgroup[ifile][jsrow] == 1 && within_flag == 0) {
       within_flag = 1;
       break;
     }
       } /* scan row loop */



     } /* ifile loop */


     /* If no swath rows within group then continue to next group */
     /* --------------------------------------------------------- */
     if (within_flag == 0) {

       for (ifile=0; ifile<nfiles; ifile++) {
     if (scan_in_rowgroup[ifile] != NULL) {
       free(scan_in_rowgroup[ifile]);
       scan_in_rowgroup[ifile] = NULL;
     }
       }
       continue;
     }


     /* Print info on rowgroup */
     /* ---------------------- */
     time(&tnow);
     tmnow = localtime(&tnow);
     printf("krow:%6d out of %6d  (%6.2f to %6.2f) ",
        krow, nrows,
        ((float32) (krow) / nrows) * 180 - 90,
        ((float32) (krow+row_group) / nrows) * 180 - 90);
     printf("%s", asctime(tmnow));



     /* Allocate # pixels in bin, bin_flag, tilt, qual, & nscenes arrays */
     /* ---------------------------------------------------------------- */
     n_filled_bins = 0;
     bin_flag = (int32 *) calloc(n_bins_in_group, sizeof(int32));
     tilt     = (int16 *) calloc(n_bins_in_group, sizeof(int16));
     qual     = (int16 *) calloc(n_bins_in_group, sizeof(int16));
     nscenes  = (int16 *) calloc(n_bins_in_group, sizeof(int16));
     lastfile = (int16 *) calloc(n_bins_in_group, sizeof(int16));

     for (i=0; i<n_bins_in_group; i++) {
       tilt[i] = -1;
       qual[i] = 3;
       lastfile[i] = -1;
     }


     /* Allocate bin accumulator & data value arrays */
     /* -------------------------------------------- */
     nobs = (int16 *) calloc(n_bins_in_group, sizeof(int16));
     allocated_space = (int16 *) calloc(n_bins_in_group, sizeof(int16));
     data_values = (float32 **) calloc(n_bins_in_group, sizeof(float32 *));
     file_index = (int16 **) calloc(n_bins_in_group, sizeof(int16 *));
     data_quality = (uint8 **) calloc(n_bins_in_group, sizeof(uint8 *));


     /* Initialize bin counters */
     /* ----------------------- */
     n_allocperbin = n_active_files * l2_str[0].nrec * l2_str[0].nsamp / 50000000;

     if (n_allocperbin <  2)  n_allocperbin =  2;
     if (n_allocperbin > MAXALLOCPERBIN)  n_allocperbin = MAXALLOCPERBIN;

     if (input.verbose == 1) {
       printf("%-20s:%8d\n", "# allocated per bin", n_allocperbin);
       printf("\n");
     }

     for (i=0; i<n_bins_in_group; i++) {
       nobs[i] = 0;
       allocated_space[i] = 0;
       lastfile[i] = -1;
     }


     /* Read L2 files and fill data_values (L3b) array */
     /* ++++++++++++++++++++++++++++++++++++++++++++++ */
     for (ifile=0; ifile<nfiles; ifile++) {

       free_rowgroup_cache();

       /* if "early" or "late" input file then skip */
       /* ----------------------------------------- */
       if (brk_scan[ifile] == -9999) continue;


       status = reopenL2(ifile, &l2_str[ifile]);


       /* if no scans in rowgroup for this file then skip */
       /* ----------------------------------------------- */
       for (jsrow=0; jsrow<l2_str[ifile].nrec; jsrow++) {
     if (scan_in_rowgroup[ifile][jsrow] == 1) {
       break;
     }
       }
       if (jsrow == l2_str[ifile].nrec) {
     closeL2(&l2_str[ifile], ifile);
     continue;
       }

       /* Get tilt flags & ranges */
       /* ----------------------- */
       ntilts = l2_str[ifile].ntilts;
       for (i=0; i<ntilts; i++) {
     tilt_flags[i] = l2_str[ifile].tilt_flags[i];
     tilt_ranges[0][i] = l2_str[ifile].tilt_ranges[0][i];
     tilt_ranges[1][i] = l2_str[ifile].tilt_ranges[1][i];
       }


       /* Get date stuff */
       /* -------------- */
       date = l2_str[ifile].syear*1000+l2_str[ifile].sday;
       diffday_beg = diffday(date, proc_day_beg);
       diffday_end = diffday(date, proc_day_end);
       sday = l2_str[ifile].sday;
       ssec = l2_str[ifile].smsec/1000;


       /* Loop over swath rows */
       /* ^^^^^^^^^^^^^^^^^^^^ */
       for (jsrow=0; jsrow<l2_str[ifile].nrec; jsrow++) {

     /* if swath row not within group then continue */
     /* ------------------------------------------- */
     if (scan_in_rowgroup[ifile][jsrow] != 1) continue;


     /* Read swath record from L2 */
     /* ------------------------- */
     status = readL2(&l2_str[ifile], ifile, jsrow, -1,
                         scan_in_rowgroup[ifile]);
     /*
     if (status == 5) {
       printf("%s not navigatable.  Removing from processing.\n",
          l2_str[ifile].filename);
       brk_scan[ifile] = -9999;
       status = 0;
       break;
     }
     */
     if (status == 5) continue;


     /* Check tilt state */
     /* ---------------- */
     foundtiltstate = FALSE;
     if (ntilts == 0) foundtiltstate = TRUE;
     for (i=0; i<ntilts; i++) {
       if ((jsrow+1) <= tilt_ranges[1][i]) {
         tiltstate = (tilt_flags[i] & 0xFF);
         foundtiltstate = TRUE;
         break;
       }
     }
     /*  if (tiltstate < 0 || tiltstate > 2) continue;*/


     /* Compute scan_frac */
     /* ----------------- */
     nsamp = compute_scanfrac(ifile, ipixl, flagusemask, required);
     if (nsamp == 0) continue;


     if ((jsrow % 100) == 0 && input.verbose == 1) {
       printf("ifile:%4d  jsrow:%6d  nsamp:%8d\n", ifile, jsrow, nsamp);
     }

 #if 0
     /* Check for bad lon/lat (2.1.6) */
     /* ----------------------------- */
     bad_lonlat = 0;
     for (isamp=0; isamp<nsamp; isamp++) {
       if (l2_str[ifile].longitude[isamp] < -180) bad_lonlat = 1;
       if (l2_str[ifile].longitude[isamp] > +180) bad_lonlat = 1;
       if (l2_str[ifile].latitude[isamp]  <  -90) bad_lonlat = 1;
       if (l2_str[ifile].latitude[isamp]  >  +90) bad_lonlat = 1;
     }
     if (bad_lonlat == 1)
       continue;
 #endif

     /* ##### Loop over L2 pixels ##### */
     /* ------------------------------- */
     for (isamp=0; isamp<nsamp; isamp++) {

       ipixl = floor((float64) scan_frac[isamp]);

       /*
       if (BINCHECK >= 0) {
         bin = getbinnum(ifile, ipixl, isamp);
         if (bin == BINCHECK) {
           printf("bin_bf: %d  ifile: %d i: %d ipixl: %d lat: %f lon: %f %d\n",
              bin,ifile,i,ipixl,
              l2_str[ifile].latitude[ipixl],l2_str[ifile].longitude[ipixl],
              l2_str[ifile].l2_flags[ipixl]);
         }
       }
       */

       /* if bin flagged then continue */
       /* ---------------------------- */
       flagcheck = (l2_str[ifile].l2_flags[ipixl] |
                l2_str[ifile].l2_flags[ipixl+input.interp]);
       if ((flagcheck & flagusemask) != 0)
         continue;
       if ((flagcheck & required) != required)
         continue;


       /* Check for dateline crossing */
       /* --------------------------- */
       if (input.night == 1) {

         if ((brk_scan[ifile] == -1) &&
         (diffday_beg == -1) && (1) &&
         (l2_str[ifile].longitude[ipixl] < 0)) continue;

         if ((brk_scan[ifile] == +1) &&
         (diffday_end == 0)  && (1) &&
         (l2_str[ifile].longitude[ipixl] > 0)) continue;

       } else {

         if ((brk_scan[ifile] == -1) &&
         (diffday_beg <= 0) &&
         (l2_str[ifile].longitude[ipixl] < 0)) continue;

         if ((brk_scan[ifile] == +1) &&
         (diffday_end >= 0) &&
         (l2_str[ifile].longitude[ipixl] > 0)) continue;
       }


       /* Check for bad value in any of the products */
       /* ------------------------------------------ */
       bad_value = 0;
       for (jprod=0; jprod<l3b_nprod; jprod++) {

         f32 = l2_str[ifile].l2_data[ipixl+off[ifile][numer[ifile][jprod]]];
         if ( isnan(f32)) {
           bad_value = 1;
           break;
         }
       }
       if (bad_value == 1) continue;


       /* Check if within longitude boundaries */
       /* ------------------------------------ */
       if (input.lonwest != 0.0 || input.loneast != 0.0) {
         if (l2_str[ifile].longitude[ipixl] < input.lonwest) continue;
         if (l2_str[ifile].longitude[ipixl] > input.loneast) continue;
       }


       /* Get Bin Number for Pixel */
       /* ------------------------ */
       bin = getbinnum(ifile, ipixl, isamp); // bin is 1-based
       if (bin == -1) {
         printf("file: %s  ipixl: %d  jsrow: %d\n",
            l2_str[ifile].filename, ipixl, jsrow);
         continue;
       }
       ibin = bin - basebin[krow];


       /* if bin not within bin row group then continue */
       /* --------------------------------------------- */
       if (ibin < 0 || bin >= basebin[krow+row_group]) continue;


       /* GOOD OBSERVATION FOUND */
       /* ---------------------- */

       if (input.dcinfo) {
         if((l2_str[ifile].longitude[ipixl] <= -160) ||
            (l2_str[ifile].longitude[ipixl] >= +160)) {
           printf("DC: %10d %12d %8.2f %8.2f\n",
              bin,
              l2_str[ifile].sday*24*3600+ssec,
              l2_str[ifile].longitude[ipixl],
              l2_str[ifile].latitude[ipixl]);
         }
       }

       /*
       if (bin == BINCHECK) {
         printf("bin_af: %d  ifile: %d i: %d ipixl: %d lat: %f lon: %f\n",
            bin,ifile,i,ipixl,
            l2_str[ifile].latitude[ipixl],l2_str[ifile].longitude[ipixl]);
       }
       */

       /* "OR" flags in swath pixel & set tilt & increment nscenes */
       /* -------------------------------------------------------- */
       bin_flag[ibin] = bin_flag[ibin] | l2_str[ifile].l2_flags[ipixl];

       tilt[ibin] = tiltstate;
       if (ifile != lastfile[ibin]) {
         nscenes[ibin]++;
         lastfile[ibin] = ifile;
       }


       /* Allocate space for file index & bin data values */
       /* ----------------------------------------------- */
       if (file_index[ibin] == NULL) {
         file_index[ibin] = (int16 *) calloc(n_allocperbin, sizeof(int16));

         data_values[ibin] =
           (float32 *) calloc(n_allocperbin*l3b_nprod, sizeof(float32));

         if (data_values[ibin] == 0x0) {
           perror(buf);
           printf("Allocation failed for data_values[ibin]: %d %s\n",
              ibin,buf);
           exit(-1);
         }

         data_quality[ibin] =
           (uint8 *) calloc(n_allocperbin, sizeof(uint8));

         if (data_quality[ibin] == 0x0) {
           perror(buf);
           printf("Allocation failed for data_quality[ibin]: %d %s\n",
              ibin,buf);
           exit(-1);
         }

         allocated_space[ibin] = n_allocperbin;
       }


       /* Set file_index for each observation */
       /* ----------------------------------- */
       file_index[ibin][nobs[ibin]] = ifile;


       /* Get data quality */
       /* ---------------- */
       if (input.qual_prod[0] != 0) {
         data_quality[ibin][nobs[ibin]] =
           l2_str[ifile].l2_data[ipixl+off[ifile][qual_prod_index[ifile]]];
       }


       /* Get data values for all L3 products */
       /* ----------------------------------- */
       for (jprod=0; jprod<l3b_nprod; jprod++) {

         f32 = l2_str[ifile].l2_data[ipixl+off[ifile][numer[ifile][jprod]]];

         /* Set -32767 value to "bad" quality */
         if (f32 == -32767)
           if (input.qual_prod[0] != 0)
         data_quality[ibin][nobs[ibin]] = 4;

         if (input.interp == 1) {
           f32 += (scan_frac[isamp] - ipixl) *
         (l2_str[ifile].l2_data[ipixl+1+off[ifile][numer[ifile][jprod]]] -
          l2_str[ifile].l2_data[ipixl+off[ifile][numer[ifile][jprod]]]);
         }

         if (denom[ifile][jprod] == -1 && f32 >= min_value[jprod])
           data_values[ibin][l3b_nprod*nobs[ibin]+jprod] = f32;

         if (denom[ifile][jprod] == -1 && f32 < min_value[jprod])
           data_values[ibin][l3b_nprod*nobs[ibin]+jprod] = min_value[jprod];

         if (denom[ifile][jprod] == -2)
           data_values[ibin][l3b_nprod*nobs[ibin]+jprod] =
         (l2_str[ifile].l2_flags[ipixl] >> numer[ifile][jprod]) & 1;


         /* ratio product */
         /* ------------- */
         if (denom[ifile][jprod] >= 0) {

           data_values[ibin][l3b_nprod*nobs[ibin]+jprod] = f32;

           f32 = l2_str[ifile].l2_data[ipixl+off[ifile][denom[ifile][jprod]]];

           if (input.interp == 1) {
         f32 += (scan_frac[isamp] - ipixl) *
           (l2_str[ifile].l2_data[ipixl+1+off[ifile][denom[ifile][jprod]]] -
            l2_str[ifile].l2_data[ipixl+off[ifile][denom[ifile][jprod]]]);
           }

           if (f32 >= min_value[jprod])
         data_values[ibin][l3b_nprod*nobs[ibin]+jprod] /= f32;
           else
         data_values[ibin][l3b_nprod*nobs[ibin]+jprod] /= min_value[jprod];
         }

       } /* jprod loop */


       /* Increment number of observations in bin */
       /* --------------------------------------- */
       nobs[ibin]++;


       /* Reallocate if necessary */
       /* ----------------------- */
       if (nobs[ibin] == allocated_space[ibin]) {

         file_index[ibin] =
           (int16 *) realloc(file_index[ibin],
                 (nobs[ibin]+n_allocperbin) * sizeof(int16));

         data_values[ibin] =
           (float32 *) realloc(data_values[ibin],
                   (nobs[ibin]+n_allocperbin) * l3b_nprod *
                   sizeof(float32));
         if (data_values[ibin] == 0x0) {
           perror(buf);
           printf("Reallocation failed for data_values[ibin]: %d %s\n",
              ibin,buf);
           exit(-1);
         }

         data_quality[ibin] =
           (uint8 *) realloc(data_quality[ibin],
                 (nobs[ibin]+n_allocperbin) * sizeof(uint8));
         if (data_quality[ibin] == 0x0) {
           perror(buf);
           printf("Reallocation failed for data_quality[ibin]: %d %s\n",
              ibin,buf);
           exit(-1);
         }

         allocated_space[ibin] += n_allocperbin;
       } /* end reallocate */

     } /* ##### i (ipixl) loop ##### */

     free(scan_frac);


       } /* ^^^^^^^^^^ jsrow loop ^^^^^^^^^^ */

       closeL2(&l2_str[ifile], ifile);


 #ifdef MALLINFO
       if (input.meminfo) {
     /*      malloc_stats();*/
     minfo = mallinfo();
     total_alloc_space = 0;
     for (i=0; i<n_bins_in_group; i++) {
       total_alloc_space += allocated_space[i];
     }
     printf("Used space: %10d\n", minfo.uordblks);
     printf("Allo space: %10d\n", total_alloc_space * (2+l3b_nprod*4));
       }
 #endif


     } /* ++++++++++ ifile loop ++++++++++ */

     time(&tnow);
     tmnow = localtime(&tnow);
     if (input.verbose == 1)
       printf("krow:%5d After data_value fill: %s\n", krow, asctime(tmnow));



 #ifdef GSL
     /* ADJUST FOR MIDAVERAGE AND MEDIAN HERE */
     /* ------------------------------------- */
     if (strncmp(input.average, "midaverage", 10) == 0)
       midaverage(n_bins_in_group, l3b_nprod, krow);

     if (strncmp(input.average, "median", 6) == 0)
       median(n_bins_in_group, l3b_nprod, krow);
     /* END MIDAVERAGE/MEDIAN SECTION */
 #endif


     /* Compute Total # of filled bins */
     /* ------------------------------ */
     for (ibin=0; ibin<n_bins_in_group; ibin++) {
       // JMG
       if (nobs[ibin] > 0 && nobs[ibin] < input.minobs)
     nobs[ibin] = 0;

       if (nobs[ibin] != 0) n_filled_bins++;
     } /* ibin loop */


     best_qual  = (uint8 *) calloc(n_bins_in_group, sizeof(uint8));
     memset(best_qual, 255, n_bins_in_group * sizeof(uint8));


     /* ********** If filled bins ********** */
     /* ------------------------------------ */
     if (n_filled_bins > 0) {

       last_group = 1;

       /* Fill "Bin List" vdata array */
       /* --------------------------- */
       a = (uint8 *) calloc(19 * n_filled_bins, 1);

       i = 0;
       for (ibin=0; ibin<n_bins_in_group; ibin++) {

     bin = ibin + basebin[krow];

     /*
     if (bin == BINCHECK) {
       for (j=0; j<nobs[ibin]; j++)
         printf("qual: %d %d %d\n",
            bin, nobs[ibin], data_quality[ibin][j]);
     }
     */

     /* Adjust for bins with "bad" quality values */
     /* ----------------------------------------- */
     if (input.qual_prod[0] != 0 && nobs[ibin] > 0) {
       best_qual[ibin] = 255;
       for (j=0; j<nobs[ibin]; j++)
         if (data_quality[ibin][j] < best_qual[ibin])
           best_qual[ibin] = data_quality[ibin][j];

       k = 0;
       for (j=0; j<nobs[ibin]; j++) {
         if ((data_quality[ibin][j] <= best_qual[ibin]) &&
         (data_quality[ibin][j] <= qual_max_allowed)) {
           if (k < j) {
         for (iprod=0; iprod < l3b_nprod; iprod++) {
           data_values[ibin][k*l3b_nprod+iprod] =
             data_values[ibin][j*l3b_nprod+iprod];
         }
           }
           k++;
         }
       }
       nobs[ibin] = k;

       if (nobs[ibin] == 0) n_filled_bins--;
     }

     if (nobs[ibin] != 0) {

       bin = ibin + basebin[krow];
       memcpy(&a[i*19],    &bin, 4);

       memcpy(&a[i*19+4],  &nobs[ibin], 2);
       memcpy(&a[i*19+6],  &nscenes[ibin], 2);
       memcpy(&a[i*19+8],  &time_rec, 2);

       if (bin == BINCHECK) {
         printf("%d %d %d\n", bin, nobs[ibin], best_qual[ibin]);
       }

       /* weights {=sqrt(# of L2 files in given bin)} */
       /* ------------------------------------------- */
       wgt = 0.0;
       for (ifile=0; ifile<=nfiles; ifile++) {
         i32 = 0;
         for (j=0; j<nobs[ibin]; j++) {
           if (file_index[ibin][j] == ifile) i32++;
         }
         wgt += sqrt(i32);
       }
       if (bin == BINCHECK) printf("%d %d %f\n", i32, i16, wgt);

       memcpy(&a[i*19+10], &wgt, 4);

       selcat = (tilt[ibin] << 2) | qual[ibin];
       memcpy(&a[i*19+14],  &selcat, 1);

       memcpy(&a[i*19+15],  &bin_flag[ibin], 4);

       i++;


       /* Update Max/Min Lon/Lat */
       /* ---------------------- */
       for (j=last_group; j<=row_group; j++) {
         if ((ibin + basebin[krow]) < basebin[krow+j]) {
           latbin = (j + krow + 0.5) * (180.0 / nrows) - 90.0;
           lonbin = 360.0 * (ibin+basebin[krow]-basebin[krow+j-1]+0.5) /
         numbin[krow+j-1];
           last_group = j;

           break;
         }
       }
       if (latbin > northmost) northmost = latbin;
       if (latbin < southmost) southmost = latbin;

       lonbin += seam_lon;
       if (lonbin > eastmost) eastmost = lonbin;
       if (lonbin < westmost) westmost = lonbin;


     } /* nobs[ibin] != 0 */
       } /* ibin loop */


       /* if no good obs left than bail */
       /* ----------------------------- */
       //      if (n_filled_bins == 0) continue;
       if (n_filled_bins == 0) goto freemem;


       /* Print info on filled row group */
       /* ------------------------------ */
       printf("%-20s:%8d\n", "# bins in row group", n_bins_in_group);
       printf("%-20s:%8d\n", "# filled bins", n_filled_bins);
       printf("\n");


       /* Write "Bin List" vdata */
       /* ---------------------- */
       type[0] = DFNT_INT32;
       type[1] = DFNT_INT16;
       type[2] = DFNT_INT16;
       type[3] = DFNT_INT16;
       type[4] = DFNT_FLOAT32;
       type[5] = DFNT_INT8;
       type[6] = DFNT_INT32;


       wr_vdata(input.ofile, fileid_w, vgid_w, "BinList", "DataMain", 7,
            n_filled_bins, fldname2, type, 0, a, input.verbose);

       free(a);


       /* Allocate sum & sum-squared arrays */
       /* --------------------------------- */
       sum_data  = (float32 *) calloc(n_bins_in_group, sizeof(float32));
       sum2_data = (float32 *) calloc(n_bins_in_group, sizeof(float32));

       /* Loop over all L3 products to fill sum arrays */
       /* -------------------------------------------- */
       for (iprod=0; iprod < l3b_nprod; iprod++) {

     memset(sum_data,  0, n_bins_in_group * sizeof(float32));
     memset(sum2_data, 0, n_bins_in_group * sizeof(float32));

     fldname3[0] = (char *) calloc(strlen(prodname[iprod])+5,sizeof(char));
     fldname3[1] = (char *) calloc(strlen(prodname[iprod])+8,sizeof(char));

     char_ptr1 = strchr(prodname[iprod], '/');
     if (char_ptr1 != NULL) *char_ptr1 = '_';

     strcpy(fldname3[0], prodname[iprod]);
     strcpy(fldname3[1], prodname[iprod]);
     strcat(fldname3[0], "_sum");
     strcat(fldname3[1], "_sum_sq");

     if (char_ptr1 != NULL) *char_ptr1 = '/';

     /* Process bins */
     /* ------------ */
     for (ibin=0; ibin<n_bins_in_group; ibin++) {

       if ((ibin % 10000) == 0) {
         time(&tnow);
         tmnow = localtime(&tnow);
       }

       if (nobs[ibin] == 0) continue;

       /* Display data values if BINCHECK */
       bin = ibin + basebin[krow];
       if (bin == BINCHECK) {
         kprod = 0;
         for (j=0; j<nobs[ibin]; j++)
           printf("value: %10d %3d %4d %10.4f\n",
              bin, nobs[ibin], file_index[ibin][j],
              data_values[ibin][j*l3b_nprod+kprod]);
       }

       // Dump data
       if (BINCHECK == -2) {
         for (j=0; j<nobs[ibin]; j++) {
           printf("iprod: %3d bin: %8d f#: %3d %14.7f\n",
              iprod, bin, file_index[ibin][j],
              data_values[ibin][j*l3b_nprod+iprod]);
         }
       }

       /* Process data file by file */
       /* ------------------------- */
       i32 = 1;
       sum  = data_values[ibin][0*l3b_nprod+iprod];
       sum2 = sum * sum;
       for (j=1; j<nobs[ibin]; j++) {
         if (file_index[ibin][j] == file_index[ibin][j-1]) {
           i32++;
           sum += data_values[ibin][j*l3b_nprod+iprod];
           sum2 += data_values[ibin][j*l3b_nprod+iprod] *
         data_values[ibin][j*l3b_nprod+iprod];
         } else {
           sum_data[ibin]  += (sum  / sqrt(i32));
           sum2_data[ibin] += (sum2 / sqrt(i32));

           i32 = 1;
           sum  = data_values[ibin][j*l3b_nprod+iprod];
           sum2 = sum * sum;
         }
       } /* observation loop */
       sum_data[ibin]  += (sum  / sqrt(i32));
       sum2_data[ibin] += (sum2 / sqrt(i32));

     } /* ibin loop */


     /* Write Product Vdatas */
     /* -------------------- */
     a = (uint8 *) calloc(8 * n_filled_bins, 1);

     /* Fill bin data array */
     /* ------------------- */
     i = 0;
     for (ibin=0; ibin<n_bins_in_group; ibin++) {
       if (nobs[ibin] != 0) {

         memcpy(&a[i*8  ], &sum_data[ibin],  4);
         memcpy(&a[i*8+4], &sum2_data[ibin], 4);
         i++;
       }
     }
         type[0] = DFNT_FLOAT32;
     type[1] = DFNT_FLOAT32;

     char_ptr1 = strchr(prodname[iprod], '/');
     if (char_ptr1 != NULL) *char_ptr1 = '_';

     wr_vdata(input.ofile, fileid_w, vgid_w, prodname[iprod],
          "DataSubordinate", 2, i, fldname3, type, noext, a,
          input.verbose);

     if (char_ptr1 != NULL) *char_ptr1 = '/';


     free(a);

     free(fldname3[0]);
     free(fldname3[1]);

       } /* iprod loop */


       /* Write Quality vdata */
       /* ------------------- */
       if (input.qual_prod[0] != 0) {
     a = (uint8 *) calloc(n_filled_bins, 1);

     i = 0;
     for (ibin=0; ibin<n_bins_in_group; ibin++) {
       if (nobs[ibin] != 0) {
         memcpy(&a[i], &best_qual[ibin], 1);
         i++;
       }
     }
     type[0] = DFNT_UINT8;

     wr_vdata(input.ofile, fileid_w, vgid_w, "qual_l3", "DataQuality", 1,
          i, fldname5, type, 0, a, input.verbose);
     free(a);
       }


       /* Free dynamic memory */
       /* ------------------- */
       if (sum_data  != NULL) free(sum_data);
       if (sum2_data != NULL) free(sum2_data);
       if (best_qual != NULL) free(best_qual);


       /* Compute "begin" & "extent" vdata entries */
       /* ---------------------------------------- */
       binnum_data = (int32 *) calloc(n_filled_bins, sizeof(int32));

       i = 0;
       for (ibin=0; ibin<n_bins_in_group; ibin++) {
     if (nobs[ibin] != 0) {
       binnum_data[i] = ibin + basebin[krow];

       if (i < 0 || i >= n_filled_bins) {
         printf("Error: %d %d %d %d\n",
            i, ibin, n_filled_bins, n_bins_in_group);
       }
       i++;
     }
       }

       get_beg_ext(n_filled_bins, binnum_data, basebin, nrows,
           beg, ext);

       free(binnum_data);

       total_filled_bins += n_filled_bins;


     } /* ********** n_filled_bin > 0 ********** */
     time(&tnow);
     tmnow = localtime(&tnow);
     if (input.verbose == 1)
       printf("krow:%5d After bin processing:  %s", krow, asctime(tmnow));


     /* Fill BinIndex Vdata */
     /* ------------------- */
     for (i=0; i<row_group; i++) {

       i32 = i + krow;

       if (i32 < 0 || i32 >= nrows) {
     printf("Error: %d %d\n", i, krow);
     exit (-1);
       }

       vsize = 180.0 / nrows;
       hsize = 360.0 / numbin[i32];

       memcpy(&bin_indx[(i+krow)*36], &i32, 4);
       memcpy(&bin_indx[(i+krow)*36+4],  &vsize, 8);
       memcpy(&bin_indx[(i+krow)*36+12], &hsize, 8);
       memcpy(&bin_indx[(i+krow)*36+20], &basebin[i32], 4);
       memcpy(&bin_indx[(i+krow)*36+24], &beg[i32], 4);
       memcpy(&bin_indx[(i+krow)*36+28], &ext[i32], 4);
       memcpy(&bin_indx[(i+krow)*36+32], &numbin[i32], 4);
     } /* row_group loop */

     /* End Bin Index Fill */


   freemem:
     /* Free Dynamic Memory */
     /* ------------------- */
     if (bin_flag != NULL) free(bin_flag);
     if (tilt != NULL) free(tilt);
     if (qual != NULL) free(qual);
     if (nscenes != NULL) free(nscenes);
     if (lastfile != NULL) free(lastfile);

     for (ifile=0; ifile<nfiles; ifile++) {
       if (scan_in_rowgroup[ifile] != NULL) free(scan_in_rowgroup[ifile]);
     }

     time(&tnow);
     tmnow = localtime(&tnow);
     if (input.verbose == 1)
       printf("krow:%5d Befre free dynic mem:  %s", krow, asctime(tmnow));

     for (i=0; i<n_bins_in_group; i++) {
       if (file_index[i] != NULL) free(file_index[i]);
       if (data_values[i] != NULL) free(data_values[i]);
       if (data_quality[i] != NULL) free(data_quality[i]);
     }

     time(&tnow);
     tmnow = localtime(&tnow);
     if (input.verbose == 1)
       printf("krow:%5d After free dynic mem:  %s", krow, asctime(tmnow));

     free(data_values);
     free(data_quality);
     free(nobs);
     free(allocated_space);
     free(file_index);

   } /* ========== End krow (Main) loop ========== */
   time(&tnow);
   tmnow = localtime(&tnow);
   printf("krow:%5d %s", krow, asctime(tmnow));

   printf("total_filled_bins: %d\n", total_filled_bins);

   if (total_filled_bins == 0) {
    strcpy(buf, "rm -f ");
    strcat(buf, input.ofile);
    strcat(buf, "*");
    printf("%s\n", buf);
    system(buf);
    ret_status = 110;
    goto bail;
   }



   /* Close BinList Vdata */
   /* ------------------- */
   wr_vdata(input.ofile, fileid_w, vgid_w, "BinList", "DataMain",
        7, 0, NULL, NULL, 0, NULL, input.verbose);


   /* Close Product Vdatas */
   /* -------------------- */
   for (iprod=0; iprod<l3b_nprod; iprod++) {

     char_ptr1 = strchr(prodname[iprod], '/');
     if (char_ptr1 != NULL) *char_ptr1 = '_';

     wr_vdata(input.ofile, fileid_w, vgid_w, prodname[iprod], "DataSubordinate",
          2, 0, NULL, NULL, 0, NULL, input.verbose);

     if (char_ptr1 != NULL) *char_ptr1 = '/';
   }


   /* Write and Close BinIndex Vdata */
   /* ------------------------------ */
   type[0] = DFNT_INT32;
   type[1] = DFNT_FLOAT64;
   type[2] = DFNT_FLOAT64;
   type[3] = DFNT_INT32;
   type[4] = DFNT_INT32;
   type[5] = DFNT_INT32;
   type[6] = DFNT_INT32;

   wr_vdata(input.ofile, fileid_w, vgid_w, "BinIndex", "Index", 7, nrows,
        fldname4, type, 0, bin_indx, input.verbose);
   wr_vdata(input.ofile, fileid_w, vgid_w, "BinIndex", "Index", 7, 0,
        NULL, NULL, 0, NULL, input.verbose);


   /* Close Quality Vdata */
   /* ------------------- */
   if (input.qual_prod[0] != 0) {
     wr_vdata(input.ofile, fileid_w, vgid_w, "qual_l3", "DataQuality", 1,
          0, NULL, NULL, 0, NULL, input.verbose);
   }


   /* Read and write global attributes */
   /* -------------------------------- */
   printf("Writing Global Attributes\n");

   status = reopenL2(0, &l2_str[0]);
   status = readL2meta(&meta_l2, 0);

   if (meta_l2.sensor_name != NULL)
     strcpy(&meta_l3b.sensor_name[0], &meta_l2.sensor_name[0]);
   if (meta_l2.mission != NULL)
     strcpy(&meta_l3b.mission[0], &meta_l2.mission[0]);
   if (meta_l2.mission_char != NULL)
     strcpy(&meta_l3b.mission_char[0], &meta_l2.mission_char[0]);
   if (meta_l2.sensor != NULL)
     strcpy(&meta_l3b.sensor[0], &meta_l2.sensor[0]);
   if (meta_l2.sensor_char != NULL)
     strcpy(&meta_l3b.sensor_char[0], &meta_l2.sensor_char[0]);
   if (meta_l2.data_center != NULL)
     strcpy(&meta_l3b.data_center[0], &meta_l2.data_center[0]);
   if (meta_l2.station != NULL)
     strcpy(&meta_l3b.station[0], &meta_l2.station[0]);
   meta_l3b.station_lat = meta_l2.station_lat;
   meta_l3b.station_lon = meta_l2.station_lon;


   strcpy(buf, input.ofile);
   status = SDsetattr(sd_id_w, "Product Name", DFNT_CHAR, strlen(buf)+1, buf);

   strcpy(buf, meta_l3b.sensor_name);
   strcat(buf, " Level-3 Binned Data");
   status = SDsetattr(sd_id_w, "Title", DFNT_CHAR, strlen(buf)+1, buf);

   strcpy(buf, meta_l3b.sensor_name);
   status = SDsetattr(sd_id_w, "Sensor Name", DFNT_CHAR, strlen(buf)+1, buf);

   //  strcpy(buf, meta_l3b.data_center);
   //status = SDsetattr(sd_id_w, "Data Center", DFNT_CHAR, strlen(buf)+1, buf);

   strcpy(buf, meta_l3b.mission);
   status = SDsetattr(sd_id_w, "Mission", DFNT_CHAR, strlen(buf)+1, buf);

   strcpy(buf, meta_l3b.mission_char);
   status = SDsetattr(sd_id_w, "Mission Characteristics", DFNT_CHAR, strlen(buf)+1, buf);

   strcpy(buf, meta_l3b.sensor);
   status = SDsetattr(sd_id_w, "Sensor", DFNT_CHAR, strlen(buf)+1, buf);

   strcpy(buf, meta_l3b.sensor_char);
   status = SDsetattr(sd_id_w, "Sensor Characteristics", DFNT_CHAR, strlen(buf)+1, buf);

   strcpy(buf, input.prodtype);
   status = SDsetattr(sd_id_w, "Product Type", DFNT_CHAR, strlen(buf)+1, buf);

   strcpy(buf, input.pversion);
   status = SDsetattr(sd_id_w, "Processing Version", DFNT_CHAR, strlen(buf)+1, buf);

   strcpy(buf, "l2bin");
   status = SDsetattr(sd_id_w, "Software Name", DFNT_CHAR, strlen(buf)+1, buf);

   strcpy(buf, VERSION);
   status = SDsetattr(sd_id_w, "Software Version", DFNT_CHAR, strlen(buf)+1, buf);

   i32 = l2_str[nfiles-1].orbit;
   status = SDsetattr(sd_id_w, "Orbit", DFNT_INT32, 1, (VOIDP) &i32);

   i32 = l2_str[0].orbit;
   status = SDsetattr(sd_id_w, "Start Orbit", DFNT_INT32, 1, (VOIDP) &i32);

   i32 = l2_str[nfiles-1].orbit;
   status = SDsetattr(sd_id_w, "End Orbit", DFNT_INT32, 1, (VOIDP) &i32);

   get_time(buf);
   status = SDsetattr(sd_id_w, "Processing Time", DFNT_CHAR, strlen(buf)+1, buf);

   strcpy(buf, argv[0]);
   for (i=1; i < argc; i++) {
     strcat(buf, " ");
     strcat(buf, argv[i]);
   }
   status = SDsetattr(sd_id_w, "Processing Control", DFNT_CHAR, strlen(buf)+1, buf);

   /* Fill in missing parmeters in input.parms */
   if (input.qual_prod[0] == 0) {
     char_ptr1 = strstr(input.parms, "QUALPROD");
     strcpy(small_buf, char_ptr1 + strlen("QUALPROD = |"));
     strcpy(char_ptr1, "QUALPROD = NONE|");
     strcat(input.parms, small_buf);
   }

   if (input.qual_max == 255) {
     char_ptr1 = strstr(input.parms, "QUAL MAX");
     if (char_ptr1 != NULL) strcpy(char_ptr1, "QUAL MAX = N/A|");
   } else {
     char_ptr1 = strstr(input.parms, "QUAL MAX");
     strcpy(small_buf, char_ptr1 + strlen("QUAL_MAX = 255|"));
     sprintf(char_ptr1, "QUAL_MAX = %d|", input.qual_max);
     strcat(input.parms, small_buf);
   }

   char_ptr1 = strstr(input.parms, "ROWGROUP");
   strcpy(small_buf, char_ptr1 + strlen("ROWGROUP = -1"));
   sprintf(char_ptr1, "ROWGROUP = %d", input.rowgroup);
   strcat(input.parms, small_buf);

   status = SDsetattr(sd_id_w, "Input Parameters", DFNT_CHAR, strlen(input.parms)+1,
              input.parms);

   strcpy(buf, l2_str[0].filename);
   if (nfiles > 1) strcat(buf, ",");
   for (ifile=1; ifile<nfiles-1; ifile++) {
     strcat(buf, l2_str[ifile].filename);
     strcat(buf, ",");
   }
   if (nfiles > 1) strcat(buf, l2_str[nfiles-1].filename);
   status = SDsetattr(sd_id_w, "Input Files", DFNT_CHAR, strlen(buf)+1, buf);

   status = SDsetattr(sd_id_w, "L2 Flag Names", DFNT_CHAR, strlen(input.flaguse)+1,
              input.flaguse );

   i16 = input.sday/1000;
   status = SDsetattr(sd_id_w, "Period Start Year", DFNT_INT16, 1, (VOIDP) &i16);
   i16 = input.sday-1000*i16;
   status = SDsetattr(sd_id_w, "Period Start Day", DFNT_INT16, 1, (VOIDP) &i16);

   i16 = input.eday/1000;
   status = SDsetattr(sd_id_w, "Period End Year", DFNT_INT16, 1, (VOIDP) &i16);
   i16 = input.eday-1000*i16;
   status = SDsetattr(sd_id_w, "Period End Day", DFNT_INT16, 1, (VOIDP) &i16);

   quot1 = div(l2_str[0].smsec, 3600000);
   quot2 = div(quot1.rem, 60000);
   quot3 = div(quot2.rem, 1000);
   sprintf(buf, "%4.4d%3.3d%2.2d%2.2d%2.2d%3.3d",
       l2_str[0].syear, l2_str[0].sday,
             quot1.quot, quot2.quot, quot3.quot, quot3.rem);
   SDsetattr(sd_id_w, "Start Time", DFNT_CHAR, strlen(buf)+1, buf);

   quot1 = div(l2_str[nfiles-1].emsec, 3600000);
   quot2 = div(quot1.rem, 60000);
   quot3 = div(quot2.rem, 1000);
   sprintf(buf, "%4.4d%3.3d%2.2d%2.2d%2.2d%3.3d",
       l2_str[nfiles-1].eyear, l2_str[nfiles-1].eday,
             quot1.quot, quot2.quot, quot3.quot, quot3.rem);
   SDsetattr(sd_id_w, "End Time", DFNT_CHAR, strlen(buf)+1, buf);

   status = SDsetattr(sd_id_w, "Start Year", DFNT_INT16, 1, (VOIDP) &l2_str[0].syear);
   status = SDsetattr(sd_id_w, "Start Day", DFNT_INT16, 1, (VOIDP) &l2_str[0].sday);
   status = SDsetattr(sd_id_w, "Start Millisec", DFNT_INT32, 1, (VOIDP) &l2_str[0].smsec);

   status = SDsetattr(sd_id_w, "End Year", DFNT_INT16, 1, (VOIDP) &l2_str[nfiles-1].eyear);
   status = SDsetattr(sd_id_w, "End Day", DFNT_INT16, 1, (VOIDP) &l2_str[nfiles-1].eday);
   status = SDsetattr(sd_id_w, "End Millisec", DFNT_INT32, 1, (VOIDP) &l2_str[nfiles-1].emsec);

   strcpy(buf, "degrees North");
   status = SDsetattr(sd_id_w, "Latitude Units", DFNT_CHAR, strlen(buf)+1, buf);

   strcpy(buf, "degrees East");
   status = SDsetattr(sd_id_w, "Longitude Units", DFNT_CHAR, strlen(buf)+1, buf);

   SDsetattr(sd_id_w, "Northernmost Latitude", DFNT_FLOAT32, 1, &northmost);
   SDsetattr(sd_id_w, "Southernmost Latitude", DFNT_FLOAT32, 1, &southmost);
   SDsetattr(sd_id_w, "Easternmost Longitude", DFNT_FLOAT32, 1, &eastmost);
   SDsetattr(sd_id_w, "Westernmost Longitude", DFNT_FLOAT32, 1, &westmost);

   status = SDsetattr(sd_id_w, "Data Bins", DFNT_INT32, 1, &total_filled_bins);

   f32 = 100 * ((float32) total_filled_bins) /
     (basebin[nrows-1]+numbin[nrows-1]-1);
   status = SDsetattr(sd_id_w, "Percent Data Bins", DFNT_FLOAT32, 1, &f32);


   strcpy(buf, meta_l3b.station );
   //  status = SDsetattr(sd_id_w, "Station Name", DFNT_CHAR, strlen(buf)+1, buf);

   //  status = SDsetattr(sd_id_w, "Station Latitude", DFNT_FLOAT32, 1, &meta_l3b.station_lat);
   //status = SDsetattr(sd_id_w, "Station Longitude", DFNT_FLOAT32, 1, &meta_l3b.station_lon);


   buf[0] = 0;
   for (iprod=0; iprod < l3b_nprod; iprod++) {
     getL3units(&l2_str[0], 0, prodname[iprod], units);
     strcat(&buf[strlen(buf)], prodname[iprod]);
     strcat(&buf[strlen(buf)], ":");
     strcat(&buf[strlen(buf)], units);
     strcat(&buf[strlen(buf)], ",");
   }
   buf[strlen(buf)-1] = 0;
   status = SDsetattr(sd_id_w, "Units", DFNT_CHAR, strlen(buf)+1, buf);

   if ( strcmp(input.resolve, "36") == 0) strcpy( buf, "36 km");
   if ( strcmp(input.resolve,  "9") == 0) strcpy( buf, "9 km");
   if ( strcmp(input.resolve,  "4") == 0) strcpy( buf, "4 km");
   if ( strcmp(input.resolve,  "2") == 0) strcpy( buf, "2 km");
   if ( strcmp(input.resolve,  "1") == 0) strcpy( buf, "1 km");
   if ( strcmp(input.resolve,  "H") == 0) strcpy( buf, "500 m");
   if ( strcmp(input.resolve,  "Q") == 0) strcpy( buf, "250 m");
   SDsetattr(sd_id, "Bin Resolution", DFNT_CHAR, strlen(buf) + 1, buf);

   closeL2(&l2_str[0], 0);


 bail:

   /* Free Dynamic Memory */
   /* ------------------- */
   printf("Freeing Dynamic Memory\n");
   free(bin_indx);

   free(ext);
   free(beg);

   free(numbin);
   free(basebin);

   free(prodname);
   free(min_value);

   for (ifile=0; ifile<nfiles; ifile++) {
     if (bscan_row[ifile] != NULL) free(bscan_row[ifile]);
     if (escan_row[ifile] != NULL) free(escan_row[ifile]);

     if (numer[ifile] != NULL) free(numer[ifile]);
     if (denom[ifile] != NULL) free(denom[ifile]);
   }



   if (input.fileuse[0] != 0) {
     fp2 = fopen(input.fileuse, "w");
     for (ifile=0; ifile<nfiles; ifile++) {
       if (brk_scan[ifile] != -9999) {
     fprintf(fp2,"%s\n", l2_str[ifile].filename);
       }
     }
     fclose(fp2);
   }


   /* Free allocated L2 memory */
   /* ------------------------ */
   printf("Freeing L2 arrays\n");
   for (ifile=0; ifile<nfiles; ifile++) {
     freeL2(&l2_str[ifile]);
   }
   freeL2(NULL);


   /* Close L3B output file */
   /* --------------------- */
   printf("Detaching L3B Vgroup\n");
   Vdetach(vgid_w);
   printf("End L3B Vgroup interface\n");
   Vend(fileid_w);
   printf("End L3B SD interface\n");
   SDend(sd_id_w);
   printf("Close L3B file\n");
   Hclose(fileid_w);

   return ret_status;
 }


 int32 compute_scanfrac(int32 ifile, int32 ipixl,
                uint32 flagusemask, uint32 required) {

   int32 i,j;
   int32 nsamp;
   float32 f32;
   float32 total_distance;
   float32 interp_distance;
   float32 *distance;
   float32 dtheta, dphi;
   uint32 flagcheck;

   /* Determine distances between pixels if interpolating */
   /* --------------------------------------------------- */
   if (input.interp == 1) {

     if (strcmp(input.resolve, "36") == 0) interp_distance =  36 * 0.75;
     if (strcmp(input.resolve, "9") == 0)  interp_distance =   9 * 0.75;
     if (strcmp(input.resolve, "4") == 0)  interp_distance =   4 * 0.75;
     if (strcmp(input.resolve, "2") == 0)  interp_distance =   2 * 0.75;
     if (strcmp(input.resolve, "1") == 0)  interp_distance =   1 * 0.75;
     if (strcmp(input.resolve, "1") == 0)  interp_distance = 0.5 * 0.75;

     distance = (float32 *) calloc(l2_str[ifile].nsamp, sizeof(float32));
     total_distance = 0.0;
     for (ipixl=1; ipixl<l2_str[ifile].nsamp; ipixl++) {


       /* Don't process flagged pixels */
       /* ---------------------------- */
       flagcheck = (l2_str[ifile].l2_flags[ipixl] |
            l2_str[ifile].l2_flags[ipixl-1]);
       if ((flagcheck & flagusemask) != 0)
     continue;
       if ((flagcheck & required) != required)
     continue;


       /* Compute delta theta & phi */
       /* ------------------------- */
       dtheta = l2_str[ifile].latitude[ipixl] -
     l2_str[ifile].latitude[ipixl-1];
       dphi = l2_str[ifile].longitude[ipixl] -
     l2_str[ifile].longitude[ipixl-1];


       /* Correct for dateline crossing */
       /* ----------------------------- */
       if (dphi < -90) dphi += 360;
       if (dphi > +90) dphi -= 360;

       f32 = dphi * cos((PI/180) * l2_str[ifile].latitude[ipixl]);
       distance[ipixl] = f32 * f32;

       f32 = dtheta * dtheta;
       distance[ipixl] += f32;
       distance[ipixl] = (PI/180) * sqrt(distance[ipixl]) * EARTH_RADIUS;

       total_distance += distance[ipixl];
     } /* ipixl loop */

     /* # of samples = total scan distance / width of "middle" pixel */
     nsamp = rint((float64) (total_distance/interp_distance));

     f32 = total_distance / nsamp;

     distance[0] = 0.0;
     for (ipixl=1; ipixl<l2_str[ifile].nsamp; ipixl++) {
       distance[ipixl] = distance[ipixl-1] + distance[ipixl];
     }

   } else {
     /* No interp */
     /* --------- */
     nsamp = l2_str[ifile].nsamp;
   } /*  if (input.interp == 1) ... */



   /* Compute scan_frac */
   /* ----------------- */
   scan_frac = (float32 *) calloc(nsamp, sizeof(float32));

   if (input.interp == 1) {
     ipixl = 0;
     for (i=0; i<nsamp; i++) {
       for (j=ipixl; j<l2_str[ifile].nsamp; j++) {
     if ((i+0.5)*f32 >= distance[j] && (i+0.5)*f32 < distance[j+1]) {
       ipixl = j;
       /*printf("%f %f %f\n", distance[j], i*f32, distance[j+1]);*/
       break;
     }
       }
       scan_frac[i] = j +
     ((i+0.5)*f32 - distance[j]) / (distance[j+1] - distance[j]);
     }
   } else {
     /* No interp */
     /* --------- */
     for (i=0; i<nsamp; i++) scan_frac[i] = (float32) i;
   }

   if (input.interp == 1) free(distance);

   return (nsamp);
 }



 int32 getbinnum(int32 ifile, int32 ipixl, int32 isamp) {

   int32 bin_row;
   int32 bin_col;
   float32 f32;
   float32 scan_lon;
   float32 scan_lat;


   if (input.interp == 1) {
     scan_lat = l2_str[ifile].latitude[ipixl] +
       (scan_frac[isamp] - ipixl) *
       (l2_str[ifile].latitude[ipixl+1] - l2_str[ifile].latitude[ipixl]);

     /* Handle dateline crossing for longitude */
     /* -------------------------------------- */
     f32 = l2_str[ifile].longitude[ipixl+1] - l2_str[ifile].longitude[ipixl];
     if (fabs(f32) < 90) {
       scan_lon = l2_str[ifile].longitude[ipixl] +
     (scan_frac[isamp] - ipixl) * f32;
     } else if (f32 < 0) {
       scan_lon = l2_str[ifile].longitude[ipixl] + (scan_frac[isamp] - ipixl) *
     (360.0 + f32);
       if (scan_lon > +180.0) scan_lon -= 360.0;
     } else if (f32 > 0) {
       scan_lon = l2_str[ifile].longitude[ipixl] + (scan_frac[isamp] - ipixl) *
     (f32 - 360.0);
       if (scan_lon < -180.0) scan_lon += 360.0;
     }

   } else {
     /* No interp */
     /* --------- */
     scan_lat = l2_str[ifile].latitude[ipixl];
     scan_lon = l2_str[ifile].longitude[ipixl];
   }


   bin_row = (int32_t) ((90.0 + scan_lat) *
                ((float64) nrows / (float64) 180.0));

   if (bin_row < 0 || bin_row >= nrows) {
     //    printf("bin_row: %d out of bounds. (%f)\n", bin_row, scan_lat);
     return -1;
   }

   bin_col = (int32_t) ((float64) numbin[bin_row] *
                (scan_lon + 180.0) / (float64) 360.0);

   return (basebin[bin_row] + bin_col);

 }


 #ifdef GSL
 int32 midaverage(int32 n_bins_in_group, int32 l3b_nprod, int32 krow) {

   int32 i,j;
   int32 ibin, bin;
   int32 iprod;
   int32 iprod_avg=-1;
   float32 f32;
   float64 *dblarr, upperq, lowerq;

   /* Check prod_avg is in L2 files */
   /* ----------------------------- */
   for (iprod=0; iprod<l2_str[0].nprod; iprod++) {
     if (strcmp(prod_avg, l2_str[0].prodname[iprod]) == 0) {
       iprod_avg = iprod;
       break;
     }
   }

   if (iprod_avg == l2_str[0].nprod) {
     printf("\"%s\" must be present in L2 file to perform midaverage\n",
        prod_avg);
     exit(-1);
   }

   /* Loop through all bins */
   /* --------------------- */
   for (ibin=0; ibin<n_bins_in_group; ibin++) {

     if (nobs[ibin] == 0) continue;

     dblarr = (float64 *) calloc(nobs[ibin],sizeof(float64));

     for (j=0; j<nobs[ibin]; j++) {
       f32 = data_values[ibin][j*l3b_nprod+iprod_avg];
       dblarr[j] = (float64) f32;
     } /* j loop */

     gsl_sort(dblarr, 1, nobs[ibin]);
     if (nobs[ibin] >= 3) {
       upperq = gsl_stats_quantile_from_sorted_data(dblarr, 1,
                            nobs[ibin], 0.75);
       lowerq = gsl_stats_quantile_from_sorted_data(dblarr, 1,
                            nobs[ibin], 0.25);
     } else if (nobs[ibin] == 2) {
       upperq = dblarr[1] + 1;
       lowerq = dblarr[0] - 1;
     } else if (nobs[ibin] == 1) {
       upperq = dblarr[0] + 1;
       lowerq = dblarr[0] - 1;
     }

     /* Display data values if BINCHECK */
     if (BINCHECK >= 0) {
       bin = ibin + basebin[krow];
       if (bin == BINCHECK) {
     for (j=0; j<nobs[ibin]; j++)
       printf("value before (midaverage): %10d %3d %4d %10.4f\n",
          bin, nobs[ibin], file_index[ibin][j],
          data_values[ibin][j*l3b_nprod+iprod_avg]);
     printf("lowerq (midaverage): %f\n", lowerq);
     printf("upperq (midaverage): %f\n", upperq);
       }
     }

     /* Regenerate unsorted chlor array */
     for (j=0; j<nobs[ibin]; j++) {
       f32 = data_values[ibin][j*l3b_nprod+iprod_avg];
       dblarr[j] = (float64) f32;
     }

     i = 0;
     for (j=0; j<nobs[ibin]; j++) {

       if (dblarr[j] >= lowerq && dblarr[j] <= upperq) {
     i++;

     /* Display data values if BINCHECK */
     if (BINCHECK >= 0) {
       if (bin == BINCHECK)
         printf("dblarr (midaverage): %d %10.4f\n", j, dblarr[j]);
     }

     for (iprod=0; iprod < l3b_nprod; iprod++) {
       data_values[ibin][i*l3b_nprod+iprod] =
         data_values[ibin][j*l3b_nprod+iprod];
     } /* iprod loop */

     file_index[ibin][i] = file_index[ibin][j];

       } /* if within midaverage */
     } /* j loop */
     /*  printf("org nobs: %d   new nobs: %d\n", nobs[ibin], i);*/

     nobs[ibin] = i;
     free(dblarr);

   } /* ibin loop */

   return 0;
 }


 int32 median(int32 n_bins_in_group, int32 l3b_nprod, int32 krow) {

   int32 i,j;
   int32 ibin, bin;
   int32 iprod;
   int32 iprod_avg=-1;
   float32 f32;
   float64 *dblarr;

   /* Check prod_avg is in L2 files */
   /* ----------------------------- */
   for (iprod=0; iprod<l2_str[0].nprod; iprod++) {
     if (strcmp(prod_avg, l2_str[0].prodname[iprod]) == 0) {
       iprod_avg = iprod;
       break;
     }
   }

   if (iprod_avg == l2_str[0].nprod) {
     printf("\"%s\" must be present in L2 file to perform midaverage\n",
        prod_avg);
     exit(-1);
   }

   /* Loop through all bins */
   /* --------------------- */
   for (ibin=0; ibin<n_bins_in_group; ibin++) {

     if (nobs[ibin] == 0) continue;

     dblarr = (float64 *) calloc(nobs[ibin],sizeof(float64));

     for (j=0; j<nobs[ibin]; j++) {
       f32 = data_values[ibin][j*l3b_nprod+iprod_avg];
       dblarr[j] = (float64) f32;
     } /* j loop */


       /* Get median chlor value */
       /* ---------------------- */
     gsl_sort(dblarr, 1, nobs[ibin]);
     f32 = (float32) gsl_stats_median_from_sorted_data(dblarr, 1, nobs[ibin]);

     /* Display data values if BINCHECK */
     if (BINCHECK >= 0) {
       bin = ibin + basebin[krow];
       if (bin == BINCHECK) {
     for (j=0; j<nobs[ibin]; j++)
       printf("value (median): %10d %3d %4d %10.4f\n",
          bin, nobs[ibin], file_index[ibin][j],
          data_values[ibin][j*l3b_nprod+iprod_avg]);
     printf("median: %f\n", f32);
       }
     }


     /* Compute difference between chlor data and median */
     /* ------------------------------------------------ */
     for (j=0; j<nobs[ibin]; j++) {
       dblarr[j] = data_values[ibin][j*l3b_nprod+iprod_avg] - f32;
     }

     /* Find element closest to median */
     /* ------------------------------ */
     f32 = 1e60;
     i = 0;
     for (j=0; j<nobs[ibin]; j++) {
       if (fabs((float32) dblarr[j]) < f32) {
     if (bin == BINCHECK)  printf("median: %d %d %f %f\n", i,j,
                      fabs((float32) dblarr[j]),f32);

     f32 = fabs((float32) dblarr[j]);
     i = j;
       }
     }

     if (BINCHECK >= 0) {
       if (bin == BINCHECK) {
     printf("median #: %d\n", i);
       }
     }

     /* Write this element for all products to 1st data_values array element */
     /* -------------------------------------------------------------------- */
     for (iprod=0; iprod < l3b_nprod; iprod++) {
       data_values[ibin][0*l3b_nprod+iprod] =
     data_values[ibin][i*l3b_nprod+iprod];
     } /* iprod loop */

     file_index[ibin][0] = file_index[ibin][i];
     nobs[ibin] = 1;

     free(dblarr);

   } /* ibin loop */

   return 0;
 }
 #endif


 int32 isleap(int32 year)
 {
 if ( ((year % 400) == 0) || (((year % 4) == 0) && ((year % 100) != 0)) )
     return TRUE;
 else
     return FALSE;
 }


 int32 diffday(int32 date1, int32 date2)
 {

   /* date1 - date 2*/
   int i;
   int32 year1, year2;
   int32 day1, day2;

   year1 = date1 / 1000;
   year2 = date2 / 1000;
   day1 = date1 % 1000;
   day2 = date2 % 1000;

   for (i=year2; i<year1; i++) {
     if (isleap(i) == TRUE) day1 += 366; else day1 += 365;
   }

   for (i=year1; i<year2; i++) {
     if (isleap(i) == TRUE) day2 += 366; else day2 += 365;
   }

   return day1 - day2;

 }

