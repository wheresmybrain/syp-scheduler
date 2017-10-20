package com.wheresmybrain.syp.scheduler.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Assorted Java-specific utility methods.
 *
 * @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public class JavaUtils {

    private static final String EOL = System.getProperty("line.separator");

    /**
     * Returns Exception stacktrace as a String.
     */
    public static String getStacktrace(Throwable t) {
        StringWriter stacktraceHolder = new StringWriter();
        t.printStackTrace(new PrintWriter(stacktraceHolder));
        return stacktraceHolder.toString ();
    }

    /**
     * Returns the root cause for the specified Throwable, even if
     * it is several layers deep.
     */
    public static Throwable getRootException(Throwable t) {
        if (t.getCause() != null) {
            return getRootException(t.getCause());
        } else {
            return t;
        }
    }

    /**
     * Returns String representation of Map in single-line format:
     * <pre>
     *     {key1=value1, key2=value2, ..., keyN=valueN}
     * </pre>
     * or multi-line format:
     * <pre>
     *     {key1=value1
     *      key2=value2,
     *      ...,
     *      keyN=valueN}
     * </pre>
     * <p/>
     * Uses the toString() method to print object values.
     *
     * @param map Map of key-value pairs to print to String
     * @param singleLine prints the Map contents on a single line if set to "true" or
     *   each key-value pair prints on its own line if "false".
     * @param indent if printing on multi-line (singleLine="false") then this indent
     *   is applied to the beginning of every line. This can be set to null or
     *   empty-string for no indent.
     * @return String representation of the map. If the map is null, then treats
     *   it like an empty map.
     */
    public static String printMap(Map<?,?> map, boolean singleLine, String indent) {
        StringBuffer buf = new StringBuffer();
        if (!singleLine && indent != null) buf.append(indent);
        buf.append("{");
        if (map != null) {
            Iterator<?> keys = map.keySet().iterator();
            for (int i=0; keys.hasNext(); i++) {
                if (i > 0) {
                    if (singleLine) {
                        buf.append(", ");
                    } else if (i > 0) {
                        buf.append(EOL).append(" ");
                        if (indent != null) buf.append(indent);
                    }
                }
                Object key = keys.next();
                buf.append(key).append("=").append(map.get(key));
            }
        }
        buf.append("}");
        return buf.toString();
    }

    /**
     * Returns String representation of Collection in single-line format:
     * <pre>
     *     {value1, value2, ..., valueN}
     * </pre>
     * or multi-line format:
     * <pre>
     *     {value1
     *      value2,
     *      ...,
     *      valueN}
     * </pre>
     * <p/>
     * Uses the toString() method to print object values.
     *
     * @param collection Collection to print to String
     * @param singleLine prints the Collection contents on a single line if set to "true"
     *   or each value prints on its own line if "false".
     * @param indent if printing on multi-line (singleLine="false") then this indent
     *   is applied to the beginning of every line. This can be set to null or
     *   empty-string for no indent.
     * @return String representation of the collection. If the collection is null, then treats
     *   it like an empty collection.
     */
    public static String printCollection(Collection<?> collection, boolean singleLine, String indent) {
        StringBuffer buf = new StringBuffer();
        if (!singleLine && indent != null) buf.append(indent);
        buf.append("{");
        if (collection != null) {
            Iterator<?> values = collection.iterator();
            for (int i=0; values.hasNext(); i++) {
                if (i > 0) {
                    if (singleLine) {
                        buf.append(", ");
                    } else if (i > 0) {
                        buf.append(EOL).append(" ");
                        if (indent != null) buf.append(indent);
                    }
                }
                buf.append(values.next());
            }
        }
        buf.append("}");
        return buf.toString();
    }

    /**
     * Converts comma-delimited list of Strings to an array.
     *
     * @param commaDelimited comma-delimited list of Strings. Can be one String with no
     *   commas, or null. Empty String or null returns empty array.
     * @return array of String values, which could be empty if the comma-delimited
     *   String was null or empty (including whitespace).
     */
    public static String[] convertFromCommaDelimitedString(String commaDelimited) {
        if (commaDelimited != null && commaDelimited.trim().length() > 0) {
            String[] array = commaDelimited.split(",");
            for (int i=0; i<array.length; i++) {
                if (array[i] != null) array[i] = array[i].trim();
            }
            return array;
        } else {
            // empty String or null returns empty array
            return new String[0];
        }
    }

}
