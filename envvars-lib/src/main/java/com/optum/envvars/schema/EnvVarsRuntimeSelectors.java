package com.optum.envvars.schema;

import com.optum.envvars.EnvVarsException;
import com.optum.envvars.mapdata.EnvVarsMapDataEngine;
import com.optum.envvars.mapdata.NodeSectionsPolicy;

import java.util.Collections;
import java.util.List;

public class EnvVarsRuntimeSelectors {
    private final List<Node> rootNodesList;

    public EnvVarsRuntimeSelectors(List<Node> rootNodesList) {
        this.rootNodesList = rootNodesList;
    }

    public List<Node> getRootNodesList() {
        return rootNodesList;
    }

    public static class Node {
        public final String context;
        public final boolean contextRequired;
        public final String selector;
        public final boolean selectorRequired;
        public final List<Node> subNodeList;
        public final String RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR;
        public final EnvVarsMapDataEngine.DefaultProcessingPolicy defaultProcessingPolicy;
        public final NodeSectionsPolicy nodeSectionsPolicy;

        public Node(String context, boolean contextRequired, String selector, boolean selectorRequired,
                    EnvVarsMapDataEngine.DefaultProcessingPolicy defaultProcessingPolicy, NodeSectionsPolicy nodeSectionsPolicy) throws EnvVarsException {
            this(context, contextRequired, selector, selectorRequired, defaultProcessingPolicy, nodeSectionsPolicy, "default", Collections.emptyList());
        }

        public Node(String context, boolean contextRequired, String selector, boolean selectorRequired,
                    EnvVarsMapDataEngine.DefaultProcessingPolicy defaultProcessingPolicy, NodeSectionsPolicy nodeSectionsPolicy, List<Node> subNodeList) throws EnvVarsException {
            this(context, contextRequired, selector, selectorRequired, defaultProcessingPolicy, nodeSectionsPolicy, "default", subNodeList);
        }

        public Node(String context, boolean contextRequired, String selector, boolean selectorRequired,
                    EnvVarsMapDataEngine.DefaultProcessingPolicy defaultProcessingPolicy, NodeSectionsPolicy nodeSectionsPolicy, String RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR, List<Node> subNodeList) throws EnvVarsException {
            if (context == null) throw new EnvVarsException("Context cannot be null.");
            if (context.length() == 0) throw new EnvVarsException("Context cannot be empty.");
            if (selector == null) throw new EnvVarsException("Selector cannot be null.");
            if (selector.length() == 0) throw new EnvVarsException("Selector cannot be empty.");
            if (RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR == null)
                throw new EnvVarsException("RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR cannot be null.");
            if (RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR.length() == 0)
                throw new EnvVarsException("RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR cannot be empty.");
            if (RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR.equals(selector))
                throw new EnvVarsException("Selector cannot be \"" + RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR + "\".  That value is reserved.");
            if (subNodeList == null) throw new EnvVarsException("SchemaSubNodeList cannot be null.");
            if (defaultProcessingPolicy == null) throw new EnvVarsException("DefaultProcessingPolicy cannot be null.");
            if (nodeSectionsPolicy == null) throw new EnvVarsException("NodeSectionsPolicy cannot be null.");

            this.defaultProcessingPolicy = defaultProcessingPolicy;
            this.context = context;
            this.contextRequired = contextRequired;
            this.selector = selector;
            this.selectorRequired = selectorRequired;
            this.RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR = RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR;
            this.subNodeList = subNodeList;
            this.nodeSectionsPolicy = nodeSectionsPolicy;
        }

        public boolean isReservedValueForDefault(String selector) {
            return RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR.equals(selector);
        }
    }
}
