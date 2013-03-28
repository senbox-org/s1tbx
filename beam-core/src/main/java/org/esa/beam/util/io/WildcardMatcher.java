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
 * @see <a href="http://ant.apache.org/manual/dirtasks.html#patterns">Patterns</a> in the Ant documentation
 * @since BEAM 4.10
 */
public class WildcardMatcher {

    private final Pattern pattern;
    private final boolean windowsFs;

    public WildcardMatcher(String wildcard) {
        this(wildcard, isWindowsOs());
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
        final File patternFile = new File(filePattern);
        if (patternFile.exists()) {
            fileSet.add(patternFile.getCanonicalFile());
            return;
        }
        boolean windowsOs = isWindowsOs();
        String[] patternSplit = splitBasePath(filePattern, windowsOs);
        String basePath = patternSplit[0];
        String patternPath = patternSplit[1];
        if (patternPath.isEmpty()) {
            // no pattern given, but no file exist
            return;
        }
        File canonicalBaseFile = new File(basePath).getCanonicalFile();

        String newpattern = canonicalBaseFile.getPath() + "/" + patternPath;
        WildcardMatcher matcher = new WildcardMatcher(newpattern);
        collectFiles(matcher, canonicalBaseFile, fileSet);

    }

    private static void collectFiles(WildcardMatcher matcher,  File dir, Set<File> fileSet) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException(String.format("Failed to access directory '%s'", dir));
        }
        for (File file : files) {
            if (matcher.matches(file.getCanonicalPath())) {
                fileSet.add(file);
            }
            if (file.isDirectory()) {
                collectFiles(matcher, file, fileSet);
            }
        }
    }

    static String[] splitBasePath(String filePattern, boolean iswindows) {
        if (iswindows) {
            filePattern = filePattern.replace("\\", "/");
        }
        String basePath = filePattern.startsWith("/") ? "/" : "";
        String[] parts = filePattern.split("/");
        int firstPatternIndex = 0;
        for (int i = 0; i < parts.length && !containsWildcardChar(parts[i]); i++) {
            if (!parts[i].isEmpty()) {
                basePath += parts[i];
                if (i < parts.length - 1) {
                    basePath += "/";
                }
                firstPatternIndex = i + 1;
            }
        }
        String patterPath = "";
        for (int i = firstPatternIndex; i < parts.length ; i++) {
            patterPath += parts[i];
            if (i < parts.length - 1) {
                patterPath += "/";
            }
        }
        return new String[] {basePath, patterPath};
    }

    private static boolean containsWildcardChar(String part) {
        return part.equals("**") || part.contains("*") || part.contains("?");
    }

    String getRegex() {
        return pattern.pattern();
    }

    static String wildcardToRegexp(String wildcard, boolean windowsFs) {

        String s = resolvePath(wildcard, windowsFs);

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

    public boolean matches(String text) {
        return pattern.matcher(resolvePath(text, windowsFs)).matches();
    }

    private static boolean isWindowsOs() {
        return System.getProperty("os.name").contains("Win");
    }

    private static String resolvePath(String text, boolean windowsFs) {
        if (windowsFs) {
            text = text.toLowerCase().replace("\\", "/");
        }
        // The functionality of this method should be extended so
        // that also '.' and '..' are removed (or better said resolved).
        while (text.startsWith("./")) {
            text = text.substring(2);
        }
        return text;
    }

}
