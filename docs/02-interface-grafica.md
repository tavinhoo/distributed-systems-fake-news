# 02 - Interface Grafica

A interface grafica esta implementada em `app.SimulationViewer` usando JavaFX. Ela foi criada para apresentar a evolucao da simulacao, alternar entre modos de processamento e acompanhar contadores, progresso, grafico e resultados finais.

## Arquitetura JavaFX

`SimulationViewer` estende `Application`, ponto de entrada padrao de aplicacoes JavaFX. A tela e montada no metodo `start(Stage stage)`.

Principais estruturas JavaFX usadas:

- `BorderPane`: container principal da tela.
- `HBox` e `VBox`: organizacao horizontal e vertical.
- `Canvas`: desenho da matriz.
- `LineChart<Number, Number>`: grafico da evolucao dos estados.
- `ComboBox`: selecao de modo de processamento e modo de execucao.
- `TextField`: entrada de enderecos de workers RMI.
- `Button`: iniciar, passar e reiniciar.
- `ProgressBar`: progresso da simulacao.
- `Label`: status, contadores e resultados.
- `Timeline`: execucao visual temporizada.
- `Task`: execucao em segundo plano no modo benchmark.

## Classe `SimulationViewer`

A classe concentra a interface e a orquestracao visual da simulacao.

Constantes relevantes:

- `MODE_SEQUENTIAL`: modo de processamento sequencial.
- `MODE_PARALLEL`: modo de processamento paralelo.
- `MODE_DISTRIBUTED`: modo distribuido RMI.
- `EXECUTION_VISUAL`: execucao visual animada.
- `EXECUTION_BENCHMARK`: execucao sem renderizar cada geracao.
- `EXECUTION_DIDACTIC`: visualizacao didatica.
- `ROWS`, `COLUMNS`, `GENERATIONS`: configuracao usada pela interface.
- `THREADS`: quantidade de threads usada no modo paralelo da interface.
- `DEFAULT_RMI_WORKERS`: enderecos padrao para o modo distribuido na interface.

Estado interno relevante:

- `currentGrid`: matriz atual.
- `generation`: geracao atual.
- `config`: configuracao da simulacao.
- `computeElapsedNanos`: tempo gasto em calculo.
- `totalElapsedNanos`: tempo total percebido pela execucao.
- `distributedSimulation`: instancia reutilizada da simulacao distribuida.
- variaveis de zoom e pan da matriz.
- variaveis especificas da visualizacao didatica.

## Componentes da interface

### Cabecalho

O cabecalho mostra o titulo, status e controles principais:

- modo de processamento: `Sequencial`, `Paralela` ou `Distribuida RMI`;
- modo de execucao: `Visual`, `Benchmark` ou `Visualizacao didatica`;
- campo de workers RMI;
- botoes `Iniciar`, `Passar` e `Reiniciar`.

### Painel lateral

O painel lateral apresenta:

- progresso percentual;
- barra de progresso;
- ranking dos estados por quantidade;
- resultado final;
- tempo total;
- tempo medido do algoritmo;
- unidades usadas;
- memoria da JVM.

### Canvas da matriz

A matriz e desenhada em `Canvas`. Isso evita criar um componente visual para cada celula, o que seria pesado em matrizes grandes.

Cada estado possui uma cor:

- `SPREADER`: vermelho.
- `INACTIVE`: azul.
- `GROK`: verde claro.
- `BOT`: amarelo.
- `INFLUENCER`: magenta.
- `ECHO_CHAMBER`: laranja.
- `FACT_CHECKER`: verde.
- `JOURNALIST`: ciano.
- `IGNORANT`: preto.

### Grafico de estados

O `LineChart` mostra a evolucao dos estados ao longo das geracoes. Ha uma serie para cada estado:

- ignorantes;
- espalhadores;
- inativos;
- `GROK`;
- bots;
- influenciadores;
- bolhas;
- checadores;
- jornalistas.

## Fluxo de execucao

### Reinicio

O metodo `reset()`:

1. Cancela benchmark em andamento, se existir.
2. Para os timelines.
3. Limpa estado didatico.
4. Reinicia contadores e tempos.
5. Cria uma nova `SimulationConfig`.
6. Gera a matriz inicial com `GridFactory.createInitialGrid(config)`.
7. Redesenha a matriz.
8. Atualiza status e grafico.

### Passo de simulacao

O metodo `nextStep()` executa uma geracao quando nao ha benchmark em andamento.

Ele escolhe o modo de processamento:

- sequencial: chama `nextSequentialGeneration()`;
- paralelo: chama `nextParallelGeneration()`;
- distribuido: chama `nextDistributedGeneration()`.

Depois atualiza:

- matriz atual;
- tempo de calculo;
- geracao;
- desenho da matriz;
- status;
- grafico.

### Execucao continua

No modo visual, um `Timeline` chama `nextStep()` periodicamente. O intervalo configurado no codigo e de 180 ms.

### Benchmark

No modo benchmark, `startBenchmarkRun()` cria uma `Task<BenchmarkOutcome>`. Essa tarefa executa as geracoes em segundo plano e mede o tempo de calculo sem redesenhar a matriz a cada geracao.

Ao final:

1. Atualiza `currentGrid`.
2. Atualiza `generation`.
3. Redesenha a matriz uma vez.
4. Atualiza status e grafico.
5. Mostra resultados finais.

## Status e resultados

O status indica:

- geracao atual;
- total de geracoes;
- modo selecionado;
- tipo de execucao.

O resultado final exibe:

- modo e tipo de execucao;
- tempo visual ou tempo total benchmark;
- tempo medido do algoritmo;
- unidades usadas;
- memoria JVM usada e maxima.

## Visualizacao e navegacao

A interface possui zoom e pan na matriz:

- zoom com roda do mouse;
- arrasto para mover a area visivel;
- duplo clique para voltar ao zoom inicial.

Quando o zoom esta baixo, a interface usa agregacao de blocos para manter a visualizacao legivel. Quando ha espaco suficiente, desenha celulas individuais.
