package org.esa.beam.dataio.spot;

import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class FileNode {

    public abstract String getName();

    public Reader getReader(String path) throws IOException {
        return new InputStreamReader(getInputStream(path));
    }

    public abstract InputStream getInputStream(String path) throws IOException;

    public abstract File getFile(String path) throws IOException;

    public abstract String[] list(String path) throws IOException;

    public abstract void close();

    public static FileNode create(File file) {
        if (file.isDirectory()) {
            return new Dir(file);
        }

        try {
            return new Zip(new ZipFile(file));
        } catch (IOException e) {
            return null;
        }
// TODO - maybe this is faster (nf)
/*
        FileInputStream stream = new FileInputStream(file);
        try {
            byte[] b = new byte[4];
            int n = stream.read(b);
            if (n == 4 && new String(b).equals("PK\003\004")) {
                return new Zip(zipFile);
            }
        } finally {
            stream.close();
        }
        return null;
*/
    }

    private static class Dir extends FileNode {
        private final File dir;

        private Dir(File file) {
            dir = file;
        }

        @Override
        public String getName() {
            return dir.getPath();
        }

        @Override
        public InputStream getInputStream(String path) throws IOException {
            return new FileInputStream(getFile(path));
        }

        @Override
        public File getFile(String path) throws IOException {
            File child = new File(dir, path);
            if (!child.exists()) {
                throw new FileNotFoundException(child.getPath());
            }
            return child;
        }

        @Override
        public String[] list(String path) throws IOException {
            File child = getFile(path);
            return child.list();
        }

        @Override
        public void close() {
        }
    }

    private static class Zip extends FileNode {
        private static final int BUFFER_SIZE = 4 * 1024 * 1024;
        private final ZipFile zipFile;
        private File tempZipFileDir;

        private Zip(ZipFile zipFile) throws IOException {
            this.zipFile = zipFile;
        }

        @Override
        public String getName() {
            return zipFile.getName();
        }

        @Override
        public InputStream getInputStream(String path) throws IOException {
            return getInputStream(getEntry(path));
        }

        @Override
        public File getFile(String path) throws IOException {

            ZipEntry zipEntry = getEntry(path);

            if (tempZipFileDir == null) {
                tempZipFileDir = new File(getTempDir(), FileUtils.getFilenameWithoutExtension(new File(zipFile.getName())));
            }

            File file = new File(tempZipFileDir, zipEntry.getName());
            if (file.exists()) {
                return file;
            }

            System.out.println("Extracting ZIP-entry to " + file);

            if (zipEntry.isDirectory()) {
                file = new File(tempZipFileDir, path);
                file.mkdirs();
                if (!file.exists()) {
                    throw new IOException("Failed to create temporary directory " + file);
                }
            } else {
                InputStream is = zipFile.getInputStream(zipEntry);
                try {
                    file = new File(tempZipFileDir, path);
                    file.getParentFile().mkdirs();

                    BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE);
                    try {
                        byte[] bytes = new byte[1024];
                        int n;
                        while ((n = is.read(bytes)) > 0) {
                            os.write(bytes, 0, n);
                        }
                    } finally {
                        os.close();
                    }
                } finally {
                    is.close();
                }

                if (!file.exists()) {
                    throw new IOException("Failed to create temporary file " + file);
                }
            }

            return file;
        }

        @Override
        public String[] list(String path) throws IOException {
            boolean dirSeen = false;
            ArrayList<String> names = new ArrayList<String>(32);
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                String name = zipEntry.getName();
                if (name.startsWith(path)) {
                    String entryName = name.substring(path.length() + 1);
                    if (!entryName.isEmpty()) {
                        names.add(entryName);
                    }
                    dirSeen = true;
                }
            }
            if (!dirSeen) {
                throw new FileNotFoundException(getName() + "!" + path);
            }
            return names.toArray(new String[names.size()]);
        }

        @Override
        public void close() {
            try {
                zipFile.close();
            } catch (IOException e) {
                // ok
            }
            if (tempZipFileDir != null) {
                SystemUtils.deleteFileTree(tempZipFileDir);
            }
        }

        private String normalizeFileName() {
            return zipFile.getName().replace("/", "_").replace("\\", "_").replace(".", "_").replace(":", "_");
        }

        private InputStream getInputStream(ZipEntry zipEntry) throws IOException {
            return zipFile.getInputStream(zipEntry);
        }

        private ZipEntry getEntry(String path) throws FileNotFoundException {
            ZipEntry zipEntry = zipFile.getEntry(path);
            if (zipEntry == null) {
                throw new FileNotFoundException(zipFile.getName() + "!" + path);
            }
            return zipEntry;
        }

        private static File getTempDir() throws IOException {
            File tempDir = null;
            String tempDirName = System.getProperty("java.io.tmpdir");
            if (tempDirName != null) {
                tempDir = new File(tempDirName);
            }
            if (tempDir == null) {
                tempDir = new File(SystemUtils.getUserHomeDir(), ".beam/temp");
                tempDir.mkdirs();
            }
            if (!tempDir.exists()) {
                throw new IOException("Temporary directory not available: " + tempDir);
            }
            return tempDir;
        }

    }

}
