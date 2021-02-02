package org.crucial.dso.client;

import org.crucial.dso.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

@Command(name = "interpreter")
public class Interpreter implements Callable<Integer> {

    public static final String DEFAULT_SERVER = "localhost:11222";

    @Option(names = "-s" ) String server = DEFAULT_SERVER;

    public static void main(String[] args) {

        // 1 - parse
        Interpreter interpreter = new Interpreter();
        CommandLine commandLine = new CommandLine(interpreter);
        AtomicCounter counter = newInstance(AtomicCounter.class);
        AtomicList list = newInstance(AtomicList.class);
        AtomicMap map = newInstance(AtomicMap.class);
        AtomicTreeMap treemap = newInstance(AtomicTreeMap.class);
        CyclicBarrier barrier = newInstance(CyclicBarrier.class);
        Pipe pipe = newInstance(Pipe.class);
        commandLine.addSubcommand("counter", counter);
        commandLine.addSubcommand("list", list);
        commandLine.addSubcommand("map", map);
        commandLine.addSubcommand("treemap", treemap);
        commandLine.addSubcommand("barrier", barrier);
        commandLine.addSubcommand("pipe", pipe);
        commandLine.registerConverter(BiFunction.class, s -> new BiFunctionTypeConverter().convert(s));
        commandLine.parseArgs(args);

        // 2 - execute
        Client client = new Client(interpreter.server);
        commandLine = new CommandLine(interpreter);
        commandLine.addSubcommand("counter",client.getAtomicCounter(counter.name, counter.count));
        commandLine.addSubcommand("list",client.getAtomicList(counter.name));
        commandLine.addSubcommand("map",client.getAtomicMap(map.name));
        commandLine.addSubcommand("treemap",client.getAtomicTreeMap(treemap.name));
        commandLine.addSubcommand("barrier",client.getCyclicBarrier(barrier.name, barrier.parties));
        commandLine.addSubcommand("pipe",client.getPipe(pipe.name));
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

}
