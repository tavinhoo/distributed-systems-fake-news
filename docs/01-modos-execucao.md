# 01 - Modos de Execução

Este documento descreve os três modos de processamento implementados no projeto: sequencial, paralelo e distribuído com Java RMI. Todos usam o mesmo modelo lógico de simulação e chamam as regras centralizadas em `SimulationRules`.

## Base comum

A simulação trabalha com uma matriz bidimensional de `CellState`. Cada posição representa uma pessoa ou agente social. A evolução ocorre por gerações discretas.

Em cada geração:

1. A matriz atual é usada apenas para leitura.
2. O algoritmo calcula o próximo estado de cada célula.
3. O resultado é escrito em uma segunda matriz.
4. Ao final da geração, a matriz calculada passa a ser a matriz atual.

Esse desenho aparece nas três versões e evita interferência dentro da mesma geração. Uma célula atualizada no início da varredura não altera o resultado de outra célula que ainda será processada naquela mesma geração.

## Sequencial

A versão sequencial está em `sequential.SequentialSimulation`.

Ela percorre todas as linhas e colunas em uma única thread. Para cada célula, chama `SimulationRules.nextState(...)` e escreve o resultado em `nextGrid`.

Fluxo:

1. Recebe a matriz inicial e a configuração.
2. Inicializa um temporizador.
3. Para cada geração, percorre a matriz inteira.
4. Para cada célula, calcula o próximo estado.
5. Conta neutralizacoes por influencia de `GROK`.
6. Troca `currentGrid` e `nextGrid`.
7. Gera um `SimulationResult` com matriz final, tempo e contagens.

Vantagens:

- Implementação direta e previsível.
- Serve como referencia de corretude.
- Não possui custo de criação de threads nem comunicação remota.

Limitações:

- Usa apenas uma thread.
- O tempo cresce diretamente com o tamanho da matriz e o número de gerações.
- Não aproveita os núcleos disponíveis da CPU.

## Paralela

A versão paralela está em `parallel.ParallelSimulation` e usa `parallel.MatrixWorker`.

Ela divide a matriz por faixas de linhas. Cada worker recebe um intervalo `[startRow, endRow)` e calcula apenas essa parte da matriz. A divisão é feita por:

```java
int startRow = index * config.getRows() / threadCount;
int endRow = (index + 1) * config.getRows() / threadCount;
```

Como cada thread escreve em linhas exclusivas de `nextGrid`, não há escrita concorrente na mesma célula. Todas as threads leem a mesma matriz `currentGrid`, que não é modificada durante a geração.

Fluxo:

1. Define a quantidade de threads.
2. Divide as linhas em faixas aproximadamente iguais.
3. Cria um `MatrixWorker` por faixa.
4. Inicia uma thread para cada worker.
5. A thread principal aguarda todas terminarem com `join()`.
6. Soma as estatísticas parciais dos workers.
7. Troca `currentGrid` e `nextGrid`.

Vantagens:

- Usa múltiplos núcleos da máquina.
- Mantem a mesma regra logica da versao sequencial.
- Evita escrita concorrente na mesma posição da matriz.
- Pode obter speedup em matrizes maiores.

Limitações:

- Cria e sincroniza threads a cada geração.
- O custo de `join()` e a criação de threads pode reduzir o ganho em matrizes pequenas.
- A eficiencia cai quando a quantidade de threads cresce alem do ganho util do problema.

## Distribuída RMI

A versão distribuída está em `distributed.DistributedSimulation` e usa Java RMI. Os principais componentes são:

- `WorkerServer`: cria o registry RMI e registra um worker.
- `MatrixWorkerRemote`: interface remota.
- `MatrixWorkerImpl`: implementacao remota que calcula uma faixa.
- `WorkerTask`: objeto serializavel enviado ao worker.
- `WorkerResult`: objeto serializavel devolvido ao coordenador.
- `DistributedSimulation`: coordenador que divide a matriz, envia tarefas e monta a matriz final.

Cada worker recebe uma faixa de linhas. Para preservar a vizinhança nas bordas entre faixas, o coordenador inclui linhas fantasmas acima e abaixo quando necessário. O raio máximo de influência é definido em `SimulationRules.MAX_INFLUENCE_RADIUS`.

Fluxo:

1. O coordenador divide a matriz entre os workers.
2. Para cada worker, monta um `WorkerTask`.
3. O bloco enviado contem a faixa principal e linhas fantasmas.
4. O worker remoto executa `computeRange(...)`.
5. O worker retorna `WorkerResult` com linhas calculadas e estatísticas.
6. O coordenador espera todos os resultados.
7. O coordenador remonta `nextGrid`.
8. A próxima geração só começa depois que todos os workers responderam.

Vantagens:

- Permite distribuir o processamento em processos e máquinas diferentes.
- Preserva a mesma logica da versao sequencial com linhas fantasmas.
- Representa a proposta de execução distribuída exigida pelo projeto.

Limitações:

- Possui custo de serializacao.
- Possui custo de chamada remota.
- Transfere blocos da matriz a cada geração.
- Pode ser mais lenta que a sequencial quando a matriz e as gerações não compensam o custo de comunicação.
- Se um worker remoto demorar, a geração inteira espera por ele.

## Diferenças principais

| Modo | Unidade de execução | Divisão da matriz | Sincronização | Custo extra principal |
|---|---:|---|---|---|
| Sequencial | 1 thread | Não divide | Não há barreira entre workers | Nenhum custo paralelo |
| Paralela | N threads | Faixas de linhas | `join()` por geração | Criação e sincronização de threads |
| Distribuída RMI | N workers RMI | Faixas de linhas com linhas fantasmas | Espera por todos os workers | Serialização, rede e montagem dos blocos |

## Corretude entre modos

O `BenchmarkRunner` compara a matriz final das versões paralela e distribuída contra a matriz final sequencial. Quando a execução termina corretamente, o campo `final_grid_match` indica se a matriz final foi igual.

Nos resultados produzidos, as execuções concluídas registraram `sim` para a igualdade da matriz final.
