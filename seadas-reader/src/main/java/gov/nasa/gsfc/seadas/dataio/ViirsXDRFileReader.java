package gov.nasa.gsfc.seadas.dataio;

import org.esa.beam.dataio.netcdf.util.NetcdfFileOpener;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.*;
import ucar.ma2.Array;
import ucar.nc2.*;
import ucar.nc2.Dimension;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

public class ViirsXDRFileReader extends SeadasFileReader {

    ViirsXDRFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws ProductIOException {

        try {
            List<Dimension> dims;
            String CollectionShortName = getCollectionShortName();

            if (productReader.getProductType() == SeadasProductReader.ProductType.VIIRS_EDR) {
                String groupName = "All_Data/" + CollectionShortName + "_All";
                Group edrGroup = ncFile.findGroup(groupName);
                dims = edrGroup.getVariables().get(0).getDimensions();
            } else if (productReader.getProductType() == SeadasProductReader.ProductType.VIIRS_SDR) {
                String varName = "All_Data/" + CollectionShortName + "_All/Radiance";
                Variable exampleRadiance = ncFile.findVariable(varName);
                dims = exampleRadiance.getDimensions();
            } else if (productReader.getProductType() == SeadasProductReader.ProductType.VIIRS_GEO) {
                String varName = "All_Data/" + CollectionShortName + "_All/Height";
                Variable exampleRadiance = ncFile.findVariable(varName);
                dims = exampleRadiance.getDimensions();
            } else if (productReader.getProductType() == SeadasProductReader.ProductType.VIIRS_IP) {
                if (CollectionShortName.equals("VIIRS-DualGain-Cal-IP")) {
                    String varName = "All_Data/" + CollectionShortName + "_All/radiance_0";
                    Variable exampleRadiance = ncFile.findVariable(varName);
                    dims = exampleRadiance.getDimensions();
                }
//                todo: One day, maybe, add support for the OBC files.
                else {
                    String message = "Unsupported VIIRS Product: " + CollectionShortName;
                    throw new ProductIOException(message);
                }
            } else {
                String message = "Unsupported VIIRS Product: " + CollectionShortName;
                throw new ProductIOException(message);
            }


            int sceneHeight = dims.get(0).getLength();
            int sceneWidth = dims.get(1).getLength();

            String productName = productReader.getInputFile().getName();

            mustFlipX = mustFlipY = mustFlipVIIRS();
            SeadasProductReader.ProductType productType = productReader.getProductType();

            Product product = new Product(productName, productType.toString(), sceneWidth, sceneHeight);
            product.setDescription(productName);

            setStartEndTime(product);

            product.setFileLocation(productReader.getInputFile());
            product.setProductReader(productReader);

            addGlobalAttributeVIIRS();
            addGlobalMetadata(product);

            variableMap = addBands(product, ncFile.getVariables());

            addGeocoding(product);

            product.setAutoGrouping("IOP:QF:nLw:Radiance:radiance:Reflectance");
            addFlagsAndMasks(product);

            setSpectralBand(product);
            return product;
        } catch (Exception e) {
            throw new ProductIOException(e.getMessage());
        }
    }

    @Override
    protected void setSpectralBand(Product product) {
        //todo Add units
        int spectralBandIndex = 0;
        for (String name : product.getBandNames()) {
            Band band = product.getBandAt(product.getBandIndex(name));
            if (name.matches(".*\\w+_\\d+.*")) {
                String wvlstr = null;
                if (name.matches("IOP.*_\\d+.*")) {
                    wvlstr = name.split("_")[2].split("nm")[0];
                } else if (name.matches("nLw_\\d+nm")) {
                    wvlstr = name.split("_")[1].split("nm")[0];
                }
                if (wvlstr != null){
                    final float wavelength = Float.parseFloat(wvlstr);
                    band.setSpectralWavelength(wavelength);
                    band.setSpectralBandIndex(spectralBandIndex++);
                }
            }
        }
    }

    @Override
    protected Band addNewBand(Product product, Variable variable) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        Band band = null;
        String[] factors = {"Radiance", "Reflectance", "BulkSST", "SkinSST"};
        int variableRank = variable.getRank();
        if (variableRank == 2) {
            final int[] dimensions = variable.getShape();
            final int height = dimensions[0];
            final int width = dimensions[1];
            if (height == sceneRasterHeight && width == sceneRasterWidth) {
                final String name = variable.getShortName();
                final int dataType = getProductDataType(variable);
                band = new Band(name, dataType, width, height);

                product.addBand(band);
                try {
                    String varname = variable.getShortName();

                    for (String v : factors) {
                        if (v.equals(varname)) {
                            String facvar = v + "Factors";
                            Group group = ncFile.getRootGroup().findGroup("All_Data").getGroups().get(0);
                            Variable factor = group.findVariable(facvar);
                            if (factor != null)     {
                                Array slpoff = factor.read();
                                float slope = slpoff.getFloat(0);

                                float intercept = slpoff.getFloat(1);

                                band.setScalingFactor((double) slope);
                                band.setScalingOffset((double) intercept);
                            }
                        }
                    }
                    //todo Add valid expression - _FillValue is not working properly - viirs uses more than one...ugh.
                    if (varname.equals("Chlorophyll_a")) {
                        band.setValidPixelExpression("Chlorophyll_a > 0.0 && Chlorophyll_a < 100.0");
                    }

                    band.setNoDataValue((double) variable.findAttribute("_FillValue").getNumericValue().floatValue());
                } catch (Exception ignored) {

                }
            }
        }
        return band;
    }

    public void addGeocoding(final Product product) throws ProductIOException {
        //todo: refine logic to get correct navGroup
        File inputFile = productReader.getInputFile();
        NetcdfFile geofile = null;
        String navGroup = "All_Data/VIIRS-MOD-GEO-TC_All";
        String geoFileName = null;
        int strlen = inputFile.getName().length();
        int detectorsInScan;
        Group geocollection = null;
        Group collection = ncFile.getRootGroup().findGroup("Data_Products").getGroups().get(0);
        String shortName = getCollectionShortName();

        String dsType = collection.findAttribute("N_Dataset_Type_Tag").getStringValue();

        if (!dsType.equals("GEO")){
            Attribute geoRef = findAttribute("N_GEO_Ref");
            if (geoRef != null) {
                geoFileName = geoRef.getStringValue().trim();
            } else {
                String platform =  findAttribute("Platform_Short_Name").getStringValue().toLowerCase();
                String procdomain = collection.findAttribute("N_Processing_Domain").getStringValue().toLowerCase();
                String datasource = findAttribute("N_Dataset_Source").getStringValue().toLowerCase();
                long orbitnum = 0;
                String startDate = null;
                String startTime = null;
                String endTime = null;
                String createDate = findAttribute("N_HDF_Creation_Date").getStringValue();
                String createTime = findAttribute("N_HDF_Creation_Time").getStringValue();
                List<Variable> dataProductList = collection.getVariables();
                for (Variable var : dataProductList) {
                    if (var.getShortName().contains("_Aggr")) {
                        orbitnum = var.findAttribute("AggregateBeginningOrbitNumber").getNumericValue().longValue();
                        startDate = var.findAttribute("AggregateBeginningDate").getStringValue().trim();
                        startTime = var.findAttribute("AggregateBeginningTime").getStringValue().trim().substring(0, 8);
                        endTime = var.findAttribute("AggregateEndingTime").getStringValue().trim().substring(0, 8);
                    }
                }
                StringBuilder geoFile = new StringBuilder();

                if (shortName.contains("DNB")){
                    geoFile.append("GDNBO");
                } else if (shortName.contains("VIIRS-I")){
                    geoFile.append("GITCO");
                } else if (dsType.equals("EDR") || shortName.contains("VIIRS-M")){
                    geoFile.append("GMTCO");
                }
                else if (shortName.contains("VIIRS-DualGain")){
                    geoFile.append("ICDBG");
                }
                geoFile.append('_');
                geoFile.append(platform);
                geoFile.append("_d");
                geoFile.append(startDate);
                geoFile.append("_t");
                geoFile.append(startTime);
                geoFile.deleteCharAt(geoFile.toString().length()-2);
                geoFile.append("_e");
                geoFile.append(endTime);
                geoFile.deleteCharAt(geoFile.toString().length()-2);
                geoFile.append("_b");
                geoFile.append(String.format("%05d",orbitnum));
                geoFile.append("_c");
                geoFile.append(createDate).append(createTime);
                geoFile.deleteCharAt(geoFile.toString().length()-1);
                geoFile.deleteCharAt(geoFile.toString().length()-7);
                geoFile.append("_");
                geoFile.append(datasource);
                geoFile.append("_");
                geoFile.append(procdomain);
                geoFile.append(".h5");
                geoFileName =  geoFile.toString();
            }

            try {

                String path = inputFile.getParent();
                File geocheck = new File(path, geoFileName);
                // remove the create time segment and try again
                if (!geocheck.exists() || geoFileName == null) {
                    File geodir = new File(path);
                    final String geoFileName_filter = inputFile.getName().substring(5, strlen).split("_c\\d{20}_")[0];

                    FilenameFilter filter = new FilenameFilter(){
                        public boolean accept
                        (File dir, String name) {
                            return name.contains(geoFileName_filter);
                        }
                    };
                    String[] geofilelist = geodir.list(filter);

                    for (String gf:geofilelist){
                        if (!gf.startsWith("ICDBG")){
                            if (!gf.startsWith("G")){
                                continue;
                            }
                        }
                        if (shortName.contains("DNB") && gf.startsWith("GDNBO")){
                            geocheck = new File(path,  gf);
                            break;
                        } else if (shortName.contains("VIIRS-I")){
                            if ( gf.startsWith("GITCO")){
                                geocheck = new File(path,  gf);
                                break;
                            } else if ( gf.startsWith("GIMGO")){
                                geocheck = new File(path,  gf);
                                // prefer the GITCO, so keep looking just in case;
                            }

                        } else if (shortName.contains("VIIRS-DualGain")){
                            if ( gf.startsWith("ICDBG")){
                                geocheck = new File(path,  gf);
                                break;
                            }
                        } else if (dsType.equals("EDR") || shortName.contains("VIIRS-M")){
                            if ( gf.startsWith("GMTCO")){
                                geocheck = new File(path,  gf);
                                break;
                            } else if ( gf.startsWith("GMODO")){
                                geocheck = new File(path,  gf);
                                //prefer the GMTCO, so keep looking just in case;
                            }

                        }
                    }
                    if (!geocheck.exists()){
                        return;
                    }
                }
                geofile = NetcdfFileOpener.open(geocheck.getPath());
                List<Group> navGroups = geofile.findGroup("All_Data").getGroups();
                for (Group ng : navGroups) {
                    if (ng.getShortName().contains("GEO")){
                        navGroup = ng.getFullName();
                        break;
                    }
                }
            } catch (Exception e) {
                throw new ProductIOException(e.getMessage());
            }
        }
        try {

            if (dsType.equals("GEO")){
                geocollection = ncFile.getRootGroup().findGroup("All_Data").getGroups().get(0);
            } else {
                if (geofile != null) {
                    geocollection = geofile.getRootGroup().findGroup("All_Data").getGroups().get(0);
                }
            }
            Variable nscans = null;
            if (geocollection != null){
                nscans = geocollection.findVariable("NumberOfScans");
                if (nscans == null){
                    nscans = geocollection.findVariable("act_scans");
                }
            }
            Array ns = nscans.read();
            detectorsInScan = product.getSceneRasterHeight() / ns.getInt(0);
        } catch (IOException e) {
            throw new ProductIOException("Could not find the number of detectors in a scan");
        }
        try{
            final String longitude = "Longitude";
            final String latitude = "Latitude";

            if (!dsType.equals("GEO")){
                Band latBand = new Band("latitude", ProductData.TYPE_FLOAT32, product.getSceneRasterWidth(), product.getSceneRasterHeight());
                Band lonBand = new Band("longitude", ProductData.TYPE_FLOAT32, product.getSceneRasterWidth(), product.getSceneRasterHeight());
                product.addBand(latBand);
                product.addBand(lonBand);


                Array latarr = geofile.findVariable(navGroup + "/" + latitude).read();
                Array lonarr = geofile.findVariable(navGroup + "/" + longitude).read();

                float[] latitudes;
                float[] longitudes;
                if (mustFlipX && mustFlipY) {
                    latitudes = (float[]) latarr.flip(0).flip(1).copyTo1DJavaArray();
                    longitudes = (float[]) lonarr.flip(0).flip(1).copyTo1DJavaArray();
                } else {
                    latitudes = (float[]) latarr.getStorage();
                    longitudes = (float[]) lonarr.getStorage();
                }

                ProductData lats = ProductData.createInstance(latitudes);
                latBand.setData(lats);
                ProductData lons = ProductData.createInstance(longitudes);
                lonBand.setData(lons);

                //product.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 5, ProgressMonitor.NULL));
                product.setGeoCoding(new BowtiePixelGeoCoding(latBand, lonBand, detectorsInScan, 0));
            } else {
                product.setGeoCoding(new BowtiePixelGeoCoding(product.getBand(latitude), product.getBand(longitude), detectorsInScan, 0));
            }
        }catch (Exception e) {
            throw new ProductIOException(e.getMessage());
        }

    }

    public boolean mustFlipVIIRS() throws ProductIOException {
        List<Variable> vars = ncFile.getVariables();
        for (Variable var : vars) {
            if (var.getShortName().contains("_Gran_")) {
                List<Attribute> attrs = var.getAttributes();
                for (Attribute attr : attrs) {
                    if (attr.getShortName().equals("Ascending_Descending_Indicator")) {
                        return attr.getNumericValue().longValue() == 0;
                    }
                }
            }
        }
        throw new ProductIOException("Cannot find Ascending/Decending_Indicator");
    }


    private void setStartEndTime(Product product) throws ProductIOException {
        List<Variable> dataProductList = ncFile.getRootGroup().findGroup("Data_Products").getGroups().get(0).getVariables();
        for (Variable var : dataProductList) {
            if (var.getShortName().contains("DR_Aggr")) {
                String startDate = var.findAttribute("AggregateBeginningDate").getStringValue().trim();
                String startTime = var.findAttribute("AggregateBeginningTime").getStringValue().trim();
                String endDate = var.findAttribute("AggregateEndingDate").getStringValue().trim();
                String endTime = var.findAttribute("AggregateEndingTime").getStringValue().trim();

                final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyyMMddHHmmss");
                try {
                    String startTimeString = startDate + startTime.substring(0, 6);
                    String endTimeString = endDate + endTime.substring(0, 6);
                    final Date startdate = dateFormat.parse(startTimeString);
                    String startmicroSeconds = startTime.substring(startTimeString.length() - 7, startTimeString.length() - 1);

                    final Date enddate = dateFormat.parse(endTimeString);
                    String endmicroSeconds = endTime.substring(endTimeString.length() - 7, startTimeString.length() - 1);

                    if (mustFlipY) {
                        product.setStartTime(ProductData.UTC.create(enddate, Long.parseLong(endmicroSeconds)));
                        product.setEndTime(ProductData.UTC.create(startdate, Long.parseLong(startmicroSeconds)));
                    } else {
                        product.setStartTime(ProductData.UTC.create(startdate, Long.parseLong(startmicroSeconds)));
                        product.setEndTime(ProductData.UTC.create(enddate, Long.parseLong(endmicroSeconds)));
                    }

                } catch (ParseException e) {
                    throw new ProductIOException("Unable to parse start/end time attributes");
                }

            }
        }
    }

    private String getCollectionShortName() throws ProductIOException {
        List<Attribute> gattr = ncFile.getGlobalAttributes();
        for (Attribute attr : gattr) {
            if (attr.getShortName().endsWith("Collection_Short_Name")) {
                return attr.getStringValue();
            }
        }
        throw new ProductIOException("Cannot find collection short name");
    }

    public void addGlobalAttributeVIIRS() {
        List<Group> DataProductGroups = ncFile.getRootGroup().findGroup("Data_Products").getGroups();

        for (Group dpgroup : DataProductGroups) {
            String groupname = dpgroup.getShortName();
//            if (groupname.matches("VIIRS-.*DR$")) {
            if (groupname.matches("VIIRS-")) {
                List<Variable> vars = dpgroup.getVariables();
                for (Variable var : vars) {
                    String varname = var.getShortName();
                    if (varname.matches(".*_(Aggr|Gran_0)$")) {
                        List<Attribute> attrs = var.getAttributes();
                        for (Attribute attr : attrs) {
                            globalAttributes.add(attr);

                        }
                    }

                }
            }

        }
    }

    @Override
    protected void addFlagsAndMasks(Product product) {
        Band QFBand = product.getBand("QF1_VIIRSOCCEDR");
        if (QFBand != null) {
            FlagCoding flagCoding = new FlagCoding("QF1");
            flagCoding.addFlag("412Qual", 0x01, "412nm OC quality");
            flagCoding.addFlag("445Qual", 0x02, "445nm OC quality");
            flagCoding.addFlag("488Qual", 0x04, "488nm OC quality");
            flagCoding.addFlag("555Qual", 0x08, "555nm OC quality");
            flagCoding.addFlag("672Qual", 0x10, "672nm OC quality");
            flagCoding.addFlag("ChlQual", 0x20, "Chlorophyll a quality");
            flagCoding.addFlag("IOP412aQual", 0x40, "IOP (a) 412nm quality");
            flagCoding.addFlag("IOP412sQual", 0x80, "IOP (s) 412nm quality");

            product.getFlagCodingGroup().add(flagCoding);
            QFBand.setSampleCoding(flagCoding);


            product.getMaskGroup().add(Mask.BandMathsType.create("412Qual", "Quality flag (poor): nLw at 412nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSOCCEDR.412Qual ",
                    Color.YELLOW, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("445Qual", "Quality flag (poor): nLw at 445nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSOCCEDR.445Qual ",
                    Color.CYAN, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("488Qual", "Quality flag (poor): nLw at 488nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSOCCEDR.488Qual ",
                    Color.LIGHT_GRAY, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("555Qual", "Quality flag (poor): nLw at 555nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSOCCEDR.555Qual ",
                    Color.MAGENTA, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("672Qual", "Quality flag (poor): nLw at 672nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSOCCEDR.672Qual ",
                    Color.BLUE, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("ChlQual", "Quality flag (poor): Chlorophyll a",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSOCCEDR.ChlQual ",
                    Color.GREEN, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("IOP412aQual", "Quality flag (poor): IOP (absorption) at 412nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSOCCEDR.IOP412aQual ",
                    Color.ORANGE, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("IOP412sQual", "Quality flag (poor): IOP (absorption) at 412nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSOCCEDR.IOP412sQual ",
                    Color.PINK, 0.2));

        }
        QFBand = product.getBand("QF2_VIIRSOCCEDR");
        if (QFBand != null) {
            FlagCoding flagCoding = new FlagCoding("QF2");
            flagCoding.addFlag("IOP445aQual", 0x01, "IOP (a) 445nm quality");
            flagCoding.addFlag("IOP445sQual", 0x02, "IOP (s) 445nm quality");
            flagCoding.addFlag("IOP488aQual", 0x04, "IOP (a) 488nm quality");
            flagCoding.addFlag("IOP488sQual", 0x08, "IOP (s) 488nm quality");
            flagCoding.addFlag("IOP555aQual", 0x10, "IOP (a) 555nm quality");
            flagCoding.addFlag("IOP555sQual", 0x20, "IOP (s) 555nm quality");
            flagCoding.addFlag("IOP672aQual", 0x40, "IOP (a) 672nm quality");
            flagCoding.addFlag("IOP672sQual", 0x80, "IOP (s) 672nm quality");
            product.getFlagCodingGroup().add(flagCoding);
            QFBand.setSampleCoding(flagCoding);


            product.getMaskGroup().add(Mask.BandMathsType.create("IOP445aQual", "Quality flag (poor): IOP (absorption) at 445nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF2_VIIRSOCCEDR.IOP445aQual ",
                    Color.YELLOW, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("IOP445sQual", "Quality flag (poor): IOP (scattering) at 445nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF2_VIIRSOCCEDR.IOP445sQual ",
                    Color.CYAN, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("IOP488aQual", "Quality flag (poor): IOP (absorption) at 488nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF2_VIIRSOCCEDR.IOP488aQual ",
                    Color.LIGHT_GRAY, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("IOP488sQual", "Quality flag (poor): IOP (scattering) at 488nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF2_VIIRSOCCEDR.IOP488sQual ",
                    Color.MAGENTA, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("IOP555aQual", "Quality flag (poor): IOP (absorption) at 555nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF2_VIIRSOCCEDR.IOP555aQual ",
                    Color.BLUE, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("IOP555sQual", "Quality flag (poor): IOP (scattering) at 555nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF2_VIIRSOCCEDR.IOP555sQual ",
                    Color.GREEN, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("IOP672aQual", "Quality flag (poor): IOP (absorption) at 672nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF2_VIIRSOCCEDR.IOP672aQual ",
                    Color.ORANGE, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("IOP672sQual", "Quality flag (poor): IOP (scattering) at 672nm",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF2_VIIRSOCCEDR.IOP672sQual ",
                    Color.PINK, 0.2));
        }
        QFBand = product.getBand("QF3_VIIRSOCCEDR");
        if (QFBand != null) {
            FlagCoding flagCoding = new FlagCoding("QF3");
            flagCoding.addFlag("SDRQual", 0x01, "Input radiance quality");
            flagCoding.addFlag("O3Qual", 0x02, "Input total Ozone Column quality");
            flagCoding.addFlag("WindSpeed", 0x04, "Wind speed > 8m/s (possible whitecap formation)");
            flagCoding.addFlag("AtmWarn", 0x08, "Epsilon value out-of-range for aerosol models (0.85 > eps > 1.35)");

            product.getFlagCodingGroup().add(flagCoding);
            QFBand.setSampleCoding(flagCoding);


            product.getMaskGroup().add(Mask.BandMathsType.create("SDRQual", "Input radiance quality (poor)",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF3_VIIRSOCCEDR.SDRQual",
                    Color.YELLOW, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("O3Qual", "Input Ozone quality (poor)",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF3_VIIRSOCCEDR.O3Qual",
                    Color.CYAN, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("WindSpeed", "Wind speed > 8m/s",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF3_VIIRSOCCEDR.WindSpeed",
                    Color.LIGHT_GRAY, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("AtmWarn", "Atmospheric correction warning",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF3_VIIRSOCCEDR.AtmWarn",
                    Color.MAGENTA, 0.25));
            product.getMaskGroup().add(Mask.BandMathsType.create("AtmFail_O3", "Atmospheric correction failure - Ozone correction",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF3_VIIRSOCCEDR & 0x70 ==  0x10",
                    SeadasFileReader.FailRed, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("AtmFail_WC", "Atmospheric correction failure - Whitecap correction",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF3_VIIRSOCCEDR & 0x70 ==  0x20",
                    SeadasFileReader.FailRed, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("AtmFail_pol", "Atmospheric correction failure - Polarization correction",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF3_VIIRSOCCEDR & 0x70 ==  0x30",
                    SeadasFileReader.FailRed, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("AtmFail_rayleigh", "Atmospheric correction failure - Rayliegh correction",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF3_VIIRSOCCEDR & 0x70 ==  0x40",
                    SeadasFileReader.FailRed, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("AtmFail_aerosol", "Atmospheric correction failure - Aerosol correction",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF3_VIIRSOCCEDR & 0x70 ==  0x50",
                    SeadasFileReader.FailRed, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("AtmFail_difftran", "Atmospheric correction failure - Diffuse transmission zero",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF3_VIIRSOCCEDR. & 0x70 ==  0x60",
                    SeadasFileReader.FailRed, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("AtmFail_NO", "Atmospheric correction failure - no correction possible",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF3_VIIRSOCCEDR & 0x70 ==  0x70",
                    SeadasFileReader.FailRed, 0.0));
        }

        QFBand = product.getBand("QF4_VIIRSOCCEDR");
        if (QFBand != null) {
            FlagCoding flagCoding = new FlagCoding("QF4");

            flagCoding.addFlag("Ice_Snow", 0x04, "Snow or Ice detected");
            flagCoding.addFlag("HighSolZ", 0x08, "Solar Zenith Angle > 70 deg.");
            flagCoding.addFlag("Glint", 0x10, "Sun Glint");
            flagCoding.addFlag("HighSenZ", 0x20, "Senzor Zenith Angle > 53 deg.");
            flagCoding.addFlag("Shallow", 0x40, "Shallow Water");

            product.getFlagCodingGroup().add(flagCoding);
            QFBand.setSampleCoding(flagCoding);


            product.getMaskGroup().add(Mask.BandMathsType.create("Ocean", "Ocean",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF4_VIIRSOCCEDR & 0x03 == 0x00",
                    Color.BLUE, 0.7));
            product.getMaskGroup().add(Mask.BandMathsType.create("CoastalWater", "Coastal Water mask",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF4_VIIRSOCCEDR & 0x03 == 0x01",
                    Color.GRAY, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("InlandWater", "Inland water mask",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF4_VIIRSOCCEDR & 0x03 == 0x02",
                    Color.DARK_GRAY, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("Land", "Land mask",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF4_VIIRSOCCEDR & 0x03 == 0x03",
                    SeadasFileReader.LandBrown, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("Ice/Snow", "Ice/snow mask.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF4_VIIRSOCCEDR.Ice_Snow",
                    Color.lightGray, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("HighSolZ", "Solar Zenith angle > 70 deg.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF4_VIIRSOCCEDR.HighSolZ",
                    SeadasFileReader.Purple, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("Glint", "Sun Glint.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF4_VIIRSOCCEDR.Glint",
                    SeadasFileReader.BrightPink, 0.1));
            product.getMaskGroup().add(Mask.BandMathsType.create("HighSenZ", "Sensor Zenith angle > 53 deg.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF4_VIIRSOCCEDR.HighSenZ",
                    SeadasFileReader.LightCyan, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("ShallowWater", "Shallow Water mask.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF4_VIIRSOCCEDR.Shallow",
                    SeadasFileReader.BurntUmber, 0.5));
        }
        QFBand = product.getBand("QF5_VIIRSOCCEDR");
        if (QFBand != null) {
            FlagCoding flagCoding = new FlagCoding("QF5");
            flagCoding.addFlag("Straylight", 0x04, "Adjacent pixel not clear, possible straylight contaminated");
            flagCoding.addFlag("Cirrus", 0x08, "Thin Cirrus cloud detected");
            flagCoding.addFlag("Shadow", 0x10, "Cloud shadow detected");
            flagCoding.addFlag("HighAer", 0x20, "Non-cloud obstruction (heavy aerosol load) detected");
            flagCoding.addFlag("AbsAer", 0x40, "Strongly absorbing aerosol detected");
            flagCoding.addFlag("HighAOT", 0x80, "Aerosol optical thickness @ 555nm > 0.3");

            product.getFlagCodingGroup().add(flagCoding);
            QFBand.setSampleCoding(flagCoding);


            product.getMaskGroup().add(Mask.BandMathsType.create("Clear", "Confidently Cloud-free.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF5_VIIRSOCCEDR & 0x03 == 0x00",
                    SeadasFileReader.Cornflower, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("LikelyClear", "Probably cloud-free",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF5_VIIRSOCCEDR & 0x03 == 0x01",
                    Color.LIGHT_GRAY, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("LikelyCloud", "Probably cloud contaminated.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF5_VIIRSOCCEDR & 0x03 == 0x02",
                    Color.DARK_GRAY, 0.25));
            product.getMaskGroup().add(Mask.BandMathsType.create("Cloud", "Confidently Cloudy.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF5_VIIRSOCCEDR & 0x03 == 0x03",
                    Color.WHITE, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("Straylight", "Adjacent pixel not clear, possible straylight contaminated.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF5_VIIRSOCCEDR.Straylight",
                    Color.YELLOW, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("Cirrus", "Thin Cirrus cloud detected.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF5_VIIRSOCCEDR.Cirrus",
                    Color.BLUE, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CloudShadow", "Cloud shadow detected.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF5_VIIRSOCCEDR.Shadow",
                    Color.GRAY, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("HighAer", "Non-cloud obstruction (heavy aerosol load) detected.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF5_VIIRSOCCEDR.HighAer",
                    SeadasFileReader.LightPink, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("AbsAer", "Strongly absorbing aerosol detected.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF5_VIIRSOCCEDR.AbsAer",
                    Color.ORANGE, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("HighAOT", "Aerosol optical thickness @ 555nm > 0.3.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF5_VIIRSOCCEDR.HighAOT",
                    Color.MAGENTA, 0.5));
        }
        QFBand = product.getBand("QF6_VIIRSOCCEDR");
        if (QFBand != null) {
            FlagCoding flagCoding = new FlagCoding("QF6");
            flagCoding.addFlag("Turbid", 0x01, "Turbid water detected (Rrs @ 555nm > 0.012)");
            flagCoding.addFlag("Coccolithophore", 0x02, "Coccolithophores detected");
            flagCoding.addFlag("HighCDOM", 0x04, "CDOM absorption @ 410nm > 2 m^-1");

            product.getFlagCodingGroup().add(flagCoding);
            QFBand.setSampleCoding(flagCoding);


            product.getMaskGroup().add(Mask.BandMathsType.create("Turbid", "Turbid water detected (Rrs @ 555nm > 0.012)",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF6_VIIRSOCCEDR.Turbid ",
                    SeadasFileReader.LightBrown, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("Coccolithophore", "Coccolithophores detected",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF6_VIIRSOCCEDR.Coccolithophore ",
                    Color.CYAN, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("HighCDOM", "CDOM absorption @ 410nm > 2 m^-1.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF6_VIIRSOCCEDR.HighCDOM ",
                    SeadasFileReader.Mustard, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("ChlFail", "No Chlorophyll retrieval possible.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF6_VIIRSOCCEDR & 0x18 == 0x00",
                    SeadasFileReader.FailRed, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("LowChl", "Chlorophyll < 1 mg m^-3",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF6_VIIRSOCCEDR & 0x18 == 0x08",
                    SeadasFileReader.Coral, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("ModChl", "Chlorophyll between 1 and 10 mg m^-3",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF6_VIIRSOCCEDR  & 0x18 == 0x10",
                    SeadasFileReader.DarkGreen, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("HighChl", "Chlorphyll > 10 mg m^-3",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF6_VIIRSOCCEDR   & 0x18 == 0x10",
                    Color.RED, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CarderEmp", "Carder Empirical algorithm used.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF6_VIIRSOCCEDR & 0xE0 == 0x20",
                    SeadasFileReader.NewGreen, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("UnpackPig", "Phytoplankton with packaged pigment",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF6_VIIRSOCCEDR & 0xE0 == 0x40",
                    SeadasFileReader.TealGreen, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("WtPigGlobal", "Weighted packaged pigment - global",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF6_VIIRSOCCEDR & 0xE0 == 0x80",
                    Color.GRAY, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("WtPigFull", "Weighted fully packaged pigment",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF6_VIIRSOCCEDR & 0xE0 == 0xA0",
                    Color.LIGHT_GRAY, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("FullPackPig", "Phytoplankton with fully packaged pigment",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF6_VIIRSOCCEDR & 0xE0 == 0xC0",
                    SeadasFileReader.TealBlue, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("NoOCC", "No ocean color chlorphyll retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF6_VIIRSOCCEDR & 0xE0 == 0xE0",
                    Color.BLACK, 0.1));
        }
        QFBand = product.getBand("QF7_VIIRSOCCEDR");
        if (QFBand != null) {
            FlagCoding flagCoding = new FlagCoding("QF7");
            flagCoding.addFlag("nLwWarn", 0x01, "nLw out-of-range (< 0.1 or > 40 W m^-2 um^-1 sr^-1)");
            flagCoding.addFlag("ChlWarn", 0x02, "Chlorophyll out-of-range (< 0.05 or > 50 mg m^-3)");
            flagCoding.addFlag("IOPaWarn", 0x04, "IOP absorption out-of-range (< 0.01 or  > 10 m^-1)");
            flagCoding.addFlag("IOPsWarn", 0x08, "IOP scattering out-of-range (< 0.01 or  > 50 m^-1)");
            flagCoding.addFlag("SSTWarn", 0x10, "Input Skin SST poor quality");
            flagCoding.addFlag("Bright", 0x20, "Bright Target flag");


            product.getFlagCodingGroup().add(flagCoding);
            QFBand.setSampleCoding(flagCoding);


            product.getMaskGroup().add(Mask.BandMathsType.create("nLwWarn", "nLw out-of-range (< 0.1 or > 40 W m^-2 um^-1 sr^-1)",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF7_VIIRSOCCEDR.nLwWarn",
                    Color.BLUE, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("ChlWarn", "Chlorophyll out-of-range (< 0.05 or > 50 mg m^-3)",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF7_VIIRSOCCEDR.ChlWarn",
                    Color.LIGHT_GRAY, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("IOPaWarn", "IOP absorption out-of-range (< 0.01 or  > 10 m^-1)",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF7_VIIRSOCCEDR.IOPaWarn",
                    Color.DARK_GRAY, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("IOPsWarn", "IOP scattering out-of-range (< 0.01 or  > 50 m^-1)",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF7_VIIRSOCCEDR.IOPsWarn",
                    Color.GREEN, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("SSTWarn", "Input Skin SST poor quality.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF7_VIIRSOCCEDR.SSTWarn",
                    Color.LIGHT_GRAY, 0.2));
            product.getMaskGroup().add(Mask.BandMathsType.create("Bright", "Bright Target flag",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF7_VIIRSOCCEDR.Bright",
                    Color.GRAY, 0.2));

        }
        QFBand = product.getBand("QF1_VIIRSMBANDSDR");
        if (QFBand != null) {
            FlagCoding flagCoding = new FlagCoding("QF1SDR");
            flagCoding.addFlag("CalQualGood", 0x00, "Calibration quality - Good");
            flagCoding.addFlag("CalQualBad", 0x01, "Calibration quality - Bad");
            flagCoding.addFlag("NoCal", 0x02, "No Calibration");
            flagCoding.addFlag("NoSatPix", 0x03, "No saturated");
            flagCoding.addFlag("LowSatPix", 0x03, "Some pixels saturated");
            flagCoding.addFlag("SatPix", 0x03, "All pixels saturated");
            flagCoding.addFlag("DataOK", 0x04, "All required data available");
            flagCoding.addFlag("BadEvRDR", 0x08, "Missing EV RDR data.");
            flagCoding.addFlag("BadCalData", 0x10, "Missing cal data (SV, CV, SD, etc)");
            flagCoding.addFlag("BadTherm", 0x20, "Missing Thermistor data");
            flagCoding.addFlag("InRange", 0x40, "All calibrated data within LUT thresholds");
            flagCoding.addFlag("BadRad", 0x40, "Radiance out-of-range LUT threshold");
            flagCoding.addFlag("BadRef", 0x40, "Reflectance out-of-range LUT threshold");
            flagCoding.addFlag("BadRadRef", 0x40, "Both Radiance & Reflectance out-of-range LUT threshold");


            product.getFlagCodingGroup().add(flagCoding);
            QFBand.setSampleCoding(flagCoding);


            product.getMaskGroup().add(Mask.BandMathsType.create("CalQualGood", "Calibration quality - Good",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSMBANDSDR & 0x02 == 0x00",
                    Color.BLUE, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CalQualBad", "Calibration quality - Bad",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSMBANDSDR & 0x02 == 0x01",
                    Color.GRAY, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("NoCal", "No Calibration",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSMBANDSDR & 0x02 == 0x02",
                    Color.DARK_GRAY, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("NoSatPix", "No saturated",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSMBANDSDR & 0x0C == 0x00",
                    Color.GREEN, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("LowSatPix", "Some pixels saturated.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSMBANDSDR & 0x0C == 0x04",
                    Color.lightGray, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("SatPix", "All pixels saturated",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSMBANDSDR & 0x0C == 0x08",
                    Color.MAGENTA, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("DataOK", "All required data available",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSMBANDSDR & 0x30 == 0x00",
                    Color.YELLOW, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("BadEvRDR", "Missing EV RDR data.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSMBANDSDR & 0x30 == 0x10",
                    Color.orange, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("BadCalData", "Missing cal data (SV, CV, SD, etc).",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSMBANDSDR & 0x30 == 0x20",
                    Color.BLUE, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("BadTherm", "Missing Thermistor data.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSMBANDSDR & 0x30 == 0x30",
                    Color.BLUE, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("InRange", "All calibrated data within LUT thresholds.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSMBANDSDR & 0xC0 == 0x00",
                    Color.BLUE, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("BadRad", "Radiance out-of-range LUT threshold.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSMBANDSDR & 0xC0 == 0x40",
                    Color.BLUE, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("BadRef", "Reflectance out-of-range LUT threshold.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSMBANDSDR & 0xC0 == 0x80",
                    Color.BLUE, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("BadRadRef", "Both Radiance & Reflectance out-of-range LUT threshold.",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "QF1_VIIRSMBANDSDR & 0xC0 == 0xC0",
                    Color.BLUE, 0.5));
        }
    }
}
