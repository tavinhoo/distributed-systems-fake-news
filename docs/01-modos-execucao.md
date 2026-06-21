# 01 - Modos de Execucao

Este documento descreve os tres modos de processamento implementados no projeto: sequencial, paralelo e distribuido com Java RMI. Todos usam o mesmo modelo logico de simulacao e chamam as regras centralizadas em `SimulationRules`.

## Base comum

A simulacao trabalha com uma matriz bidimensional de `CellState`. Cada posicao representa uma pessoa ou agente social. A evolucao ocorre por geracoes discretas.

Em cada geracao:

1. A matriz atual e usada apenas para leitura.
2. O algoritmo calcula o proximo estado de cada celula.
3. O resultado e escrito em uma segunda matriz.
4. Ao final da geracao, a matriz calculada passa a ser a matriz atual.

Esse desenho aparece nas tres versoes e evita interferencia dentro da mesma geracao. Uma celula atualizada no inicio da varredura nao altera o resultado de outra celula que ainda sera processada naquela mesma geracao.

## Sequencial

A versao sequencial esta em `sequential.SequentialSimulation`.

Ela percorre todas as linhas e colunas em uma unica thread. Para cada celula, chama `SimulationRules.nextState(...)` e escreve o resultado em `nextGrid`.

Fluxo:

1. Recebe a matriz inicial e a configuracao.
2. Inicializa um temporizador.
3. Para cada geracao, percorre a matriz inteira.
4. Para cada celula, calcula o proximo estado.
5. Conta neutralizacoes por influencia de `GROK`.
6. Troca `currentGrid` e `nextGrid`.
7. Gera um `SimulationResult` com matriz final, tempo e contagens.

Vantagens:

- Implementacao direta e previsivel.
- Serve como referencia de corretude.
- Nao possui custo de criacao de threads nem comunicacao remota.

Limitacoes:

- Usa apenas uma thread.
- O tempo cresce diretamente com tamanho da matriz e numero de geracoes.
- Nao aproveita os nucleos disponiveis da CPU.

## Paralela

A versao paralela esta em `parallel.ParallelSimulation` e usa `parallel.MatrixWorker`.

Ela divide a matriz por faixas de linhas. Cada worker recebe um intervalo `[startRow, endRow)` e calcula apenas essa parte da matriz. A divisao e feita por:

```java
int startRow = index * config.getRows() / threadCount;
int endRow = (index + 1) * config.getRows() / threadCount;
```

Como cada thread escreve em linhas exclusivas de `nextGrid`, nao ha escrita concorrente na mesma celula. Todas as threads leem a mesma matriz `currentGrid`, que nao e modificada durante a geracao.

Fluxo:

1. Define a quantidade de threads.
2. Divide as linhas em faixas aproximadamente iguais.
3. Cria um `MatrixWorker` por faixa.
4. Inicia uma thread para cada worker.
5. A thread principal aguarda todas terminarem com `join()`.
6. Soma as estatisticas parciais dos workers.
7. Troca `currentGrid` e `nextGrid`.

Vantagens:

- Usa multiplos nucleos da maquina.
- Mantem a mesma regra logica da versao sequencial.
- Evita escrita concorrente na mesma posicao da matriz.
- Pode obter speedup em matrizes maiores.

Limitacoes:

- Cria e sincroniza threads a cada geracao.
- O custo de `join()` e criacao de threads pode reduzir o ganho em matrizes pequenas.
- A eficiencia cai quando a quantidade de threads cresce alem do ganho util do problema.

## Distribuida RMI

A versao distribuida esta em `distributed.DistributedSimulation` e usa Java RMI. Os principais componentes sao:

- `WorkerServer`: cria o registry RMI e registra um worker.
- `MatrixWorkerRemote`: interface remota.
- `MatrixWorkerImpl`: implementacao remota que calcula uma faixa.
- `WorkerTask`: objeto serializavel enviado ao worker.
- `WorkerResult`: objeto serializavel devolvido ao coordenador.
- `DistributedSimulation`: coordenador que divide a matriz, envia tarefas e monta a matriz final.

Cada worker recebe uma faixa de linhas. Para preservar a vizinhanca nas bordas entre faixas, o coordenador inclui linhas fantasmas acima e abaixo quando necessario. O raio maximo de influencia e definido em `SimulationRules.MAX_INFLUENCE_RADIUS`.

Fluxo:

1. O coordenador divide a matriz entre os workers.
2. Para cada worker, monta um `WorkerTask`.
3. O bloco enviado contem a faixa principal e linhas fantasmas.
4. O worker remoto executa `computeRange(...)`.
5. O worker retorna `WorkerResult` com linhas calculadas e estatisticas.
6. O coordenador espera todos os resultados.
7. O coordenador remonta `nextGrid`.
8. A proxima geracao so comeca depois que todos os workers responderam.

Vantagens:

- Permite distribuir o processamento em processos e maquinas diferentes.
- Preserva a mesma logica da versao sequencial com linhas fantasmas.
- Representa a proposta de execucao distribuida exigida pelo projeto.

Limitacoes:

- Possui custo de serializacao.
- Possui custo de chamada remota.
- Transfere blocos da matriz a cada geracao.
- Pode ser mais lenta que a sequencial quando matriz e geracoes nao compensam o custo de comunicacao.
- Se um worker remoto demorar, a geracao inteira espera por ele.

## Diferencas principais

| Modo | Unidade de execucao | Divisao da matriz | Sincronizacao | Custo extra principal |
|---|---:|---|---|---|
| Sequencial | 1 thread | Nao divide | Nao ha barreira entre workers | Nenhum custo paralelo |
| Paralela | N threads | Faixas de linhas | `join()` por geracao | Criacao e sincronizacao de threads |
| Distribuida RMI | N workers RMI | Faixas de linhas com linhas fantasmas | Espera por todos os workers | Serializacao, rede e montagem dos blocos |

## Corretude entre modos

O `BenchmarkRunner` compara a matriz final das versoes paralela e distribuida contra a matriz final sequencial. Quando a execucao termina corretamente, o campo `final_grid_match` indica se a matriz final foi igual.

Nos resultados produzidos, as execucoes concluidas registraram `sim` para a igualdade da matriz final.
