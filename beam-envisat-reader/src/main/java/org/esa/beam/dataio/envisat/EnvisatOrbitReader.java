package org.esa.beam.dataio.envisat;

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

    private TreeMap orbitVectorMap = new TreeMap(new DateComparator());

    public EnvisatOrbitReader() {
        super();
    }

    public void readOrbitData() throws IOException {
        if (_productFile instanceof DorisOrbitProductFile) {
            DorisOrbitProductFile dorisProdFile = (DorisOrbitProductFile) _productFile;
            Record orbitRecord = dorisProdFile.readerOrbitData();


            OrbitVector orb = null;
            final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSSSSS");

            orbitVectorMap.clear();

            int numFields = orbitRecord.getNumFields();
            for (int i = 0; i < numFields; ++i) {
                Field f = orbitRecord.getFieldAt(i);

                String fieldName = f.getName();
                if (fieldName.contains("blank")) {
                    continue;
                } else if (fieldName.contains("utc_time")) {
                    orb = new OrbitVector();
                    try {
                        orb.utcTime = dateFormat.parse(f.getData().getElemString());
                    } catch (ParseException e) {
                        throw new IOException("Failed to parse UTC time " + e.getMessage());
                    }
                } else if (fieldName.contains("delta_ut1")) {
                    if (orb != null)
                        orb.delta_ut1 = f.getData().getElemInt();
                } else if (fieldName.contains("abs_orbit")) {
                    if (orb != null)
                        orb.absOrbit = f.getData().getElemInt();
                } else if (fieldName.contains("x_pos")) {
                    if (orb != null)
                        orb.xPos = f.getData().getElemDouble();
                } else if (fieldName.contains("y_pos")) {
                    if (orb != null)
                        orb.yPos = f.getData().getElemDouble();
                } else if (fieldName.contains("z_pos")) {
                    if (orb != null)
                        orb.zPos = f.getData().getElemDouble();
                } else if (fieldName.contains("x_vel")) {
                    if (orb != null)
                        orb.xVel = f.getData().getElemDouble();
                } else if (fieldName.contains("y_vel")) {
                    if (orb != null)
                        orb.yVel = f.getData().getElemDouble();
                } else if (fieldName.contains("z_vel")) {
                    if (orb != null)
                        orb.zVel = f.getData().getElemDouble();
                } else if (fieldName.contains("qual_flags")) {
                    if (orb != null)
                        orb.qualFlags = f.getData().getElemString();
                }

                if(orb != null)
                    orbitVectorMap.put(orb.utcTime, orb);
            }
        }
    }

    public OrbitVector getOrbitVector(Date date)
    {
        SortedMap tail = orbitVectorMap.tailMap(date);
        if(tail.isEmpty()) return null;

        return (OrbitVector) orbitVectorMap.get(tail.firstKey());
    }

    public static class OrbitVector {

        Date utcTime;
        double delta_ut1;
        int absOrbit;
        double xPos;
        double yPos;
        double zPos;
        double xVel;
        double yVel;
        double zVel;
        String qualFlags;
    }
}
