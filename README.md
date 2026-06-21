# Projeto Fake News em Sistemas Paralelos e Distribuídos

![Banner do projeto](src/main/java/assets/banner-project.png)

Projeto academico em Java para simular a propagação de fake news em uma população representada por uma matriz bidimensional. Cada celula da matriz representa uma pessoa ou agente social, e a simulação evolui por gerações discretas.

O projeto possui tres formas de processamento: sequencial, paralela com threads e distribuída com Java RMI. Tambem inclui uma interface gráfica JavaFX para visualização da matriz, acompanhamento dos estados e execução em modo visual, benchmark ou visualização didática.

## Indice da documentação

- [01 - Modos de execução](docs/01-modos-execução.md)  
  Explica as versoes sequencial, paralela e distribuída RMI, incluindo divisão da matriz, fluxo de processamento, vantagens, limitacoes e diferencas tecnicas.

- [02 - Interface gráfica](docs/02-interface-gráfica.md)  
  Descreve a arquitetura JavaFX, a classe `SimulationViewer`, os componentes da interface, o fluxo de execução, os graficos, os indicadores de status e a visualização da matriz.

- [03 - Modos de visualização](docs/03-modos-visualização.md)  
  Detalha os modos `Visual`, `Benchmark` e `Visualização didática`, explicando o objetivo e o funcionamento de cada um.

- [04 - Agentes](docs/04-agentes.md)  
  Documenta todos os estados e agentes do modelo, suas regras de interação, probabilidades, efeitos de amplificação, resistencia e neutralização.

- [05 - Resultados](docs/05-resultados.md)  
  Reune os resultados experimentais produzidos, a configuração usada, a tabela comparativa, a analise de desempenho, gargalos, sincronização, comunicação e limitacoes.

- [06 - Melhorias](docs/06-melhorias.md)  
  Lista as melhorias e extensoes implementadas no projeto, indicando quais itens da proposta foram atendidos, parcialmente atendidos ou não implementados.

## Visão geral

A simulação usa atualização por gerações. Em cada geração, o programa lê a matriz atual (`currentGrid`) e escreve os novos estados em outra matriz (`nextGrid`). Ao final da geração, as referencias são trocadas. Esse fluxo evita que uma celula recem-atualizada influencie outra celula dentro da mesma geração.

Os estados principais são:

- `IGNORANT`: ainda não recebeu ou não acredita na informação.
- `SPREADER`: acredita e compartilha a fake news.
- `INACTIVE`: recebeu a informação, mas não compartilha mais.
- `GROK`: IA verificadora que reduz a propagação e pode neutralizar conversoes.
- `BOT`: conta automatizada que amplia a propagação.
- `INFLUENCER`: perfil de maior alcance.
- `ECHO_CHAMBER`: bolha social que reforça a fake news.
- `FACT_CHECKER`: checador de fatos que reduz conversoes e pode neutralizar tentativas.
- `JOURNALIST`: agente jornalístico com efeito local de redução.

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

## Execução rápida

Compilar:

```bash
mvn compile
```

Executar versão sequencial:

```bash
java -cp target/classes app.Main sequential
```

Executar versão paralela com 4 threads:

```bash
java -cp target/classes app.Main parallel 4
```

Executar interface gráfica:

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
