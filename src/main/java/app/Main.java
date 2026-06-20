package app;

import distributed.DistributedSimulation;
import core.GridFactory;
import model.CellState;
import model.SimulationConfig;
import model.SimulationResult;
import parallel.ParallelSimulation;
import sequential.SequentialSimulation;
import util.Timer;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0].toLowerCase() : "sequential";
        SimulationConfig config = SimulationConfig.defaultConfig();
        CellState[][] initialGrid = GridFactory.createInitialGrid(config);

        SimulationResult result;
        if ("parallel".equals(mode)) {
            int threads = args.length > 1 ? Integer.parseInt(args[1]) : 4;
            result = new ParallelSimulation(threads).run(initialGrid, config);
        } else if ("distributed".equals(mode)) {
            result = new DistributedSimulation(parseWorkerAddresses(args)).run(initialGrid, config);
        } else {
            result = new SequentialSimulation().run(initialGrid, config);
        }

        printResult(mode, result);
    }

    private static void printResult(String mode, SimulationResult result) {
        System.out.println("Modo: " + mode);
        System.out.printf("Tempo total: %.3f ms%n", Timer.toMillis(result.getElapsedNanos()));
        System.out.printf("IGNORANT=%d, SPREADER=%d, INACTIVE=%d, GROK=%d, BOT=%d, INFLUENCER=%d, ECHO=%d, FACT_CHECKER=%d, JOURNALIST=%d%n",
                result.getIgnorantCount(), result.getSpreaderCount(),
                result.getInactiveCount(), result.getGrokCount(),
                result.getBotCount(), result.getInfluencerCount(),
                result.getEchoChamberCount(), result.getFactCheckerCount(),
                result.getJournalistCount());
        System.out.println("Neutralizados por influencia GROK: " + result.getNeutralizedByGrokCount());
    }

    private static List<DistributedSimulation.WorkerAddress> parseWorkerAddresses(String[] args) {
        if (args.length <= 1) {
            return DistributedSimulation.localWorkers(2, 9100);
        }

        List<DistributedSimulation.WorkerAddress> addresses = new ArrayList<>();
        for (int index = 1; index < args.length; index++) {
            String[] parts = args[index].split(":");
            if (parts.length < 2 || parts.length > 3) {
                throw new IllegalArgumentException("Use host:porta ou host:porta:nome");
            }
            String name = parts.length == 3 ? parts[2] : "worker";
            addresses.add(new DistributedSimulation.WorkerAddress(parts[0], Integer.parseInt(parts[1]), name));
        }
        return addresses;
    }
}
