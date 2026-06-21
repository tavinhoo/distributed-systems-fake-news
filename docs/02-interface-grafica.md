# 02 - Interface Gráfica

A interface gráfica está implementada em `app.SimulationViewer` usando JavaFX. Ela foi criada para apresentar a evolução da simulação, alternar entre modos de processamento e acompanhar contadores, progresso, gráfico e resultados finais.

## Arquitetura JavaFX

`SimulationViewer` estende `Application`, ponto de entrada padrão de aplicações JavaFX. A tela é montada no método `start(Stage stage)`.

Principais estruturas JavaFX usadas:

- `BorderPane`: container principal da tela.
- `HBox` e `VBox`: organização horizontal e vertical.
- `Canvas`: desenho da matriz.
- `LineChart<Number, Number>`: gráfico da evolução dos estados.
- `ComboBox`: seleção de modo de processamento e modo de execução.
- `TextField`: entrada de endereços de workers RMI.
- `Button`: iniciar, passar e reiniciar.
- `ProgressBar`: progresso da simulação.
- `Label`: status, contadores e resultados.
- `Timeline`: execução visual temporizada.
- `Task`: execução em segundo plano no modo benchmark.

## Classe `SimulationViewer`

A classe concentra a interface e a orquestração visual da simulação.

Constantes relevantes:

- `MODE_SEQUENTIAL`: modo de processamento sequencial.
- `MODE_PARALLEL`: modo de processamento paralelo.
- `MODE_DISTRIBUTED`: modo distribuído RMI.
- `EXECUTION_VISUAL`: execução visual animada.
- `EXECUTION_BENCHMARK`: execução sem renderizar cada geração.
- `EXECUTION_DIDACTIC`: visualização didática.
- `ROWS`, `COLUMNS`, `GENERATIONS`: configuração usada pela interface.
- `THREADS`: quantidade de threads usada no modo paralelo da interface.
- `DEFAULT_RMI_WORKERS`: endereços padrão para o modo distribuído na interface.

Estado interno relevante:

- `currentGrid`: matriz atual.
- `generation`: geração atual.
- `config`: configuração da simulação.
- `computeElapsedNanos`: tempo gasto em cálculo.
- `totalElapsedNanos`: tempo total percebido pela execução.
- `distributedSimulation`: instância reutilizada da simulação distribuída.
- variaveis de zoom e pan da matriz.
- variáveis específicas da visualização didática.

## Componentes da interface

### Cabeçalho

O cabeçalho mostra o título, status e controles principais:

- modo de processamento: `Sequencial`, `Paralela` ou `Distribuída RMI`;
- modo de execução: `Visual`, `Benchmark` ou `Visualização didática`;
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
- memória da JVM.

### Canvas da matriz

A matriz é desenhada em `Canvas`. Isso evita criar um componente visual para cada célula, o que seria pesado em matrizes grandes.

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

### Gráfico de estados

O `LineChart` mostra a evolução dos estados ao longo das gerações. Há uma série para cada estado:

- ignorantes;
- espalhadores;
- inativos;
- `GROK`;
- bots;
- influenciadores;
- bolhas;
- checadores;
- jornalistas.

## Fluxo de execução

### Reinício

O método `reset()`:

1. Cancela benchmark em andamento, se existir.
2. Para os timelines.
3. Limpa estado didático.
4. Reinicia contadores e tempos.
5. Cria uma nova `SimulationConfig`.
6. Gera a matriz inicial com `GridFactory.createInitialGrid(config)`.
7. Redesenha a matriz.
8. Atualiza status e gráfico.

### Passo de simulação

O método `nextStep()` executa uma geração quando não há benchmark em andamento.

Ele escolhe o modo de processamento:

- sequencial: chama `nextSequentialGeneration()`;
- paralelo: chama `nextParallelGeneration()`;
- distribuído: chama `nextDistributedGeneration()`.

Depois atualiza:

- matriz atual;
- tempo de cálculo;
- geração;
- desenho da matriz;
- status;
- gráfico.

### Execução contínua

No modo visual, um `Timeline` chama `nextStep()` periodicamente. O intervalo configurado no código é de 180 ms.

### Benchmark

No modo benchmark, `startBenchmarkRun()` cria uma `Task<BenchmarkOutcome>`. Essa tarefa executa as gerações em segundo plano e mede o tempo de cálculo sem redesenhar a matriz a cada geração.

Ao final:

1. Atualiza `currentGrid`.
2. Atualiza `generation`.
3. Redesenha a matriz uma vez.
4. Atualiza status e gráfico.
5. Mostra resultados finais.

## Status e resultados

O status indica:

- geração atual;
- total de gerações;
- modo selecionado;
- tipo de execução.

O resultado final exibe:

- modo e tipo de execução;
- tempo visual ou tempo total benchmark;
- tempo medido do algoritmo;
- unidades usadas;
- memória JVM usada e máxima.

## Visualização e navegação

A interface possui zoom e pan na matriz:

- zoom com roda do mouse;
- arrasto para mover a área visível;
- duplo clique para voltar ao zoom inicial.

Quando o zoom está baixo, a interface usa agregação de blocos para manter a visualização legível. Quando há espaço suficiente, desenha células individuais.
