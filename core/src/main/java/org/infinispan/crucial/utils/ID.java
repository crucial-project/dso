package org.infinispan.crucial.utils;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;

import java.util.Random;
import java.util.UUID;

/**
 * @author Pierre Sutra
 *
 */
public class ID {

    private static final NoArgGenerator generator
            = Generators.randomBasedGenerator(new Random(System.nanoTime()));

    private static ThreadLocal<UUID> id = ThreadLocal.withInitial(() -> generator.generate());

    /**
     * @return a distributed unique identifier for each thread.
     */
    public static UUID threadID(){
        return id.get();
    }

    public static NoArgGenerator generator(){
        return generator;
    }


}
