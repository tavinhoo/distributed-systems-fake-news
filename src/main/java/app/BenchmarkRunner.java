package app;

import core.GridFactory;
import model.CellState;
import model.SimulationConfig;
import model.SimulationResult;
import parallel.ParallelSimulation;
import sequential.SequentialSimulation;
import util.CsvWriter;
import util.Timer;

public class BenchmarkRunner {
    public static void main(String[] args) throws Exception {
        SimulationConfig config = SimulationConfig.defaultConfig();
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        String csvFile = "benchmark-results.csv";

        CsvWriter.writeBenchmarkHeader(csvFile);

        CellState[][] sequentialInitial = GridFactory.createInitialGrid(config);
        SimulationResult sequential = new SequentialSimulation().run(sequentialInitial, config);
        double sequentialMillis = Timer.toMillis(sequential.getElapsedNanos());
        printLine("Sequencial", sequentialMillis, 1.0, 1.0, 1);
        writeCsvLine(csvFile, "Sequencial", sequentialMillis, 1.0, 1.0, 1, sequential);

        CellState[][] parallelInitial = GridFactory.createInitialGrid(config);
        SimulationResult parallel = new ParallelSimulation(threads).run(parallelInitial, config);
        validateSameCounts("Paralela", sequential, parallel);
        writeComparedResult(csvFile, "Paralela", sequential, parallel, threads);

        System.out.println("CSV gerado em: " + csvFile);
        compareWithAndWithoutGrok(config);
    }

    private static void writeComparedResult(String csvFile,
                                            String version,
                                            SimulationResult sequential,
                                            SimulationResult current,
                                            int units) throws Exception {
        double sequentialMillis = Timer.toMillis(sequential.getElapsedNanos());
        double currentMillis = Timer.toMillis(current.getElapsedNanos());
        double speedup = sequentialMillis / currentMillis;
        double efficiency = speedup / units;

        printLine(version, currentMillis, speedup, efficiency, units);
        writeCsvLine(csvFile, version, currentMillis, speedup, efficiency, units, current);
    }

    private static void writeCsvLine(String csvFile,
                                     String version,
                                     double totalMillis,
                                     double speedup,
                                     double efficiency,
                                     int units,
                                     SimulationResult result) throws Exception {
        CsvWriter.appendBenchmarkLine(csvFile, version, totalMillis, speedup, efficiency, units,
                result.getIgnorantCount(), result.getSpreaderCount(), result.getInactiveCount(),
                result.getGrokCount(), result.getNeutralizedByGrokCount());
    }

    private static void printLine(String version,
                                  double totalMillis,
                                  double speedup,
                                  double efficiency,
                                  int units) {
        System.out.printf("%-12s tempo=%9.3f ms | speedup=%6.3f | eficiencia=%6.3f | unidades=%d%n",
                version, totalMillis, speedup, efficiency, units);
    }

    private static void validateSameCounts(String version,
                                           SimulationResult expected,
                                           SimulationResult current) {
        boolean same = expected.getIgnorantCount() == current.getIgnorantCount()
                && expected.getSpreaderCount() == current.getSpreaderCount()
                && expected.getInactiveCount() == current.getInactiveCount()
                && expected.getGrokCount() == current.getGrokCount()
                && expected.getNeutralizedByGrokCount() == current.getNeutralizedByGrokCount();

        if (!same) {
            System.out.println("Aviso: " + version + " terminou com contagens diferentes da sequencial.");
        }
    }

    private static void compareWithAndWithoutGrok(SimulationConfig config) {
        SimulationConfig withoutGrok = config.withoutGrok();
        CellState[][] baseWithGrok = GridFactory.createInitialGrid(config);
        CellState[][] baseWithoutGrok = GridFactory.copyReplacingGrokWithIgnorant(baseWithGrok);

        SimulationResult withGrok = new SequentialSimulation().run(baseWithGrok, config);
        SimulationResult noGrok = new SequentialSimulation().run(baseWithoutGrok, withoutGrok);

        System.out.println();
        System.out.println("Comparacao social sequencial:");
        System.out.printf("Com GROK:    IGNORANT=%d, SPREADER=%d, INACTIVE=%d, GROK=%d, neutralizados=%d%n",
                withGrok.getIgnorantCount(), withGrok.getSpreaderCount(), withGrok.getInactiveCount(),
                withGrok.getGrokCount(), withGrok.getNeutralizedByGrokCount());
        System.out.printf("Sem GROK:    IGNORANT=%d, SPREADER=%d, INACTIVE=%d, GROK=%d, neutralizados=%d%n",
                noGrok.getIgnorantCount(), noGrok.getSpreaderCount(), noGrok.getInactiveCount(),
                noGrok.getGrokCount(), noGrok.getNeutralizedByGrokCount());
    }
}
