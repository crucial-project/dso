package org.crucial.dso.server;

import org.infinispan.commons.configuration.io.NamingStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 14.0
 **/
public enum Element {
    // Must be first
    UNKNOWN(null),
    DSO;

    private static final Map<String, Element> MAP;

    static {
        final Map<String, Element> map = new HashMap<>(8);
        for (Element element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    private final String name;

    Element(final String name) {
        this.name = name;
    }

    Element() {
        this.name = NamingStrategy.KEBAB_CASE.convert(name()).toLowerCase();
    }

    public static Element forName(String localName) {
        final Element element = MAP.get(localName);
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
