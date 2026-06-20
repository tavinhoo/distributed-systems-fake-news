package app;

import core.GridFactory;
import core.SimulationRules;
import distributed.DistributedSimulation;
import distributed.MatrixWorkerImpl;
import distributed.WorkerServer;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.CellState;
import model.SimulationConfig;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class SimulationViewer extends Application {
    private static final int ROWS = 60;
    private static final int COLUMNS = 60;
    private static final int GENERATIONS = 120;
    private static final int THREADS = 4;
    private static final int RMI_WORKERS = 2;
    private static final int RMI_BASE_PORT = 9600;

    private final Canvas canvas = new Canvas(660, 660);
    private final Label statusLabel = new Label();
    private final Label ignorantLabel = new Label();
    private final Label spreaderLabel = new Label();
    private final Label inactiveLabel = new Label();
    private final Label grokLabel = new Label();
    private final Label whatsAppGroupLabel = new Label();
    private final Label influencerLabel = new Label();
    private final Label progressPercentLabel = new Label("0%");
    private final Label resultModeLabel = new Label("-");
    private final Label resultTimeLabel = new Label("-");
    private final Label resultUnitsLabel = new Label("-");
    private final Label resultMemoryLabel = new Label("-");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final ComboBox<String> modeBox = new ComboBox<>();
    private final List<LocalWorker> localWorkers = new ArrayList<>();
    private Button runButton;

    private SimulationConfig config;
    private CellState[][] currentGrid;
    private int generation;
    private long runStartNanos;
    private Timeline timeline;
    private DistributedSimulation distributedSimulation;

    @Override
    public void start(Stage stage) {
        modeBox.getItems().addAll("Sequencial", "Paralela", "Distribuida RMI");
        modeBox.setValue("Sequencial");
        modeBox.setPrefWidth(155);

        runButton = new Button("Iniciar");
        Button stepButton = new Button("Passo");
        Button resetButton = new Button("Reiniciar");

        timeline = new Timeline(new KeyFrame(Duration.millis(180), event -> nextStep()));
        timeline.setCycleCount(Animation.INDEFINITE);

        runButton.setOnAction(event -> toggleRun(runButton));
        stepButton.setOnAction(event -> nextStep());
        resetButton.setOnAction(event -> reset());

        HBox controls = new HBox(10, modeBox, runButton, stepButton, resetButton);
        controls.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Propagacao de Fake News");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        statusLabel.setStyle("-fx-text-fill: #cbd5e1;");

        VBox header = new VBox(8, title, controls, statusLabel);
        header.setPadding(new Insets(14));
        header.setStyle("-fx-background-color: #1f2937;");

        VBox sidePanel = new VBox(12);
        sidePanel.setPadding(new Insets(14));
        sidePanel.setPrefWidth(250);
        sidePanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d0d7de; -fx-border-width: 0 0 0 1;");

        Label statsTitle = sectionTitle("Estados");
        progressBar.setPrefWidth(220);
        progressPercentLabel.setMinWidth(220);
        progressPercentLabel.setAlignment(Pos.CENTER_RIGHT);
        progressPercentLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        VBox progressBox = new VBox(5, sectionTitle("Progresso"), progressPercentLabel, progressBar);

        sidePanel.getChildren().addAll(
                progressBox,
                statsTitle,
                counterItem(ignorantLabel, colorOf(CellState.IGNORANT)),
                counterItem(spreaderLabel, colorOf(CellState.SPREADER)),
                counterItem(inactiveLabel, colorOf(CellState.INACTIVE)),
                counterItem(grokLabel, colorOf(CellState.GROK)),
                counterItem(whatsAppGroupLabel, colorOf(CellState.WHATSAPP_GROUP)),
                counterItem(influencerLabel, colorOf(CellState.INFLUENCER)),
                sectionTitle("Legenda"),
                legendItem("Ignorante", colorOf(CellState.IGNORANT)),
                legendItem("Espalhador", colorOf(CellState.SPREADER)),
                legendItem("Inativo", colorOf(CellState.INACTIVE)),
                legendItem("GROK", colorOf(CellState.GROK)),
                legendItem("Grupo WhatsApp", colorOf(CellState.WHATSAPP_GROUP)),
                legendItem("Influenciador", colorOf(CellState.INFLUENCER)),
                sectionTitle("Resultado final"),
                resultModeLabel,
                resultTimeLabel,
                resultUnitsLabel,
                resultMemoryLabel);

        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setPadding(new Insets(16));
        canvasPane.setStyle("-fx-background-color: #0f172a;");

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(canvasPane);
        root.setRight(sidePanel);

        reset();

        stage.setTitle("Propagacao de Fake News");
        stage.setScene(new Scene(root));
        stage.setOnCloseRequest(event -> stopWorkers());
        stage.show();
    }

    private void toggleRun(Button runButton) {
        if (timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.pause();
            runButton.setText("Iniciar");
        } else {
            if (generation >= config.getGenerations()) {
                reset();
            }
            if (runStartNanos == 0) {
                runStartNanos = System.nanoTime();
            }
            timeline.play();
            runButton.setText("Pausar");
        }
    }

    private void reset() {
        timeline.stop();
        generation = 0;
        runStartNanos = 0;
        config = new SimulationConfig(ROWS, COLUMNS, GENERATIONS, 0.03, 0.03,
                0.015, 0.01, 0.25, 0.50, 42L);
        currentGrid = GridFactory.createInitialGrid(config);
        runButton.setText("Iniciar");
        clearResults();
        drawGrid();
        updateStatus();
    }

    private void nextStep() {
        if (generation >= config.getGenerations()) {
            finishSimulation();
            return;
        }

        try {
            if (runStartNanos == 0) {
                runStartNanos = System.nanoTime();
            }
            String mode = modeBox.getValue();
            if ("Paralela".equals(mode)) {
                currentGrid = nextParallelGeneration();
            } else if ("Distribuida RMI".equals(mode)) {
                currentGrid = nextDistributedGeneration();
            } else {
                currentGrid = nextSequentialGeneration();
            }
            generation++;
            drawGrid();
            updateStatus();
            if (generation >= config.getGenerations()) {
                finishSimulation();
            }
        } catch (Exception exception) {
            timeline.stop();
            runButton.setText("Iniciar");
            statusLabel.setText("Erro: " + exception.getMessage());
        }
    }

    private void finishSimulation() {
        timeline.stop();
        runButton.setText("Iniciar");
        updateResults();
    }

    private CellState[][] nextSequentialGeneration() {
        CellState[][] nextGrid = new CellState[config.getRows()][config.getColumns()];
        for (int row = 0; row < config.getRows(); row++) {
            for (int col = 0; col < config.getColumns(); col++) {
                nextGrid[row][col] = SimulationRules.nextState(currentGrid, row, col, generation, config);
            }
        }
        return nextGrid;
    }

    private CellState[][] nextParallelGeneration() throws InterruptedException {
        CellState[][] nextGrid = new CellState[config.getRows()][config.getColumns()];
        Thread[] threads = new Thread[THREADS];

        for (int index = 0; index < THREADS; index++) {
            int startRow = index * config.getRows() / THREADS;
            int endRow = (index + 1) * config.getRows() / THREADS;

            threads[index] = new Thread(() -> {
                for (int row = startRow; row < endRow; row++) {
                    for (int col = 0; col < config.getColumns(); col++) {
                        nextGrid[row][col] = SimulationRules.nextState(currentGrid, row, col, generation, config);
                    }
                }
            });
            threads[index].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        return nextGrid;
    }

    private CellState[][] nextDistributedGeneration() throws Exception {
        if (distributedSimulation == null) {
            startWorkers();
            distributedSimulation = new DistributedSimulation(
                    DistributedSimulation.localWorkers(RMI_WORKERS, RMI_BASE_PORT));
        }
        return distributedSimulation.nextGeneration(currentGrid, config, generation);
    }

    private void startWorkers() throws Exception {
        if (!localWorkers.isEmpty()) {
            return;
        }

        for (int index = 0; index < RMI_WORKERS; index++) {
            Registry registry = LocateRegistry.createRegistry(RMI_BASE_PORT + index);
            MatrixWorkerImpl worker = new MatrixWorkerImpl();
            registry.rebind(WorkerServer.DEFAULT_WORKER_NAME, worker);
            localWorkers.add(new LocalWorker(registry, worker));
        }
    }

    private void stopWorkers() {
        for (LocalWorker worker : localWorkers) {
            try {
                worker.registry().unbind(WorkerServer.DEFAULT_WORKER_NAME);
                UnicastRemoteObject.unexportObject(worker.remote(), true);
                UnicastRemoteObject.unexportObject(worker.registry(), true);
            } catch (Exception ignored) {
                // A janela pode ser fechada antes de iniciar o modo distribuido.
            }
        }
        localWorkers.clear();
    }

    private void drawGrid() {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        double cellWidth = canvas.getWidth() / config.getColumns();
        double cellHeight = canvas.getHeight() / config.getRows();

        graphics.setFill(Color.web("#0b1020"));
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (int row = 0; row < config.getRows(); row++) {
            for (int col = 0; col < config.getColumns(); col++) {
                graphics.setFill(colorOf(currentGrid[row][col]));
                graphics.fillRect(col * cellWidth, row * cellHeight, cellWidth - 0.5, cellHeight - 0.5);
            }
        }

        graphics.setStroke(Color.web("#e5e7eb"));
        graphics.setLineWidth(2);
        graphics.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private Color colorOf(CellState state) {
        if (state == CellState.SPREADER) {
            return Color.web("#ff1744");
        }
        if (state == CellState.INACTIVE) {
            return Color.web("#3f51b5");
        }
        if (state == CellState.GROK) {
            return Color.web("#00e676");
        }
        if (state == CellState.WHATSAPP_GROUP) {
            return Color.web("#ffab00");
        }
        if (state == CellState.INFLUENCER) {
            return Color.web("#d500f9");
        }
        return Color.web("#111111");
    }

    private HBox legendItem(String text, Color color) {
        Rectangle sample = colorSample(color);
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px;");
        HBox item = new HBox(8, sample, label);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private HBox counterItem(Label label, Color color) {
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        HBox item = new HBox(8, colorSample(color), label);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private Rectangle colorSample(Color color) {
        Rectangle sample = new Rectangle(20, 20, color);
        sample.setArcWidth(4);
        sample.setArcHeight(4);
        sample.setStroke(Color.web("#111827"));
        return sample;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        return label;
    }

    private void updateStatus() {
        int ignorant = 0;
        int spreader = 0;
        int inactive = 0;
        int grok = 0;
        int whatsAppGroup = 0;
        int influencer = 0;

        for (CellState[] row : currentGrid) {
            for (CellState state : row) {
                if (state == CellState.IGNORANT) {
                    ignorant++;
                } else if (state == CellState.SPREADER) {
                    spreader++;
                } else if (state == CellState.INACTIVE) {
                    inactive++;
                } else if (state == CellState.GROK) {
                    grok++;
                } else if (state == CellState.WHATSAPP_GROUP) {
                    whatsAppGroup++;
                } else {
                    influencer++;
                }
            }
        }

        statusLabel.setText(String.format(
                "Geracao %d de %d | Modo: %s",
                generation, config.getGenerations(), modeBox.getValue()));
        ignorantLabel.setText("Ignorantes: " + ignorant);
        spreaderLabel.setText("Espalhadores: " + spreader);
        inactiveLabel.setText("Inativos: " + inactive);
        grokLabel.setText("GROK: " + grok);
        whatsAppGroupLabel.setText("Grupos WhatsApp: " + whatsAppGroup);
        influencerLabel.setText("Influenciadores: " + influencer);
        progressBar.setProgress((double) generation / config.getGenerations());
        progressPercentLabel.setText(String.format("%.0f%%", 100.0 * generation / config.getGenerations()));
    }

    private void clearResults() {
        resultModeLabel.setText("Modo: -");
        resultTimeLabel.setText("Tempo: -");
        resultUnitsLabel.setText("Unidades: -");
        resultMemoryLabel.setText("Memoria JVM: -");
    }

    private void updateResults() {
        long elapsedNanos = runStartNanos == 0 ? 0 : System.nanoTime() - runStartNanos;
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemoryMb = runtime.maxMemory() / (1024 * 1024);

        resultModeLabel.setText("Modo: " + modeBox.getValue());
        resultTimeLabel.setText(String.format("Tempo: %.3f ms", elapsedNanos / 1_000_000.0));
        resultUnitsLabel.setText("Unidades: " + executionUnits());
        resultMemoryLabel.setText(String.format("Memoria JVM: %d/%d MB", usedMemoryMb, maxMemoryMb));
    }

    private String executionUnits() {
        if ("Paralela".equals(modeBox.getValue())) {
            return THREADS + " threads";
        }
        if ("Distribuida RMI".equals(modeBox.getValue())) {
            return RMI_WORKERS + " workers RMI";
        }
        return "1 thread";
    }

    private record LocalWorker(Registry registry, MatrixWorkerImpl remote) {
    }

    public static void main(String[] args) {
        launch(args);
    }
}
