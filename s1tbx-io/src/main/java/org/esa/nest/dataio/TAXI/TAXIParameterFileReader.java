package org.esa.nest.dataio.TAXI;

import org.esa.snap.util.XMLSupport;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Read DLR parameter file
 */
public class TAXIParameterFileReader {

    private final File file;

    public String timeutc;
    public int naz, nrg, naz_ml, nrg_ml;
    public double azml, rgml, ps_rg, ps_az, tcycle;
    public double sensorVelocity, radarFrequency, waveLength;
    public double[] slantRange, kt, v, DopplerCentroid;

    public TAXIParameterFileReader(final File file) {
        this.file = file;
    }

    public void readParameterFile() throws Exception {

        try {
            final Document xmlDoc = XMLSupport.LoadXML(file.getAbsolutePath());
            final Element xmlRoot = xmlDoc.getRootElement();

            final Element object = xmlRoot.getChild("object");

            final List children = object.getContent();
            for (Object aChild : children) {
                if (aChild instanceof Element) {
                    final Element elem = (Element) aChild;
                    final Attribute nameAttr = elem.getAttribute("name");
                    if(nameAttr != null) {
                        final String name = nameAttr.getValue();
                        switch (name) {
                            case "timeutc": {
                                timeutc = getElementString(elem);
                            }
                            break;
                            case "naz": {
                                naz = getElementInt(elem);
                            }
                            break;
                            case "nrg": {
                                nrg = getElementInt(elem);
                            }
                            break;
                            case "naz_ml": {
                                naz_ml = getElementInt(elem);
                            }
                            break;
                            case "nrg_ml": {
                                nrg_ml = getElementInt(elem);
                            }
                            break;
                            case "azml": {
                                azml = getElementDouble(elem);
                            }
                            break;
                            case "rgml": {
                                rgml = getElementDouble(elem);
                            }
                            break;
                            case "ps_az": {
                                ps_az = getElementDouble(elem);
                            }
                            break;
                            case "ps_rg": {
                                ps_rg = getElementDouble(elem);
                            }
                            break;
                            case "r": {
                                slantRange = getElementArray(elem);
                            }
                            break;
                            case "tcycle": {
                                tcycle = getElementDouble(elem);
                            }
                            break;
                            case "kt": {
                                kt = getElementArray(elem);
                            }
                            break;
                            case "v": {
                                v = getElementArray(elem);
                            }
                            break;
                            case "vs": {
                                sensorVelocity = getElementDouble(elem);
                            }
                            break;
                            case "f0": {
                                radarFrequency = getElementDouble(elem);
                            }
                            break;
                            case "lambda": {
                                waveLength = getElementDouble(elem);
                            }
                            break;
                            case "fdc": {
                                DopplerCentroid = getElementArray(elem);
                            }
                            break;
                            default:
                                break;
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
    }

    private static String getElementString(final Element elem) {
        final Element value = elem.getChild("value");
        return value.getValue();
    }

    private static int getElementInt(final Element elem) {
        final Element value = elem.getChild("value");
        return Integer.parseInt(value.getValue());
    }

    private static double getElementDouble(final Element elem) {
        final Element value = elem.getChild("value");
        return Double.parseDouble(value.getValue());
    }

    private static double[] getElementArray(final Element elem) {
        final Element value = elem.getChild("value");
        final Element ptr = value.getChild("parameter");
        final Element val = ptr.getChild("value");

        String str = val.getValue();
        str = str.replace("[","").replace("]","");

        return stringToDoubleArray(str, ",");
    }

    public static double[] stringToDoubleArray(final String csvString, final String delim) {
        final StringTokenizer tokenizer = new StringTokenizer(csvString, delim);
        final List<String> strList = new ArrayList<>(tokenizer.countTokens());
        while (tokenizer.hasMoreTokens()) {
            strList.add(tokenizer.nextToken());
        }
        final double[] array = new double[strList.size()];
        int i=0;
        for(String s : strList) {
            array[i++] = Double.parseDouble(s);
        }
        return array;
    }
}
