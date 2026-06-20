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
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
import java.util.Comparator;
import java.util.List;

public class SimulationViewer extends Application {
    private static final String MODE_SEQUENTIAL = "Sequencial";
    private static final String MODE_PARALLEL = "Paralela";
    private static final String MODE_DISTRIBUTED = "Distribuida RMI";
    private static final String EXECUTION_VISUAL = "Visual";
    private static final String EXECUTION_BENCHMARK = "Benchmark";
    private static final int ROWS = 500;
    private static final int COLUMNS = 500;
    private static final int GENERATIONS = 500;
    private static final int THREADS = 12;
    private static final int RMI_WORKERS = 2;
    private static final int RMI_BASE_PORT = 9600;
    private static final double MIN_GRID_ZOOM = 1.0;
    private static final double MAX_GRID_ZOOM = 32.0;
    private static final double AGGREGATED_BLOCK_TARGET_PIXELS = 6.0;
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
    private final Label whatsAppGroupLabel = new Label();
    private final Label influencerLabel = new Label();
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
    private final XYChart.Series<Number, Number> whatsAppGroupSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> influencerSeries = new XYChart.Series<>();
    private final ComboBox<String> modeBox = new ComboBox<>();
    private final ComboBox<String> executionBox = new ComboBox<>();
    private final List<LocalWorker> localWorkers = new ArrayList<>();
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
    private Task<BenchmarkOutcome> benchmarkTask;
    private DistributedSimulation distributedSimulation;

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
        whatsAppGroupSeries.setName("Grupos WhatsApp");
        influencerSeries.setName("Influenciadores");
        stateChart.getData().add(ignorantSeries);
        stateChart.getData().add(spreaderSeries);
        stateChart.getData().add(inactiveSeries);
        stateChart.getData().add(grokSeries);
        stateChart.getData().add(whatsAppGroupSeries);
        stateChart.getData().add(influencerSeries);
    }

    @Override
    public void start(Stage stage) {
        modeBox.getItems().addAll(MODE_SEQUENTIAL, MODE_PARALLEL, MODE_DISTRIBUTED);
        modeBox.setValue(MODE_SEQUENTIAL);
        modeBox.setPrefWidth(170);
        styleComboBox(modeBox);

        executionBox.getItems().addAll(EXECUTION_VISUAL, EXECUTION_BENCHMARK);
        executionBox.setValue(EXECUTION_VISUAL);
        executionBox.setPrefWidth(135);
        styleComboBox(executionBox);

        runButton = new Button("Iniciar");
        stepButton = new Button("Passar");
        Button resetButton = new Button("Reiniciar");
        stylePrimaryButton(runButton);
        styleSecondaryButton(stepButton);
        styleSecondaryButton(resetButton);

        timeline = new Timeline(new KeyFrame(Duration.millis(180), event -> nextStep()));
        timeline.setCycleCount(Animation.INDEFINITE);

        runButton.setOnAction(event -> toggleRun(runButton));
        stepButton.setOnAction(event -> nextStep());
        resetButton.setOnAction(event -> reset());

        VBox processingControl = controlGroup("Processamento", modeBox);
        VBox executionControl = controlGroup("Execução", executionBox);

        HBox selectors = new HBox(10, processingControl, executionControl);
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
        stage.setOnCloseRequest(event -> stopWorkers());
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
            activeExecution = EXECUTION_VISUAL;
            modeBox.setDisable(true);
            executionBox.setDisable(true);
            runSegmentStartNanos = System.nanoTime();
            timeline.play();
            runButton.setText("Pausar");
        }
    }

    private void reset() {
        if (benchmarkTask != null && benchmarkTask.isRunning()) {
            benchmarkTask.cancel();
        }
        timeline.stop();
        generation = 0;
        totalElapsedNanos = 0;
        runSegmentStartNanos = 0;
        computeElapsedNanos = 0;
        gridZoom = MIN_GRID_ZOOM;
        gridViewRow = 0;
        gridViewColumn = 0;
        activeMode = null;
        activeExecution = null;
        config = new SimulationConfig(ROWS, COLUMNS, GENERATIONS, 0.03, 0.03,
                0.015, 0.01, 0.25, 0.50, 42L);
        currentGrid = GridFactory.createInitialGrid(config);
        runButton.setText("Iniciar");
        modeBox.setDisable(false);
        executionBox.setDisable(false);
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

        try {
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
            runButton.setText("Iniciar");
            modeBox.setDisable(false);
            executionBox.setDisable(false);
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
        String benchmarkMode = activeMode;
        totalElapsedNanos = 0;
        runSegmentStartNanos = System.nanoTime();
        computeElapsedNanos = 0;
        clearResults();
        modeBox.setDisable(true);
        executionBox.setDisable(true);
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
            modeBox.setDisable(false);
            executionBox.setDisable(false);
            stepButton.setDisable(false);
        });

        benchmarkTask.setOnCancelled(event -> {
            totalElapsedNanos = System.nanoTime() - runSegmentStartNanos;
            runSegmentStartNanos = 0;
            runButton.setText("Iniciar");
            statusLabel.setText("Benchmark cancelado na geracao " + generation + ".");
            activeMode = null;
            activeExecution = null;
            modeBox.setDisable(false);
            executionBox.setDisable(false);
            stepButton.setDisable(false);
        });

        benchmarkTask.setOnFailed(event -> {
            runSegmentStartNanos = 0;
            runButton.setText("Iniciar");
            Throwable exception = benchmarkTask.getException();
            statusLabel.setText("Erro: " + (exception == null ? "falha desconhecida" : exception.getMessage()));
            activeMode = null;
            activeExecution = null;
            modeBox.setDisable(false);
            executionBox.setDisable(false);
            stepButton.setDisable(false);
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
        runButton.setText("Iniciar");
        updateResults(selectedExecutionMode());
        activeMode = null;
        activeExecution = null;
        modeBox.setDisable(false);
        executionBox.setDisable(false);
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
        if (distributedSimulation == null) {
            startWorkers();
            distributedSimulation = new DistributedSimulation(
                    DistributedSimulation.localWorkers(RMI_WORKERS, RMI_BASE_PORT));
        }
        return distributedSimulation.nextGeneration(grid, config, generation);
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
        if (benchmarkTask != null && benchmarkTask.isRunning()) {
            benchmarkTask.cancel();
        }
        for (LocalWorker worker : localWorkers) {
            try {
                worker.registry().unbind(WorkerServer.DEFAULT_WORKER_NAME);
                UnicastRemoteObject.unexportObject(worker.remote(), true);
                UnicastRemoteObject.unexportObject(worker.registry(), true);
            } catch (Exception ignored) {
            }
        }
        localWorkers.clear();
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

    private VBox controlGroup(String text, ComboBox<String> control) {
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
                "Geração %d de %d | Modo: %s | Execução: %s",
                generation, config.getGenerations(), selectedExecutionMode(), selectedExecutionType()));
        ignorantLabel.setText("Ignorantes: " + ignorant);
        spreaderLabel.setText("Espalhadores: " + spreader);
        inactiveLabel.setText("Inativos: " + inactive);
        grokLabel.setText("GROK: " + grok);
        whatsAppGroupLabel.setText("Grupos WhatsApp: " + whatsAppGroup);
        influencerLabel.setText("Influenciadores: " + influencer);
        progressBar.setProgress((double) generation / config.getGenerations());
        progressPercentLabel.setText(String.format("%.0f%%", 100.0 * generation / config.getGenerations()));
        updateStateRanking(ignorant, spreader, inactive, grok, whatsAppGroup, influencer);
    }

    private void updateStateRanking(int ignorant,
                                    int spreader,
                                    int inactive,
                                    int grok,
                                    int whatsAppGroup,
                                    int influencer) {
        List<StateCount> counts = new ArrayList<>();
        counts.add(new StateCount(ignorantLabel, "Ignorantes", CellState.IGNORANT, ignorant));
        counts.add(new StateCount(spreaderLabel, "Espalhadores", CellState.SPREADER, spreader));
        counts.add(new StateCount(inactiveLabel, "Inativos", CellState.INACTIVE, inactive));
        counts.add(new StateCount(grokLabel, "GROK", CellState.GROK, grok));
        counts.add(new StateCount(whatsAppGroupLabel, "Grupos WhatsApp", CellState.WHATSAPP_GROUP, whatsAppGroup));
        counts.add(new StateCount(influencerLabel, "Influenciadores", CellState.INFLUENCER, influencer));

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
        whatsAppGroupSeries.getData().clear();
        influencerSeries.getData().clear();
        updateChart();
    }

    private void updateChart() {
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

        ignorantSeries.getData().add(new XYChart.Data<>(generation, ignorant));
        spreaderSeries.getData().add(new XYChart.Data<>(generation, spreader));
        inactiveSeries.getData().add(new XYChart.Data<>(generation, inactive));
        grokSeries.getData().add(new XYChart.Data<>(generation, grok));
        whatsAppGroupSeries.getData().add(new XYChart.Data<>(generation, whatsAppGroup));
        influencerSeries.getData().add(new XYChart.Data<>(generation, influencer));
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
            return RMI_WORKERS + " workers RMI";
        }
        return "1 thread";
    }

    private record LocalWorker(Registry registry, MatrixWorkerImpl remote) {
    }

    private record StateCount(Label label, String name, CellState state, int count) {
    }

    private record BenchmarkOutcome(CellState[][] grid, int generation, long computeElapsedNanos) {
    }

    public static void main(String[] args) {
        launch(args);
    }
}
