package org.esa.beam.dataio.landsat.tgz;

import com.bc.ceres.core.VirtualDir;
import org.esa.beam.util.io.FileUtils;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class VirtualDirTgz extends VirtualDir {

    private final File archiveFile;
    private File extractDir;

    public VirtualDirTgz(File tgz) throws IOException {
        if (tgz == null) {
            throw new IllegalArgumentException("Input file shall not be null");
        }
        archiveFile = tgz;
        extractDir = null;
    }

    @Override
    public String getBasePath() {
        return archiveFile.getPath();
    }

    @Override
    public InputStream getInputStream(String path) throws IOException {
        final File file = getFile(path);
        return new FileInputStream(file);
    }

    @Override
    public File getFile(String path) throws IOException {
        ensureUnpacked();
        final File file = new File(extractDir, path);
        if (!(file.isFile() || file.isDirectory())) {
            throw new IOException();
        }
        return file;
    }

    @Override
    public String[] list(String path) throws IOException {
        final File file = getFile(path);
        return file.list();
    }

    @Override
    public void close() {
        if (extractDir != null) {
            FileUtils.deleteTree(extractDir);
            extractDir = null;
        }
    }

    @Override
    public boolean isCompressed() {
        return isTgz(archiveFile.getName());
    }

    @Override
    public boolean isArchive() {
        return true;
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        close();
    }

    @Override
    public File getTempDir() throws IOException {
        return extractDir;
    }

    static String getFilenameFromPath(String path) {
        int lastSepIndex = path.lastIndexOf("/");
        if (lastSepIndex == -1) {
            lastSepIndex = path.lastIndexOf("\\");
            if (lastSepIndex == -1) {
                return path;
            }
        }

        return path.substring(lastSepIndex + 1, path.length());
    }

    static boolean isTgz(String filename) {
        final String extension = FileUtils.getExtension(filename);
        return (".tgz".equals(extension) || ".gz".equals(extension));
    }

    private void ensureUnpacked() throws IOException {
        if (extractDir == null) {
            extractDir = VirtualDir.createUniqueTempDir();

            final TarInputStream tis;
            if (isTgz(archiveFile.getName())) {
                tis = new TarInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(archiveFile))));
            } else {
                tis = new TarInputStream(new BufferedInputStream(new FileInputStream(archiveFile)));
            }
            TarEntry entry;

            while ((entry = tis.getNextEntry()) != null) {
                final String entryName = entry.getName();
                if (entry.isDirectory()) {
                    final File directory = new File(extractDir, entryName);
                    ensureDirectory(directory);
                    continue;
                }

                final String fileNameFromPath = getFilenameFromPath(entryName);
                final int pathIndex = entryName.indexOf(fileNameFromPath);
                String tarPath = null;
                if (pathIndex > 0) {
                    tarPath = entryName.substring(0, pathIndex - 1);
                }

                File targetDir;
                if (tarPath != null) {
                    targetDir = new File(extractDir, tarPath);
                } else {
                    targetDir = extractDir;
                }

                ensureDirectory(targetDir);
                final File targetFile = new File(targetDir, fileNameFromPath);
                if (targetFile.isFile()) {
                    continue;
                }

                if (!targetFile.createNewFile()) {
                    throw new IOException("Unable to create file: " + targetFile.getAbsolutePath());
                }

                final OutputStream outStream = new BufferedOutputStream(new FileOutputStream(targetFile));
                final byte data[] = new byte[1024 * 1024];
                int count;
                while ((count = tis.read(data)) != -1) {
                    outStream.write(data, 0, count);
                }

                // @todo 3 tb/tb try finally - make sure everything is closed
                outStream.flush();
                outStream.close();
            }
        }
    }

    private void ensureDirectory(File targetDir) throws IOException {
        if (!targetDir.isDirectory()) {
            if (!targetDir.mkdirs()) {
                throw new IOException("unable to create directory: " + targetDir.getAbsolutePath());
            }
        }
    }
}
