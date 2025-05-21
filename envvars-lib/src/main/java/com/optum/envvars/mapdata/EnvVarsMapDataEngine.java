package com.optum.envvars.mapdata;

import com.optum.envvars.EnvVarsException;
import com.optum.envvars.schema.EnvVarsSchemaReader;
import com.optum.envvars.schema.EnvVarsStaticSchema;
import com.optum.envvars.schema.EnvVarsVariableFilters;
import com.optum.envvars.schema.EnvVarsVariableFilter;
import com.optum.envvars.schema.EnvVarsRuntimeSelectors;
import com.optum.envvars.schema.VariableNameMustMatchException;
import com.optum.envvars.schema.VariableNameMustNotMatchException;
import com.optum.envvars.schema.VariableNameNotOneOfException;
import com.optum.envvars.set.EnvVarsStaticSets;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * This class performs the heavy-lifting of converting a Java Map into PreEnvVarsData.  Processing is controlled by
 * a Node List, which contains the required schema definitions (contexts) and this specfic invocations
 * arguments (selectors).  The processing is recursive - to the degree specified by the Nodes - but not unboundedly
 * recursive.
 * The code tries to guard against bad data as much as possible, throwing exceptions with [hopefully] helpful messages
 * for fixing the bad data.
 */
public class EnvVarsMapDataEngine {
    private final EnvVarsStaticSchema envVarsStaticSchema;
    private final EnvVarsStaticSets envVarsStaticSets;
    private final Map rawSourceMap;

    public EnvVarsMapDataEngine(Map rawSourceMap) {
        this.rawSourceMap = rawSourceMap;
        this.envVarsStaticSchema = null;
        this.envVarsStaticSets = null;
    }

    public EnvVarsMapDataEngine(EnvVarsSchemaReader envVarsSchemaReader, EnvVarsStaticSets envVarsStaticSets, Map rawSourceMap) throws EnvVarsException {
        this.rawSourceMap = rawSourceMap;

        final Object schemaAsObject = rawSourceMap.get("schema");
        if (envVarsSchemaReader!=null && schemaAsObject!=null) {
            final String schemaAsString = schemaAsObject.toString();
            final URL schemaUrl;
            try {
                schemaUrl = new URL(schemaAsString);
            } catch (MalformedURLException e) {
                throw new EnvVarsException("Unable to load EnvVars schema from: " + schemaAsString);
            }
            this.envVarsStaticSchema = envVarsSchemaReader.buildFromURL(schemaUrl);
        } else {
            this.envVarsStaticSchema = null;
        }

        if (envVarsStaticSets!=null) {
            this.envVarsStaticSets = envVarsStaticSets;
        } else {
            this.envVarsStaticSets = new EnvVarsStaticSets();
        }
    }

    /**
     * This is the main [only] public method.
     */
    public EnvVarsMapData get(EnvVarsRuntimeSelectors envVarsRuntimeSelectors) throws EnvVarsException {
        EnvVarsMapData results = new EnvVarsMapData(envVarsStaticSets);
        for(EnvVarsRuntimeSelectors.Node node : envVarsRuntimeSelectors.getRootNodesList()) {
            getContext(node, rawSourceMap, results, node.contextRequired);
        }

        if (envVarsStaticSchema!=null) {
            EnvVarsVariableFilters envVarsVariableFilters = envVarsStaticSchema.getEnvVarsVariableFilters();
            if (envVarsVariableFilters != null) {
                validateKeys(results, envVarsVariableFilters);
            }
        }
        return results;
    }

    public void validateKeys(EnvVarsMapData results, EnvVarsVariableFilters envVarsVariableFilters) throws EnvVarsException {
        // By using a tree map indexed by variable name, the error message is SORTED and therefore WELL-DEFINED (REPEATABLE).
        final TreeMap<String, String> badKeys = new TreeMap<>();

        Collection<String> allKeys = new HashSet<>();
        allKeys.addAll(results.getEnvVarsDefineSet().keySet());
        allKeys.addAll(results.getEnvVarsDefineSecretSet().keySet());

        for(String k: allKeys) {
            try {
                envVarsVariableFilters.confirmSuitability(k);
            } catch (VariableNameMustMatchException e) {
                badKeys.put(e.getVariableName(), e.getVariableName() + " must match " + e.getOffendingRule().getName() + ". (" + e.getOffendingRule().getDescription() + ") - " + e.getOffendingRule().getUsage());
            } catch (VariableNameMustNotMatchException e) {
                badKeys.put(e.getVariableName(), e.getVariableName() + " must not match " + e.getOffendingRule().getName() + ". (" + e.getOffendingRule().getDescription() + ") - " + e.getOffendingRule().getUsage());
            } catch (VariableNameNotOneOfException e) {
                StringBuilder sb = new StringBuilder();
                sb.append(e.getVariableName()).append(" must match one of these, but matches none: ");
                for(EnvVarsVariableFilter allow : e.getOneOf()) {
                    sb.append(allow.getName()).append(". (").append(allow.getDescription()).append(") - ").append(allow.getUsage());
                }
                badKeys.put(e.getVariableName(), sb.toString());
            }
        }
        if (!badKeys.isEmpty()) {
            throw new EnvVarsException("The following variable names are not suitable:\n" + String.join("\n", badKeys.values()));
        }
    }

    public enum DefaultProcessingPolicy {
        SUPPORTED {
            protected void process(EnvVarsMapDataEngine engine, Map currentScope, EnvVarsMapData envVarsData, EnvVarsRuntimeSelectors.Node node) throws EnvVarsException {
                engine.addEnvVarsForDefaultSelector(currentScope, envVarsData, node, node.RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR, false);
            }
        },
        REQUIRED {
            protected void process(EnvVarsMapDataEngine engine, Map currentScope, EnvVarsMapData envVarsData, EnvVarsRuntimeSelectors.Node node) throws EnvVarsException {
                engine.addEnvVarsForDefaultSelector(currentScope, envVarsData, node, node.RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR, true);
            }
        },
        FORBIDDEN {
            protected void process(EnvVarsMapDataEngine engine, Map currentScope, EnvVarsMapData envVarsData, EnvVarsRuntimeSelectors.Node node) throws EnvVarsException {
                if (currentScope.containsKey(node.RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR)) {
                    throw new EnvVarsException("Node "+node.context+" contains a default element, but default elements are forbidden.");
                }
            }
        },
        IGNORED {
            protected void process(EnvVarsMapDataEngine engine, Map currentScope, EnvVarsMapData envVarsData, EnvVarsRuntimeSelectors.Node node) throws EnvVarsException
            {
                // For IGNORED, don't check anything and don't do anything.
            }
        };
        protected abstract void process(EnvVarsMapDataEngine engine, Map currentScope, EnvVarsMapData envVarsData, EnvVarsRuntimeSelectors.Node node) throws EnvVarsException;
    }

    void addEnvVarsForSelector(Map previousScope, EnvVarsMapData envVarsData, EnvVarsRuntimeSelectors.Node node) throws EnvVarsException {
        Map selectorScope = addEnvVarsForSelectorImpl(previousScope, envVarsData, node, node.selector, node.selectorRequired);
        if (selectorScope != null) {
            // Then we [recursively] perform any subNode processing FROM WITHIN THE SELECTOR'S CONTEXT
            for(EnvVarsRuntimeSelectors.Node subNode: node.subNodeList) {
                getContext(subNode, selectorScope, envVarsData, subNode.contextRequired);
            }
        }
    }

    void addEnvVarsForDefaultSelector(Map previousScope, EnvVarsMapData envVarsData, EnvVarsRuntimeSelectors.Node node, String selector, boolean required) throws EnvVarsException {
        Map selectorScope = addEnvVarsForSelectorImpl(previousScope, envVarsData, node, selector, required);
        if (selectorScope != null) {
            // Then we [recursively] perform any subNode processing FROM WITHIN THE SELECTOR'S CONTEXT
            for(EnvVarsRuntimeSelectors.Node subNode: node.subNodeList) {
                getContext(subNode, selectorScope, envVarsData, false);
            }
        }
    }

    /**
     * This method is delegated to for non-nesting selectors (like DEFAULT) and nesting selectors (like "stage")
     *
     * envvarsRequired tells whether a found selector is required to have an envvars block.  This code doesn't decide
     * when, but other code implements the policy that envvarsRequired is true for a Node if and only if that node has subNodes.
     * Said another way: we don't want empty nodes.  A node can have envvars, envvars plus subnodes, or just subnodes.
     *
     * @return The scope of the selector, if found, otherwise null.
     */
    Map addEnvVarsForSelectorImpl(Map previousScope, EnvVarsMapData envVarsData, EnvVarsRuntimeSelectors.Node node, String selector, boolean required) throws EnvVarsException {
        final String messageContext = "[" + node.context + ":" + selector + "]";

        /////////////////////////////////////
        // We start by looking for a SELECTOR
        /////////////////////////////////////
        Object currentScopeObject = previousScope.get(selector);
        if ((currentScopeObject == null)) {
            if (required) {
                throw new EnvVarsException("Unable to find " + messageContext + " which is required.");
            } else {
                // Selector not found.  Selector not required.  Do nothing, and be happy about it.
                return null;
            }
        }

        if (!(currentScopeObject instanceof Map)) {
            throw new EnvVarsException("The element " + messageContext + " must be a map, but it is not.");
        }

        Map currentScope = (Map)currentScopeObject;

        if (isThisMapAStandardScope(currentScope)) {
            processCurrentScope(currentScope, envVarsData, node, selector);
        } else {
            envVarsData.putAllEnvVarsDefine(processImpliedDefine("define", currentScope, node, selector));
        }
        return currentScope;
    }

    private static boolean isThisMapAStandardScope(Map<?,?> currentScope) {
        if (currentScope.containsKey("inject") ||
                currentScope.containsKey("remap") ||
                currentScope.containsKey("skipInjectIfNotDefined") ||
                currentScope.containsKey("declare") ||
                currentScope.containsKey("declareSecrets") ||
                currentScope.containsKey("define") ||
                currentScope.containsKey("defineSecrets")) {
            return true;
        } else {
            for(Map.Entry entry : currentScope.entrySet()) {
                Object kO = entry.getKey();
                Object vO = entry.getValue();
                if (!(kO instanceof String))
                    return true;
                if (!(vO instanceof String))
                    return true;
            }
            return false;
        }
    }

    /**
     * A CurrentScope is the Map belonging to a found Selector.  This method extracts all of the possible PreEnvVarsData
     * out of this scope.
     */
    private void processCurrentScope(Map currentScope, EnvVarsMapData preEnvVarsData, EnvVarsRuntimeSelectors.Node node, String selector) throws EnvVarsException {
        preEnvVarsData.putAll(processInjectEnvvarsNode("inject", currentScope, node, selector));

        preEnvVarsData.putAll(processRemapEnvvarsNode("remap", currentScope, node, selector));

        preEnvVarsData.putAll(processAllowMissing("skipInjectIfNotDefined", currentScope, node, selector));

        preEnvVarsData.putAll(processDeclareEnvvarsNode("declare", currentScope, node, selector));

        preEnvVarsData.putAll(processDeclareSecretsEnvvarsNode("declareSecrets", currentScope, node, selector));

        preEnvVarsData.putAllEnvVarsDefine(processCurrentScopeMap("define", currentScope, node, selector));

        preEnvVarsData.putAllEnvVarsDefineSecret(processCurrentScopeMap("defineSecrets", currentScope, node, selector));

        preEnvVarsData.putAllEnvVarsDefineReference(processCurrentScopeMap("defineReferences", currentScope, node, selector));
    }

    /**
     * Used to validate and extract Map data (key:value pairs) from a current scope map.
     */
    private static Map<String, String> processCurrentScopeMap(String mapName, Map currentScope, EnvVarsRuntimeSelectors.Node node, String selector) throws EnvVarsException {
        final String messageContext = "[" + node.context + ":" + selector + ":" + mapName + "]";
        Object envvarsObject = currentScope.get(mapName);

        if ("declare".equals(mapName) && (envvarsObject!=null) && (!node.nodeSectionsPolicy.isDeclareAllowed())) {
            throw new EnvVarsException("The element " + messageContext + " is not allowed to contain a declare section.");
        }

        if ("define".equals(mapName) && (envvarsObject!=null) && (!node.nodeSectionsPolicy.isDefineAllowed())) {
            throw new EnvVarsException("The element " + messageContext + " is not allowed to contain a define section.");
        }

        if ("defineSecrets".equals(mapName) && (envvarsObject!=null) && (!node.nodeSectionsPolicy.isDefineSecretsAllowed())) {
            throw new EnvVarsException("The element " + messageContext + " is not allowed to contain a defineSecrets section.");
        }

        if (envvarsObject == null) {
            envvarsObject = Collections.emptyMap();
        }

        if (!(envvarsObject instanceof Map)) {
            throw new EnvVarsException("The element " + messageContext + " must be a map, but it is not.");
        }

        Map<String, String> results = new HashMap<>();

        Map<?,?> envvarsMap = (Map)envvarsObject;
        for(Map.Entry entry : envvarsMap.entrySet()) {
            String k = validateKeyCandidateObject(entry.getKey(), messageContext);

            Object vo = entry.getValue();
            if (vo instanceof String) {
                String v = (String) vo;
                results.put(k, v);
            } else {
                String name = (vo!=null) ? vo.toString() : "null";
                throw new EnvVarsException("Invalid value for " + k + " in " + messageContext + ".  The value is " + name + ".  It must be a String but is not.");
            }
        }
        return results;
    }

    private static Map<String, String> processImpliedDefine(String mapName, Map<?,?> envvarsMap, EnvVarsRuntimeSelectors.Node node, String selector) throws EnvVarsException {
        final String messageContext = "[" + node.context + ":" + selector + ":" + mapName + "]";

        if ((!node.nodeSectionsPolicy.isDefineAllowed())) {
            throw new EnvVarsException("The element " + messageContext + " is not allowed to contain a define section.");
        }

        Map<String, String> results = new HashMap<>();

        for(Map.Entry entry : envvarsMap.entrySet()) {
            String k = validateKeyCandidateObject(entry.getKey(), messageContext);

            Object vo = entry.getValue();
            if (vo instanceof String) {
                String v = (String) vo;
                results.put(k, v);
            } else {
                String name = (vo!=null) ? vo.toString() : "null";
                throw new EnvVarsException("Invalid value " + name + " in " + messageContext + ".  It must be a String but is not.");
            }
        }
        return results;
    }

    private EnvVarsMapData processAllowMissing(String listName, Map currentScope, EnvVarsRuntimeSelectors.Node node, String selector) throws EnvVarsException {
        final String messageContext = "[" + node.context + ":" + selector + ":" + listName + "]";
        Object amObject = currentScope.get(listName);

        if ("skipInjectIfNotDefined".equals(listName) && (amObject!=null) && (!node.nodeSectionsPolicy.isSkipInjectIfNotDefinedAllowed())) {
            throw new EnvVarsException("The element " + messageContext + " is not allowed to contain a skipInjectIfNotDefined section.");
        }

        if (amObject == null) {
            amObject = Collections.emptyList();
        }

        if (!(amObject instanceof List)) {
            throw new EnvVarsException("The element " + messageContext + " must be a list, but it is not.");
        }

        List<?> amList = (List)amObject;
        return processAllowMissing(messageContext, amList);
    }

    public EnvVarsMapData processAllowMissing(String messageContext, List<?> amList) throws EnvVarsException {
        EnvVarsMapData results = new EnvVarsMapData(envVarsStaticSets);

        for(Object entry : amList) {
            String k = validateKeyCandidateObject(entry, messageContext);
            results.putSkipInjectIfNotDefined(k);
        }
        return results;

    }

    private EnvVarsMapData processDeclareEnvvarsNode(String listName, Map currentScope, EnvVarsRuntimeSelectors.Node node, String selector) throws EnvVarsException {
        final String messageContext = "[" + node.context + ":" + selector + ":" + listName + "]";
        Object envvarsObject = currentScope.get(listName);

        if ("declare".equals(listName) && (envvarsObject!=null) && (!node.nodeSectionsPolicy.isDeclareAllowed())) {
            throw new EnvVarsException("The element " + messageContext + " is not allowed to contain a declare section.");
        }

        if (envvarsObject == null) {
            envvarsObject = Collections.emptyList();
        }

        if (!(envvarsObject instanceof List)) {
            throw new EnvVarsException("The element " + messageContext + " must be a list, but it is not.");
        }

        List<?> envvarsList = (List)envvarsObject;
        return processDeclareEnvvars(messageContext, envvarsList);
    }

    private EnvVarsMapData processDeclareSecretsEnvvarsNode(String listName, Map currentScope, EnvVarsRuntimeSelectors.Node node, String selector) throws EnvVarsException {
        final String messageContext = "[" + node.context + ":" + selector + ":" + listName + "]";
        Object envvarsObject = currentScope.get(listName);

        if ("declareSecrets".equals(listName) && (envvarsObject!=null) && (!node.nodeSectionsPolicy.isDefineSecretsAllowed())) {
            throw new EnvVarsException("The element " + messageContext + " is not allowed to contain a declare section.");
        }

        if (envvarsObject == null) {
            envvarsObject = Collections.emptyList();
        }

        if (!(envvarsObject instanceof List)) {
            throw new EnvVarsException("The element " + messageContext + " must be a list, but it is not.");
        }

        List<?> envvarsList = (List)envvarsObject;
        return processDeclareSecretsEnvvars(messageContext, envvarsList);
    }

    public EnvVarsMapData processDeclareEnvvars(String messageContext, List<?> envvarsList) throws EnvVarsException {
        EnvVarsMapData results = new EnvVarsMapData(envVarsStaticSets);

        for(Object entry : envvarsList) {
            String k = validateKeyCandidateObject(entry, messageContext);
            results.putEnvVarDeclare(k);
        }
        return results;
    }

    public EnvVarsMapData processDeclareSecretsEnvvars(String messageContext, List<?> envvarsList) throws EnvVarsException {
        EnvVarsMapData results = new EnvVarsMapData(envVarsStaticSets);

        for(Object entry : envvarsList) {
            String k = validateKeyCandidateObject(entry, messageContext);
            results.putEnvVarDeclareSecrets(k);
        }
        return results;
    }

    private EnvVarsMapData processInjectEnvvarsNode(String listName, Map currentScope, EnvVarsRuntimeSelectors.Node node, String selector) throws EnvVarsException {
        final String messageContext = "[" + node.context + ":" + selector + ":" + listName + "]";
        Object envvarsObject = currentScope.get(listName);

        if ("inject".equals(listName) && (envvarsObject!=null) && (!node.nodeSectionsPolicy.isInjectAllowed())) {
            throw new EnvVarsException("The element " + messageContext + " is not allowed to contain an inject section.");
        }

        if (envvarsObject == null) {
            envvarsObject = Collections.emptyList();
        }

        if (!(envvarsObject instanceof List)) {
            throw new EnvVarsException("The element " + messageContext + " must be a list, but it is not.");
        }

        List<?> envvarsList = (List)envvarsObject;
        return processInjectEnvvars(messageContext, envvarsList);
    }

    public EnvVarsMapData processInjectEnvvars(String messageContext, List<?> rawEnvvarsList) throws EnvVarsException {
        final List<?> envvarsList;
        final List<String> skipInjectList;

        if (envVarsStaticSets!=null) {
            EnvVarsStaticSets.ExpandResults results = envVarsStaticSets.expandInjectSetReferences(rawEnvvarsList, messageContext);
            envvarsList = results.lines;
            skipInjectList = results.optionalLines;
        } else {
            envvarsList = rawEnvvarsList;
            skipInjectList = Collections.emptyList();
        }

        EnvVarsMapData results = new EnvVarsMapData(envVarsStaticSets);

        // Add injects
        for(Object entry : envvarsList) {
            String k = validateKeyCandidateObject(entry, messageContext);

            String[] pieces = k.split(" from ");
            if (pieces.length==2) {
                String subK = pieces[0];
                String qualifier = pieces[1];
                if (subK.length() == 0) {
                    throw new EnvVarsException("The element " + messageContext + " contains an invalid entry:\"" + k + "\" - a blank from target is not allowed.");
                }
                if (qualifier.length() == 0) {
                    throw new EnvVarsException("The element " + messageContext + " contains an invalid entry:\"" + k + "\" - a blank from source is not allowed.");
                }
                results.putEnvVarInject(subK, qualifier);
            } else {
                results.putEnvVarInject(k, null);
            }
        }

        // Add skipInjects
        for(String entry : skipInjectList) {
            String k = validateKeyCandidateObject(entry, messageContext);

            String[] pieces = k.split(" from ");
            if (pieces.length==2) {
                String subK = pieces[0];
                String qualifier = pieces[1];
                if (subK.length() == 0) {
                    throw new EnvVarsException("The element " + messageContext + " contains an invalid entry:\"" + k + "\" - a blank from target is not allowed.");
                }
                if (qualifier.length() == 0) {
                    throw new EnvVarsException("The element " + messageContext + " contains an invalid entry:\"" + k + "\" - a blank from source is not allowed.");
                }
                results.putSkipInjectIfNotDefined(subK);
            } else {
                results.putSkipInjectIfNotDefined(k);
            }
        }

        return results;
    }

    private EnvVarsMapData processRemapEnvvarsNode(String listName, Map currentScope, EnvVarsRuntimeSelectors.Node node, String selector) throws EnvVarsException {
        final String messageContext = "[" + node.context + ":" + selector + ":" + listName + "]";
        Object envvarsObject = currentScope.get(listName);

        if ((envvarsObject!=null) && (!node.nodeSectionsPolicy.isRemapAllowed()) && ("remap".equals(listName))) {
            throw new EnvVarsException("The element " + messageContext + " is not allowed to contain a remap section.");
        }

        if (envvarsObject == null) {
            envvarsObject = Collections.emptyList();
        }

        if (!(envvarsObject instanceof List)) {
            throw new EnvVarsException("The element " + messageContext + " must be a list, but it is not.");
        }

        List<?> envvarsList = (List)envvarsObject;
        return processRemapEnvvars(messageContext, envvarsList);
    }

    public EnvVarsMapData processRemapEnvvars(String messageContext, List<?> rawEnvvarsList) throws EnvVarsException {
        final List<?> envvarsList;
        EnvVarsMapData results = new EnvVarsMapData(envVarsStaticSets);

        if (envVarsStaticSets!=null) {
            EnvVarsStaticSets.ExpandResults expandResults = envVarsStaticSets.expandInjectSetReferences(rawEnvvarsList, messageContext);
            envvarsList = expandResults.lines;
            if (!expandResults.optionalLines.isEmpty()) {
                throw new EnvVarsException("The element " + messageContext + " contains an optional inject:\"?\" - optional injects are not allowed in a remap block.");
            }
        } else {
            envvarsList = rawEnvvarsList;
        }

        for(Object entry : envvarsList) {
            String k = validateKeyCandidateObject(entry, messageContext);

            String[] pieces = k.split(" from ");
            if (pieces.length!=2) {
                throw new EnvVarsException("The element " + messageContext + " contains an invalid entry:\"" + k + "\" - no \" from \" found.");
            }

            String subK = pieces[0];
            String qualifier = pieces[1];
            if (subK.length() == 0) {
                throw new EnvVarsException("The element " + messageContext + " contains an invalid entry:\"" + k + "\" - a blank from target is not allowed.");
            }
            if (qualifier.length() == 0) {
                throw new EnvVarsException("The element " + messageContext + " contains an invalid entry:\"" + k + "\" - a blank from source is not allowed.");
            }
            results.putEnvVarRemap(subK, qualifier);
        }
        return results;
    }

    private static String validateKeyCandidateObject(Object ko, String errorContext) throws EnvVarsException {
        if (ko == null) {
            throw new EnvVarsException("Null key not allowed.");
        }

        if (!(ko instanceof String)) {
            throw new EnvVarsException("Invalid key "+ko.toString() + " in " + errorContext + ".  It must be a String but is not.");
        }
        String k = (String)ko;
        if (k.length()==0) {
            throw new EnvVarsException("Invalid blank key in " + errorContext + ".  It must not be blank.");
        }
        if (k.contains("\"")) {
            throw new EnvVarsException("Key " + k + " in " + errorContext + " contains a double quote.  Double quotes are not allowed.");
        }
        return k;
    }

    /**
     * This is a tail-recursive building of the resulting data from calls to addEnvVars and tree navigation.
     */
    private void getContext(EnvVarsRuntimeSelectors.Node node, Map previousScope, EnvVarsMapData preEnvVarsData, boolean contextRequired) throws EnvVarsException {
        ////////////////////////////////////
        // We start by looking for a CONTEXT
        ////////////////////////////////////
        Object currentScopeObject = previousScope.get(node.context);

        if (currentScopeObject==null) {
            if (contextRequired) {
                throw new EnvVarsException("Expected context " + node.context + " was not found.");
            } else {
                return;
            }
        }

        if (!(currentScopeObject instanceof Map)) {
            throw new EnvVarsException("Context "+node.context+" must be as map, but it is not.");
        }

        Map currentScope = (Map) currentScopeObject;

        // The currentScope map keys are all the foreignKeys for the context node.context.
        // Extract all the keys as Strings
        Set<String> foundForeignKeys = new HashSet<>();
        for(Object o : currentScope.keySet()) {
            if (o==null) {
                throw new EnvVarsException("Context "+node.context+" encountered a null key.");
            }
            if (!(o instanceof String)) {
                throw new EnvVarsException("Context "+node.context+" encountered a non-String key.");
            }
            final String key = (String)o;
            // Only add if this key is not the reserved value for default.  Basically, don't add the key "default".
            if (!node.isReservedValueForDefault(key)) {
                foundForeignKeys.add(key);
            }
        }
        preEnvVarsData.putEnvVarContextForeignKeysFound(node.context, foundForeignKeys);

        // First we perform the expected processing of the DEFAULT element at this Node.
        node.defaultProcessingPolicy.process(this, currentScope, preEnvVarsData, node);

        // Then we perform the processing of the selector element of this Node.
        addEnvVarsForSelector(currentScope, preEnvVarsData, node);
    }

}
