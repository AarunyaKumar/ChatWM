package com.thefunone.chatwm.networking;

import com.thefunone.chatwm.ChatMsg;
import com.thefunone.chatwm.ChatMsgType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Android alternative to JEP 290 ObjectInputFilter.
 * Enforces a strict whitelist of allowed classes before deserialization,
 * and normalizes class descriptors to prevent serialVersionUID mismatch errors.
 */
public class FilteredObjectInputStream extends ObjectInputStream {

    private final List<String> allowedPrefixes = new ArrayList<>();
    private final Set<String> allowedClasses = new HashSet<>();
    private boolean rejectRemaining = true;

    // Standard Java primitive arrays and base types always safe to deserialize
    private static final Set<String> DEFAULT_SAFE_BASE_CLASSES = new HashSet<>(Arrays.asList(
            "[B", // byte[]
            "[C", // char[]
            "[I", // int[]
            "[J", // long[]
            "[Z", // boolean[]
            "java.lang.String",
            "java.lang.Number",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Boolean",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Character",
            "java.lang.Enum"
    ));

    /**
     * Default constructor with sensible defaults for ChatWM:
     * Permits com.thefunone.chatwm.** and standard base types.
     */
    public FilteredObjectInputStream(InputStream in) throws IOException {
        this(in, "com.thefunone.chatwm.**;java.base/*;!*");
    }

    /**
     * Constructor accepting a pattern string identical to JEP 290 format,
     * e.g.: "com.thefunone.chatwm.ChatMsg;com.thefunone.chatwm.networking.*;java.base/*;!*"
     */
    public FilteredObjectInputStream(InputStream in, String filterPattern) throws IOException {
        super(in);
        initFilter(filterPattern);
    }

    /**
     * Constructor accepting an array of allowed patterns.
     */
    public FilteredObjectInputStream(InputStream in, String[] allowedPatterns) throws IOException {
        super(in);
        StringBuilder sb = new StringBuilder();
        if (allowedPatterns != null) {
            for (String pattern : allowedPatterns) {
                if (pattern != null && !pattern.trim().isEmpty()) {
                    sb.append(pattern.trim()).append(";");
                }
            }
        }
        sb.append("java.base/*;!*");
        initFilter(sb.toString());
    }

    private void initFilter(String patternString) {
        allowedClasses.addAll(DEFAULT_SAFE_BASE_CLASSES);

        if (patternString == null || patternString.trim().isEmpty()) {
            allowedPrefixes.add("com.thefunone.chatwm.");
            return;
        }

        String[] parts = patternString.split(";");
        for (String rawPart : parts) {
            String part = rawPart.trim();
            if (part.isEmpty()) continue;

            if (part.equals("!*")) {
                rejectRemaining = true;
            } else if (part.equals("java.base/*")) {
                allowedPrefixes.add("java.lang.");
                allowedPrefixes.add("java.util.");
            } else if (part.endsWith(".**")) {
                String prefix = part.substring(0, part.length() - 2);
                allowedPrefixes.add(prefix);
            } else if (part.endsWith(".*")) {
                String prefix = part.substring(0, part.length() - 1);
                allowedPrefixes.add(prefix);
            } else {
                allowedClasses.add(part);
            }
        }
    }

    /**
     * Overrides descriptor resolution to match local class definitions,
     * preventing InvalidClassException when serialVersionUID differs across platforms.
     */
    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass streamDesc = super.readClassDescriptor();
        Class<?> localClass = resolveClass(streamDesc);
        if (localClass != null) {
            ObjectStreamClass localDesc = ObjectStreamClass.lookup(localClass);
            if (localDesc != null) {
                return localDesc;
            }
        }
        return streamDesc;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String className = desc.getName();

        if (!isClassAllowed(className)) {
            throw new InvalidClassException("Unauthorized deserialization attempt: " + className);
        }

        // Seamless fallback remapping for cross-version compatibility
        if ("com.thefunone.chatwm.networking.ConnectionMsg".equals(className)) {
            return ConnectionMsg.class;
        }
        if ("com.thefunone.chatwm.networking.DataMsgType".equals(className)) {
            return DataMsgType.class;
        }
        if ("com.thefunone.chatwm.networking.ChatMsg".equals(className) || "ChatMsg".equals(className)) {
            return ChatMsg.class;
        }
        if ("com.thefunone.chatwm.networking.ChatMsgType".equals(className) || "ChatMsgType".equals(className)) {
            return ChatMsgType.class;
        }

        return super.resolveClass(desc);
    }

    private boolean isClassAllowed(String className) {
        if (allowedClasses.contains(className)) {
            return true;
        }

        if (className.startsWith("[")) {
            int index = className.lastIndexOf('[');
            if (index >= 0 && index < className.length() - 1) {
                if (className.charAt(index + 1) == 'L' && className.endsWith(";")) {
                    className = className.substring(index + 2, className.length() - 1);
                }
            }
        }

        for (String prefix : allowedPrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }

        return !rejectRemaining;
    }
}