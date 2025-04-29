package io.konveyor.tackle.core.internal.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.IPackageFragmentRoot;
import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

public class OpenSourceLibraryExclusionManager {
    // TODO (pgaikwad): set this file path at build time
    private static final String MAVEN_INDEX_FILE = "/usr/local/etc/maven.default.index";
    private static volatile OpenSourceLibraryExclusionManager instance;

    private final List<Pattern> exclusionPatterns = new ArrayList<>();
    private final ConcurrentHashMap<String, Boolean> exclusionCache = new ConcurrentHashMap<>();

    private OpenSourceLibraryExclusionManager() {
        loadExclusionPatterns();
    }

    public static OpenSourceLibraryExclusionManager getInstance() {
        if (instance == null) {
            synchronized (OpenSourceLibraryExclusionManager.class) {
                if (instance == null) {
                    instance = new OpenSourceLibraryExclusionManager();
                }
            }
        }
        return instance;
    }

    private void loadExclusionPatterns() {
        try (BufferedReader reader = new BufferedReader(new FileReader(MAVEN_INDEX_FILE, StandardCharsets.UTF_8))) {
            List<String> patterns = reader.lines()
                    .filter(line -> !line.trim().isEmpty())
                    .collect(Collectors.toList());

            for (String patternStr : patterns) {
                try {
                    String prefix = ".*";
                    exclusionPatterns.add(Pattern.compile(prefix + patternStr));
                } catch (Exception e) {
                    logInfo("Invalid exclusion pattern: " + patternStr);
                }
            }
        } catch (IOException e) {
            logInfo("Failed to load maven index " + e.toString());
        }
    }

    public boolean shouldExcludeLibrary(String libraryPath) {
        return exclusionCache.computeIfAbsent(libraryPath, path -> {
            for (Pattern pattern : exclusionPatterns) {
                if (pattern.matcher(path).matches()) {
                    return true;
                }
            }
            return false;
        });
    }

    public boolean shouldExcludePackageRoot(IPackageFragmentRoot root) {
        // not a library
        if (!root.isArchive()) {
            return false;
        }
        try {
            String path = root.getPath().toString();
            return shouldExcludeLibrary(path);
        } catch (Exception e) {
            return false;
        }
    }
}