package app;

import core.GridFactory;
import distributed.DistributedSimulation;
import distributed.MatrixWorkerImpl;
import model.CellState;
import model.SimulationConfig;
import model.SimulationResult;
import parallel.ParallelSimulation;
import sequential.SequentialSimulation;
import util.CsvWriter;
import util.Timer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

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

        int workers = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        int basePort = args.length > 2 ? Integer.parseInt(args[2]) : 9100;
        List<Registry> registries = startLocalWorkers(workers, basePort);
        try {
            CellState[][] distributedInitial = GridFactory.createInitialGrid(config);

            // MODO A: Terminais separados na mesma máquina (Localhost).
            // comentar as duas linhas para usar em pcs separados.
            List<DistributedSimulation.WorkerAddress> addresses = DistributedSimulation.localWorkers(workers, basePort);
            SimulationResult distributed = new DistributedSimulation(addresses).run(distributedInitial, config);

            // MODO B: Máquinas separadas de verdade (Rede Local).
            // Descomentar e alterar os ips e portas das máquinas.
            /*
            List<DistributedSimulation.WorkerAddress> addresses = new ArrayList<>();
            addresses.add(new DistributedSimulation.WorkerAddress("000.000.0.00", 1099, "worker1"));
            addresses.add(new DistributedSimulation.WorkerAddress("000.000.0.00", 1099, "worker2"));
            SimulationResult distributed = new DistributedSimulation(addresses).run(distributedInitial, config);
            */

            validateSameCounts("Distribuida", sequential, distributed);
            writeComparedResult(csvFile, "Distribuida", sequential, distributed, workers);
        } finally {
            stopLocalWorkers(registries);
        }

        System.out.println("CSV gerado em: " + csvFile);
        compareWithAndWithoutGrok(config);

        // o RMI vai manter as threads ativas mesmo apos o unexport dos registries a JVM e ai finalizada explicitamente para o processo encerrar.
        System.exit(0);
    }

    private static List<Registry> startLocalWorkers(int workerCount, int basePort) throws Exception {
        List<Registry> registries = new ArrayList<>();
        for (int index = 0; index < workerCount; index++) {
            int port = basePort + index;
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("worker", new MatrixWorkerImpl());
            registries.add(registry);
        }
        return registries;
    }

    private static void stopLocalWorkers(List<Registry> registries) throws Exception {
        for (int index = 0; index < registries.size(); index++) {
            registries.get(index).unbind("worker");
            UnicastRemoteObject.unexportObject(registries.get(index), true);
        }
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