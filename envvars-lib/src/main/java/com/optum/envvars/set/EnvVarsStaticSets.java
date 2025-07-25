package com.optum.envvars.set;

import com.optum.envvars.EnvVarsException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnvVarsStaticSets {
    public final Map<String, List<String>> injectSets;
    public final Map<String, Map<String, String>> defineSets;

    public EnvVarsStaticSets() {
        this.injectSets = new HashMap<>();
        this.defineSets = new HashMap<>();
    }

    public EnvVarsStaticSets(Map<String, List<String>> injectSets, Map<String, Map<String, String>> defineSets) {
        this.injectSets = (injectSets!=null) ? injectSets : new HashMap<>();
        this.defineSets = (defineSets!=null) ? defineSets : new HashMap<>();
    }

    public void add(EnvVarsStaticSets other) {
        injectSets.putAll(other.injectSets);
        defineSets.putAll(other.defineSets);
    }

    public Map<String, String> expandDefineSetReferences(List<String> input, String errorContext) throws EnvVarsException {
        Map<String, String> results = new HashMap<>();

        for(Object o : input) {
            if (o == null) {
                throw new EnvVarsException("Null key not allowed.");
            }

            if (!(o instanceof String)) {
                throw new EnvVarsException("Invalid key "+o.toString() + " in " + errorContext + ".  It must be a String but is not.");
            }
            String key = (String)o;
            Map<String, String> setValues = processDefineSetReference(key);
            if (setValues==null) {
                throw new EnvVarsException("Detected a define_set reference: \"" + key + "\" but that set does not exist.  Empty sets are allowed, but every set must be defined.");
            } else {
                results.putAll(setValues);
            }
        }
        return results;
    }

    private Map<String, String> processDefineSetReference(String key) throws EnvVarsException {
        final Matcher matcher = pattern.matcher(key);
        if (matcher.matches()) {
            final String realKey = matcher.group(1);
            final String delimitedArgList = matcher.group(2);
            final String[] args = delimitedArgList.split(",");
            return processParameterizedDefineSetReferences(realKey, args);
        } else {
            return processSimpleDefineSetReference(key);
        }
    }

    private Map<String, String> processParameterizedDefineSetReferences(String setKey, String[] args) throws EnvVarsException {
        Map<String, String> results = new HashMap<>();

        /**
         * We need to mutate keys... which doesn't play nice in Maps (clearly).
         * So we rip apart the twisted pair of key,value into two ordered lists.
         * Manipulate away IN PLACE.
         * Then glue them back together into the final results.
         */
        final List<String> keys = new ArrayList<>();
        final List<String> values = new ArrayList<>();
        final Map<String, String> source = defineSets.get(setKey);
        if (source==null) {
            throw new EnvVarsException("Unable to find a define_set for key " + setKey);
        }
        final int count = source.size();

        for(Map.Entry<String, String> entry : source.entrySet()) {
            keys.add(entry.getKey());
            values.add(entry.getValue());
        }

        int whichArg = 1;
        for(String rawarg : args) {
            String arg = rawarg.trim();
            boolean foundOneOfThisArg = false;

            if (argSwap(whichArg, arg, keys)) {
                foundOneOfThisArg = true;
            }

            if (argSwap(whichArg, arg, values)) {
                foundOneOfThisArg = true;
            }

            if (!foundOneOfThisArg) {
                throw new EnvVarsException("Parameterized Inject Set \"" + setKey + "\" was invoked with argument list \"" + Arrays.asList(args) + "\" but the definition does not contain a use of \"$" + whichArg + "\".  Set values are: " + source);
            }
            whichArg++;
        }

        // Glue it back together
        for(int i=0; i<count; i++) {
            String key = keys.get(i);
            String value = values.get(i);
            results.put(key, value);
        }
        return results;
    }

    private boolean argSwap(int argNumber, String value, List<String> strings) {
        boolean foundOneOfThisArg = false;

        final String DELIMITERS = "[ ,.;:?&@#/()<>_\\-\\\\|]";
        final Pattern PREFIX_PATTERN = Pattern.compile("(" + DELIMITERS + "*)([\\^~]?)");
        final Pattern SUFFIX_PATTERN = Pattern.compile("(" + DELIMITERS + "*)");
        final String variable = "\\$" + argNumber;
        final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{" + PREFIX_PATTERN + "(" + variable + ")" + SUFFIX_PATTERN + "}}");

        // Replace keys
        for(int i=0; i<strings.size(); i++) {
            String key = strings.get(i);
            String intermediateValue = key;
            Matcher matcher = TEMPLATE_PATTERN.matcher(intermediateValue);
            while (matcher.find()) {
                foundOneOfThisArg = true;
                String prefix = matcher.group(1);
                String caseShift = matcher.group(2);
                // String templateVar = matcher.group(3);
                String suffix = matcher.group(4);
                int start = matcher.start();
                int end = matcher.end();
                boolean toUpper = "^".equals(caseShift);
                boolean toLower = "~".equals(caseShift);
                String replacement;
                if (value.isEmpty()) {
                    replacement = "";
                } else {
                    StringBuilder sb = new StringBuilder(prefix.length() + value.length() + suffix.length());
                    sb.append(prefix);
                    if (toUpper) {
                        sb.append(value.toUpperCase().replace("-", "_").replace(".", "_"));
                    } else if (toLower) {
                        sb.append(value.toLowerCase().replace("_", "-"));
                    } else {
                        sb.append(value);
                    }
                    sb.append(suffix);
                    replacement = sb.toString();
                }
                intermediateValue = intermediateValue.substring(0, start) + replacement + intermediateValue.substring(end);
                matcher = TEMPLATE_PATTERN.matcher(intermediateValue);
            }
            strings.set(i, intermediateValue);
        }
        return foundOneOfThisArg;
    }

    private Map<String, String> processSimpleDefineSetReference(String simpleKey) {
        return defineSets.get(simpleKey);
    }

    static public class ExpandResults {
        public final List<String> lines = new ArrayList<>();
        public final List<String> optionalLines = new ArrayList<>();
    }

    public ExpandResults expandInjectSetReferences(List<?> input, String errorContext) throws EnvVarsException {
        ExpandResults results = new ExpandResults();

        for(Object o : input) {
            if (o == null) {
                throw new EnvVarsException("Null key not allowed.");
            }

            if (!(o instanceof String)) {
                throw new EnvVarsException("Invalid key "+o.toString() + " in " + errorContext + ".  It must be a String but is not.");
            }
            String value = (String)o;

            if (value.startsWith("*")) {
                String key = value.substring(1);
                supportRecursiveInjectSets(results, key, 0);
            } else {
                results.lines.add(value);
            }
        }
        return results;
    }

    private void supportRecursiveInjectSets(ExpandResults results, String key, int recursiveLimit) throws EnvVarsException {
        List<String> setValues = processInjectSetReference(key);
        if (setValues==null) {
            throw new EnvVarsException("Detected an inject_set reference: \"*" + key + "\" but that set does not exist.  Empty sets are allowed, but every set must be defined.");
        } else {
            for (String setValue : setValues) {
                if (setValue.startsWith("?")) {
                    final String optionalKey = setValue.substring(1);
                    results.lines.add(optionalKey);
                    results.optionalLines.add(optionalKey);
                } else if (setValue.startsWith("*")) {
                    if (recursiveLimit>=10)
                        throw new EnvVarsException("Detected a recursive inject_set reference: \"" + setValue + "\" beyond the recursive depth limit.  Recursion is only allow to depth 10.");

                    String subkey = setValue.substring(1);
                    supportRecursiveInjectSets(results, subkey, (recursiveLimit+1));
                } else {
                    results.lines.add(setValue);
                }
            }
        }
    }

    private final String regex = "([^<>]+)<(.+)>";
    private final Pattern pattern = Pattern.compile(regex);

    private List<String> processInjectSetReference(String key) throws EnvVarsException {
        final Matcher matcher = pattern.matcher(key);
        if (matcher.matches()) {
            final String realKey = matcher.group(1);
            final String delimitedArgList = matcher.group(2);
            final String[] args = delimitedArgList.split(",");
            return processParameterizedInjectSetReferences(realKey, args);
        } else {
            return processSimpleInjectSetReference(key);
        }
    }

    private List<String> processParameterizedInjectSetReferences(String key, String[] args) throws EnvVarsException {
        List<String> set = injectSets.get(key);
        if (set==null) {
            throw new EnvVarsException("Parameterized Inject Set \"" + key + "\" was not found.");
        }
        List<String> values = new ArrayList<>(set);
        int whichArg = 1;
        for(String rawarg : args) {
            String arg = rawarg.trim();
            boolean foundOneOfThisArg = false;

            if (argSwap(whichArg, arg, values)) {
                foundOneOfThisArg = true;
            }

            if (!foundOneOfThisArg) {
                throw new EnvVarsException("Parameterized Inject Set \"" + key + "\" was invoked with argument list \"" + Arrays.asList(args) + "\" but the definition does not contain a use of \"$" + whichArg + "\".  Set values are: " + values);
            }
            whichArg++;
        }
        return values;
    }

    private List<String> processSimpleInjectSetReference(String simpleKey) {
        return injectSets.get(simpleKey);
    }

}
