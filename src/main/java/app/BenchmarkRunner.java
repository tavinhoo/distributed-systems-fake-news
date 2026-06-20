package app;

import core.GridFactory;
import distributed.DistributedSimulation;
import distributed.MatrixWorkerImpl;
import distributed.WorkerServer;
import model.CellState;
import model.SimulationConfig;
import model.SimulationResult;
import parallel.ParallelSimulation;
import sequential.SequentialSimulation;
import util.CsvWriter;
import util.Timer;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkRunner {
    private static final String CSV_FILE = "benchmark-results.csv";
    private static final String ENVIRONMENT_FILE = "experimental-environment.txt";

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "batch".equalsIgnoreCase(args[0])) {
            int basePort = args.length > 1 ? Integer.parseInt(args[1]) : 9100;
            runBatch(basePort);
            return;
        }

        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int workers = args.length > 1 ? Integer.parseInt(args[1]) : 2;
        int basePort = args.length > 2 ? Integer.parseInt(args[2]) : 9100;
        runSingle(SimulationConfig.defaultConfig(), threads, workers, basePort);
    }

    private static void runSingle(SimulationConfig config,
                                  int threads,
                                  int workers,
                                  int basePort) throws Exception {
        CsvWriter.writeBenchmarkHeader(CSV_FILE);
        writeEnvironmentFile();

        List<LocalWorker> localWorkers = startLocalWorkers(workers, basePort);
        try {
            runScenario("padrao", config, new int[]{threads}, new int[]{workers}, basePort);
        } finally {
            stopLocalWorkers(localWorkers);
        }

        System.out.println("CSV gerado em: " + CSV_FILE);
        System.out.println("Ambiente registrado em: " + ENVIRONMENT_FILE);
        compareWithAndWithoutGrok(config);
    }

    private static void runBatch(int basePort) throws Exception {
        CsvWriter.writeBenchmarkHeader(CSV_FILE);
        writeEnvironmentFile();

        Scenario[] scenarios = {
                new Scenario("base_80x80", new SimulationConfig(80, 80, 100, 0.02, 0.03,
                        0.35, 0.15, 0.25, 0.50, 42L), new int[]{2, 4}, new int[]{2}),
                new Scenario("matriz_120x120", new SimulationConfig(120, 120, 100, 0.02, 0.03,
                        0.35, 0.15, 0.25, 0.50, 42L), new int[]{4}, new int[]{2}),
                new Scenario("geracoes_200", new SimulationConfig(80, 80, 200, 0.02, 0.03,
                        0.35, 0.15, 0.25, 0.50, 42L), new int[]{4}, new int[]{2}),
                new Scenario("espalhadores_5pct", new SimulationConfig(80, 80, 100, 0.05, 0.03,
                        0.35, 0.15, 0.25, 0.50, 42L), new int[]{4}, new int[]{2}),
                new Scenario("workers_1_2_4", new SimulationConfig(80, 80, 100, 0.02, 0.03,
                        0.35, 0.15, 0.25, 0.50, 42L), new int[]{4}, new int[]{1, 2, 4})
        };

        int maxWorkers = maxWorkerCount(scenarios);
        List<LocalWorker> localWorkers = startLocalWorkers(maxWorkers, basePort);
        try {
            warmUp(basePort);
            for (Scenario scenario : scenarios) {
                runScenario(scenario.name(), scenario.config(), scenario.threadCounts(),
                        scenario.workerCounts(), basePort);
            }
        } finally {
            stopLocalWorkers(localWorkers);
        }

        System.out.println("CSV de experimentos gerado em: " + CSV_FILE);
        System.out.println("Ambiente registrado em: " + ENVIRONMENT_FILE);
    }

    private static void warmUp(int basePort) throws Exception {
        SimulationConfig config = new SimulationConfig(30, 30, 20, 0.02, 0.03,
                0.35, 0.15, 0.25, 0.50, 42L);

        new SequentialSimulation().run(GridFactory.createInitialGrid(config), config);
        new ParallelSimulation(2).run(GridFactory.createInitialGrid(config), config);
        new DistributedSimulation(DistributedSimulation.localWorkers(1, basePort))
                .run(GridFactory.createInitialGrid(config), config);
    }

    private static void runScenario(String scenarioName,
                                    SimulationConfig config,
                                    int[] threadCounts,
                                    int[] workerCounts,
                                    int basePort) throws Exception {
        System.out.println();
        System.out.println("Cenario: " + scenarioName);

        CellState[][] sequentialInitial = GridFactory.createInitialGrid(config);
        SimulationResult sequential = new SequentialSimulation().run(sequentialInitial, config);
        double sequentialMillis = Timer.toMillis(sequential.getElapsedNanos());
        printLine("Sequencial", sequentialMillis, 1.0, 1.0, 1);
        writeCsvLine(scenarioName, "Sequencial", config, 1, sequentialMillis, 1.0, 1.0, sequential);

        for (int threads : threadCounts) {
            CellState[][] parallelInitial = GridFactory.createInitialGrid(config);
            SimulationResult parallel = new ParallelSimulation(threads).run(parallelInitial, config);
            validateSameCounts("Paralela-" + threads, sequential, parallel);
            writeComparedResult(scenarioName, "Paralela", config, sequential, parallel, threads);
        }

        for (int workers : workerCounts) {
            CellState[][] distributedInitial = GridFactory.createInitialGrid(config);
            SimulationResult distributed = new DistributedSimulation(
                    DistributedSimulation.localWorkers(workers, basePort)).run(distributedInitial, config);
            validateSameCounts("Distribuida-" + workers, sequential, distributed);
            writeComparedResult(scenarioName, "Distribuida", config, sequential, distributed, workers);
        }
    }

    private static List<LocalWorker> startLocalWorkers(int workerCount, int basePort) throws Exception {
        List<LocalWorker> workers = new ArrayList<>();
        for (int index = 0; index < workerCount; index++) {
            Registry registry = LocateRegistry.createRegistry(basePort + index);
            MatrixWorkerImpl worker = new MatrixWorkerImpl();
            registry.rebind(WorkerServer.DEFAULT_WORKER_NAME, worker);
            workers.add(new LocalWorker(registry, worker));
        }
        return workers;
    }

    private static void stopLocalWorkers(List<LocalWorker> workers) throws Exception {
        for (LocalWorker worker : workers) {
            worker.registry().unbind(WorkerServer.DEFAULT_WORKER_NAME);
            UnicastRemoteObject.unexportObject(worker.remote(), true);
            UnicastRemoteObject.unexportObject(worker.registry(), true);
        }
    }

    private static int maxWorkerCount(Scenario[] scenarios) {
        int max = 1;
        for (Scenario scenario : scenarios) {
            for (int workers : scenario.workerCounts()) {
                max = Math.max(max, workers);
            }
        }
        return max;
    }

    private static void writeComparedResult(String scenarioName,
                                            String version,
                                            SimulationConfig config,
                                            SimulationResult sequential,
                                            SimulationResult current,
                                            int units) throws Exception {
        double sequentialMillis = Timer.toMillis(sequential.getElapsedNanos());
        double currentMillis = Timer.toMillis(current.getElapsedNanos());
        double speedup = sequentialMillis / currentMillis;
        double efficiency = speedup / units;

        printLine(version, currentMillis, speedup, efficiency, units);
        writeCsvLine(scenarioName, version, config, units, currentMillis, speedup, efficiency, current);
    }

    private static void writeCsvLine(String scenarioName,
                                     String version,
                                     SimulationConfig config,
                                     int units,
                                     double totalMillis,
                                     double speedup,
                                     double efficiency,
                                     SimulationResult result) throws Exception {
        CsvWriter.appendBenchmarkLine(CSV_FILE, scenarioName, version,
                config.getRows(), config.getColumns(), config.getGenerations(),
                config.getInitialSpreaderRate(), units, totalMillis, speedup, efficiency,
                result.getIgnorantCount(), result.getSpreaderCount(), result.getInactiveCount(),
                result.getGrokCount(), result.getNeutralizedByGrokCount());
    }

    private static void writeEnvironmentFile() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        try (PrintWriter writer = new PrintWriter(new FileWriter(ENVIRONMENT_FILE))) {
            writer.println("Sistema operacional: " + System.getProperty("os.name")
                    + " " + System.getProperty("os.version")
                    + " (" + System.getProperty("os.arch") + ")");
            writer.println("Java: " + System.getProperty("java.version")
                    + " - " + System.getProperty("java.vendor"));
            writer.println("Processadores logicos: " + runtime.availableProcessors());
            writer.println("Memoria maxima da JVM (MB): " + runtime.maxMemory() / (1024 * 1024));
            writer.println("Linguagem: Java");
            writer.println("Ambiente: execucao local via JVM, Threads e Java RMI");
        }
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

    private record Scenario(String name,
                            SimulationConfig config,
                            int[] threadCounts,
                            int[] workerCounts) {
    }

    private record LocalWorker(Registry registry, MatrixWorkerImpl remote) {
    }
}
