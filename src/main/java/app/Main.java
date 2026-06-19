package app;

import core.GridFactory;
import model.CellState;
import model.SimulationConfig;
import model.SimulationResult;
import parallel.ParallelSimulation;
import sequential.SequentialSimulation;
import util.Timer;

public class Main {
    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0].toLowerCase() : "sequential";
        SimulationConfig config = SimulationConfig.defaultConfig();
        CellState[][] initialGrid = GridFactory.createInitialGrid(config);

        SimulationResult result;
        if ("parallel".equals(mode)) {
            int threads = args.length > 1 ? Integer.parseInt(args[1]) : 4;
            result = new ParallelSimulation(threads).run(initialGrid, config);
        } else {
            result = new SequentialSimulation().run(initialGrid, config);
        }

        printResult(mode, result);
    }

    private static void printResult(String mode, SimulationResult result) {
        System.out.println("Modo: " + mode);
        System.out.printf("Tempo total: %.3f ms%n", Timer.toMillis(result.getElapsedNanos()));
        System.out.printf("IGNORANT=%d, SPREADER=%d, INACTIVE=%d, GROK=%d%n",
                result.getIgnorantCount(), result.getSpreaderCount(),
                result.getInactiveCount(), result.getGrokCount());
        System.out.println("Neutralizados por influencia GROK: " + result.getNeutralizedByGrokCount());
    }
}
