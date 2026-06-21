package app;

import distributed.DistributedSimulation;
import distributed.MatrixWorkerImpl;
import distributed.WorkerServer;
import core.GridFactory;
import model.CellState;
import model.SimulationConfig;
import model.SimulationResult;
import parallel.ParallelSimulation;
import sequential.SequentialSimulation;
import util.Timer;

import java.util.ArrayList;
import java.util.List;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

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
            List<DistributedSimulation.WorkerAddress> addresses = parseWorkerAddresses(args);
            if (allLocal(addresses)) {
                List<LocalWorkerHandle> localWorkers = null;
                try {
                    if (allWorkersReachable(addresses)) {
                        result = new DistributedSimulation(addresses).run(initialGrid, config);
                    } else if (noneWorkersReachable(addresses)) {
                        localWorkers = startLocalWorkers(addresses);
                        result = new DistributedSimulation(addresses).run(initialGrid, config);
                    } else {
                        throw new IllegalStateException("Os workers locais estao parcialmente ativos. Pare os processos manuais ou deixe todos fechados para permitir o auto-inicio.");
                    }
                } finally {
                    if (localWorkers != null) {
                        stopLocalWorkers(localWorkers);
                    }
                }
            } else {
                result = new DistributedSimulation(addresses).run(initialGrid, config);
            }
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

    private static boolean allLocal(List<DistributedSimulation.WorkerAddress> addresses) {
        for (DistributedSimulation.WorkerAddress address : addresses) {
            String host = address.host().trim().toLowerCase();
            if (!"localhost".equals(host) && !"127.0.0.1".equals(host) && !"::1".equals(host)) {
                return false;
            }
        }
        return true;
    }

    private static boolean allWorkersReachable(List<DistributedSimulation.WorkerAddress> addresses) {
        for (DistributedSimulation.WorkerAddress address : addresses) {
            if (!isWorkerReachable(address)) {
                return false;
            }
        }
        return true;
    }

    private static boolean noneWorkersReachable(List<DistributedSimulation.WorkerAddress> addresses) {
        for (DistributedSimulation.WorkerAddress address : addresses) {
            if (isWorkerReachable(address)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWorkerReachable(DistributedSimulation.WorkerAddress address) {
        try {
            Registry registry = LocateRegistry.getRegistry(address.host(), address.port());
            registry.lookup(address.name());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private static List<LocalWorkerHandle> startLocalWorkers(List<DistributedSimulation.WorkerAddress> addresses) throws Exception {
        List<LocalWorkerHandle> workers = new ArrayList<>();
        if (!addresses.isEmpty()) {
            System.setProperty("java.rmi.server.hostname", addresses.get(0).host());
        }
        for (DistributedSimulation.WorkerAddress address : addresses) {
            Registry registry = LocateRegistry.createRegistry(address.port());
            MatrixWorkerImpl worker = new MatrixWorkerImpl(address.name(), 0);
            registry.rebind(address.name(), worker);
            workers.add(new LocalWorkerHandle(address, registry, worker));
        }
        return workers;
    }

    private static void stopLocalWorkers(List<LocalWorkerHandle> workers) throws Exception {
        for (LocalWorkerHandle worker : workers) {
            try {
                worker.registry().unbind(worker.address().name());
            } catch (Exception ignored) {
            }
            UnicastRemoteObject.unexportObject(worker.remote(), true);
            UnicastRemoteObject.unexportObject(worker.registry(), true);
        }
    }

    private record LocalWorkerHandle(DistributedSimulation.WorkerAddress address,
                                     Registry registry,
                                     MatrixWorkerImpl remote) {
    }
}
