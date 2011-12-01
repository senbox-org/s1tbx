package org.esa.beam.dataio.landsat.tgz;

import com.bc.ceres.core.VirtualDir;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;

import java.io.*;

public class VirtualDirTgz extends VirtualDir {

    private ArchiveInputStream archiveInputStream;

    public VirtualDirTgz(File tgz) throws IOException {
        // @todo 1 tb/tb ungzip when compressed
//        final InputStream fileInputStream = new FileInputStream(tgz);
//        final FilterInputStream tgzInputStream = new BufferedInputStream(fileInputStream);
//        try {
//            archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(tgzInputStream);
//        } catch (ArchiveException e) {
//            throw new IOException(e.getMessage());
//        }
    }

    @Override
    public String getBasePath() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public InputStream getInputStream(String path) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public File getFile(String path) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String[] list(String path) throws IOException {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
