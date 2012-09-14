package org.esa.nest.gpf.orbits;

import com.bc.ceres.core.NullProgressMonitor;
import org.esa.beam.dataio.envisat.EnvisatOrbitReader;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Orbits;
import org.esa.nest.util.Settings;
import org.esa.nest.util.ftpUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * DORIS Orbit File
 */
public class DorisOrbitFile extends BaseOrbitFile {

    private EnvisatOrbitReader dorisReader = null;

    public static final String DORIS_POR = "DORIS Preliminary POR";
    public static final String DORIS_VOR = "DORIS Precise VOR";

    public DorisOrbitFile(final String orbitType, final MetadataElement absRoot,
                          final Product sourceProduct) throws IOException {
        super(orbitType, absRoot);

        init(sourceProduct);
    }

    /**
     * Get orbit information for given time.
     * @param utc The UTC in days.
     * @return The orbit information.
     * @throws Exception The exceptions.
     */
    public Orbits.OrbitData getOrbitData(final double utc) throws Exception {

        final Orbits.OrbitData orbitData = new Orbits.OrbitData();

        final EnvisatOrbitReader.OrbitVector orb = dorisReader.getOrbitVector(utc);
        orbitData.xPos = orb.xPos;
        orbitData.yPos = orb.yPos;
        orbitData.zPos = orb.zPos;
        orbitData.xVel = orb.xVel;
        orbitData.yVel = orb.yVel;
        orbitData.zVel = orb.zVel;

        return orbitData;
    }

    /**
     * Get DORIS orbit file.
     * @param sourceProduct the input product
     * @throws java.io.IOException The exception.
     */
    private void init(final Product sourceProduct) throws IOException {

        dorisReader = EnvisatOrbitReader.getInstance();
        final int absOrbit = absRoot.getAttributeInt(AbstractMetadata.ABS_ORBIT, 0);

        // construct path to the orbit file folder
        String orbitPath = "";
        String remoteBaseFolder = "";
        if(orbitType.contains(DORIS_VOR)) {
            orbitPath = Settings.instance().get("OrbitFiles/dorisVOROrbitPath");
            remoteBaseFolder = Settings.instance().get("OrbitFiles/dorisFTP_vor_remotePath");
        } else if(orbitType.contains(DORIS_POR)) {
            orbitPath = Settings.instance().get("OrbitFiles/dorisPOROrbitPath");
            remoteBaseFolder = Settings.instance().get("OrbitFiles/dorisFTP_por_remotePath");
        }

        final Date startDate = sourceProduct.getStartTime().getAsDate();
        final int month = startDate.getMonth()+1;
        String folder = String.valueOf(startDate.getYear() + 1900);
        if(month < 10) {
            folder +='0';
        }
        folder += month;
        orbitPath += File.separator + folder;
        final File localPath = new File(orbitPath);

        // find orbit file in the folder
        orbitFile = FindDorisOrbitFile(dorisReader, localPath, startDate, absOrbit);
        if(orbitFile == null) {
            final String remotePath = remoteBaseFolder +'/'+ folder;
            getRemoteDorisFiles(remotePath, localPath);
            // find again in newly downloaded folder
            orbitFile = FindDorisOrbitFile(dorisReader, localPath, startDate, absOrbit);
        }

        if(orbitFile == null) {
            throw new IOException("Unable to find suitable DORIS orbit file in\n"+orbitPath);
        }
    }

    /**
     * Find DORIS orbit file.
     * @param dorisReader The ENVISAT oribit reader.
     * @param path The path to the orbit file.
     * @param productDate The start date of the product.
     * @param absOrbit The absolute orbit number.
     * @return The orbit file.
     * @throws IOException
     */
    private static File FindDorisOrbitFile(EnvisatOrbitReader dorisReader, File path, Date productDate, int absOrbit)
            throws IOException {

        final File[] list = path.listFiles();
        if(list == null) return null;

        // loop through all orbit files in the given folder
        for(File f : list) {

            if(f.isDirectory()) {
                final File foundFile = FindDorisOrbitFile(dorisReader, f, productDate, absOrbit);
                if(foundFile != null) {
                    return foundFile;
                }
            }

            try {
                // open each orbit file
                dorisReader.readProduct(f);

                // get the start and end dates and compare them against product start date
                final Date startDate = dorisReader.getSensingStart();
                final Date stopDate = dorisReader.getSensingStop();
                if (productDate.after(startDate) && productDate.before(stopDate)) {

                    // get the absolute orbit code and compare it against the orbit code in the product
                    dorisReader.readOrbitData();
                    //EnvisatOrbitReader.OrbitVector orb = dorisReader.getOrbitVector(0);
                    //if (absOrbit == orb.absOrbit) {
                        return f;
                    //}
                }
            } catch(Exception e) {
                System.out.println(e.getMessage());
                // continue
            }
        }

        return null;
    }

    private void getRemoteDorisFiles(final String remotePath, final File localPath) {
        final String dorisFTP = Settings.instance().get("OrbitFiles/dorisFTP");
        try {
            if(ftp == null) {
                final String user = Settings.instance().get("OrbitFiles/dorisFTP_user");
                final String pass = Settings.instance().get("OrbitFiles/dorisFTP_pass");
                ftp = new ftpUtils(dorisFTP, user, pass);
                fileSizeMap = ftpUtils.readRemoteFileList(ftp, dorisFTP, remotePath);
            }

            if(!localPath.exists())
                localPath.mkdirs();

            if(VisatApp.getApp() != null) {
                final DownloadOrbitWorker worker = new DownloadOrbitWorker(VisatApp.getApp(), "Download Orbit Files",
                        ftp, fileSizeMap, remotePath, localPath);
                worker.executeWithBlocking();

            } else {
                getRemoteFiles(ftp, fileSizeMap, remotePath, localPath, new NullProgressMonitor());
            }

        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
