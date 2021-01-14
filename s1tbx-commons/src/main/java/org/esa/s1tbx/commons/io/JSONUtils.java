package org.esa.s1tbx.commons.io;

import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class JSONUtils {

    public static Object loadJSONFile(final File file) throws Exception {
        final BufferedReader streamReader = new BufferedReader(new FileReader(file.getPath()));
        final JSONParser parser = new JSONParser();
        return parser.parse(streamReader);
    }
}
