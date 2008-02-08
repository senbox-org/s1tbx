package org.esa.beam.dataio.shapefile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class FileFactory {

    public static File getFile(String resourcePath) throws IOException {
        final URL resource = FileFactory.class.getResource(resourcePath);
        if (resource == null) {
            throw new FileNotFoundException(resourcePath);
        }
        try {
            File file = new File(resource.toURI().getPath());
            if (!file.exists()) {
                throw new FileNotFoundException(file.getPath());
            }
            return file;
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
