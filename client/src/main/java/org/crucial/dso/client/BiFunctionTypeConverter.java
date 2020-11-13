package org.crucial.dso.client;

import org.crucial.dso.RemoteBiFunction;
import picocli.CommandLine;

import java.util.function.BiFunction;

public class BiFunctionTypeConverter implements CommandLine.ITypeConverter<BiFunction> {

    @Override
    public BiFunction convert(String s) throws Exception {
        return RemoteBiFunction.BIFUNCTIONS.get(s);
    }

}
