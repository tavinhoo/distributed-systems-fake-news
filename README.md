# Projeto Fake News em Sistemas Paralelos e Distribuidos

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
|-- distributed/
|   |-- MatrixWorkerRemote.java
|   |-- RangeResult.java
|   |-- MatrixWorkerImpl.java
|   |-- WorkerServer.java
|   `-- DistributedSimulation.java
`-- util/
    |-- CsvWriter.java
    `-- Timer.java
```

## Versao sequencial

`SequentialSimulation` percorre toda a matriz em uma unica thread. Ela serve como base de comparacao para tempo, speedup e eficiencia.

## Versao paralela

`ParallelSimulation` divide a matriz por faixas de linhas. Cada `MatrixWorker` implementa `Runnable` e calcula apenas a sua faixa.

Como as threads leem somente `currentGrid` e escrevem somente na sua parte de `nextGrid`, nao ha escrita concorrente na mesma celula. A thread principal chama `join()` em todas as threads antes de trocar `currentGrid` por `nextGrid`, garantindo que uma nova geracao so comece quando a anterior terminou.

## Versao distribuida

`DistributedSimulation` segue a mesma ideia da versao paralela, mas em vez de threads, cada faixa de linhas é calculada por um processo separado (worker RMI), que pode estar na mesma maquina ou em maquinas diferentes na rede.

- `MatrixWorkerRemote`: interface RMI que define o metodo remoto `computeRange`.
- `MatrixWorkerImpl`: implementacao do worker, com a mesma logica de calculo do `MatrixWorker`, mas devolvendo o resultado em vez de escrever direto numa matriz compartilhada (processos nao compartilham memoria).
- `RangeResult`: objeto serializavel que viaja pela rede com a faixa calculada e o total de neutralizados por `GROK`.
- `WorkerServer`: processo que sobe um RMI Registry e expoe um worker em uma porta.

A cada geração, `DistributedSimulation` envia o `currentGrid` completo para cada worker, recebe de volta apenas a faixa de linhas calculada por ele, remonta o `nextGrid` e faz a troca, da mesma forma que a versao paralela faz com as threads.

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

## Como executar a versao distribuida

A versao distribuida pode ser executada de duas formas: automatica (via benchmark) ou manual, simulando processos/maquinas separadas.

### Forma automatica

O proprio `BenchmarkRunner` sobe e derruba os workers RMI necessarios. Basta executar o benchmark normalmente:

```bash
java -cp . app.BenchmarkRunner 4 4 9100
```

### Forma manual, com processos separados

Util para simular varias maquinas mesmo estando tudo em um unico computador. Abra um terminal por worker.

Para executar:

```bash
java -cp out distributed.WorkerServer 9100 worker
```

```bash
java -cp out distributed.WorkerServer 9101 worker
```

```bash
java -cp out distributed.WorkerServer 9102 worker
```

```bash
java -cp out distributed.WorkerServer 9103 worker
```


Cada `WorkerServer` fica aguardando conexoes na porta indicada. Em outro terminal, execute o coordenador (`DistributedSimulation`) apontando para as mesmas portas usadas acima.

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

`BenchmarkRunner` executa as versoes sequencial, paralela e distribuida, mede o tempo com `System.nanoTime()` e calcula:

- tempo total;
- `speedup = tempoSequencial / tempoVersao`;
- `eficiencia = speedup / numeroDeThreadsOuWorkers`;
- quantidade final de `GROK`;
- total de espalhadores neutralizados por influencia de `GROK`.

Executar benchmark com 4 threads, 4 workers distribuidos e porta inicial 9100:

```bash
java -cp out app.BenchmarkRunner 4 4 9100
```

Os tres argumentos sao opcionais, nessa ordem: `threads`, `workers`, `basePort`. Sem argumentos, o padrao e 4 threads, 4 workers e porta inicial 9100.

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
Distribuida  tempo= 1240.421 ms | speedup= 0.065 | eficiencia= 0.016 | unidades=4
CSV gerado em: benchmark-results.csv

Comparacao social sequencial:
Com GROK:    IGNORANT=34, SPREADER=0, INACTIVE=6174, GROK=192, neutralizados=543
Sem GROK:    IGNORANT=0, SPREADER=0, INACTIVE=6400, GROK=0, neutralizados=0
```

A versao distribuida tende a ser mais lenta que a sequencial e a paralela em matrizes pequenas, pois o grid completo e enviado pela rede a cada geracao. O overhead de comunicacao domina o tempo de calculo nesse cenario, o que e um ponto relevante para a analise de custo de comunicacao e limitacoes do trabalho.

## IntelliJ

Abra a pasta do projeto no IntelliJ e execute:

- `app.Main` para rodar uma versao especifica;
- `app.BenchmarkRunner` para rodar o benchmark.