package org.crucial.dso.server;

import org.infinispan.commons.configuration.io.NamingStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 14.0
 **/
public enum Attribute {
    UNKNOWN(null),
    IDEMPOTENT;

    private static final Map<String, Attribute> MAP;

    static {
        final Map<String, Attribute> map = new HashMap<>(8);
        for (Attribute element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    private final String name;

    Attribute(final String name) {
        this.name = name;
    }

    Attribute() {
        this.name = NamingStrategy.KEBAB_CASE.convert(name()).toLowerCase();
    }

    public static Attribute forName(String localName) {
        final Attribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
