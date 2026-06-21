# 04 - Agentes

Este documento descreve os estados e agentes do modelo, com base em `model.CellState`, `core.GridFactory`, `core.SimulationRules`, `core.Statistics` e `model.SimulationConfig`.

## Estados basicos

### `IGNORANT`

Representa uma pessoa que ainda nao recebeu ou nao acredita na fake news.

Pode virar `SPREADER` quando ha influencia suficiente na vizinhanca. A probabilidade base vem de `SimulationConfig.getSpreadProbability()`.

Tambem pode virar um agente de amplificacao quando existe um vizinho `SPREADER` e as probabilidades especificas de criacao sao satisfeitas.

### `SPREADER`

Representa uma pessoa que acredita e compartilha a fake news.

Pode:

- continuar como `SPREADER`;
- virar `INACTIVE` pela probabilidade de inatividade;
- virar `GROK` em caso raro de reabilitacao por influencia de `GROK`;
- virar `INACTIVE` por correcao de `JOURNALIST`;
- virar `INACTIVE` por correcao de `FACT_CHECKER`.

Quando esta perto de `ECHO_CHAMBER`, sua probabilidade de virar `INACTIVE` e reduzida.

### `INACTIVE`

Representa uma pessoa que recebeu a informacao, mas nao compartilha mais.

No codigo atual, `INACTIVE` permanece `INACTIVE`.

## Agentes de amplificacao

### `BOT`

Representa uma conta automatizada.

Efeitos:

- aumenta a probabilidade de propagacao com `BOT_SPREAD_BONUS`;
- influencia em raio curto quando ha tambem `SPREADER` em raio maior;
- pode surgir em regioes com espalhadores;
- pode virar `INACTIVE` por decadencia.

Parametros no codigo:

- `BOT_SPREAD_BONUS = 0.06`;
- `BOT_DECAY_PROBABILITY = 0.014`;
- `BOT_CREATION_PROBABILITY = 0.010`.

### `INFLUENCER`

Representa um perfil de grande alcance.

Efeitos:

- aumenta a probabilidade de propagacao com bonus maior que o bot;
- influencia em raio maior;
- pode surgir em regioes com espalhadores;
- pode perder relevancia e virar `INACTIVE`.

Parametros no codigo:

- `INFLUENCER_SPREAD_BONUS = 0.12`;
- `INFLUENCER_DECAY_PROBABILITY = 0.006`;
- `INFLUENCER_CREATION_PROBABILITY = 0.004`.

### `ECHO_CHAMBER`

Representa uma bolha social que reforca a fake news localmente.

Efeitos:

- aumenta a probabilidade de propagacao com `ECHO_CHAMBER_SPREAD_BONUS`;
- reduz a chance de um `SPREADER` virar `INACTIVE`;
- pode surgir em regioes com espalhadores;
- pode virar `INACTIVE` por decadencia.

Parametros no codigo:

- `ECHO_CHAMBER_SPREAD_BONUS = 0.04`;
- `ECHO_CHAMBER_DECAY_PROBABILITY = 0.003`;
- `ECHO_CHAMBER_CREATION_PROBABILITY = 0.003`.

## Agentes de resistencia e correcao

### `GROK`

Representa uma IA verificadora.

Efeitos:

- reduz a probabilidade de propagacao de celulas proximas pelo fator configurado em `SimulationConfig`;
- pode neutralizar tentativa de convencimento de `IGNORANT`;
- pode reabilitar um `SPREADER` para `GROK` com probabilidade baixa;
- pode ser corrompido e virar `SPREADER` quando exposto a espalhadores.

Parametros no codigo:

- `GROK_CORRUPTION_PROBABILITY = 0.001`;
- `GROK_REHABILITATION_PROBABILITY = 0.0004`;
- `grokCorrectionProbability`, vindo de `SimulationConfig`;
- `grokInfluenceReductionFactor`, vindo de `SimulationConfig`.

### `FACT_CHECKER`

Representa checadores de fatos.

Efeitos:

- reduz a probabilidade de propagacao local;
- pode neutralizar tentativa de convencimento;
- pode corrigir um `SPREADER`, transformando-o em `INACTIVE`;
- pode perder atuacao e virar `INACTIVE`.

Parametros no codigo:

- `FACT_CHECKER_SPREAD_REDUCTION_FACTOR = 0.78`;
- `FACT_CHECKER_DECAY_PROBABILITY = 0.006`;
- `FACT_CHECKER_NEUTRALIZATION_PROBABILITY = 0.08`;
- `FACT_CHECKER_CORRECTION_PROBABILITY = 0.0025`.

### `JOURNALIST`

Representa um agente jornalistico local.

Efeitos:

- reduz a probabilidade de propagacao local;
- pode corrigir um `SPREADER`, transformando-o em `INACTIVE`;
- pode perder atuacao e virar `INACTIVE`.

Parametros no codigo:

- `JOURNALIST_SPREAD_REDUCTION_FACTOR = 0.72`;
- `JOURNALIST_DECAY_PROBABILITY = 0.004`;
- `JOURNALIST_CORRECTION_PROBABILITY = 0.004`.

## Calculo da propagacao

A probabilidade de propagacao e calculada em `SimulationRules.spreadProbability(...)`.

Ela parte da probabilidade base de `SimulationConfig` e e ajustada por:

- bonus de `BOT`;
- bonus de `INFLUENCER`;
- bonus de `ECHO_CHAMBER`;
- reducao por `GROK`;
- reducao por `FACT_CHECKER`;
- reducao por `JOURNALIST`.

A probabilidade final e limitada por `MAX_SPREAD_PROBABILITY = 0.27`.

## Aleatoriedade deterministica

As regras usam `deterministicRandom(...)`, que combina:

- seed;
- geracao;
- linha;
- coluna;
- identificador da regra.

Isso permite repetir os mesmos resultados quando a configuracao e a matriz inicial sao iguais. Tambem ajuda a preservar o comportamento entre execucoes sequenciais, paralelas e distribuidas.

## Inicializacao

`GridFactory.createInitialGrid(config)` cria a matriz inicial com base nas porcentagens de `SimulationConfig`:

- espalhadores iniciais;
- `GROK`;
- bots;
- influenciadores;
- bolhas;
- checadores;
- jornalistas.

As demais celulas ficam como `IGNORANT`.

## Estatisticas

`Statistics.buildResult(...)` percorre a matriz final e conta:

- ignorantes;
- espalhadores;
- inativos;
- `GROK`;
- bots;
- influenciadores;
- bolhas;
- checadores;
- jornalistas;
- neutralizacoes por `GROK`.
