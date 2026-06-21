package distributed;

import java.net.InetAddress;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.CountDownLatch;

public class WorkerServer {
    public static final String DEFAULT_WORKER_NAME = "worker";

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9100;
        String workerName = args.length > 1 ? args[1] : DEFAULT_WORKER_NAME;
        String advertisedHost = args.length > 2 ? args[2] : resolveLocalHost();

        System.setProperty("java.rmi.server.hostname", advertisedHost);

        log("iniciando WorkerServer | host=%s | porta=%d | nome=%s", advertisedHost, port, workerName);
        Registry registry = LocateRegistry.createRegistry(port);
        MatrixWorkerImpl worker = new MatrixWorkerImpl(workerName, 0);
        registry.rebind(workerName, worker);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(registry, worker, port, workerName)));

        log("MatrixWorker publicado no registry RMI | host=%s | porta=%d | nome=%s", advertisedHost, port, workerName);
        new CountDownLatch(1).await();
    }

    private static void log(String format, Object... args) {
        System.out.printf("[%tT.%<tL] [WorkerServer] %s%n",
                System.currentTimeMillis(), String.format(format, args));
    }

    private static String resolveLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception exception) {
            return "127.0.0.1";
        }
    }

    private static void shutdown(Registry registry, MatrixWorkerImpl worker, int port, String workerName) {
        try {
            registry.unbind(workerName);
        } catch (Exception ignored) {
        }

        try {
            UnicastRemoteObject.unexportObject(worker, true);
        } catch (Exception ignored) {
        }

        try {
            UnicastRemoteObject.unexportObject(registry, true);
        } catch (Exception ignored) {
        }

        log("WorkerServer encerrado | porta=%d | nome=%s", port, workerName);
    }
}
