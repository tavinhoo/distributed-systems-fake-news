# Projeto Fake News em Sistemas Paralelos e Distribuidos

![Banner do projeto](src/main/java/assets/banner-project.png)

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

### Como os agentes funcionam

![Legenda dos agentes](src/main/java/assets/agents-and-how-it-works.png)

## Arquitetura

```text
src/main/java/
|-- app/
|   |-- Main.java
|   |-- BenchmarkRunner.java
|   `-- SimulationViewer.java
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

## Pre-requisitos

- JDK 17 ou superior.
- Maven.
- Ambiente grafico ativo para executar a interface JavaFX.

Verificar instalacao:

```bash
java -version
mvn -version
```

## Como executar pelo Maven

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

Executar interface grafica JavaFX:

```bash
mvn javafx:run
```

A interface grafica mostra uma grade da simulacao e permite alternar entre os modos `Sequencial`, `Paralela` e `Distribuida RMI`. Ela serve para demonstracao visual da propagacao; os resultados experimentais devem ser gerados pelo `BenchmarkRunner`.

## Como executar com javac

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

## Como executar a versao distribuida manualmente

Primeiro compile o projeto com Maven:

```bash
mvn compile
```

Abra um terminal para cada worker RMI.

Worker 1:

```bash
java -cp target/classes distributed.WorkerServer 9100 worker
```

Worker 2:

```bash
java -cp target/classes distributed.WorkerServer 9101 worker
```

Em outro terminal, execute o coordenador:

```bash
java -cp target/classes app.Main distributed 127.0.0.1:9100:worker 127.0.0.1:9101:worker
```

Tambem e possivel usar os comandos com `out` quando a compilacao for feita por `javac`:

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

Em maquinas diferentes, troque `127.0.0.1` pelo IP da maquina onde cada worker esta rodando. Verifique firewall e portas.

## Benchmark

`BenchmarkRunner` executa as versoes sequencial, paralela e distribuida, mede o tempo com `System.nanoTime()` e calcula:

- tempo total;
- `speedup = tempoSequencial / tempoVersao`;
- `eficiencia = speedup / numeroDeThreadsOuWorkers`;
- contagens finais de todos os estados;
- validacao de consistencia entre as versoes.

Executar benchmark rapido com 4 threads, 2 workers RMI locais e porta inicial 9100:

```bash
java -cp target/classes app.BenchmarkRunner 4 2 9100
```

Os argumentos sao opcionais e seguem esta ordem: `threads`, `workers`, `basePort`. O benchmark sobe os workers RMI locais automaticamente, executa a simulacao distribuida e depois encerra os objetos RMI.

Executar benchmark em lote para gerar dados experimentais:

```bash
java -cp target/classes app.BenchmarkRunner batch 9100
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

Exemplo resumido de saida esperada:

```text
Cenario: base_80x80
Sequencial   tempo=   22.486 ms | speedup= 1.000 | eficiencia= 1.000 | unidades=1
Paralela     tempo=   30.908 ms | speedup= 0.728 | eficiencia= 0.364 | unidades=2
Paralela     tempo=   31.751 ms | speedup= 0.708 | eficiencia= 0.177 | unidades=4
Distribuida  tempo=  232.315 ms | speedup= 0.097 | eficiencia= 0.048 | unidades=2
```

A versao distribuida pode ser mais lenta em matrizes pequenas, porque ha custo de chamada remota e transferencia de blocos da matriz a cada geracao. Esse comportamento e esperado e deve ser discutido na analise de custo de comunicacao e limitacoes.

## Resultados experimentais

O arquivo `benchmark-results.csv` gerado pelo modo `batch` contem os dados usados para tabelas e graficos. Os cenarios atuais variam:

- tamanho da matriz;
- numero de geracoes;
- percentual inicial de espalhadores;
- numero de threads;
- numero de workers RMI.

Os resultados ja calculam:

- tempo total;
- speedup;
- eficiencia;
- contagens finais dos estados.

Para a analise principal, recomenda-se usar primeiro os resultados da versao sequencial e paralela. A versao distribuida tambem esta implementada e medida, mas pode ser analisada separadamente porque o custo de RMI em execucao local tende a dominar o tempo em matrizes pequenas.

## Melhorias e inovacoes

As melhorias implementadas no modelo foram:

- `GROK`: representa individuo resistente/imune a fake news.
- `WHATSAPP_GROUP`: representa grupo ativo que aumenta o alcance da fake news quando existe espalhador por perto.
- `INFLUENCER`: representa perfil de grande alcance, ampliando a propagacao em raio maior.
- Propagacao probabilistica: evita que a fake news domine toda a matriz instantaneamente.
- Interface grafica JavaFX: permite visualizar a evolucao da simulacao.
- Benchmark em lote: automatiza a geracao de dados experimentais para comparacao.

Essas melhorias se enquadram nos criterios de inovacao por adicionarem resistencia, agentes de amplificacao social, visualizacao grafica, estatisticas adicionais e comparacao automatizada.

## Interface grafica

Executar:

```bash
mvn javafx:run
```

A interface mostra:

- grade da simulacao;
- legenda por cor;
- contadores por estado;
- progresso da simulacao;
- tempo e uso de memoria ao final.

Tela inicial da interface:

![Interface grafica no inicio](src/main/java/assets/fake-news-start.png)

Programa em execucao:

![Interface grafica em execucao](src/main/java/assets/fake-news-execution.gif)

## IntelliJ

Abra a pasta do projeto no IntelliJ e execute:

- `app.Main` para rodar uma versao especifica;
- `app.BenchmarkRunner` para rodar o benchmark.
- `app.SimulationViewer` ou `mvn javafx:run` para abrir a interface grafica.

## Guia detalhado

Tambem ha um guia separado com os comandos principais:

```text
GUIA_EXECUCAO.md
```

## Referencias

1. Vosoughi, S.; Roy, D.; Aral, S. **The spread of true and false news online**. *Science*, 2018.  
   Justifica por que a fake news tende a se espalhar rapidamente e motiva o cenario pessimista da simulacao.  
   https://www.science.org/doi/10.1126/science.aap9559

2. Pierri, F.; Piccardi, C.; Ceri, S. **Topology comparison of Twitter diffusion networks effectively reveals misleading information**. 2019.  
   Fundamenta a propagacao da informacao em redes sociais e ajuda a justificar os agentes `WHATSAPP_GROUP` e `INFLUENCER`.  
   https://arxiv.org/abs/1905.03043

3. Schiff, J. L. **Cellular Automata: A Discrete View of the World**. Wiley, 2008.  
   Justifica o uso de matriz com estados discretos e atualizacao por geracoes.

4. **Moore Neighborhood**.  
   Justifica tecnicamente por que cada celula interage com ate oito vizinhos na vizinhanca basica.  
   https://en.wikipedia.org/wiki/Moore_neighborhood

5. Oracle. **The Java Tutorials: Concurrency**.  
   Justifica o uso de Threads, sincronizacao, barreiras e prevencao de race conditions.  
   https://docs.oracle.com/javase/tutorial/essential/concurrency/

6. Oracle. **Java Remote Method Invocation (RMI)**.  
   Fundamenta a implementacao distribuida utilizando objetos remotos.  
   https://docs.oracle.com/javase/tutorial/rmi/

7. OpenJFX. **OpenJFX Documentation**.  
   Justifica a escolha da interface grafica JavaFX.  
   https://openjfx.io/

8. Amdahl, G. M. **Validity of the Single Processor Approach to Achieving Large Scale Computing Capabilities**. AFIPS, 1967.  
   Fundamenta a analise de speedup, eficiencia e limites do paralelismo.  
   https://doi.org/10.1145/1465482.1465560
