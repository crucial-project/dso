package org.crucial.dso.server;


import org.infinispan.commons.configuration.io.ConfigurationReader;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.parsing.*;
import org.kohsuke.MetaInfServices;

/**
 * Anchored keys parser extension.
 * <p>
 * This extension parses elements in the "urn:crucial:dso" namespace
 *
 * @author Tristan Tarrant
 */
@MetaInfServices
@Namespace(root = "dso")
@Namespace(uri = "urn:crucial:dso:*", root = "dso", since = "13.0")
public class DSOConfigurationParser implements ConfigurationParser {

    @Override
    public void readElement(ConfigurationReader reader, ConfigurationBuilderHolder holder) {
        if (!holder.inScope(ParserScope.CACHE) && !holder.inScope(ParserScope.CACHE_TEMPLATE)) {
            throw new IllegalStateException("WRONG SCOPE");
        }
        ConfigurationBuilder builder = holder.getCurrentConfigurationBuilder();

        Element element = Element.forName(reader.getLocalName());
        switch (element) {
            case DSO: {
                DSOConfigurationBuilder dsoBuilder = builder.addModule(DSOConfigurationBuilder.class);
                parseDso(reader, dsoBuilder);
                break;
            }
            default: {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void parseDso(ConfigurationReader reader, DSOConfigurationBuilder builder) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeName(i));
            switch (attribute) {
                case IDEMPOTENT: {
                    builder.idempotent(Boolean.parseBoolean(value));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    @Override
    public Namespace[] getNamespaces() {
        return ParseUtils.getNamespaceAnnotations(getClass());
    }
}
