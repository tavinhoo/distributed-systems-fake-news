package distributed;

import core.SimulationRules;
import model.CellState;
import model.SimulationConfig;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class MatrixWorkerImpl extends UnicastRemoteObject implements MatrixWorkerRemote {
    public MatrixWorkerImpl() throws RemoteException {
        super();
    }

    @Override
    public WorkerResult computeRange(WorkerTask task) throws RemoteException {
        SimulationConfig config = task.getConfig();
        CellState[][] block = task.getBlockWithGhostRows();
        CellState[][] computedRows = new CellState[task.getInnerRowCount()][config.getColumns()];
        int neutralizedByGrok = 0;

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

        return new WorkerResult(task.getGlobalStartRow(), computedRows, neutralizedByGrok);
    }
}
