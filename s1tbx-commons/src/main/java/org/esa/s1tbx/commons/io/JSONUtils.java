package org.esa.s1tbx.commons.io;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class JSONUtils {

    public static Object loadJSONFile(final File file) throws Exception {
        final JSONParser parser = new JSONParser();
        try (FileReader fileReader = new FileReader(file)) {
            return parser.parse(fileReader);
        }
    }

    public static void writeJSON(final JSONObject json, final File file) throws Exception {
        try (FileWriter fileWriter = new FileWriter(file)) {
            final File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            fileWriter.write(json.toJSONString());
            fileWriter.flush();
        }
    }
}
