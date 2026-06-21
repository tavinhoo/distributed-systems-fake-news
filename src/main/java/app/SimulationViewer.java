package app;

import core.GridFactory;
import core.SimulationRules;
import distributed.DistributedSimulation;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.CellState;
import model.SimulationConfig;

import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SimulationViewer extends Application {
    private static final String MODE_SEQUENTIAL = "Sequencial";
    private static final String MODE_PARALLEL = "Paralela";
    private static final String MODE_DISTRIBUTED = "Distribuida RMI";
    private static final String EXECUTION_VISUAL = "Visual";
    private static final String EXECUTION_BENCHMARK = "Benchmark";
    private static final String EXECUTION_DIDACTIC = "Visualização didática";
    private static final int ROWS = 1000;
    private static final int COLUMNS = 1000;
    private static final int GENERATIONS = 500;
    private static final int THREADS = 12;
    private static final String DEFAULT_RMI_WORKERS = "127.0.0.1:9100, 127.0.0.1:9101";
    private static final double MIN_GRID_ZOOM = 1.0;
    private static final double MAX_GRID_ZOOM = 32.0;
    private static final double AGGREGATED_BLOCK_TARGET_PIXELS = 6.0;
    private static final int DIDACTIC_SEQUENTIAL_ROWS_PER_FRAME = 18;
    private static final int DIDACTIC_WORKER_ROWS_PER_FRAME = 16;
    private static final String SURFACE_DARK = "#111827";
    private static final String SURFACE_DARK_ALT = "#0f172a";
    private static final String SURFACE_LIGHT = "#f8fafc";
    private static final String BORDER_LIGHT = "#d0d7de";

    private final Canvas canvas = new Canvas(1400, 600);
    private final Label statusLabel = new Label();
    private final Label ignorantLabel = new Label();
    private final Label spreaderLabel = new Label();
    private final Label inactiveLabel = new Label();
    private final Label grokLabel = new Label();
    private final Label botLabel = new Label();
    private final Label influencerLabel = new Label();
    private final Label echoChamberLabel = new Label();
    private final Label factCheckerLabel = new Label();
    private final Label journalistLabel = new Label();
    private final Label progressPercentLabel = new Label("0%");
    private final Label resultModeLabel = new Label("-");
    private final Label resultTimeLabel = new Label("-");
    private final Label resultComputeTimeLabel = new Label("-");
    private final Label resultUnitsLabel = new Label("-");
    private final Label resultMemoryLabel = new Label("-");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final LineChart<Number, Number> stateChart;
    private final VBox stateRankingBox = new VBox(8);
    private final XYChart.Series<Number, Number> ignorantSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> spreaderSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> inactiveSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> grokSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> botSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> influencerSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> echoChamberSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> factCheckerSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> journalistSeries = new XYChart.Series<>();
    private final ComboBox<String> modeBox = new ComboBox<>();
    private final ComboBox<String> executionBox = new ComboBox<>();
    private final TextField workerAddressesField = new TextField(DEFAULT_RMI_WORKERS);
    private Button runButton;
    private Button stepButton;

    private SimulationConfig config;
    private CellState[][] currentGrid;
    private int generation;
    private long totalElapsedNanos;
    private long runSegmentStartNanos;
    private long computeElapsedNanos;
    private String activeMode;
    private String activeExecution;
    private double gridZoom = MIN_GRID_ZOOM;
    private double gridViewRow;
    private double gridViewColumn;
    private double dragStartX;
    private double dragStartY;
    private double dragStartViewRow;
    private double dragStartViewColumn;
    private Timeline timeline;
    private Timeline didacticTimeline;
    private Task<BenchmarkOutcome> benchmarkTask;
    private DistributedSimulation distributedSimulation;
    private String distributedWorkerList;
    private String activeWorkerAddresses;
    private final List<LocalWorkerHandle> autoStartedWorkers = new ArrayList<>();
    private boolean didacticStepRunning;
    private CellState[][] didacticSourceGrid;
    private CellState[][] didacticNextGrid;
    private int didacticNextRow;
    private int didacticWorkerCount;
    private int[] didacticWorkerStartRows = new int[0];
    private int[] didacticWorkerEndRows = new int[0];
    private int[] didacticWorkerNextRows = new int[0];
    private String[] didacticWorkerLabels = new String[0];

    private record LocalWorkerHandle(DistributedSimulation.WorkerAddress address,
                                     Registry registry,
                                     distributed.MatrixWorkerImpl worker) {
    }

    public SimulationViewer() {
        NumberAxis generationAxis = new NumberAxis();
        NumberAxis countAxis = new NumberAxis();
        generationAxis.setLabel("Geracao");
        countAxis.setLabel("Quantidade");

        stateChart = new LineChart<>(generationAxis, countAxis);
        stateChart.setTitle("Evolução dos agentes");
        stateChart.setCreateSymbols(false);
        stateChart.setAnimated(false);
        stateChart.setLegendVisible(true);
        stateChart.setPrefHeight(260);
        stateChart.setStyle("-fx-background-color: " + SURFACE_LIGHT
                + "; -fx-padding: 10; -fx-background-radius: 6; -fx-border-radius: 6;"
                + " -fx-border-color: " + BORDER_LIGHT + ";");
        generationAxis.setStyle("-fx-tick-label-fill: #111827;");
        countAxis.setStyle("-fx-tick-label-fill: #111827;");

        ignorantSeries.setName("Ignorantes");
        spreaderSeries.setName("Espalhadores");
        inactiveSeries.setName("Inativos");
        grokSeries.setName("GROK");
        botSeries.setName("Bots");
        influencerSeries.setName("Influenciadores");
        echoChamberSeries.setName("Bolhas");
        factCheckerSeries.setName("Checadores");
        journalistSeries.setName("Jornalistas");
        stateChart.getData().add(ignorantSeries);
        stateChart.getData().add(spreaderSeries);
        stateChart.getData().add(inactiveSeries);
        stateChart.getData().add(grokSeries);
        stateChart.getData().add(botSeries);
        stateChart.getData().add(influencerSeries);
        stateChart.getData().add(echoChamberSeries);
        stateChart.getData().add(factCheckerSeries);
        stateChart.getData().add(journalistSeries);
    }

    @Override
    public void start(Stage stage) {
        modeBox.getItems().addAll(MODE_SEQUENTIAL, MODE_PARALLEL, MODE_DISTRIBUTED);
        modeBox.setValue(MODE_SEQUENTIAL);
        modeBox.setPrefWidth(170);
        styleComboBox(modeBox);

        executionBox.getItems().addAll(EXECUTION_VISUAL, EXECUTION_BENCHMARK, EXECUTION_DIDACTIC);
        executionBox.setValue(EXECUTION_VISUAL);
        executionBox.setPrefWidth(190);
        styleComboBox(executionBox);

        workerAddressesField.setPrefWidth(300);
        workerAddressesField.setPromptText("host:porta, host:porta:nome");
        workerAddressesField.textProperty().addListener((observable, oldValue, newValue) -> {
            distributedSimulation = null;
            distributedWorkerList = null;
        });
        styleTextField(workerAddressesField);

        runButton = new Button("Iniciar");
        stepButton = new Button("Passar");
        Button resetButton = new Button("Reiniciar");
        stylePrimaryButton(runButton);
        styleSecondaryButton(stepButton);
        styleSecondaryButton(resetButton);

        timeline = new Timeline(new KeyFrame(Duration.millis(180), event -> nextStep()));
        timeline.setCycleCount(Animation.INDEFINITE);
        didacticTimeline = new Timeline(new KeyFrame(Duration.millis(70), event -> continueDidacticStep()));
        didacticTimeline.setCycleCount(Animation.INDEFINITE);

        runButton.setOnAction(event -> toggleRun(runButton));
        stepButton.setOnAction(event -> nextStep());
        resetButton.setOnAction(event -> reset());

        VBox processingControl = controlGroup("Processamento", modeBox);
        VBox executionControl = controlGroup("Execução", executionBox);
        VBox workersControl = controlGroup("Workers RMI", workerAddressesField);

        HBox selectors = new HBox(10, processingControl, executionControl, workersControl);
        selectors.setAlignment(Pos.BOTTOM_LEFT);

        HBox actions = new HBox(8, runButton, stepButton, resetButton);
        actions.setAlignment(Pos.BOTTOM_LEFT);

        HBox controls = new HBox(18, selectors, actions);
        controls.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Propagação de Fake News");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        statusLabel.setStyle("-fx-text-fill: #cbd5e1;");

        VBox titleBox = new VBox(4, title, statusLabel);
        HBox headerContent = new HBox(24, titleBox, controls);
        headerContent.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(headerContent);
        header.setPadding(new Insets(16));
        header.setStyle("-fx-background-color: " + SURFACE_DARK + ";");

        VBox sidePanel = new VBox(12);
        sidePanel.setPadding(new Insets(14));
        sidePanel.setPrefWidth(250);
        sidePanel.setStyle("-fx-background-color: " + SURFACE_DARK + ";");

        Label statsTitle = sectionTitle("Estados");
        stateRankingBox.setFillWidth(true);
        progressBar.setPrefWidth(220);
        progressPercentLabel.setMinWidth(220);
        progressPercentLabel.setAlignment(Pos.CENTER_RIGHT);
        progressPercentLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        VBox progressBox = new VBox(5, sectionTitle("Progresso"), progressPercentLabel, progressBar);
        styleSideBlock(progressBox);
        VBox statesBox = new VBox(8, statsTitle, stateRankingBox);
        styleSideBlock(statesBox);
        VBox resultBox = new VBox(8,
                sectionTitle("Resultado final"),
                resultModeLabel,
                resultTimeLabel,
                resultComputeTimeLabel,
                resultUnitsLabel,
                resultMemoryLabel);
        styleSideBlock(resultBox);
        styleResultLabel(resultModeLabel);
        styleResultLabel(resultTimeLabel);
        styleResultLabel(resultComputeTimeLabel);
        styleResultLabel(resultUnitsLabel);
        styleResultLabel(resultMemoryLabel);

        sidePanel.getChildren().addAll(
                progressBox,
                statesBox,
                resultBox);

        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setPadding(new Insets(16));
        canvasPane.setStyle("-fx-background-color: " + SURFACE_DARK_ALT + ";");
        installGridInteractions();

        HBox simulationContent = new HBox(10, canvasPane, sidePanel);
        simulationContent.setStyle("-fx-background-color: " + SURFACE_DARK + ";");
        HBox.setHgrow(canvasPane, Priority.ALWAYS);
        stateChart.setMaxWidth(Double.MAX_VALUE);

        VBox centerContent = new VBox(10, simulationContent, stateChart);
        centerContent.setStyle("-fx-background-color: " + SURFACE_DARK + ";");

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(centerContent);

        reset();

        stage.setTitle("Propagação de Fake News");
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/simulation-viewer.css").toExternalForm());
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> stopRunningTasks());
        stage.show();
    }

    private void toggleRun(Button runButton) {
        if (benchmarkTask != null && benchmarkTask.isRunning()) {
            benchmarkTask.cancel();
            return;
        }

        if (EXECUTION_BENCHMARK.equals(executionBox.getValue())) {
            startBenchmarkRun();
            return;
        }

        if (timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.pause();
            if (didacticStepRunning) {
                didacticTimeline.pause();
            }
            totalElapsedNanos += System.nanoTime() - runSegmentStartNanos;
            runSegmentStartNanos = 0;
            runButton.setText("Iniciar");
        } else {
            if (generation >= config.getGenerations()) {
                reset();
            }
            if (activeMode == null) {
                activeMode = modeBox.getValue();
            }
            activeExecution = executionBox.getValue();
            activeWorkerAddresses = workerAddressesField.getText().trim();
            modeBox.setDisable(true);
            executionBox.setDisable(true);
            workerAddressesField.setDisable(true);
            runSegmentStartNanos = System.nanoTime();
            timeline.play();
            if (didacticStepRunning) {
                didacticTimeline.play();
            }
            runButton.setText("Pausar");
        }
    }

    private void reset() {
        if (benchmarkTask != null && benchmarkTask.isRunning()) {
            benchmarkTask.cancel();
        }
        timeline.stop();
        didacticTimeline.stop();
        stopAutomaticWorkers();
        didacticStepRunning = false;
        clearDidacticState();
        generation = 0;
        totalElapsedNanos = 0;
        runSegmentStartNanos = 0;
        computeElapsedNanos = 0;
        gridZoom = MIN_GRID_ZOOM;
        gridViewRow = 0;
        gridViewColumn = 0;
        activeMode = null;
        activeExecution = null;
        activeWorkerAddresses = null;
        distributedSimulation = null;
        distributedWorkerList = null;
        config = new SimulationConfig(ROWS, COLUMNS, GENERATIONS, 0.03, 0.03,
                0.015, 0.01, 0.25, 0.50, 42L);
        currentGrid = GridFactory.createInitialGrid(config);
        runButton.setText("Iniciar");
        modeBox.setDisable(false);
        executionBox.setDisable(false);
        workerAddressesField.setDisable(false);
        stepButton.setDisable(false);
        clearResults();
        clearChart();
        drawGrid();
        updateStatus();
    }

    private void nextStep() {
        if (benchmarkTask != null && benchmarkTask.isRunning()) {
            return;
        }

        if (generation >= config.getGenerations()) {
            finishSimulation();
            return;
        }

        if (didacticStepRunning) {
            return;
        }

        try {
            if (EXECUTION_DIDACTIC.equals(selectedExecutionType())) {
                startDidacticStep();
                return;
            }

            String mode = selectedExecutionMode();
            long generationStartNanos = System.nanoTime();
            if (MODE_PARALLEL.equals(mode)) {
                currentGrid = nextParallelGeneration();
            } else if (MODE_DISTRIBUTED.equals(mode)) {
                currentGrid = nextDistributedGeneration();
            } else {
                currentGrid = nextSequentialGeneration();
            }
            computeElapsedNanos += System.nanoTime() - generationStartNanos;
            generation++;
            drawGrid();
            updateStatus();
            updateChart();
            if (generation >= config.getGenerations()) {
                finishSimulation();
            }
        } catch (Exception exception) {
            timeline.stop();
            didacticTimeline.stop();
            didacticStepRunning = false;
            clearDidacticState();
            runButton.setText("Iniciar");
            activeMode = null;
            activeExecution = null;
            activeWorkerAddresses = null;
            modeBox.setDisable(false);
            executionBox.setDisable(false);
            workerAddressesField.setDisable(false);
            stepButton.setDisable(false);
            statusLabel.setText("Erro: " + exception.getMessage());
        }
    }

    private void startBenchmarkRun() {
        if (generation >= config.getGenerations()) {
            reset();
        }

        activeMode = modeBox.getValue();
        activeExecution = EXECUTION_BENCHMARK;
        activeWorkerAddresses = workerAddressesField.getText().trim();
        String benchmarkMode = activeMode;

        try {
            if (MODE_DISTRIBUTED.equals(benchmarkMode)) {
                prepareDistributedSimulation(activeWorkerAddresses);
            } else {
                stopAutomaticWorkers();
            }
        } catch (Exception exception) {
            activeMode = null;
            activeExecution = null;
            activeWorkerAddresses = null;
            statusLabel.setText("Erro: " + exception.getMessage());
            modeBox.setDisable(false);
            executionBox.setDisable(false);
            workerAddressesField.setDisable(false);
            stepButton.setDisable(false);
            runButton.setText("Iniciar");
            return;
        }

        totalElapsedNanos = 0;
        runSegmentStartNanos = System.nanoTime();
        computeElapsedNanos = 0;
        clearResults();
        modeBox.setDisable(true);
        executionBox.setDisable(true);
        workerAddressesField.setDisable(true);
        stepButton.setDisable(true);
        runButton.setText("Cancelar");
        statusLabel.setText(String.format(
                "Benchmark em execução | Modo: %s | Renderizacao fora da medicao",
                activeMode));

        benchmarkTask = new Task<>() {
            @Override
            protected BenchmarkOutcome call() throws Exception {
                CellState[][] grid = currentGrid;
                long measuredComputeNanos = 0;
                int currentGeneration = generation;

                while (currentGeneration < config.getGenerations() && !isCancelled()) {
                    long generationStartNanos = System.nanoTime();
                    grid = nextGeneration(grid, currentGeneration, benchmarkMode);
                    measuredComputeNanos += System.nanoTime() - generationStartNanos;
                    currentGeneration++;
                }

                return new BenchmarkOutcome(grid, currentGeneration, measuredComputeNanos);
            }
        };

        benchmarkTask.setOnSucceeded(event -> {
            BenchmarkOutcome outcome = benchmarkTask.getValue();
            totalElapsedNanos = System.nanoTime() - runSegmentStartNanos;
            runSegmentStartNanos = 0;
            computeElapsedNanos = outcome.computeElapsedNanos();
            currentGrid = outcome.grid();
            generation = outcome.generation();
            drawGrid();
            updateStatus();
            updateChart();
            runButton.setText("Iniciar");
            updateResults(benchmarkMode);
            activeMode = null;
            activeExecution = null;
            activeWorkerAddresses = null;
            modeBox.setDisable(false);
            executionBox.setDisable(false);
            workerAddressesField.setDisable(false);
            stepButton.setDisable(false);
            stopAutomaticWorkers();
        });

        benchmarkTask.setOnCancelled(event -> {
            totalElapsedNanos = System.nanoTime() - runSegmentStartNanos;
            runSegmentStartNanos = 0;
            runButton.setText("Iniciar");
            statusLabel.setText("Benchmark cancelado na geracao " + generation + ".");
            activeMode = null;
            activeExecution = null;
            activeWorkerAddresses = null;
            modeBox.setDisable(false);
            executionBox.setDisable(false);
            workerAddressesField.setDisable(false);
            stepButton.setDisable(false);
            stopAutomaticWorkers();
        });

        benchmarkTask.setOnFailed(event -> {
            runSegmentStartNanos = 0;
            runButton.setText("Iniciar");
            Throwable exception = benchmarkTask.getException();
            statusLabel.setText("Erro: " + (exception == null ? "falha desconhecida" : exception.getMessage()));
            activeMode = null;
            activeExecution = null;
            activeWorkerAddresses = null;
            modeBox.setDisable(false);
            executionBox.setDisable(false);
            workerAddressesField.setDisable(false);
            stepButton.setDisable(false);
            stopAutomaticWorkers();
        });

        Thread thread = new Thread(benchmarkTask, "simulation-benchmark");
        thread.setDaemon(true);
        thread.start();
    }

    private void finishSimulation() {
        if (runSegmentStartNanos != 0) {
            totalElapsedNanos += System.nanoTime() - runSegmentStartNanos;
            runSegmentStartNanos = 0;
        }
        timeline.stop();
        didacticTimeline.stop();
        stopAutomaticWorkers();
        didacticStepRunning = false;
        clearDidacticState();
        runButton.setText("Iniciar");
        updateResults(selectedExecutionMode());
        activeMode = null;
        activeExecution = null;
        activeWorkerAddresses = null;
        modeBox.setDisable(false);
        executionBox.setDisable(false);
        workerAddressesField.setDisable(false);
        stepButton.setDisable(false);
    }

    private String selectedExecutionMode() {
        return activeMode == null ? modeBox.getValue() : activeMode;
    }

    private CellState[][] nextGeneration(CellState[][] grid, int generation, String mode) throws Exception {
        if (MODE_PARALLEL.equals(mode)) {
            return nextParallelGeneration(grid, generation);
        }
        if (MODE_DISTRIBUTED.equals(mode)) {
            return nextDistributedGeneration(grid, generation);
        }
        return nextSequentialGeneration(grid, generation);
    }

    private void startDidacticStep() {
        if (generation >= config.getGenerations()) {
            finishSimulation();
            return;
        }

        activeMode = selectedExecutionMode();
        activeExecution = EXECUTION_DIDACTIC;
        activeWorkerAddresses = workerAddressesField.getText().trim();
        didacticSourceGrid = currentGrid;
        didacticNextGrid = copyGrid(currentGrid);
        currentGrid = copyGrid(currentGrid);
        didacticNextRow = 0;
        configureDidacticWorkers(activeMode);
        didacticStepRunning = true;
        modeBox.setDisable(true);
        executionBox.setDisable(true);
        workerAddressesField.setDisable(true);
        stepButton.setDisable(true);
        statusLabel.setText(String.format(
                "Visualização didática | Geração %d | %s",
                generation + 1,
                didacticStatusDetail()));
        didacticTimeline.playFromStart();
    }

    private void continueDidacticStep() {
        if (!didacticStepRunning) {
            didacticTimeline.stop();
            return;
        }

        long frameStartNanos = System.nanoTime();
        if (MODE_SEQUENTIAL.equals(activeMode)) {
            processSequentialDidacticFrame();
        } else {
            processWorkerDidacticFrame();
        }
        computeElapsedNanos += System.nanoTime() - frameStartNanos;

        drawGrid();
        statusLabel.setText(String.format(
                "Visualização didática | Geração %d | %s",
                generation + 1,
                didacticStatusDetail()));

        if (isDidacticGenerationComplete()) {
            finishDidacticStep();
        }
    }

    private void processSequentialDidacticFrame() {
        int endRow = Math.min(config.getRows(), didacticNextRow + DIDACTIC_SEQUENTIAL_ROWS_PER_FRAME);
        processDidacticRows(didacticNextRow, endRow);
        didacticNextRow = endRow;
    }

    private void processWorkerDidacticFrame() {
        for (int workerIndex = 0; workerIndex < didacticWorkerCount; workerIndex++) {
            int startRow = didacticWorkerNextRows[workerIndex];
            int endRow = Math.min(
                    didacticWorkerEndRows[workerIndex],
                    startRow + DIDACTIC_WORKER_ROWS_PER_FRAME);
            processDidacticRows(startRow, endRow);
            didacticWorkerNextRows[workerIndex] = endRow;
        }
    }

    private void processDidacticRows(int startRow, int endRow) {
        for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < config.getColumns(); col++) {
                CellState nextState = SimulationRules.nextState(didacticSourceGrid, row, col, generation, config);
                didacticNextGrid[row][col] = nextState;
                currentGrid[row][col] = nextState;
            }
        }
    }

    private void finishDidacticStep() {
        didacticTimeline.stop();
        didacticStepRunning = false;
        currentGrid = didacticNextGrid;
        clearDidacticState();
        generation++;
        drawGrid();
        updateStatus();
        updateChart();
        stepButton.setDisable(false);
        if (timeline.getStatus() != Animation.Status.RUNNING) {
            activeMode = null;
            activeExecution = null;
            activeWorkerAddresses = null;
            modeBox.setDisable(false);
            executionBox.setDisable(false);
            workerAddressesField.setDisable(false);
        }

        if (generation >= config.getGenerations()) {
            finishSimulation();
        }
    }

    private boolean isDidacticGenerationComplete() {
        if (MODE_SEQUENTIAL.equals(activeMode)) {
            return didacticNextRow >= config.getRows();
        }

        for (int workerIndex = 0; workerIndex < didacticWorkerCount; workerIndex++) {
            if (didacticWorkerNextRows[workerIndex] < didacticWorkerEndRows[workerIndex]) {
                return false;
            }
        }
        return true;
    }

    private void configureDidacticWorkers(String mode) {
        if (MODE_SEQUENTIAL.equals(mode)) {
            didacticWorkerCount = 0;
            didacticWorkerStartRows = new int[0];
            didacticWorkerEndRows = new int[0];
            didacticWorkerNextRows = new int[0];
            didacticWorkerLabels = new String[0];
            return;
        }

        List<DistributedSimulation.WorkerAddress> rmiAddresses = MODE_DISTRIBUTED.equals(mode)
                ? configuredRmiWorkers()
                : List.of();
        didacticWorkerCount = MODE_DISTRIBUTED.equals(mode) ? rmiAddresses.size() : THREADS;
        didacticWorkerStartRows = new int[didacticWorkerCount];
        didacticWorkerEndRows = new int[didacticWorkerCount];
        didacticWorkerNextRows = new int[didacticWorkerCount];
        didacticWorkerLabels = new String[didacticWorkerCount];

        for (int workerIndex = 0; workerIndex < didacticWorkerCount; workerIndex++) {
            int startRow = workerIndex * config.getRows() / didacticWorkerCount;
            int endRow = (workerIndex + 1) * config.getRows() / didacticWorkerCount;
            didacticWorkerStartRows[workerIndex] = startRow;
            didacticWorkerEndRows[workerIndex] = endRow;
            didacticWorkerNextRows[workerIndex] = startRow;
            didacticWorkerLabels[workerIndex] = MODE_DISTRIBUTED.equals(mode)
                    ? "Worker " + (workerIndex + 1) + " | " + rmiAddresses.get(workerIndex)
                    : "Thread " + (workerIndex + 1);
        }
    }

    private String didacticStatusDetail() {
        if (MODE_SEQUENTIAL.equals(activeMode)) {
            return String.format("varredura linha %d/%d", Math.min(didacticNextRow, config.getRows()), config.getRows());
        }

        return String.format("%d regiões simultâneas em processamento", didacticWorkerCount);
    }

    private int configuredRmiWorkerCount() {
        return configuredRmiWorkers().size();
    }

    private List<DistributedSimulation.WorkerAddress> configuredRmiWorkers() {
        String workerList = activeWorkerAddresses == null
                ? workerAddressesField.getText()
                : activeWorkerAddresses;
        return DistributedSimulation.parseWorkerAddresses(workerList);
    }

    private void prepareDistributedSimulation(String workerList) throws Exception {
        List<DistributedSimulation.WorkerAddress> addresses = DistributedSimulation.parseWorkerAddresses(workerList);
        boolean allLocal = true;
        int reachableWorkers = 0;
        DistributedSimulation.WorkerAddress unreachableAddress = null;
        Exception unreachableCause = null;

        for (DistributedSimulation.WorkerAddress address : addresses) {
            if (!isLocalWorker(address)) {
                allLocal = false;
            }

            Exception probeFailure = probeWorker(address);
            if (probeFailure == null) {
                reachableWorkers++;
            } else if (unreachableAddress == null) {
                unreachableAddress = address;
                unreachableCause = probeFailure;
            }
        }

        if (allLocal) {
            if (!autoStartedWorkers.isEmpty() && !autoWorkersMatch(addresses)) {
                stopAutomaticWorkers();
            }

            if (reachableWorkers == 0) {
                startAutomaticWorkers(addresses);
            } else if (reachableWorkers != addresses.size()) {
                throw new IllegalStateException(
                        "Os workers locais estao em estado misto. Pare os processos manuais ou deixe todos fechados para permitir o auto-inicio.");
            }
        } else {
            stopAutomaticWorkers();
            if (reachableWorkers != addresses.size()) {
                throw new IllegalStateException(workerUnavailableMessage(
                        unreachableAddress == null ? addresses.get(0) : unreachableAddress,
                        unreachableCause));
            }
        }

        if (distributedSimulation == null || !workerList.equals(distributedWorkerList)) {
            distributedSimulation = new DistributedSimulation(addresses);
            distributedWorkerList = workerList;
        }
    }

    private boolean autoWorkersMatch(List<DistributedSimulation.WorkerAddress> addresses) {
        if (autoStartedWorkers.size() != addresses.size()) {
            return false;
        }

        for (int index = 0; index < addresses.size(); index++) {
            DistributedSimulation.WorkerAddress expected = addresses.get(index);
            DistributedSimulation.WorkerAddress current = autoStartedWorkers.get(index).address();
            if (!expected.host().equals(current.host())
                    || expected.port() != current.port()
                    || !expected.name().equals(current.name())) {
                return false;
            }
        }
        return true;
    }

    private void startAutomaticWorkers(List<DistributedSimulation.WorkerAddress> addresses) throws Exception {
        stopAutomaticWorkers();
        if (addresses.isEmpty()) {
            return;
        }

        System.setProperty("java.rmi.server.hostname", addresses.get(0).host());
        try {
            for (DistributedSimulation.WorkerAddress address : addresses) {
                Registry registry = LocateRegistry.createRegistry(address.port());
                distributed.MatrixWorkerImpl worker = new distributed.MatrixWorkerImpl(address.name(), 0);
                registry.rebind(address.name(), worker);
                autoStartedWorkers.add(new LocalWorkerHandle(address, registry, worker));
            }
        } catch (Exception exception) {
            stopAutomaticWorkers();
            throw new IllegalStateException("Nao foi possivel iniciar os workers locais automaticamente: "
                    + rootCauseMessage(exception), exception);
        }
    }

    private void stopAutomaticWorkers() {
        for (LocalWorkerHandle handle : autoStartedWorkers) {
            try {
                handle.registry().unbind(handle.address().name());
            } catch (Exception ignored) {
            }

            try {
                UnicastRemoteObject.unexportObject(handle.worker(), true);
            } catch (Exception ignored) {
            }

            try {
                UnicastRemoteObject.unexportObject(handle.registry(), true);
            } catch (Exception ignored) {
            }
        }
        autoStartedWorkers.clear();
    }

    private boolean isLocalWorker(DistributedSimulation.WorkerAddress address) {
        String host = address.host().trim().toLowerCase();
        return "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    private Exception probeWorker(DistributedSimulation.WorkerAddress address) {
        try {
            Registry registry = LocateRegistry.getRegistry(address.host(), address.port());
            registry.lookup(address.name());
            return null;
        } catch (Exception exception) {
            return exception;
        }
    }

    private String workerFailureMessage(DistributedSimulation.WorkerAddress address, Throwable cause) {
        String rootCause = cause == null ? "indisponivel" : rootCauseMessage(cause);
        return String.format("Falha no worker RMI %s:%d:%s | causa: %s",
                address.host(), address.port(), address.name(), rootCause);
    }

    private String workerUnavailableMessage(DistributedSimulation.WorkerAddress address, Throwable cause) {
        String hint = isLocalWorker(address)
                ? " Inicie o WorkerServer manualmente ou deixe a GUI iniciar os workers locais automaticamente."
                : " Inicie o WorkerServer manualmente nessa maquina e verifique firewall/porta.";
        return workerFailureMessage(address, cause) + hint;
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private void clearDidacticState() {
        didacticSourceGrid = null;
        didacticNextGrid = null;
        didacticNextRow = 0;
        didacticWorkerCount = 0;
        didacticWorkerStartRows = new int[0];
        didacticWorkerEndRows = new int[0];
        didacticWorkerNextRows = new int[0];
        didacticWorkerLabels = new String[0];
    }

    private CellState[][] copyGrid(CellState[][] source) {
        CellState[][] copy = new CellState[source.length][source[0].length];
        for (int row = 0; row < source.length; row++) {
            System.arraycopy(source[row], 0, copy[row], 0, source[row].length);
        }
        return copy;
    }

    private CellState[][] nextSequentialGeneration() {
        return nextSequentialGeneration(currentGrid, generation);
    }

    private CellState[][] nextSequentialGeneration(CellState[][] grid, int generation) {
        CellState[][] nextGrid = new CellState[config.getRows()][config.getColumns()];
        for (int row = 0; row < config.getRows(); row++) {
            for (int col = 0; col < config.getColumns(); col++) {
                nextGrid[row][col] = SimulationRules.nextState(grid, row, col, generation, config);
            }
        }
        return nextGrid;
    }

    private CellState[][] nextParallelGeneration() throws InterruptedException {
        return nextParallelGeneration(currentGrid, generation);
    }

    private CellState[][] nextParallelGeneration(CellState[][] grid, int generation) throws InterruptedException {
        CellState[][] nextGrid = new CellState[config.getRows()][config.getColumns()];
        Thread[] threads = new Thread[THREADS];

        for (int index = 0; index < THREADS; index++) {
            int startRow = index * config.getRows() / THREADS;
            int endRow = (index + 1) * config.getRows() / THREADS;

            threads[index] = new Thread(() -> {
                for (int row = startRow; row < endRow; row++) {
                    for (int col = 0; col < config.getColumns(); col++) {
                        nextGrid[row][col] = SimulationRules.nextState(grid, row, col, generation, config);
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
        return nextDistributedGeneration(currentGrid, generation);
    }

    private CellState[][] nextDistributedGeneration(CellState[][] grid, int generation) throws Exception {
        String workerList = activeWorkerAddresses == null
                ? workerAddressesField.getText().trim()
                : activeWorkerAddresses;
        prepareDistributedSimulation(workerList);
        return distributedSimulation.nextGeneration(grid, config, generation);
    }

    private void stopRunningTasks() {
        if (benchmarkTask != null && benchmarkTask.isRunning()) {
            benchmarkTask.cancel();
        }
        didacticTimeline.stop();
        didacticStepRunning = false;
        clearDidacticState();
        stopAutomaticWorkers();
    }

    private void drawGrid() {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        double cellSize = gridCellSize();
        int blockSize = aggregationBlockSize(cellSize);
        clampGridView();

        graphics.setFill(Color.web("#0b1020"));
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (blockSize > 1) {
            drawAggregatedGrid(graphics, cellSize, blockSize);
        } else {
            drawIndividualGrid(graphics, cellSize);
        }

        drawDidacticOverlay(graphics, cellSize);

        graphics.setStroke(Color.web("#e5e7eb"));
        graphics.setLineWidth(2);
        graphics.strokeRect(gridOriginX(cellSize), gridOriginY(cellSize),
                config.getColumns() * cellSize, config.getRows() * cellSize);
    }

    private void installGridInteractions() {
        canvas.setOnScroll(event -> {
            if (config == null || currentGrid == null) {
                return;
            }

            double oldZoom = gridZoom;
            double zoomFactor = event.getDeltaY() > 0 ? 1.2 : 1 / 1.2;
            double nextZoom = Math.max(MIN_GRID_ZOOM, Math.min(MAX_GRID_ZOOM, gridZoom * zoomFactor));
            if (nextZoom == oldZoom) {
                return;
            }

            double focusColumn = screenToGridColumn(event.getX(), gridCellSize());
            double focusRow = screenToGridRow(event.getY(), gridCellSize());
            gridZoom = nextZoom;
            double nextCellSize = gridCellSize();
            if (config.getColumns() * nextCellSize > canvas.getWidth()) {
                gridViewColumn = focusColumn - event.getX() / nextCellSize;
            }
            if (config.getRows() * nextCellSize > canvas.getHeight()) {
                gridViewRow = focusRow - event.getY() / nextCellSize;
            }
            clampGridView();
            drawGrid();
            event.consume();
        });

        canvas.setOnMousePressed(event -> {
            dragStartX = event.getX();
            dragStartY = event.getY();
            dragStartViewRow = gridViewRow;
            dragStartViewColumn = gridViewColumn;
        });

        canvas.setOnMouseDragged(event -> {
            double cellSize = gridCellSize();
            gridViewColumn = dragStartViewColumn - (event.getX() - dragStartX) / cellSize;
            gridViewRow = dragStartViewRow - (event.getY() - dragStartY) / cellSize;
            clampGridView();
            drawGrid();
            event.consume();
        });

        canvas.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                gridZoom = MIN_GRID_ZOOM;
                gridViewRow = 0;
                gridViewColumn = 0;
                drawGrid();
            }
        });
    }

    private void drawIndividualGrid(GraphicsContext graphics, double cellSize) {
        int startRow = Math.max(0, (int) Math.floor(screenToGridRow(0, cellSize)));
        int endRow = Math.min(config.getRows(), (int) Math.ceil(screenToGridRow(canvas.getHeight(), cellSize)));
        int startColumn = Math.max(0, (int) Math.floor(screenToGridColumn(0, cellSize)));
        int endColumn = Math.min(config.getColumns(), (int) Math.ceil(screenToGridColumn(canvas.getWidth(), cellSize)));
        double originX = gridOriginX(cellSize);
        double originY = gridOriginY(cellSize);
        double gap = cellSize >= 5 ? 0.5 : 0;

        for (int row = startRow; row < endRow; row++) {
            for (int col = startColumn; col < endColumn; col++) {
                graphics.setFill(colorOf(currentGrid[row][col]));
                graphics.fillRect(
                        originX + col * cellSize,
                        originY + row * cellSize,
                        Math.max(1, cellSize - gap),
                        Math.max(1, cellSize - gap));
            }
        }
    }

    private void drawAggregatedGrid(GraphicsContext graphics, double cellSize, int blockSize) {
        int startRow = Math.max(0, ((int) Math.floor(screenToGridRow(0, cellSize)) / blockSize) * blockSize);
        int endRow = Math.min(config.getRows(), (int) Math.ceil(screenToGridRow(canvas.getHeight(), cellSize)));
        int startColumn = Math.max(0, ((int) Math.floor(screenToGridColumn(0, cellSize)) / blockSize) * blockSize);
        int endColumn = Math.min(config.getColumns(), (int) Math.ceil(screenToGridColumn(canvas.getWidth(), cellSize)));
        double originX = gridOriginX(cellSize);
        double originY = gridOriginY(cellSize);

        for (int row = startRow; row < endRow; row += blockSize) {
            int rowsInBlock = Math.min(blockSize, config.getRows() - row);
            for (int col = startColumn; col < endColumn; col += blockSize) {
                int columnsInBlock = Math.min(blockSize, config.getColumns() - col);
                graphics.setFill(colorOf(representativeState(row, col, rowsInBlock, columnsInBlock)));
                graphics.fillRect(
                        originX + col * cellSize,
                        originY + row * cellSize,
                        Math.max(1, columnsInBlock * cellSize - 0.35),
                        Math.max(1, rowsInBlock * cellSize - 0.35));
            }
        }
    }

    private CellState representativeState(int startRow, int startColumn, int rows, int columns) {
        int[] counts = new int[CellState.values().length];
        for (int row = startRow; row < startRow + rows; row++) {
            for (int col = startColumn; col < startColumn + columns; col++) {
                counts[currentGrid[row][col].ordinal()]++;
            }
        }

        int ignorantIndex = CellState.IGNORANT.ordinal();
        int bestIndex = -1;
        for (int index = 0; index < counts.length; index++) {
            if (index == ignorantIndex) {
                continue;
            }
            if (counts[index] > 0 && (bestIndex == -1 || counts[index] > counts[bestIndex])) {
                bestIndex = index;
            }
        }
        if (bestIndex == -1) {
            bestIndex = ignorantIndex;
        }
        return CellState.values()[bestIndex];
    }

    private void drawDidacticOverlay(GraphicsContext graphics, double cellSize) {
        if (!didacticStepRunning) {
            return;
        }

        if (MODE_SEQUENTIAL.equals(activeMode)) {
            drawDidacticSequentialOverlay(graphics, cellSize);
        } else {
            drawDidacticWorkerOverlay(graphics, cellSize);
        }
        graphics.setGlobalAlpha(1.0);
    }

    private void drawDidacticSequentialOverlay(GraphicsContext graphics, double cellSize) {
        int sweepStart = didacticNextRow;
        int sweepEnd = Math.min(config.getRows(), didacticNextRow + DIDACTIC_SEQUENTIAL_ROWS_PER_FRAME);

        graphics.setGlobalAlpha(0.20);
        graphics.setFill(Color.web("#facc15"));
        fillVisibleRowRange(graphics, 0, didacticNextRow, cellSize);

        graphics.setGlobalAlpha(0.45);
        graphics.setFill(Color.web("#22d3ee"));
        fillVisibleRowRange(graphics, sweepStart, sweepEnd, cellSize);
    }

    private void drawDidacticWorkerOverlay(GraphicsContext graphics, double cellSize) {
        Color[] colors = {
                Color.web("#22d3ee"),
                Color.web("#facc15"),
                Color.web("#fb7185"),
                Color.web("#a78bfa"),
                Color.web("#34d399"),
                Color.web("#f97316")
        };

        if (MODE_DISTRIBUTED.equals(activeMode)) {
            drawCoordinatorOverlay(graphics, cellSize);
        }

        for (int workerIndex = 0; workerIndex < didacticWorkerCount; workerIndex++) {
            Color color = colors[workerIndex % colors.length];
            int startRow = didacticWorkerStartRows[workerIndex];
            int currentRow = didacticWorkerNextRows[workerIndex];
            int endRow = didacticWorkerEndRows[workerIndex];
            int activeEndRow = Math.min(endRow, currentRow + DIDACTIC_WORKER_ROWS_PER_FRAME);

            graphics.setGlobalAlpha(0.18);
            graphics.setFill(color);
            fillVisibleRowRange(graphics, startRow, endRow, cellSize);

            graphics.setGlobalAlpha(0.36);
            graphics.setFill(color);
            fillVisibleRowRange(graphics, startRow, currentRow, cellSize);

            graphics.setGlobalAlpha(0.58);
            graphics.setFill(color);
            fillVisibleRowRange(graphics, currentRow, activeEndRow, cellSize);

            graphics.setGlobalAlpha(0.95);
            graphics.setStroke(color);
            graphics.setLineWidth(2);
            strokeVisibleRowRange(graphics, startRow, endRow, cellSize);

            drawDidacticWorkerLabel(graphics, workerIndex, color, startRow, currentRow, endRow, cellSize);
        }
    }

    private void drawCoordinatorOverlay(GraphicsContext graphics, double cellSize) {
        double originX = gridOriginX(cellSize);
        double gridWidth = config.getColumns() * cellSize;
        double x = Math.max(8, originX);
        double width = Math.min(canvas.getWidth() - x - 8, gridWidth);
        if (width <= 0) {
            return;
        }

        graphics.setGlobalAlpha(0.88);
        graphics.setFill(Color.web("#0f172a"));
        graphics.fillRect(x, 8, width, 30);
        graphics.setStroke(Color.web("#e5e7eb"));
        graphics.strokeRect(x, 8, width, 30);
        graphics.setFill(Color.web("#f8fafc"));
        graphics.setFont(Font.font("Consolas", 12));
        graphics.fillText("Coordinator -> distribuindo blocos para " + didacticWorkerCount + " workers RMI",
                x + 10, 28);
    }

    private void drawDidacticWorkerLabel(GraphicsContext graphics,
                                         int workerIndex,
                                         Color color,
                                         int startRow,
                                         int currentRow,
                                         int endRow,
                                         double cellSize) {
        int visibleStartRow = Math.max(startRow, Math.max(0, (int) Math.floor(screenToGridRow(0, cellSize))));
        int visibleEndRow = Math.min(endRow, Math.min(config.getRows(),
                (int) Math.ceil(screenToGridRow(canvas.getHeight(), cellSize))));
        if (visibleEndRow <= visibleStartRow) {
            return;
        }

        double originX = gridOriginX(cellSize);
        double originY = gridOriginY(cellSize);
        double x = Math.max(10, originX + 10);
        double y = originY + visibleStartRow * cellSize + 18;
        y = Math.max(52, Math.min(canvas.getHeight() - 12, y));
        String label = didacticWorkerLabels.length > workerIndex
                ? didacticWorkerLabels[workerIndex]
                : "Worker " + (workerIndex + 1);
        String progress = String.format("linhas [%d,%d) | atual %d/%d",
                startRow, endRow, Math.min(currentRow, endRow), endRow);

        graphics.setGlobalAlpha(0.88);
        graphics.setFill(Color.web("#020617"));
        graphics.fillRect(x - 6, y - 15, 330, 36);
        graphics.setStroke(color);
        graphics.strokeRect(x - 6, y - 15, 330, 36);
        graphics.setFill(Color.web("#f8fafc"));
        graphics.setFont(Font.font("Consolas", 11));
        graphics.fillText(label, x, y);
        graphics.setFill(Color.web("#cbd5e1"));
        graphics.fillText(progress, x, y + 14);
    }

    private void strokeVisibleRowRange(GraphicsContext graphics, int startRow, int endRow, double cellSize) {
        if (endRow <= startRow) {
            return;
        }

        int visibleStartRow = Math.max(startRow, Math.max(0, (int) Math.floor(screenToGridRow(0, cellSize))));
        int visibleEndRow = Math.min(endRow, Math.min(config.getRows(),
                (int) Math.ceil(screenToGridRow(canvas.getHeight(), cellSize))));
        if (visibleEndRow <= visibleStartRow) {
            return;
        }

        double originX = gridOriginX(cellSize);
        double originY = gridOriginY(cellSize);
        graphics.strokeRect(
                originX,
                originY + visibleStartRow * cellSize,
                config.getColumns() * cellSize,
                (visibleEndRow - visibleStartRow) * cellSize);
    }

    private void fillVisibleRowRange(GraphicsContext graphics, int startRow, int endRow, double cellSize) {
        if (endRow <= startRow) {
            return;
        }

        int visibleStartRow = Math.max(startRow, Math.max(0, (int) Math.floor(screenToGridRow(0, cellSize))));
        int visibleEndRow = Math.min(endRow, Math.min(config.getRows(),
                (int) Math.ceil(screenToGridRow(canvas.getHeight(), cellSize))));
        if (visibleEndRow <= visibleStartRow) {
            return;
        }

        double originX = gridOriginX(cellSize);
        double originY = gridOriginY(cellSize);
        double gridWidth = config.getColumns() * cellSize;

        graphics.fillRect(
                originX,
                originY + visibleStartRow * cellSize,
                gridWidth,
                (visibleEndRow - visibleStartRow) * cellSize);
    }

    private double baseGridCellSize() {
        return Math.min(canvas.getWidth() / config.getColumns(), canvas.getHeight() / config.getRows());
    }

    private double gridCellSize() {
        return baseGridCellSize() * gridZoom;
    }

    private int aggregationBlockSize(double cellSize) {
        if (cellSize >= 4) {
            return 1;
        }
        return Math.max(2, (int) Math.ceil(AGGREGATED_BLOCK_TARGET_PIXELS / cellSize));
    }

    private double gridOriginX(double cellSize) {
        double gridWidth = config.getColumns() * cellSize;
        if (gridWidth <= canvas.getWidth()) {
            return (canvas.getWidth() - gridWidth) / 2.0;
        }
        return -gridViewColumn * cellSize;
    }

    private double gridOriginY(double cellSize) {
        double gridHeight = config.getRows() * cellSize;
        if (gridHeight <= canvas.getHeight()) {
            return (canvas.getHeight() - gridHeight) / 2.0;
        }
        return -gridViewRow * cellSize;
    }

    private double screenToGridColumn(double x, double cellSize) {
        return (x - gridOriginX(cellSize)) / cellSize;
    }

    private double screenToGridRow(double y, double cellSize) {
        return (y - gridOriginY(cellSize)) / cellSize;
    }

    private void clampGridView() {
        double cellSize = gridCellSize();
        double maxColumn = Math.max(0, config.getColumns() - canvas.getWidth() / cellSize);
        double maxRow = Math.max(0, config.getRows() - canvas.getHeight() / cellSize);
        gridViewColumn = Math.max(0, Math.min(maxColumn, gridViewColumn));
        gridViewRow = Math.max(0, Math.min(maxRow, gridViewRow));
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
        if (state == CellState.BOT) {
            return Color.web("#ffab00");
        }
        if (state == CellState.INFLUENCER) {
            return Color.web("#d500f9");
        }
        if (state == CellState.ECHO_CHAMBER) {
            return Color.web("#f97316");
        }
        if (state == CellState.FACT_CHECKER) {
            return Color.web("#22c55e");
        }
        if (state == CellState.JOURNALIST) {
            return Color.web("#06b6d4");
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

    private VBox controlGroup(String text, Node control) {
        Label label = new Label(text + ":");
        label.setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 12px; -fx-font-weight: bold;");
        VBox box = new VBox(4, label, control);
        box.setAlignment(Pos.BOTTOM_LEFT);
        return box;
    }

    private void stylePrimaryButton(Button button) {
        button.setStyle("-fx-background-color: #14b8a6; -fx-text-fill: #05201d;"
                + " -fx-font-weight: bold; -fx-background-radius: 5; -fx-border-radius: 5;"
                + " -fx-padding: 8 14 8 14;");
    }

    private void styleSecondaryButton(Button button) {
        button.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #111827;"
                + " -fx-font-weight: bold; -fx-background-radius: 5; -fx-border-radius: 5;"
                + " -fx-padding: 8 14 8 14;");
    }

    private void styleComboBox(ComboBox<String> comboBox) {
        comboBox.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #94a3b8;"
                + " -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    private void styleTextField(TextField textField) {
        textField.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #94a3b8;"
                + " -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    private void styleResultLabel(Label label) {
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: #1f2937;");
    }

    private void styleSideBlock(VBox box) {
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: " + SURFACE_LIGHT
                + "; -fx-border-color: " + BORDER_LIGHT
                + "; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    private void updateStatus() {
        int ignorant = 0;
        int spreader = 0;
        int inactive = 0;
        int grok = 0;
        int bot = 0;
        int influencer = 0;
        int echoChamber = 0;
        int factChecker = 0;
        int journalist = 0;

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
                } else if (state == CellState.BOT) {
                    bot++;
                } else if (state == CellState.INFLUENCER) {
                    influencer++;
                } else if (state == CellState.ECHO_CHAMBER) {
                    echoChamber++;
                } else if (state == CellState.FACT_CHECKER) {
                    factChecker++;
                } else {
                    journalist++;
                }
            }
        }

        statusLabel.setText(String.format(
                "Geração %d de %d | Modo: %s | Execução: %s",
                generation, config.getGenerations(), selectedExecutionMode(), selectedExecutionType()));
        ignorantLabel.setText("Ignorantes: " + ignorant);
        spreaderLabel.setText("Espalhadores: " + spreader);
        inactiveLabel.setText("Inativos: " + inactive);
        grokLabel.setText("GROK: " + grok);
        botLabel.setText("Bots: " + bot);
        influencerLabel.setText("Influenciadores: " + influencer);
        echoChamberLabel.setText("Bolhas: " + echoChamber);
        factCheckerLabel.setText("Checadores: " + factChecker);
        journalistLabel.setText("Jornalistas: " + journalist);
        progressBar.setProgress((double) generation / config.getGenerations());
        progressPercentLabel.setText(String.format("%.0f%%", 100.0 * generation / config.getGenerations()));
        updateStateRanking(ignorant, spreader, inactive, grok, bot, influencer,
                echoChamber, factChecker, journalist);
    }

    private void updateStateRanking(int ignorant,
                                    int spreader,
                                    int inactive,
                                    int grok,
                                    int bot,
                                    int influencer,
                                    int echoChamber,
                                    int factChecker,
                                    int journalist) {
        List<StateCount> counts = new ArrayList<>();
        counts.add(new StateCount(ignorantLabel, "Ignorantes", CellState.IGNORANT, ignorant));
        counts.add(new StateCount(spreaderLabel, "Espalhadores", CellState.SPREADER, spreader));
        counts.add(new StateCount(inactiveLabel, "Inativos", CellState.INACTIVE, inactive));
        counts.add(new StateCount(grokLabel, "GROK", CellState.GROK, grok));
        counts.add(new StateCount(botLabel, "Bots", CellState.BOT, bot));
        counts.add(new StateCount(influencerLabel, "Influenciadores", CellState.INFLUENCER, influencer));
        counts.add(new StateCount(echoChamberLabel, "Bolhas", CellState.ECHO_CHAMBER, echoChamber));
        counts.add(new StateCount(factCheckerLabel, "Checadores", CellState.FACT_CHECKER, factChecker));
        counts.add(new StateCount(journalistLabel, "Jornalistas", CellState.JOURNALIST, journalist));

        counts.sort(Comparator.comparingInt(StateCount::count).reversed());

        stateRankingBox.getChildren().clear();
        for (int index = 0; index < counts.size(); index++) {
            StateCount count = counts.get(index);
            count.label().setText((index + 1) + ". " + count.name() + ": " + count.count());
            stateRankingBox.getChildren().add(counterItem(count.label(), colorOf(count.state())));
        }
    }

    private void clearChart() {
        ignorantSeries.getData().clear();
        spreaderSeries.getData().clear();
        inactiveSeries.getData().clear();
        grokSeries.getData().clear();
        botSeries.getData().clear();
        influencerSeries.getData().clear();
        echoChamberSeries.getData().clear();
        factCheckerSeries.getData().clear();
        journalistSeries.getData().clear();
        updateChart();
    }

    private void updateChart() {
        int ignorant = 0;
        int spreader = 0;
        int inactive = 0;
        int grok = 0;
        int bot = 0;
        int influencer = 0;
        int echoChamber = 0;
        int factChecker = 0;
        int journalist = 0;

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
                } else if (state == CellState.BOT) {
                    bot++;
                } else if (state == CellState.INFLUENCER) {
                    influencer++;
                } else if (state == CellState.ECHO_CHAMBER) {
                    echoChamber++;
                } else if (state == CellState.FACT_CHECKER) {
                    factChecker++;
                } else {
                    journalist++;
                }
            }
        }

        ignorantSeries.getData().add(new XYChart.Data<>(generation, ignorant));
        spreaderSeries.getData().add(new XYChart.Data<>(generation, spreader));
        inactiveSeries.getData().add(new XYChart.Data<>(generation, inactive));
        grokSeries.getData().add(new XYChart.Data<>(generation, grok));
        botSeries.getData().add(new XYChart.Data<>(generation, bot));
        influencerSeries.getData().add(new XYChart.Data<>(generation, influencer));
        echoChamberSeries.getData().add(new XYChart.Data<>(generation, echoChamber));
        factCheckerSeries.getData().add(new XYChart.Data<>(generation, factChecker));
        journalistSeries.getData().add(new XYChart.Data<>(generation, journalist));
    }

    private void clearResults() {
        resultModeLabel.setText("Modo: -");
        resultTimeLabel.setText("Tempo: -");
        resultComputeTimeLabel.setText("Tempo de calculo: -");
        resultUnitsLabel.setText("Unidades: -");
        resultMemoryLabel.setText("Memória JVM: -");
    }

    private void updateResults(String mode) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemoryMb = runtime.maxMemory() / (1024 * 1024);
        String execution = selectedExecutionType();
        String totalLabel = EXECUTION_BENCHMARK.equals(execution)
                ? "Tempo total benchmark"
                : "Tempo visual";
        String computeLabel = EXECUTION_BENCHMARK.equals(execution)
                ? "Tempo medido do algoritmo"
                : "Tempo total de calculo";

        resultModeLabel.setText("Modo: " + mode + " | Execução: " + execution);
        resultTimeLabel.setText(String.format(
                "%s: %.3f ms para %d geracoes",
                totalLabel,
                totalElapsedNanos / 1_000_000.0,
                generation));
        resultComputeTimeLabel.setText(String.format(
                "%s: %.3f ms",
                computeLabel,
                computeElapsedNanos / 1_000_000.0));
        resultUnitsLabel.setText("Unidades: " + executionUnits(mode));
        resultMemoryLabel.setText(String.format("Memoria JVM: %d/%d MB", usedMemoryMb, maxMemoryMb));
    }

    private String selectedExecutionType() {
        return activeExecution == null ? executionBox.getValue() : activeExecution;
    }

    private String executionUnits(String mode) {
        if (MODE_PARALLEL.equals(mode)) {
            return THREADS + " threads";
        }
        if (MODE_DISTRIBUTED.equals(mode)) {
            return configuredRmiWorkerCount() + " workers RMI";
        }
        return "1 thread";
    }

    private record StateCount(Label label, String name, CellState state, int count) {
    }

    private record BenchmarkOutcome(CellState[][] grid, int generation, long computeElapsedNanos) {
    }

    public static void main(String[] args) {
        launch(args);
    }
}
