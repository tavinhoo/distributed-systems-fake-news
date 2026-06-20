# Projeto Fake News em Sistemas Paralelos e Distribuidos

Projeto academico simples em Java para simular a propagacao de fake news em uma populacao representada por uma matriz bidimensional.

## Tema

Cada posicao da matriz representa uma pessoa. Os estados possiveis sao:

- `IGNORANT`: ainda nao recebeu/acredita na informacao.
- `SPREADER`: acredita e compartilha a informacao.
- `INACTIVE`: recebeu a informacao, mas nao compartilha mais.
- `GROK`: agente verificador que combate a fake news.
- `WHATSAPP_GROUP`: grupo ativo que amplia a propagacao ao redor.
- `INFLUENCER`: perfil de grande alcance que amplia ainda mais a propagacao.

A simulacao acontece em geracoes discretas. Em cada geracao, o programa le a matriz atual (`currentGrid`) e escreve o resultado na proxima matriz (`nextGrid`). Isso evita que uma celula atualizada no inicio da varredura influencie outra celula na mesma geracao.

## Regras da simulacao

- A vizinhanca usada e a de Moore: ate 8 vizinhos ao redor de uma celula.
- Uma pessoa `IGNORANT` pode virar `SPREADER` se tiver pelo menos um vizinho `SPREADER`, usando uma probabilidade configuravel.
- Uma pessoa `SPREADER` pode virar `INACTIVE`, mas tambem pode continuar espalhando por mais geracoes.
- Uma pessoa `INACTIVE` permanece `INACTIVE`.
- Uma pessoa `GROK` permanece `GROK` durante toda a simulacao.
- O estado `GROK` representa uma pessoa resistente/imune: ela nao vira `SPREADER`.
- Um `WHATSAPP_GROUP` permanece fixo e nao cria fake news sozinho.
- Um `INFLUENCER` permanece fixo e nao cria fake news sozinho.
- Se um `IGNORANT` estiver perto de um `WHATSAPP_GROUP`, um `SPREADER` em raio 2 pode influencia-lo com chance maior.
- Se um `IGNORANT` estiver perto de um `INFLUENCER`, um `SPREADER` em raio 3 pode influencia-lo com chance ainda maior.
- A seed fica em `SimulationConfig`, permitindo repetir a mesma configuracao inicial.

Todas as versoes calculam a proxima geracao lendo apenas a matriz atual e escrevendo em outra matriz. Assim, uma celula atualizada nao influencia outra celula dentro da mesma geracao.

As configuracoes principais ficam em `SimulationConfig`:

- tamanho da matriz;
- numero de geracoes;
- percentual inicial de espalhadores;
- `initialGrokPercentage`: percentual inicial de agentes verificadores.
- `initialWhatsAppGroupPercentage`: percentual inicial de grupos de WhatsApp.
- `initialInfluencerPercentage`: percentual inicial de influenciadores.

No cenario padrao, o `GROK` funciona como uma melhoria simples do modelo: individuos resistentes permanecem imunes durante toda a simulacao. Os grupos de WhatsApp e influenciadores tornam o cenario mais pessimista, pois aumentam o alcance da fake news quando ha espalhadores por perto. As probabilidades foram ajustadas para evitar uma propagacao instantanea: a fake news circula por mais tempo, mas ainda pode perder forca ao longo das geracoes.

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
|   |-- DistributedSimulation.java
|   |-- MatrixWorkerImpl.java
|   |-- MatrixWorkerRemote.java
|   |-- WorkerResult.java
|   |-- WorkerServer.java
|   `-- WorkerTask.java
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

`DistributedSimulation` usa Java RMI para distribuir o calculo entre workers. Cada worker recebe uma faixa de linhas da matriz, junto com as linhas fantasmas superior e inferior quando elas existem. Essas linhas extras permitem calcular corretamente a vizinhanca de Moore nas fronteiras entre faixas.

- `MatrixWorkerRemote`: interface remota RMI.
- `MatrixWorkerImpl`: implementacao remota que calcula uma faixa de linhas.
- `WorkerTask`: objeto serializavel enviado ao worker com o bloco da matriz, geracao e indices.
- `WorkerResult`: objeto serializavel devolvido com as linhas calculadas.
- `WorkerServer`: processo que cria o RMI Registry e registra um worker.

O coordenador espera todos os workers devolverem suas faixas antes de montar a proxima matriz. Assim, uma nova geracao so comeca depois que a geracao anterior terminou em todos os processos.

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

Executar workers RMI em terminais separados:

```bash
java -cp out distributed.WorkerServer 9100 worker
```

```bash
java -cp out distributed.WorkerServer 9101 worker
```

Executar a versao distribuida apontando para os workers:

```bash
java -cp out app.Main distributed 127.0.0.1:9100:worker 127.0.0.1:9101:worker
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

Executar a interface grafica JavaFX:

```bash
mvn javafx:run
```

A interface grafica mostra uma grade pequena da simulacao e permite alternar entre os modos `Sequencial`, `Paralela` e `Distribuida RMI`. Ela serve para demonstracao visual da propagacao; os resultados experimentais devem ser gerados pelo `BenchmarkRunner`.

Se o plugin `exec-maven-plugin` nao estiver configurado no ambiente, use a execucao por `javac` ou configure a classe principal no IntelliJ.

## Benchmark

`BenchmarkRunner` executa as versoes sequencial, paralela e distribuida, mede o tempo com `System.nanoTime()` e calcula:

- tempo total;
- `speedup = tempoSequencial / tempoVersao`;
- `eficiencia = speedup / numeroDeThreadsOuWorkers`;
- quantidade final de `GROK`;
- total de neutralizacoes por `GROK` quando houver regra especifica para isso.

Executar benchmark rapido com 4 threads, 2 workers RMI locais e porta inicial 9100:

```bash
java -cp out app.BenchmarkRunner 4 2 9100
```

Os argumentos sao opcionais e seguem esta ordem: `threads`, `workers`, `basePort`. O benchmark sobe os workers RMI locais automaticamente, executa a simulacao distribuida e depois encerra os objetos RMI.

Executar benchmark em lote para gerar dados experimentais:

```bash
java -cp out app.BenchmarkRunner batch 9100
```

O modo `batch` executa cenarios simples variando:

- tamanho da matriz;
- numero de geracoes;
- percentual inicial de espalhadores;
- numero de threads;
- numero de workers RMI.

O benchmark gera o arquivo:

```text
benchmark-results.csv
```

O CSV inclui o nome do cenario, versao, tamanho da matriz, geracoes, percentual inicial de espalhadores, unidades de execucao, tempo, speedup, eficiencia e contagens finais. O benchmark tambem gera:

```text
experimental-environment.txt
```

Esse arquivo registra sistema operacional, versao do Java, processadores logicos, memoria maxima da JVM e ambiente usado na execucao.

Ao final, o benchmark tambem imprime uma comparacao social sequencial entre:

- cenario com `GROK`;
- cenario sem `GROK`, usando a mesma matriz inicial, mas convertendo os agentes `GROK` para `IGNORANT`.

Exemplo de saida esperada:

```text
Sequencial   tempo=   80.500 ms | speedup= 1.000 | eficiencia= 1.000 | unidades=1
Paralela     tempo=   35.200 ms | speedup= 2.287 | eficiencia= 0.572 | unidades=4
Distribuida  tempo= 1240.421 ms | speedup= 0.065 | eficiencia= 0.033 | unidades=2
CSV gerado em: benchmark-results.csv

Comparacao social sequencial:
Com GROK:    IGNORANT=34, SPREADER=0, INACTIVE=6174, GROK=192, neutralizados=543
Sem GROK:    IGNORANT=0, SPREADER=0, INACTIVE=6400, GROK=0, neutralizados=0
```

A versao distribuida pode ser mais lenta em matrizes pequenas, porque ha custo de chamada remota e transferencia de blocos da matriz a cada geracao. Esse comportamento e esperado e deve ser discutido na analise de custo de comunicacao e limitacoes.

## IntelliJ

Abra a pasta do projeto no IntelliJ e execute:

- `app.Main` para rodar uma versao especifica;
- `app.BenchmarkRunner` para rodar o benchmark.
