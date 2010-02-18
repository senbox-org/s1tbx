package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Apr 29, 2008
 * To change this template use File | Settings | File Templates.
 */
public class EnvisatOrbitReader extends EnvisatAuxReader {

    static class DateComparator implements Comparator<Date>
    {
        public int compare(Date d1, Date d2) {
            return d1.compareTo(d2);
        }
    }

    private OrbitVector[] dataRecords = null;
    private double[] recordTimes = null;

    public EnvisatOrbitReader() {
        super();
    }

    public void readOrbitData() throws IOException {

        if (_productFile instanceof DorisOrbitProductFile) {

            final DorisOrbitProductFile dorisProdFile = (DorisOrbitProductFile) _productFile;
            final Record orbitRecord = dorisProdFile.readOrbitData();

            OrbitVector orb = null;
            final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSSSSS");
		    final ArrayList<OrbitVector> orbitVectorList = new ArrayList<OrbitVector>();

            final int numFields = orbitRecord.getNumFields();
            for (int i = 0; i < numFields; ++i) {
                
                final Field f = orbitRecord.getFieldAt(i);

                final String fieldName = f.getName();
                if (fieldName.contains("blank")) {
                    continue;
                } else if (fieldName.contains("utc_time")) {
                    orb = new OrbitVector();
                    try {
                        orb.utcTime = ProductData.UTC.parse(f.getData().getElemString()).getMJD();
                    } catch (ParseException e) {
                        throw new IllegalFileFormatException("Failed to parse UTC time " + e.getMessage());
                    }
                } else if (fieldName.contains("delta_ut1")) {
                    if (orb != null)
                        orb.delta_ut1 = Double.parseDouble(f.getData().getElemString());
                } else if (fieldName.contains("abs_orbit")) {
                    if (orb != null)
                        orb.absOrbit = Integer.parseInt(f.getData().getElemString().replace("+",""));
                } else if (fieldName.contains("x_pos")) {
                    if (orb != null)
                        orb.xPos = Double.parseDouble(f.getData().getElemString());
                } else if (fieldName.contains("y_pos")) {
                    if (orb != null)
                        orb.yPos = Double.parseDouble(f.getData().getElemString());
                } else if (fieldName.contains("z_pos")) {
                    if (orb != null)
                        orb.zPos = Double.parseDouble(f.getData().getElemString());
                } else if (fieldName.contains("x_vel")) {
                    if (orb != null)
                        orb.xVel = Double.parseDouble(f.getData().getElemString());
                } else if (fieldName.contains("y_vel")) {
                    if (orb != null)
                        orb.yVel = Double.parseDouble(f.getData().getElemString());
                } else if (fieldName.contains("z_vel")) {
                    if (orb != null)
                        orb.zVel = Double.parseDouble(f.getData().getElemString());
                } else if (fieldName.contains("qual_flags")) {
                    if (orb != null)
                        orb.qualFlags = f.getData().getElemString();
                        orbitVectorList.add(orb);
                }
            }

            dataRecords = orbitVectorList.toArray(new OrbitVector[orbitVectorList.size()]);

            recordTimes = new double[dataRecords.length];
            for (int i = 0; i < dataRecords.length; i++) {
                recordTimes[i] = dataRecords[i].utcTime;
            }
        }
    }

    public OrbitVector getOrbitVector(int n) {
        return dataRecords[n];
    }

    public int getNumRecords() {
        return dataRecords.length;
    }

    /**
     * Get orbit vector for given UTC time.
     * @param utc The UTC time
     * @throws Exception for incorrect time
     * @return The orbit vector
     */
    public OrbitVector getOrbitVector(double utc) throws Exception {

        final int n = Arrays.binarySearch(recordTimes, utc);
		
		if (n >= 0) {
			return dataRecords[n];
		}
		
		final int n2 = -n - 1;
        final int n0 = n2 - 2;
        final int n1 = n2 - 1;
        final int n3 = n2 + 1;

        if (n0 < 0 || n1 < 0 || n2 >= recordTimes.length || n3 >= recordTimes.length) {
            throw new Exception("Incorrect UTC time");
        }

        final double dt = (utc - recordTimes[n1]) / (recordTimes[n2] - recordTimes[n1]);
        final double w0 = w(dt + 1.0);
        final double w1 = w(dt);
        final double w2 = w(1.0 - dt);
        final double w3 = w(2.0 - dt);

        final OrbitVector orb = new OrbitVector();
        orb.utcTime = utc;
        orb.absOrbit = dataRecords[n1].absOrbit;
        orb.qualFlags = dataRecords[n1].qualFlags;

        orb.delta_ut1 = w0*dataRecords[n0].delta_ut1 +
                        w1*dataRecords[n1].delta_ut1 +
                        w2*dataRecords[n2].delta_ut1 +
                        w3*dataRecords[n3].delta_ut1;

        orb.xPos = w0*dataRecords[n0].xPos +
                   w1*dataRecords[n1].xPos +
                   w2*dataRecords[n2].xPos +
                   w3*dataRecords[n3].xPos;

        orb.yPos = w0*dataRecords[n0].yPos +
                   w1*dataRecords[n1].yPos +
                   w2*dataRecords[n2].yPos +
                   w3*dataRecords[n3].yPos;

        orb.zPos = w0*dataRecords[n0].zPos +
                   w1*dataRecords[n1].zPos +
                   w2*dataRecords[n2].zPos +
                   w3*dataRecords[n3].zPos;

        orb.xVel = w0*dataRecords[n0].xVel +
                   w1*dataRecords[n1].xVel +
                   w2*dataRecords[n2].xVel +
                   w3*dataRecords[n3].xVel;

        orb.yVel = w0*dataRecords[n0].yVel +
                   w1*dataRecords[n1].yVel +
                   w2*dataRecords[n2].yVel +
                   w3*dataRecords[n3].yVel;

        orb.zVel = w0*dataRecords[n0].zVel +
                   w1*dataRecords[n1].zVel +
                   w2*dataRecords[n2].zVel +
                   w3*dataRecords[n3].zVel;

        return orb;
    }

    /**
     * Weight function for cubic interpolation.
     * @param x The variable
     * @return The weight
     */
    private static double w(final double x) {

        final double a = -0.5;
        final double absX = Math.abs(x);
        final double absX2 = absX*absX;
        if (absX >= 0.0 && absX < 1.0) {
            return (a + 2.0)*absX2*absX - (a + 3.0)*absX2 + 1.0;
        } else if (absX >= 1.0 && absX < 2) {
            return a*absX2*absX - 5.0*a*absX2 +8.0*a*absX - 4.0*a;
        } else {
            return 0.0;
        }
    }

    public final static class OrbitVector {
        public double utcTime = 0;
        public double delta_ut1 = 0;
        public int absOrbit = 0;
        public double xPos = 0;
        public double yPos = 0;
        public double zPos = 0;
        public double xVel = 0;
        public double yVel = 0;
        public double zVel = 0;
        public String qualFlags = null;
    }
}
