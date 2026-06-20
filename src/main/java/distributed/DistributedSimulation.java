package distributed;

import core.GridFactory;
import core.Statistics;
import model.CellState;
import model.SimulationConfig;
import model.SimulationResult;
import util.Timer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class DistributedSimulation {
    private final List<WorkerAddress> workerAddresses;

    public DistributedSimulation(List<WorkerAddress> workerAddresses) {
        if (workerAddresses == null || workerAddresses.isEmpty()) {
            throw new IllegalArgumentException("Informe pelo menos um worker RMI.");
        }
        this.workerAddresses = workerAddresses;
    }

    public static void main(String[] args) throws Exception {
        List<WorkerAddress> addresses = parseAddresses(args);
        SimulationConfig config = SimulationConfig.defaultConfig();
        CellState[][] initialGrid = GridFactory.createInitialGrid(config);

        SimulationResult result = new DistributedSimulation(addresses).run(initialGrid, config);
        System.out.printf("Distribuida concluida em %.3f ms%n", Timer.toMillis(result.getElapsedNanos()));
        System.out.printf("IGNORANT=%d, SPREADER=%d, INACTIVE=%d, GROK=%d%n",
                result.getIgnorantCount(), result.getSpreaderCount(),
                result.getInactiveCount(), result.getGrokCount());
        System.out.println("Neutralizados por influencia GROK: " + result.getNeutralizedByGrokCount());
    }

    public static List<WorkerAddress> localWorkers(int workerCount, int basePort) {
        List<WorkerAddress> addresses = new ArrayList<>();
        for (int index = 0; index < workerCount; index++) {
            addresses.add(new WorkerAddress("127.0.0.1", basePort + index, WorkerServer.DEFAULT_WORKER_NAME));
        }
        return addresses;
    }

    public SimulationResult run(CellState[][] initialGrid, SimulationConfig config) throws Exception {
        Timer timer = new Timer();
        timer.start();

        CellState[][] currentGrid = initialGrid;
        int neutralizedByGrok = 0;

        for (int generation = 0; generation < config.getGenerations(); generation++) {
            List<WorkerResult> results = sendGenerationTasks(currentGrid, config, generation);
            neutralizedByGrok += countNeutralized(results);
            currentGrid = assembleNextGrid(results, config);
        }

        return Statistics.buildResult(currentGrid, config.getGenerations(), timer.elapsedNanos(), neutralizedByGrok);
    }

    public CellState[][] nextGeneration(CellState[][] currentGrid,
                                        SimulationConfig config,
                                        int generation) throws Exception {
        return assembleNextGrid(sendGenerationTasks(currentGrid, config, generation), config);
    }

    private int countNeutralized(List<WorkerResult> results) {
        int total = 0;
        for (WorkerResult result : results) {
            total += result.getNeutralizedByGrok();
        }
        return total;
    }

    private CellState[][] assembleNextGrid(List<WorkerResult> results, SimulationConfig config) {
        CellState[][] nextGrid = new CellState[config.getRows()][config.getColumns()];
        for (WorkerResult result : results) {
            CellState[][] rows = result.getComputedRows();
            for (int localRow = 0; localRow < rows.length; localRow++) {
                System.arraycopy(rows[localRow], 0,
                        nextGrid[result.getStartRow() + localRow], 0,
                        config.getColumns());
            }
        }
        return nextGrid;
    }

    private List<WorkerResult> sendGenerationTasks(CellState[][] currentGrid,
                                                   SimulationConfig config,
                                                   int generation) throws Exception {
        List<Thread> threads = new ArrayList<>();
        List<WorkerResult> results = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        Object lock = new Object();

        for (int workerIndex = 0; workerIndex < workerAddresses.size(); workerIndex++) {
            int startRow = workerIndex * config.getRows() / workerAddresses.size();
            int endRow = (workerIndex + 1) * config.getRows() / workerAddresses.size();
            WorkerTask task = createTask(currentGrid, config, generation, startRow, endRow);
            WorkerAddress address = workerAddresses.get(workerIndex);

            Thread thread = new Thread(() -> {
                try {
                    WorkerResult result = callWorker(address, task);
                    synchronized (lock) {
                        results.add(result);
                    }
                } catch (Exception exception) {
                    synchronized (lock) {
                        errors.add(exception);
                    }
                }
            }, "rmi-client-" + workerIndex);

            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        if (!errors.isEmpty()) {
            throw errors.get(0);
        }

        if (results.size() != workerAddresses.size()) {
            throw new IllegalStateException("Nem todos os workers devolveram resultado.");
        }

        return results;
    }

    private WorkerTask createTask(CellState[][] grid,
                                  SimulationConfig config,
                                  int generation,
                                  int startRow,
                                  int endRow) {
        int ghostStart = Math.max(0, startRow - 1);
        int ghostEnd = Math.min(config.getRows(), endRow + 1);
        CellState[][] block = new CellState[ghostEnd - ghostStart][config.getColumns()];

        for (int row = ghostStart; row < ghostEnd; row++) {
            System.arraycopy(grid[row], 0, block[row - ghostStart], 0, config.getColumns());
        }

        int firstInnerRow = startRow - ghostStart;
        return WorkerTask.create(config, block, generation, startRow, firstInnerRow, endRow - startRow);
    }

    private WorkerResult callWorker(WorkerAddress address, WorkerTask task) throws Exception {
        Registry registry = LocateRegistry.getRegistry(address.host(), address.port());
        MatrixWorkerRemote worker = (MatrixWorkerRemote) registry.lookup(address.name());
        return worker.computeRange(task);
    }

    private static List<WorkerAddress> parseAddresses(String[] args) {
        if (args.length == 0) {
            return localWorkers(2, 9100);
        }

        List<WorkerAddress> addresses = new ArrayList<>();
        for (String arg : args) {
            String[] parts = arg.split(":");
            if (parts.length < 2 || parts.length > 3) {
                throw new IllegalArgumentException("Use host:porta ou host:porta:nome");
            }
            String name = parts.length == 3 ? parts[2] : WorkerServer.DEFAULT_WORKER_NAME;
            addresses.add(new WorkerAddress(parts[0], Integer.parseInt(parts[1]), name));
        }
        return addresses;
    }

    public record WorkerAddress(String host, int port, String name) {
    }
}
