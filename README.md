# Projeto Fake News em Sistemas Paralelos e Distribuidos

![Banner do projeto](src/main/java/assets/banner-project.png)

Projeto academico em Java para simular a propagacao de fake news em uma populacao representada por uma matriz bidimensional. Cada celula da matriz representa uma pessoa ou agente social, e a simulacao evolui por geracoes discretas.

O projeto possui tres formas de processamento: sequencial, paralela com threads e distribuida com Java RMI. Tambem inclui uma interface grafica JavaFX para visualizacao da matriz, acompanhamento dos estados e execucao em modo visual, benchmark ou visualizacao didatica.

## Indice da documentacao

- [01 - Modos de execucao](docs/01-modos-execucao.md)  
  Explica as versoes sequencial, paralela e distribuida RMI, incluindo divisao da matriz, fluxo de processamento, vantagens, limitacoes e diferencas tecnicas.

- [02 - Interface grafica](docs/02-interface-grafica.md)  
  Descreve a arquitetura JavaFX, a classe `SimulationViewer`, os componentes da interface, o fluxo de execucao, os graficos, os indicadores de status e a visualizacao da matriz.

- [03 - Modos de visualizacao](docs/03-modos-visualizacao.md)  
  Detalha os modos `Visual`, `Benchmark` e `Visualizacao didatica`, explicando o objetivo e o funcionamento de cada um.

- [04 - Agentes](docs/04-agentes.md)  
  Documenta todos os estados e agentes do modelo, suas regras de interacao, probabilidades, efeitos de amplificacao, resistencia e neutralizacao.

- [05 - Resultados](docs/05-resultados.md)  
  Reune os resultados experimentais produzidos, a configuracao usada, a tabela comparativa, a analise de desempenho, gargalos, sincronizacao, comunicacao e limitacoes.

- [06 - Melhorias](docs/06-melhorias.md)  
  Lista as melhorias e extensoes implementadas no projeto, indicando quais itens da proposta foram atendidos, parcialmente atendidos ou nao implementados.

## Visao geral

A simulacao usa atualizacao por geracoes. Em cada geracao, o programa le a matriz atual (`currentGrid`) e escreve os novos estados em outra matriz (`nextGrid`). Ao final da geracao, as referencias sao trocadas. Esse fluxo evita que uma celula recem-atualizada influencie outra celula dentro da mesma geracao.

Os estados principais sao:

- `IGNORANT`: ainda nao recebeu ou nao acredita na informacao.
- `SPREADER`: acredita e compartilha a fake news.
- `INACTIVE`: recebeu a informacao, mas nao compartilha mais.
- `GROK`: IA verificadora que reduz a propagacao e pode neutralizar conversoes.
- `BOT`: conta automatizada que amplia a propagacao.
- `INFLUENCER`: perfil de maior alcance.
- `ECHO_CHAMBER`: bolha social que reforca a fake news.
- `FACT_CHECKER`: checador de fatos que reduz conversoes e pode neutralizar tentativas.
- `JOURNALIST`: agente jornalistico com efeito local de reducao.

## Estrutura principal

```text
src/main/java/
|-- app/
|   |-- Main.java
|   |-- BenchmarkRunner.java
|   `-- SimulationViewer.java
|-- core/
|   |-- GridFactory.java
|   |-- SimulationRules.java
|   `-- Statistics.java
|-- distributed/
|   |-- DistributedSimulation.java
|   |-- MatrixWorkerImpl.java
|   |-- MatrixWorkerRemote.java
|   |-- WorkerResult.java
|   |-- WorkerServer.java
|   `-- WorkerTask.java
|-- model/
|   |-- CellState.java
|   |-- SimulationConfig.java
|   `-- SimulationResult.java
|-- parallel/
|   |-- MatrixWorker.java
|   `-- ParallelSimulation.java
|-- sequential/
|   `-- SequentialSimulation.java
`-- util/
    |-- CsvWriter.java
    `-- Timer.java
```

## Execucao rapida

Compilar:

```bash
mvn compile
```

Executar versao sequencial:

```bash
java -cp target/classes app.Main sequential
```

Executar versao paralela com 4 threads:

```bash
java -cp target/classes app.Main parallel 4
```

Executar interface grafica:

```bash
mvn javafx:run
```

Executar benchmark em lote com workers locais:

```bash
java -cp target/classes app.BenchmarkRunner batch 9100
```

Executar benchmark com workers locais e remoto:

```bash
java -cp target/classes app.BenchmarkRunner batch-all 9200 192.168.18.76:9100:worker
```
