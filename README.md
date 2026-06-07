# Projeto Fake News em Sistemas Paralelos

Projeto academico simples em Java para simular a propagacao de fake news em uma populacao representada por uma matriz bidimensional.

## Tema

Cada posicao da matriz representa uma pessoa. Os estados possiveis sao:

- `IGNORANT`: ainda nao recebeu/acredita na informacao.
- `SPREADER`: acredita e compartilha a informacao.
- `INACTIVE`: recebeu a informacao, mas nao compartilha mais.
- `GROK`: agente verificador que combate a fake news.

A simulacao acontece em geracoes discretas. Em cada geracao, o programa le a matriz atual (`currentGrid`) e escreve o resultado na proxima matriz (`nextGrid`). Isso evita que uma celula atualizada no inicio da varredura influencie outra celula na mesma geracao.

## Regras da simulacao

- A vizinhanca usada e a de Moore: ate 8 vizinhos ao redor de uma celula.
- Uma pessoa `IGNORANT` pode virar `SPREADER` se tiver pelo menos um vizinho `SPREADER`, usando uma probabilidade configuravel.
- Uma pessoa `SPREADER` pode virar `INACTIVE`, usando uma probabilidade configuravel.
- Uma pessoa `INACTIVE` permanece `INACTIVE`.
- Uma pessoa `GROK` permanece `GROK` durante toda a simulacao.
- Se um `SPREADER` tiver pelo menos um `GROK` na vizinhanca, ele pode virar `INACTIVE` por correcao/verificacao.
- Se um `IGNORANT` tiver vizinhos `SPREADER` e `GROK` ao mesmo tempo, a chance de virar `SPREADER` e reduzida.
- A seed fica em `SimulationConfig`, permitindo repetir a mesma configuracao inicial.

Para manter as versoes comparaveis, a aleatoriedade das regras e calculada de forma deterministica com base em seed, geracao, linha, coluna e regra aplicada.

As configuracoes principais do agente `GROK` ficam em `SimulationConfig`:

- `initialGrokPercentage`: percentual inicial de agentes verificadores.
- `grokCorrectionProbability`: chance de um `GROK` vizinho neutralizar um `SPREADER`.
- `grokInfluenceReductionFactor`: fator que reduz a chance de um `IGNORANT` acreditar na fake news quando tambem ha `GROK` por perto.

No cenario padrao, a influencia do `GROK` e limitada: ele reduz a propagacao, mas nao elimina automaticamente a fake news. Isso mantem o modelo equilibrado para comparacao experimental.

## Arquitetura

```text
src/main/java/
|-- app/
|   |-- Main.java
|   `-- BenchmarkRunner.java
|-- model/
|   |-- CellState.java
|   |-- SimulationConfig.java
|   `-- SimulationResult.java
|-- core/
|   |-- GridFactory.java
|   |-- SimulationRules.java
|   `-- Statistics.java
|-- sequential/
|   `-- SequentialSimulation.java
|-- parallel/
|   |-- ParallelSimulation.java
|   `-- MatrixWorker.java
`-- util/
    |-- CsvWriter.java
    `-- Timer.java
```

## Versao sequencial

`SequentialSimulation` percorre toda a matriz em uma unica thread. Ela serve como base de comparacao para tempo, speedup e eficiencia.

## Versao paralela

`ParallelSimulation` divide a matriz por faixas de linhas. Cada `MatrixWorker` implementa `Runnable` e calcula apenas a sua faixa.

Como as threads leem somente `currentGrid` e escrevem somente na sua parte de `nextGrid`, nao ha escrita concorrente na mesma celula. A thread principal chama `join()` em todas as threads antes de trocar `currentGrid` por `nextGrid`, garantindo que uma nova geracao so comece quando a anterior terminou.

## Como executar pelo terminal

Compile com `javac`:

```bash
javac -d out $(find src/main/java -name "*.java")
```

No Windows PowerShell:

```powershell
javac -d out (Get-ChildItem -Recurse src/main/java/*.java)
```

Executar versao sequencial:

```bash
java -cp out app.Main sequential
```

Executar versao paralela com 4 threads:

```bash
java -cp out app.Main parallel 4
```

## Como executar com Maven

O projeto tambem possui um `pom.xml` simples. Para compilar:

```bash
mvn compile
```

Para executar pelo Maven:

```bash
mvn exec:java -Dexec.mainClass=app.Main -Dexec.args="parallel 4"
```

Se o plugin `exec-maven-plugin` nao estiver configurado no ambiente, use a execucao por `javac` ou configure a classe principal no IntelliJ.

## Benchmark

`BenchmarkRunner` executa as versoes sequencial e paralela, mede o tempo com `System.nanoTime()` e calcula:

- tempo total;
- `speedup = tempoSequencial / tempoVersao`;
- `eficiencia = speedup / numeroDeThreads`;
- quantidade final de `GROK`;
- total de espalhadores neutralizados por influencia de `GROK`.

Executar benchmark com 4 threads:

```bash
java -cp out app.BenchmarkRunner 4
```

O benchmark gera o arquivo:

```text
benchmark-results.csv
```

Ao final, o benchmark tambem imprime uma comparacao social sequencial entre:

- cenario com `GROK`;
- cenario sem `GROK`, usando a mesma matriz inicial, mas convertendo os agentes `GROK` para `IGNORANT`.

Exemplo de saida esperada:

```text
Sequencial   tempo=   80.500 ms | speedup= 1.000 | eficiencia= 1.000 | unidades=1
Paralela     tempo=   35.200 ms | speedup= 2.287 | eficiencia= 0.572 | unidades=4
CSV gerado em: benchmark-results.csv

Comparacao social sequencial:
Com GROK:    IGNORANT=34, SPREADER=0, INACTIVE=6174, GROK=192, neutralizados=543
Sem GROK:    IGNORANT=0, SPREADER=0, INACTIVE=6400, GROK=0, neutralizados=0
```

## IntelliJ

Abra a pasta do projeto no IntelliJ e execute:

- `app.Main` para rodar uma versao especifica;
- `app.BenchmarkRunner` para rodar o benchmark.
