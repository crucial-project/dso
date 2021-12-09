package org.crucial.dso.server;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.attributes.ConfigurationElement;

@BuiltBy(DSOConfigurationBuilder.class)
public class DSOConfiguration extends ConfigurationElement<DSOConfiguration> {
    static final AttributeDefinition<Boolean> IDEMPOTENT =
            AttributeDefinition.builder("idempotent", false).immutable().build();

    public DSOConfiguration(AttributeSet attributes) {
        super(Element.DSO, attributes);
    }

    public static AttributeSet attributeDefinitionSet() {
        return new AttributeSet(DSOConfiguration.class, IDEMPOTENT);
    }

    public boolean idempotent() {
        return attributes.attribute(IDEMPOTENT).get();
    }
}
