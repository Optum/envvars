package com.optum.envvars.set;

import com.optum.envvars.EnvVarsException;
import com.optum.templ.MapDelegateTemplDataSource;
import com.optum.templ.TemplEngine;
import com.optum.templ.exceptions.MissingKeyTemplException;
import com.optum.templ.exceptions.TemplException;

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

        final Map<String, String> argsMap = new HashMap<>();
        for(int whichArg = 0; whichArg < args.length; whichArg++) {
            // Array index is 0-based
            final String arg = args[whichArg].trim();
            // This is the argument key syntax which is 1-based
            argsMap.put("$"+(whichArg+1), arg);
        }

        MapDelegateTemplDataSource ds = new MapDelegateTemplDataSource(argsMap);
        TemplEngine te = new TemplEngine(ds);


        // Replace keys
        for(int i=0; i<count; i++) {
            String key = keys.get(i);
            try {
                final String newValue = te.processTemplate(key);
                keys.set(i, newValue);
            } catch (MissingKeyTemplException e) {
                throw new EnvVarsException("Parameterized Inject Set \"" + setKey + "\" was invoked with argument list \"" + Arrays.asList(args) + "\" but the arguments list does not contain a use of \"" + e.getKey() + "\" for set key " + (i+1) + ": \""+ key + "\"");
            } catch (TemplException e) {
                throw new EnvVarsException(e.getMessage());
            }
        }

        // Replace values
        for(int i=0; i<count; i++) {
            String value = values.get(i);
            try {
                final String newValue = te.processTemplate(value);
                values.set(i, newValue);
            } catch (MissingKeyTemplException e) {
                throw new EnvVarsException("Parameterized Inject Set \"" + setKey + "\" was invoked with argument list \"" + Arrays.asList(args) + "\" but the arguments list does not contain a use of \"" + e.getKey() + "\" for set value " + (i+1) + ": \""+ value + "\"");
            } catch (TemplException e) {
                throw new EnvVarsException(e.getMessage());
            }
        }

        // Glue it back together
        for(int i=0; i<count; i++) {
            String key = keys.get(i);
            String value = values.get(i);
            results.put(key, value);
        }
        return results;
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
            final String argHolder = "{{$" + (whichArg) + "}}";
            final String uppercaseArgHolder = "{{^$" + (whichArg) + "}}";
            for(int i=0; i<values.size(); i++) {
                String value = values.get(i);
                if (value.contains(argHolder)) {
                    foundOneOfThisArg = true;
                    String newValue = value.replace(argHolder, arg);
                    values.set(i, newValue);
                } else if (value.contains(uppercaseArgHolder)) {
                    foundOneOfThisArg = true;
                    String newValue = value.replace(uppercaseArgHolder, arg.toUpperCase());
                    values.set(i, newValue);
                }
            }
            if (!foundOneOfThisArg) {
                throw new EnvVarsException("Parameterized Inject Set \"" + key + "\" was invoked with argument list \"" + Arrays.asList(args) + "\" but the definition does not contain a use of \"" + argHolder + "\".  Set values are: " + values);
            }
            whichArg++;
        }
        return values;
    }

    private List<String> processSimpleInjectSetReference(String simpleKey) {
        return injectSets.get(simpleKey);
    }

}
