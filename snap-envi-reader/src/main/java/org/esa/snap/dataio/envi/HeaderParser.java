package org.esa.snap.dataio.envi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses a ENVI HDR header in key value pairs.
 * see http://www.exelisvis.com/docs/ENVIHeaderFiles.html
 */
class HeaderParser {

    private final Map<String, String> header;
    private final Map<String, String> history;

    public HeaderParser(Map<String, String> header, Map<String, String> history) {
        this.header = header;
        this.history = history;
    }

    public static HeaderParser parse(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        Map<String, String> header = new LinkedHashMap<>();
        Map<String, String> history = new LinkedHashMap<>();
        Map<String, String> currentMap = header;
        for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
            line = line.trim();
            if (line.equals("history = begins")) {
                currentMap = history;
            } else if (line.equals("history = ends")) {
                currentMap = header;
            } else {
                if (line.contains("=")) {
                    String[] keyValue = line.split("=", 2);
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    if (value.startsWith("{")) {
                        value = value.substring(1).trim();
                        while (!value.endsWith("}")) {
                            String continuedLine = bufferedReader.readLine();
                            value = value + " " + continuedLine.trim();
                            value = value.trim();
                        }
                        value = value.substring(0, value.length() - 1);
                    }
                    currentMap.put(key, value);
                }
            }
        }
        return new HeaderParser(header, history);
    }

    boolean contains(String key) {
        return header.containsKey(key);
    }

    String getString(String key) {
        if (!contains(key)) {
            throw new IllegalArgumentException("Missing mandatory header key: " + key);
        }
        return header.get(key);
    }

    String getString(String key, String defaultValue) {
        return contains(key) ? getString(key) : defaultValue;
    }

    String[] getStrings(String key) {
        String v = header.get(key);
        if (v == null) {
            return new String[0];
        } else {
            String[] splits = v.split(",");
            List<String> splitsTrimmed = new ArrayList<>(splits.length);
            for (String split : splits) {
                String trimmed = split.trim();
                if (!trimmed.isEmpty()) {
                    splitsTrimmed.add(trimmed);
                }
            }
            return splitsTrimmed.toArray(new String[splitsTrimmed.size()]);
        }
    }

    int getInt(String key) {
        if (!contains(key)) {
            throw new IllegalArgumentException("Missing mandatory header key: " + key);
        }
        return Integer.parseInt(getString(key));
    }

    int getInt(String key, int defaultValue) {
        return contains(key) ? getInt(key) : defaultValue;
    }

    int[] getInts(String key) {
        String[] elems = getStrings(key);
        int[] ints = new int[elems.length];
        for (int i = 0; i < elems.length; i++) {
            ints[i] = Integer.parseInt(elems[i]);
        }
        return ints;
    }

    double getDouble(String key) {
        if (!contains(key)) {
            throw new IllegalArgumentException("Missing mandatory header key: " + key);
        }
        return Double.parseDouble(getString(key));
    }

    double[] getDoubles(String key) {
        String[] elems = getStrings(key);
        double[] doubles = new double[elems.length];
        for (int i = 0; i < elems.length; i++) {
            doubles[i] = Double.parseDouble(elems[i]);
        }
        return doubles;
    }

    Set<Map.Entry<String, String>> getHeaderEntries() {
        return header.entrySet();
    }

    Set<Map.Entry<String, String>> getHistoryEntries() {
        return history.entrySet();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Header\n");
        mapToString(sb, header);
        sb.append("History\n");
        mapToString(sb, history);
        return sb.toString();
    }

    private static void mapToString(StringBuilder sb, Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
    }

    public static void main(String[] args) throws IOException {
        HeaderParser enviHeader;
        try (FileReader fileReader = new FileReader(args[0])) {
            enviHeader = HeaderParser.parse(fileReader);
        }
        System.out.println(enviHeader);
    }
}
