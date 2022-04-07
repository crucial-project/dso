package org.crucial.dso.server;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;

import static org.crucial.dso.server.Attribute.IDEMPOTENT;

public class DSOConfigurationBuilder implements Builder<DSOConfiguration> {
    private final AttributeSet attributes;

    public DSOConfigurationBuilder() {
        this(null);
    }

    public DSOConfigurationBuilder(GlobalConfigurationBuilder builder) {
        this.attributes = DSOConfiguration.attributeDefinitionSet();
    }

    @Override
    public DSOConfiguration create() {
        return new DSOConfiguration(attributes.protect());
    }

    @Override
    public Builder<?> read(DSOConfiguration template) {
        this.read(template);
        return this;
    }

    public DSOConfigurationBuilder idempotent(boolean idempotent) {
        attributes.attribute(IDEMPOTENT).set(idempotent);
        return this;
    }
}
