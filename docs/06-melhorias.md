# 06 - Melhorias

Este documento consolida as melhorias e extensões implementadas no projeto, usando a lista levantada a partir de `orientacoes.md`.

## Melhorias implementadas

### Probabilidades diferentes de convencimento

Implementado.

O modelo usa uma probabilidade base de propagação configurada em `SimulationConfig`, mas essa probabilidade é alterada por agentes sociais:

- `BOT` aumenta a chance de propagação.
- `INFLUENCER` aumenta a chance com alcance maior.
- `ECHO_CHAMBER` aumenta a propagação local.
- `GROK` reduz a chance de conversão.
- `FACT_CHECKER` reduz a chance de conversão.
- `JOURNALIST` reduz a chance de conversão.

A probabilidade final é limitada por `MAX_SPREAD_PROBABILITY`.

### Influenciadores digitais

Implementado.

O estado `INFLUENCER` representa perfis de maior alcance. Ele aparece na inicialização, pode surgir durante a simulação e influencia células em raio maior.

### Bots automatizados

Implementado.

O estado `BOT` representa contas automatizadas. Bots aumentam a propagação em regiões com espalhadores e podem virar `INACTIVE` por decadência.

### Resistência à propagação

Implementado.

O projeto inclui agentes de resistência:

- `GROK`;
- `FACT_CHECKER`;
- `JOURNALIST`.

Esses agentes reduzem conversoes, neutralizam tentativas ou corrigem espalhadores dependendo da regra.

### Visualização gráfica

Implementado.

A interface `SimulationViewer` usa JavaFX para exibir:

- matriz da simulação;
- contadores por estado;
- progresso;
- gráfico de evolução;
- resultados finais;
- seleção de modos de processamento e execução.

### Estatísticas adicionais

Implementado.

O projeto mede e exibe:

- contagem final por estado;
- neutralizações por `GROK`;
- tempo total;
- speedup;
- eficiencia;
- unidades de execução;
- validacao da matriz final no benchmark;
- memória JVM na interface.

### Otimizações computacionais

Implementado.

O projeto inclui:

- processamento paralelo por faixas de linhas;
- execução distribuída por faixas de linhas;
- renderização com `Canvas`;
- agregação visual de blocos em zoom baixo;
- modo benchmark separado da animação.

### Análise probabilística

Implementado.

O modelo usa regras probabilísticas com seed fixa. A aleatoriedade é calculada por `deterministicRandom(...)`, combinando seed, geração, linha, coluna e identificador de regra. Isso permite repetir resultados com a mesma configuração.

## Melhorias parcialmente implementadas

### Redução da comunicação distribuída

Parcialmente implementado.

A versão distribuída usa faixas de linhas e envia linhas fantasmas apenas para preservar a vizinhança nas bordas. Isso evita enviar informação sem relação com o bloco processado.

No entanto, ainda há transferência de blocos da matriz a cada geração. Não há compressão, envio por delta ou cache distribuído.

## Melhorias não implementadas

### Múltiplas fake news simultâneas

Não implementado.

O modelo possui uma dinâmica principal de fake news. Os demais estados representam agentes de amplificação, resistência ou inatividade, mas não diferentes fake news concorrentes.

### Uso de grafos ou redes sociais

Não implementado.

A estrutura principal continua sendo uma matriz bidimensional com vizinhança espacial. O projeto não usa grafo explícito de usuários ou arestas sociais.

### Balanceamento dinamico de carga

Não implementado.

A divisão de trabalho é estática por faixas de linhas. A quantidade de linhas por thread ou worker é definida antes do processamento da geração.

## Lista consolidada

| Item das orientacoes | Situacao no projeto |
|---|---|
| Probabilidades diferentes de convencimento | Implementado |
| Influenciadores digitais | Implementado |
| Bots automatizados | Implementado |
| Múltiplas fake news simultâneas | Não implementado |
| Resistência à propagação | Implementado |
| Uso de grafos/redes sociais | Não implementado |
| Balanceamento dinâmico de carga | Não implementado |
| Visualização gráfica | Implementado |
| Estatísticas adicionais | Implementado |
| Otimizações computacionais | Implementado |
| Redução da comunicação distribuída | Parcialmente implementado |
| Análise probabilística | Implementado |

## Justificativa técnica

As melhorias implementadas deixam o modelo mais rico que uma propagação binária simples. O projeto diferencia agentes que amplificam a fake news e agentes que reduzem sua propagação. Também separa a parte de processamento da parte visual, permitindo demonstrar a simulação e medir desempenho sem misturar os custos da animação.

A implementação paralela e distribuída permite discutir ganhos e custos de coordenação. Os resultados mostram que paralelizar ou distribuir não garante ganho automático: o tamanho da matriz, o número de gerações, a quantidade de unidades e o custo de comunicação influenciam diretamente o desempenho.
