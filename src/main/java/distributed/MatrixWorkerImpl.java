package distributed;

import core.SimulationRules;
import model.CellState;
import model.SimulationConfig;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class MatrixWorkerImpl extends UnicastRemoteObject implements MatrixWorkerRemote {
    private final String workerName;

    public MatrixWorkerImpl() throws RemoteException {
        this("worker-local", 0);
    }

    public MatrixWorkerImpl(int exportPort) throws RemoteException {
        super(exportPort);
        this.workerName = "worker";
        log("worker remoto iniciado | exportPort=%d", exportPort);
    }

    public MatrixWorkerImpl(String workerName, int exportPort) throws RemoteException {
        super(exportPort);
        this.workerName = workerName;
        log("worker remoto iniciado | exportPort=%d", exportPort);
    }

    @Override
    public WorkerResult computeRange(WorkerTask task) throws RemoteException {
        SimulationConfig config = task.getConfig();
        CellState[][] block = task.getBlockWithGhostRows();
        CellState[][] computedRows = new CellState[task.getInnerRowCount()][config.getColumns()];
        int neutralizedByGrok = 0;
        int startRow = task.getGlobalStartRow();
        int endRow = startRow + task.getInnerRowCount();

        log("recebeu bloco da geração %d | linhas globais [%d, %d) | linhas com ghost=%d",
                task.getGeneration(), startRow, endRow, block.length);
        log("iniciando processamento | linhas [%d, %d) | colunas=%d",
                startRow, endRow, config.getColumns());
        long startNanos = System.nanoTime();

        for (int localRow = 0; localRow < task.getInnerRowCount(); localRow++) {
            int blockRow = task.getFirstInnerRow() + localRow;
            int globalRow = task.getGlobalStartRow() + localRow;

            for (int col = 0; col < config.getColumns(); col++) {
                if (SimulationRules.wasNeutralizedByGrok(
                        block, blockRow, col, task.getGeneration(), config, globalRow)) {
                    neutralizedByGrok++;
                }
                computedRows[localRow][col] = SimulationRules.nextState(
                        block, blockRow, col, task.getGeneration(), config, globalRow);
            }
        }

        long processingNanos = System.nanoTime() - startNanos;
        log("finalizou processamento | linhas [%d, %d) | tempo=%.3f ms | neutralizados=%d",
                startRow, endRow, processingNanos / 1_000_000.0, neutralizedByGrok);
        log("devolvendo resultado ao coordenador | linhas [%d, %d)",
                startRow, endRow);
        return new WorkerResult(task.getGlobalStartRow(), computedRows, neutralizedByGrok, processingNanos);
    }

    private void log(String format, Object... args) {
        System.out.printf("[%tT.%<tL] [MatrixWorker:%s] %s%n",
                System.currentTimeMillis(), workerName, String.format(format, args));
    }
}
