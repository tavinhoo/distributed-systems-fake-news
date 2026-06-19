package parallel;

import core.Statistics;
import model.CellState;
import model.SimulationConfig;
import model.SimulationResult;
import util.Timer;

public class ParallelSimulation {
    private final int threadCount;

    public ParallelSimulation(int threadCount) {
        this.threadCount = Math.max(1, threadCount);
    }

    public SimulationResult run(CellState[][] initialGrid, SimulationConfig config) throws InterruptedException {
        Timer timer = new Timer();
        timer.start();

        CellState[][] currentGrid = initialGrid;
        CellState[][] nextGrid = new CellState[config.getRows()][config.getColumns()];
        int neutralizedByGrok = 0;

        for (int generation = 0; generation < config.getGenerations(); generation++) {
            Thread[] threads = new Thread[threadCount];
            MatrixWorker[] workers = new MatrixWorker[threadCount];

            for (int index = 0; index < threadCount; index++) {
                int startRow = index * config.getRows() / threadCount;
                int endRow = (index + 1) * config.getRows() / threadCount;

                MatrixWorker worker = new MatrixWorker(
                        currentGrid, nextGrid, config, generation, startRow, endRow);
                workers[index] = worker;
                threads[index] = new Thread(worker, "matrix-worker-" + index);
                threads[index].start();
            }

            // A proxima geracao so inicia depois que todas as threads terminarem.
            for (Thread thread : threads) {
                thread.join();
            }

            for (MatrixWorker worker : workers) {
                neutralizedByGrok += worker.getNeutralizedByGrok();
            }

            CellState[][] temp = currentGrid;
            currentGrid = nextGrid;
            nextGrid = temp;
        }

        return Statistics.buildResult(currentGrid, config.getGenerations(), timer.elapsedNanos(), neutralizedByGrok);
    }
}
