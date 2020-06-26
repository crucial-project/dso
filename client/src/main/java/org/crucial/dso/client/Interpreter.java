package org.crucial.dso.client;

import com.google.common.collect.ImmutableMap;
import org.crucial.dso.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

@Command(name = "interpreter")
public class Interpreter implements Callable<Integer> {

    public static final String DEFAULT_SERVER = "localhost:11222";

    @Option(names = "-s" ) String server = DEFAULT_SERVER;

    public static void main(String[] args) {

        // 1 - parse
        Interpreter client = new Interpreter();
        CommandLine commandLine = new CommandLine(client);
        AtomicCounter counter = newInstance(AtomicCounter.class);
        AtomicList list = newInstance(AtomicList.class);
        AtomicMap map = newInstance(AtomicMap.class);
        CyclicBarrier barrier = newInstance(CyclicBarrier.class);
        commandLine.addSubcommand("counter", counter);
        commandLine.addSubcommand("list", list);
        commandLine.addSubcommand("map", map);
        commandLine.addSubcommand("barrier", barrier);
        commandLine.registerConverter(BiFunction.class, s -> new BiFunctionTypeConverter().convert(s));
        commandLine.parseArgs(args);

        // 2 - execute
        Factory.get(client.server);
        commandLine = new CommandLine(client);
        commandLine.addSubcommand("counter",new AtomicCounter(counter.name, counter.count));
        commandLine.addSubcommand("list",new AtomicList<>(counter.name));
        commandLine.addSubcommand("map",new AtomicMap<>(map.name));
        commandLine.addSubcommand("barrier",new CyclicBarrier(barrier.name, barrier.parties));
        commandLine.registerConverter(BiFunction.class, s -> new BiFunctionTypeConverter().convert(s));
        commandLine.execute(args);

        Object executionResult = commandLine.getExecutionResult();
        CommandLine.ParseResult parseResult = commandLine.getParseResult();
        while (parseResult.subcommand() != null) {
            CommandLine sub = parseResult.subcommand().commandSpec().commandLine();
            executionResult = sub.getExecutionResult();
            parseResult = sub.getParseResult();
        }

        // 3 - print result (in Unix shell format)
        String result = "";
        if (executionResult == null){
            result = null;
        } else if (executionResult instanceof Object[]) {
            for (Object o : (Object[]) executionResult) {
                result += o.toString() + " ";
            }
        } else if (executionResult instanceof Collection) {
            for (Object o : (Collection) executionResult) {
                result += o.toString() + " ";
            }
        } else {
            result = executionResult.toString();
        }
        if (result != null) System.out.println(result);

        System.exit(0);
    }

    @Override
    public Integer call() throws Exception {
        Factory.get(this.server);
        return 0;
    }


    private static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static final Map<String, RemoteBiFunction<String,String,String>> BIFUNCTIONS = ImmutableMap.of(
            "sum", (x,y) -> Integer.toString(Integer.valueOf(x) + Integer.valueOf(y))
    );

    public static class BiFunctionTypeConverter implements CommandLine.ITypeConverter<BiFunction> {

        @Override
        public BiFunction convert(String s) throws Exception {
            return BIFUNCTIONS.get(s);
        }

    }

}
