# 03 - Modos de Visualização

A interface JavaFX possui três modos de execução visual: `Visual`, `Benchmark` e `Visualização didática`. Eles usam a mesma simulação, mas medem e exibem o processamento de formas diferentes.

## Visual

O modo `Visual` executa a simulação acompanhando a animação da matriz.

Funcionamento:

1. O usuário escolhe o modo de processamento.
2. O botao `Iniciar` ativa um `Timeline`.
3. A cada intervalo, a interface chama `nextStep()`.
4. Uma geração é processada.
5. A matriz e redesenhada.
6. Status, contadores e gráfico são atualizados.

Objetivo:

- demonstrar a propagação ao longo do tempo;
- permitir inspeção visual da matriz;
- acompanhar a evolução dos estados em tempo real.

Característica importante:

O tempo total no modo visual inclui a cadência da animação JavaFX. Por isso ele não representa apenas o custo do algoritmo. Se cada quadro aguarda um intervalo fixo, o tempo visual também inclui essa espera.

## Benchmark

O modo `Benchmark` executa a simulação sem redesenhar cada geração.

Funcionamento:

1. O usuario escolhe o modo de processamento.
2. A interface cria uma `Task<BenchmarkOutcome>`.
3. A tarefa roda em segundo plano até completar as gerações.
4. A cada geração, mede apenas o tempo de cálculo.
5. Ao final, a matriz e atualizada e redesenhada.
6. A interface mostra tempo total benchmark e tempo medido do algoritmo.

Objetivo:

- medir o custo computacional com menor interferência da renderização;
- comparar sequencial, paralela e distribuída dentro da interface;
- evitar que a animação distorça a leitura de desempenho.

No painel final, a interface diferencia:

- `Tempo total benchmark`: tempo total da tarefa.
- `Tempo medido do algoritmo`: soma do tempo das gerações processadas.

## Visualização didática

O modo `Visualização didática` foi criado para mostrar como a matriz é percorrida e dividida entre unidades de processamento.

Ele usa estado interno proprio para representar:

- matriz de origem da geração didática;
- matriz de destino;
- linha atual processada;
- quantidade de workers;
- faixas de linhas por worker;
- progresso de cada worker.

No modo didático, a interface desenha overlays sobre a matriz. Esses overlays destacam as faixas de linhas e o avanço do processamento.

Para o modo distribuído, a interface também desenha uma indicação do coordenador distribuindo blocos para workers RMI.

Objetivo:

- explicar visualmente a divisão da matriz;
- mostrar que cada worker processa uma faixa de linhas;
- tornar apresentável a diferença entre sequencial, paralela e distribuída;
- demonstrar a ideia de barreira por geração.

## Comparação entre os modos

| Modo | Atualiza a tela a cada geração | Mede algoritmo com menor interferência visual | Objetivo principal |
|---|---:|---:|---|
| Visual | Sim | Não | Demonstração da propagação |
| Benchmark | Não | Sim | Medição de desempenho |
| Visualização didática | Sim, com overlays | Não | Explicação do processamento |

## Relação com os modos de processamento

Os modos de visualização são independentes dos modos de processamento.

O usuario pode combinar:

- `Visual` com sequencial, paralela ou distribuída;
- `Benchmark` com sequencial, paralela ou distribuída;
- `Visualização didática` com sequencial, paralela ou distribuída.

Essa separação permite usar a interface tanto para apresentação quanto para medição.
