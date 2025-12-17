// Base                 : Conveyor Sortaion Controller
// Class                : IniConfigLoader Class
// Programmer           : Giresh
// Release Date         : 2025-06-19
// Revision Number      : 1.0
// Description          : code module 
// ================================================================================
// Change history 
// Rev.     Date         Programmer    Description                               
// --------------------------------------------------------------------------------
//01.00    2025.06.19    Giresh         Initial Version



package platform.core.config;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IniConfigLoader {

    private final Map<String, Map<String, String>> config = new HashMap<>();
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    public IniConfigLoader(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String section = ""; // "" represents the global section
            config.putIfAbsent(section, new HashMap<>());

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) continue;

                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line.substring(1, line.length() - 1).trim();
                    config.putIfAbsent(section, new HashMap<>());
                } else if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim();
                    String value = resolvePlaceholders(parts[1].trim());
                    config.get(section).put(key, value);
                }
            }
        }
    }

    public String get(String section, String key) {
        Map<String, String> sectionMap = config.get(section);
        return sectionMap != null ? sectionMap.get(key) : null;
    }

    public int getInt(String section, String key) {
        return Integer.parseInt(get(section, key));
    }

    public String getGlobal(String key) {
        return get("", key);
    }

    private String resolvePlaceholders(String value) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuffer resolved = new StringBuffer();

        while (matcher.find()) {
            String placeholderKey = matcher.group(1);
            String replacement = findReplacement(placeholderKey);
            matcher.appendReplacement(resolved, replacement != null ? Matcher.quoteReplacement(replacement) : "");
        }

        matcher.appendTail(resolved);
        return resolved.toString();
    }

    private String findReplacement(String key) {
        // 1. Try from global section
        String fromGlobal = getGlobal(key);
        if (fromGlobal != null) return fromGlobal;

        // 2. Try from system environment (optional)
        String fromEnv = System.getenv(key);
        if (fromEnv != null) return fromEnv;

        // 3. If not found, warn or return the original placeholder
        return null; // or return "${" + key + "}" to leave it unresolved
    }
}
