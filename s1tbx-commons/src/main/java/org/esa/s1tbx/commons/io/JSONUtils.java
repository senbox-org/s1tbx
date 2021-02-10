package org.esa.s1tbx.commons.io;

import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;

public class JSONUtils {

    public static Object loadJSONFile(final File file) throws Exception {
        final JSONParser parser = new JSONParser();
        try (FileReader fileReader = new FileReader(file)) {
            return parser.parse(fileReader);
        }
    }
}
