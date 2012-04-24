package org.esa.beam.util.io;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Utility class that can be used to "glob" the filesystem using (extended) Unix-style wildcards:
 * <ol>
 * <li>'**' recursively matches directories and sub-directories;</li>
 * <li>'*' matches parts or entire path names;</li>
 * <li>'?' matches single characters;</li>
 * <li>and character ranges expressed with '[]' will be correctly matched.</li>
 * </ol>
 * However, no tilde expansion is done.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 * @see <a href="http://ant.apache.org/manual/dirtasks.html#patterns">Patterns</a> in the Ant documentation
 */
public class WildcardMatcher {

    private final Pattern pattern;
    private final boolean windowsFs;

    public WildcardMatcher(String wildcard) {
        this(wildcard, System.getProperty("os.name").contains("Win"));
    }

    WildcardMatcher(String wildcard, boolean windowsFs) {
        this.pattern = Pattern.compile(wildcardToRegexp(wildcard.trim(), windowsFs));
        this.windowsFs = windowsFs;
    }

    boolean isWindowsFs() {
        return windowsFs;
    }

    public static File[] glob(String filePattern) throws IOException {
        Set<File> fileSet = new TreeSet<File>();
        glob(filePattern, fileSet);
        return fileSet.toArray(new File[fileSet.size()]);
    }

    public static void glob(String filePattern, Set<File> fileSet) throws IOException {
        final File file = new File(filePattern);
        if (file.exists()) {
            fileSet.add(file.getCanonicalFile());
            return;
        }
        WildcardMatcher matcher = new WildcardMatcher(filePattern);
        String basePath = matcher.getBasePath(filePattern);
        File dir = new File(basePath).getCanonicalFile();
        int validPos = dir.getPath().indexOf(basePath);
        collectFiles(matcher, validPos, dir, fileSet);
    }

    private static void collectFiles(WildcardMatcher matcher, int validPos, File dir, Set<File> fileSet) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException(String.format("Failed to access directory '%s'", dir));
        }
        for (File file : files) {
            String text;
            if (validPos > 0) {
                text = file.getPath().substring(validPos);
            } else {
                text = file.getPath();
            }
            if (matcher.matches(text)) {
                fileSet.add(file);
            }
            if (file.isDirectory()) {
                collectFiles(matcher, validPos, file, fileSet);
            }
        }
    }

    String getBasePath(String filePattern) {
        if (isWindowsFs()) {
            filePattern = filePattern.replace("\\", "/");
        }
        String basePath = filePattern.startsWith("/") ? "/" : "";
        String[] parts = filePattern.split("/");
        for (int i = 0; i < parts.length && !parts[i].equals("**") && !parts[i].contains("*") && !parts[i].contains("?"); i++) {
            if (!parts[i].isEmpty()) {
                basePath += parts[i];
                if (i < parts.length - 1) {
                    basePath += "/";
                }
            }
        }
        return new File(basePath).getPath();
    }

    String getRegex() {
        return pattern.pattern();
    }

    static String wildcardToRegexp(String wildcard, boolean windowsFs) {

        String s = wildcard;

        if (windowsFs) {
            s = normaliseWindowsPath(s);
        }

        s = s.replace("/**/", "_%SLASHSTARSTARSLASH%_");
        s = s.replace("/**", "_%SLASHSTARSTAR%_");
        s = s.replace("**/", "_%STARSTARSLASH%_");
        s = s.replace("*", "_%STAR%_");
        s = s.replace("?", "_%QUOTE%_");

        String[] metas = new String[]{"\\", "|", "^", "$", "+", ".", "(", ")", "{", "}", "<", ">"};
        for (String meta : metas) {
            s = s.replace(meta, "\\" + meta);
        }

        s = s.replace("_%SLASHSTARSTARSLASH%_", "((/.*/)?|/)");
        s = s.replace("_%SLASHSTARSTAR%_", "(/.*)?");
        s = s.replace("_%STARSTARSLASH%_", "(.*/)?");
        s = s.replace("_%STAR%_", "[^/:]*");
        s = s.replace("_%QUOTE%_", ".");

        return s;
    }

    private static String normaliseWindowsPath(String s) {
        return s.toLowerCase().replace("\\", "/");
    }

    public boolean matches(String text) {
        if (windowsFs) {
            text = normaliseWindowsPath(text);
        }
        return pattern.matcher(text).matches();
    }

}
