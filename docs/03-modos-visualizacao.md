# 03 - Modos de Visualizacao

A interface JavaFX possui tres modos de execucao visual: `Visual`, `Benchmark` e `Visualizacao didatica`. Eles usam a mesma simulacao, mas medem e exibem o processamento de formas diferentes.

## Visual

O modo `Visual` executa a simulacao acompanhando a animacao da matriz.

Funcionamento:

1. O usuario escolhe o modo de processamento.
2. O botao `Iniciar` ativa um `Timeline`.
3. A cada intervalo, a interface chama `nextStep()`.
4. Uma geracao e processada.
5. A matriz e redesenhada.
6. Status, contadores e grafico sao atualizados.

Objetivo:

- demonstrar a propagacao ao longo do tempo;
- permitir inspecao visual da matriz;
- acompanhar a evolucao dos estados em tempo real.

Caracteristica importante:

O tempo total no modo visual inclui a cadencia da animacao JavaFX. Por isso ele nao representa apenas o custo do algoritmo. Se cada quadro aguarda um intervalo fixo, o tempo visual tambem inclui essa espera.

## Benchmark

O modo `Benchmark` executa a simulacao sem redesenhar cada geracao.

Funcionamento:

1. O usuario escolhe o modo de processamento.
2. A interface cria uma `Task<BenchmarkOutcome>`.
3. A tarefa roda em segundo plano ate completar as geracoes.
4. A cada geracao, mede apenas o tempo de calculo.
5. Ao final, a matriz e atualizada e redesenhada.
6. A interface mostra tempo total benchmark e tempo medido do algoritmo.

Objetivo:

- medir o custo computacional com menor interferencia da renderizacao;
- comparar sequencial, paralela e distribuida dentro da interface;
- evitar que a animacao distorca a leitura de desempenho.

No painel final, a interface diferencia:

- `Tempo total benchmark`: tempo total da tarefa.
- `Tempo medido do algoritmo`: soma do tempo das geracoes processadas.

## Visualizacao didatica

O modo `Visualizacao didatica` foi criado para mostrar como a matriz e percorrida e dividida entre unidades de processamento.

Ele usa estado interno proprio para representar:

- matriz de origem da geracao didatica;
- matriz de destino;
- linha atual processada;
- quantidade de workers;
- faixas de linhas por worker;
- progresso de cada worker.

No modo didatico, a interface desenha overlays sobre a matriz. Esses overlays destacam as faixas de linhas e o avanco do processamento.

Para o modo distribuido, a interface tambem desenha uma indicacao do coordenador distribuindo blocos para workers RMI.

Objetivo:

- explicar visualmente a divisao da matriz;
- mostrar que cada worker processa uma faixa de linhas;
- tornar apresentavel a diferenca entre sequencial, paralela e distribuida;
- demonstrar a ideia de barreira por geracao.

## Comparacao entre os modos

| Modo | Atualiza a tela a cada geracao | Mede algoritmo com menor interferencia visual | Objetivo principal |
|---|---:|---:|---|
| Visual | Sim | Nao | Demonstracao da propagacao |
| Benchmark | Nao | Sim | Medicao de desempenho |
| Visualizacao didatica | Sim, com overlays | Nao | Explicacao do processamento |

## Relacao com os modos de processamento

Os modos de visualizacao sao independentes dos modos de processamento.

O usuario pode combinar:

- `Visual` com sequencial, paralela ou distribuida;
- `Benchmark` com sequencial, paralela ou distribuida;
- `Visualizacao didatica` com sequencial, paralela ou distribuida.

Essa separacao permite usar a interface tanto para apresentacao quanto para medicao.
