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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BenchmarkRunner {
    private static final String CSV_FILE = "benchmark-results.csv";
    private static final String ENVIRONMENT_FILE = "experimental-environment.txt";
    private static final String RESULTS_FILE = "RESULTADOS.md";
    private static final String DEFAULT_REMOTE_WORKER = "192.168.18.76:9100:worker";

    public static void main(String[] args) throws Exception {
        System.setProperty("sun.rmi.transport.tcp.connectTimeout", "5000");
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", "10000");

        if (args.length > 0 && "batch".equalsIgnoreCase(args[0])) {
            runBatchWithLocalWorkers(args);
            return;
        }

        if (args.length > 0 && "batch-remote".equalsIgnoreCase(args[0])) {
            String workerList = args.length > 1 ? args[1] : DEFAULT_REMOTE_WORKER;
            runBatch("RMI remoto", DistributedSimulation.parseWorkerAddresses(workerList));
            return;
        }

        if (args.length > 0 && "batch-all".equalsIgnoreCase(args[0])) {
            int basePort = args.length > 1 ? Integer.parseInt(args[1]) : 9200;
            List<DistributedSimulation.WorkerAddress> remoteWorkers =
                    DistributedSimulation.parseWorkerAddresses(args.length > 2 ? args[2] : DEFAULT_REMOTE_WORKER);
            runBatchAll(basePort, remoteWorkers);
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
        List<DistributedSimulation.WorkerAddress> addresses = DistributedSimulation.localWorkers(workers, basePort);
        List<LocalWorker> localWorkers = startLocalWorkers(workers, basePort);
        try {
            runBatch("RMI local", addresses, new Scenario[]{
                    new Scenario("padrao", config, new int[]{threads}, workerPrefixes(addresses))
            });
        } finally {
            stopLocalWorkers(localWorkers);
        }
    }

    private static void runBatchWithLocalWorkers(String[] args) throws Exception {
        int basePort = args.length > 1 ? Integer.parseInt(args[1]) : 9100;
        int workers = args.length > 2 ? Integer.parseInt(args[2]) : 4;
        List<LocalWorker> localWorkers = startLocalWorkers(workers, basePort);
        try {
            runBatch("RMI local", DistributedSimulation.localWorkers(workers, basePort));
        } finally {
            stopLocalWorkers(localWorkers);
        }
    }

    private static void runBatchAll(int basePort,
                                    List<DistributedSimulation.WorkerAddress> remoteWorkers) throws Exception {
        List<LocalWorker> localWorkers = startLocalWorkers(2, basePort);
        List<DistributedSimulation.WorkerAddress> allWorkers =
                new ArrayList<>(DistributedSimulation.localWorkers(2, basePort));
        allWorkers.addAll(remoteWorkers);
        try {
            runBatch("RMI local + remoto", allWorkers);
        } finally {
            stopLocalWorkers(localWorkers);
        }
    }

    private static void runBatch(String distributionLabel,
                                 List<DistributedSimulation.WorkerAddress> workerAddresses) throws Exception {
        runBatch(distributionLabel, workerAddresses, defaultScenarios(workerAddresses));
    }

    private static void runBatch(String distributionLabel,
                                 List<DistributedSimulation.WorkerAddress> workerAddresses,
                                 Scenario[] scenarios) throws Exception {
        CsvWriter.writeBenchmarkHeader(CSV_FILE);
        writeEnvironmentFile(distributionLabel, workerAddresses);

        List<BenchmarkRecord> records = new ArrayList<>();
        warmUp(workerAddresses, records);

        for (Scenario scenario : scenarios) {
            runScenario(scenario, records);
        }

        writeResultsMarkdown(distributionLabel, workerAddresses, records);
        System.out.println("CSV de experimentos gerado em: " + CSV_FILE);
        System.out.println("Ambiente registrado em: " + ENVIRONMENT_FILE);
        System.out.println("Relatorio gerado em: " + RESULTS_FILE);
    }

    private static Scenario[] defaultScenarios(List<DistributedSimulation.WorkerAddress> workerAddresses) {
        List<List<DistributedSimulation.WorkerAddress>> workerGroups = workerPrefixes(workerAddresses);
        return new Scenario[]{
                new Scenario("base_80x80", new SimulationConfig(80, 80, 100, 0.02, 0.03,
                        0.015, 0.01, 0.25, 0.50, 42L), new int[]{2, 4, 8}, workerGroups),
                new Scenario("matriz_120x120", new SimulationConfig(120, 120, 100, 0.02, 0.03,
                        0.015, 0.01, 0.25, 0.50, 42L), new int[]{2, 4, 8}, workerGroups),
                new Scenario("matriz_180x180", new SimulationConfig(180, 180, 100, 0.02, 0.03,
                        0.015, 0.01, 0.25, 0.50, 42L), new int[]{2, 4, 8}, workerGroups),
                new Scenario("geracoes_200", new SimulationConfig(80, 80, 200, 0.02, 0.03,
                        0.015, 0.01, 0.25, 0.50, 42L), new int[]{2, 4, 8}, workerGroups),
                new Scenario("espalhadores_5pct", new SimulationConfig(80, 80, 100, 0.05, 0.03,
                        0.015, 0.01, 0.25, 0.50, 42L), new int[]{2, 4, 8}, workerGroups)
        };
    }

    private static void warmUp(List<DistributedSimulation.WorkerAddress> workerAddresses,
                               List<BenchmarkRecord> records) {
        SimulationConfig config = new SimulationConfig(30, 30, 10, 0.02, 0.03,
                0.015, 0.01, 0.25, 0.50, 42L);
        try {
            new SequentialSimulation().run(GridFactory.createInitialGrid(config), config);
            new ParallelSimulation(2).run(GridFactory.createInitialGrid(config), config);
            new DistributedSimulation(workerAddresses.subList(0, 1)).run(GridFactory.createInitialGrid(config), config);
        } catch (Exception exception) {
            String message = shortError(exception);
            records.add(BenchmarkRecord.failure("aquecimento_distribuido", "Distribuida", config,
                    Math.min(1, workerAddresses.size()), "nao_aplicavel", message));
            System.out.println("Aviso: aquecimento distribuido falhou: " + message);
        }
    }

    private static void runScenario(Scenario scenario,
                                    List<BenchmarkRecord> records) throws Exception {
        SimulationConfig config = scenario.config();
        System.out.println();
        System.out.println("Cenario: " + scenario.name());

        SimulationResult sequential = new SequentialSimulation().run(GridFactory.createInitialGrid(config), config);
        BenchmarkRecord sequentialRecord = BenchmarkRecord.success(scenario.name(), "Sequencial", config,
                1, Timer.toMillis(sequential.getElapsedNanos()), 1.0, 1.0,
                "referencia", sequential, "");
        records.add(sequentialRecord);
        writeCsvLine(sequentialRecord);
        printLine(sequentialRecord);

        for (int threads : scenario.threadCounts()) {
            try {
                SimulationResult parallel = new ParallelSimulation(threads)
                        .run(GridFactory.createInitialGrid(config), config);
                writeComparedRecord(scenario.name(), "Paralela", config, sequential, parallel, threads, records);
            } catch (Exception exception) {
                BenchmarkRecord record = BenchmarkRecord.failure(scenario.name(), "Paralela", config,
                        threads, "falhou", shortError(exception));
                records.add(record);
                writeCsvLine(record);
                printLine(record);
            }
        }

        for (List<DistributedSimulation.WorkerAddress> workers : scenario.workerGroups()) {
            try {
                SimulationResult distributed = new DistributedSimulation(workers)
                        .run(GridFactory.createInitialGrid(config), config);
                writeComparedRecord(scenario.name(), "Distribuida", config, sequential, distributed,
                        workers.size(), records);
            } catch (Exception exception) {
                BenchmarkRecord record = BenchmarkRecord.failure(scenario.name(), "Distribuida", config,
                        workers.size(), "falhou", shortError(exception));
                records.add(record);
                writeCsvLine(record);
                printLine(record);
            }
        }
    }

    private static void writeComparedRecord(String scenarioName,
                                            String version,
                                            SimulationConfig config,
                                            SimulationResult sequential,
                                            SimulationResult current,
                                            int units,
                                            List<BenchmarkRecord> records) throws Exception {
        double sequentialMillis = Timer.toMillis(sequential.getElapsedNanos());
        double currentMillis = Timer.toMillis(current.getElapsedNanos());
        double speedup = sequentialMillis / currentMillis;
        double efficiency = speedup / units;
        String finalGridMatch = gridsMatch(sequential.getFinalGrid(), current.getFinalGrid()) ? "sim" : "nao";

        BenchmarkRecord record = BenchmarkRecord.success(scenarioName, version, config, units,
                currentMillis, speedup, efficiency, finalGridMatch, current, "");
        records.add(record);
        writeCsvLine(record);
        printLine(record);
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

    private static List<List<DistributedSimulation.WorkerAddress>> workerPrefixes(
            List<DistributedSimulation.WorkerAddress> workerAddresses) {
        List<List<DistributedSimulation.WorkerAddress>> groups = new ArrayList<>();
        for (int count = 1; count <= workerAddresses.size(); count++) {
            groups.add(new ArrayList<>(workerAddresses.subList(0, count)));
        }
        return groups;
    }

    private static void writeCsvLine(BenchmarkRecord record) throws Exception {
        CsvWriter.appendBenchmarkLine(CSV_FILE, record.scenario(), record.version(), record.status(),
                record.config().getRows(), record.config().getColumns(), record.config().getGenerations(),
                record.config().getInitialSpreaderRate(), record.units(), record.totalMillis(),
                record.speedup(), record.efficiency(), record.finalGridMatch(),
                record.ignorant(), record.spreader(), record.inactive(), record.grok(),
                record.bot(), record.influencer(), record.echoChamber(), record.factChecker(),
                record.journalist(), record.neutralizedByGrok(), record.error());
    }

    private static void writeEnvironmentFile(String distributionLabel,
                                             List<DistributedSimulation.WorkerAddress> workerAddresses) throws Exception {
        Runtime runtime = Runtime.getRuntime();
        try (PrintWriter writer = new PrintWriter(new FileWriter(ENVIRONMENT_FILE))) {
            writer.println("Execucao: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            writer.println("Sistema operacional medido: " + System.getProperty("os.name")
                    + " " + System.getProperty("os.version")
                    + " (" + System.getProperty("os.arch") + ")");
            writer.println("Java: " + System.getProperty("java.version")
                    + " - " + System.getProperty("java.vendor"));
            writer.println("Processadores logicos disponiveis na JVM: " + runtime.availableProcessors());
            writer.println("Memoria maxima da JVM (MB): " + runtime.maxMemory() / (1024 * 1024));
            writer.println("Coordenador informado: Windows 11, Ryzen 5 7600X, 32 GB RAM 6000 MHz");
            writer.println("Worker distribuido informado: Fedora, Intel i5-12450H, 20 GB RAM 3200 MHz");
            writer.println("Modo distribuido: " + distributionLabel);
            writer.println("Workers RMI usados: " + workerAddresses);
        }
    }

    private static void writeResultsMarkdown(String distributionLabel,
                                             List<DistributedSimulation.WorkerAddress> workerAddresses,
                                             List<BenchmarkRecord> records) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(RESULTS_FILE))) {
            writer.println("# Resultados Experimentais");
            writer.println();
            writer.println("Gerado automaticamente em " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ".");
            writer.println();
            writer.println("## Configuracao experimental");
            writer.println();
            writer.println("- Coordenador: Windows 11, Ryzen 5 7600X, 32 GB RAM 6000 MHz.");
            writer.println("- Worker distribuido: Fedora, Intel i5-12450H, 20 GB RAM 3200 MHz.");
            writer.println("- Enderecos RMI usados: `" + workerAddresses + "`.");
            writer.println("- Modo distribuido medido: " + distributionLabel + ".");
            writer.println("- JVM: " + System.getProperty("java.version") + " - " + System.getProperty("java.vendor") + ".");
            writer.println("- Metodo: cada cenario executa primeiro a versao sequencial; speedup = tempo sequencial / tempo da versao; eficiencia = speedup / unidades.");
            writer.println("- Validacao: as versoes paralela e distribuida comparam a matriz final exata contra a sequencial quando a execucao termina.");
            writer.println();
            writer.println("## Tabela comparativa");
            writer.println();
            writer.println("| Cenario | Modo | Status | Matriz | Geracoes | Espalhadores iniciais | Unidades | Tempo (ms) | Speedup | Eficiencia | Matriz final igual | Erro |");
            writer.println("|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|");
            for (BenchmarkRecord record : records) {
                writer.printf(Locale.US,
                        "| %s | %s | %s | %dx%d | %d | %.2f%% | %d | %s | %s | %s | %s | %s |%n",
                        record.scenario(), record.version(), record.status(),
                        record.config().getRows(), record.config().getColumns(),
                        record.config().getGenerations(),
                        record.config().getInitialSpreaderRate() * 100.0,
                        record.units(), mdNumber(record.totalMillis()),
                        mdNumber(record.speedup()), mdNumber(record.efficiency()),
                        record.finalGridMatch(), escapeMd(record.error()));
            }
            writer.println();
            writeAnalysis(writer, records);
        }
    }

    private static void writeAnalysis(PrintWriter writer, List<BenchmarkRecord> records) {
        List<BenchmarkRecord> successes = records.stream()
                .filter(record -> "OK".equals(record.status()))
                .toList();
        List<BenchmarkRecord> failures = records.stream()
                .filter(record -> !"OK".equals(record.status()))
                .toList();

        writer.println("## Analise de desempenho");
        writer.println();
        writer.println(bestSummary(successes, "Paralela"));
        writer.println(bestSummary(successes, "Distribuida"));
        writer.println();
        writer.println("## Analise de gargalos");
        writer.println();
        writer.println("- A versao paralela cria e sincroniza threads a cada geracao; em matrizes pequenas, esse custo compete com o processamento util.");
        writer.println("- A versao distribuida transfere blocos da matriz em todas as geracoes, incluindo linhas fantasmas. Quando o tempo de rede e serializacao domina, o speedup fica abaixo de 1.");
        writer.println("- Resultados com eficiencia baixa indicam que aumentar unidades nao trouxe ganho proporcional para o tamanho de problema medido.");
        writer.println();
        writer.println("## Analise de sincronizacao");
        writer.println();
        writer.println("- A sequencial e a referencia de corretude.");
        writer.println("- A paralela usa `join()` como barreira ao fim de cada geracao; nenhuma geracao seguinte comeca antes de todas as faixas terminarem.");
        writer.println("- A distribuida espera todos os workers RMI devolverem seus blocos antes de montar a matriz da proxima geracao.");
        writer.println("- As execucoes concluidas compararam a matriz final exata contra a sequencial; divergencias aparecem na coluna `Matriz final igual`.");
        writer.println();
        writer.println("## Analise do custo de comunicacao");
        writer.println();
        writer.println("- No RMI, cada geracao envia uma fatia da matriz para cada worker e recebe outra fatia calculada. Esse custo cresce com matriz, geracoes e quantidade de workers.");
        writer.println("- O uso de linhas fantasmas preserva a vizinhanca entre fronteiras, mas aumenta o volume serializado.");
        writer.println("- Com poucos workers ou matriz pequena, o custo fixo da chamada remota pode superar o ganho de processamento distribuido.");
        writer.println();
        writer.println("## Limitacoes observadas");
        writer.println();
        if (failures.isEmpty()) {
            writer.println("- Nenhum teste registrou falha de execucao.");
        } else {
            for (BenchmarkRecord failure : failures) {
                writer.printf("- `%s` / `%s` com %d unidade(s): %s.%n",
                        failure.scenario(), failure.version(), failure.units(), escapeMd(failure.error()));
            }
        }
        writer.println("- As medicoes foram feitas em uma unica rodada por configuracao; para estatistica mais robusta, recomenda-se repetir cada cenario e reportar media e desvio padrao.");
        writer.println("- O worker remoto disponivel informa apenas um processo RMI quando usado sozinho; portanto, a variacao distribuida fica limitada aos enderecos informados ao runner.");
    }

    private static String bestSummary(List<BenchmarkRecord> records, String version) {
        return records.stream()
                .filter(record -> version.equals(record.version()))
                .max((first, second) -> Double.compare(first.speedup(), second.speedup()))
                .map(record -> String.format(Locale.US,
                        "- Melhor resultado %s: cenario `%s`, %d unidade(s), %.4f ms, speedup %.4f e eficiencia %.4f.",
                        version.toLowerCase(Locale.ROOT), record.scenario(), record.units(),
                        record.totalMillis(), record.speedup(), record.efficiency()))
                .orElse("- Nao houve execucao " + version.toLowerCase(Locale.ROOT) + " concluida com sucesso.");
    }

    private static void printLine(BenchmarkRecord record) {
        if ("OK".equals(record.status())) {
            System.out.printf(Locale.US,
                    "%-12s tempo=%9.3f ms | speedup=%6.3f | eficiencia=%6.3f | unidades=%d | matriz=%s%n",
                    record.version(), record.totalMillis(), record.speedup(), record.efficiency(),
                    record.units(), record.finalGridMatch());
        } else {
            System.out.printf("%-12s FALHOU | unidades=%d | erro=%s%n",
                    record.version(), record.units(), record.error());
        }
    }

    private static boolean gridsMatch(CellState[][] expected, CellState[][] current) {
        if (expected.length != current.length) {
            return false;
        }
        for (int row = 0; row < expected.length; row++) {
            if (!Arrays.equals(expected[row], current[row])) {
                return false;
            }
        }
        return true;
    }

    private static String shortError(Exception exception) {
        String message = exception.getClass().getSimpleName();
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            message += ": " + exception.getMessage();
        }
        return message.replace('\n', ' ').replace('\r', ' ');
    }

    private static String mdNumber(double value) {
        if (Double.isNaN(value)) {
            return "";
        }
        return String.format(Locale.US, "%.4f", value);
    }

    private static String escapeMd(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", "\\|");
    }

    private record Scenario(String name,
                            SimulationConfig config,
                            int[] threadCounts,
                            List<List<DistributedSimulation.WorkerAddress>> workerGroups) {
    }

    private record LocalWorker(Registry registry, MatrixWorkerImpl remote) {
    }

    private record BenchmarkRecord(String scenario,
                                   String version,
                                   String status,
                                   SimulationConfig config,
                                   int units,
                                   double totalMillis,
                                   double speedup,
                                   double efficiency,
                                   String finalGridMatch,
                                   int ignorant,
                                   int spreader,
                                   int inactive,
                                   int grok,
                                   int bot,
                                   int influencer,
                                   int echoChamber,
                                   int factChecker,
                                   int journalist,
                                   int neutralizedByGrok,
                                   String error) {
        private static BenchmarkRecord success(String scenario,
                                               String version,
                                               SimulationConfig config,
                                               int units,
                                               double totalMillis,
                                               double speedup,
                                               double efficiency,
                                               String finalGridMatch,
                                               SimulationResult result,
                                               String error) {
            return new BenchmarkRecord(scenario, version, "OK", config, units, totalMillis,
                    speedup, efficiency, finalGridMatch, result.getIgnorantCount(),
                    result.getSpreaderCount(), result.getInactiveCount(), result.getGrokCount(),
                    result.getBotCount(), result.getInfluencerCount(), result.getEchoChamberCount(),
                    result.getFactCheckerCount(), result.getJournalistCount(),
                    result.getNeutralizedByGrokCount(), error);
        }

        private static BenchmarkRecord failure(String scenario,
                                               String version,
                                               SimulationConfig config,
                                               int units,
                                               String finalGridMatch,
                                               String error) {
            return new BenchmarkRecord(scenario, version, "FALHA", config, units, Double.NaN,
                    Double.NaN, Double.NaN, finalGridMatch, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, error);
        }
    }
}
