# 04 - Agentes

Este documento descreve os estados e agentes do modelo, com base em `model.CellState`, `core.GridFactory`, `core.SimulationRules`, `core.Statistics` e `model.SimulationConfig`.

## Estados básicos

### `IGNORANT`

Representa uma pessoa que ainda não recebeu ou não acredita na fake news.

Pode virar `SPREADER` quando há influência suficiente na vizinhança. A probabilidade base vem de `SimulationConfig.getSpreadProbability()`.

Também pode virar um agente de amplificação quando existe um vizinho `SPREADER` e as probabilidades específicas de criação são satisfeitas.

### `SPREADER`

Representa uma pessoa que acredita e compartilha a fake news.

Pode:

- continuar como `SPREADER`;
- virar `INACTIVE` pela probabilidade de inatividade;
- virar `GROK` em caso raro de reabilitacao por influencia de `GROK`;
- virar `INACTIVE` por correção de `JOURNALIST`;
- virar `INACTIVE` por correção de `FACT_CHECKER`.

Quando esta perto de `ECHO_CHAMBER`, sua probabilidade de virar `INACTIVE` e reduzida.

### `INACTIVE`

Representa uma pessoa que recebeu a informação, mas não compartilha mais.

No código atual, `INACTIVE` permanece `INACTIVE`.

## Agentes de amplificação

### `BOT`

Representa uma conta automatizada.

Efeitos:

- aumenta a probabilidade de propagação com `BOT_SPREAD_BONUS`;
- influencia em raio curto quando há também `SPREADER` em raio maior;
- pode surgir em regiões com espalhadores;
- pode virar `INACTIVE` por decadência.

Parametros no codigo:

- `BOT_SPREAD_BONUS = 0.06`;
- `BOT_DECAY_PROBABILITY = 0.014`;
- `BOT_CREATION_PROBABILITY = 0.010`.

### `INFLUENCER`

Representa um perfil de grande alcance.

Efeitos:

- aumenta a probabilidade de propagação com bônus maior que o bot;
- influencia em raio maior;
- pode surgir em regiões com espalhadores;
- pode perder relevância e virar `INACTIVE`.

Parametros no codigo:

- `INFLUENCER_SPREAD_BONUS = 0.12`;
- `INFLUENCER_DECAY_PROBABILITY = 0.006`;
- `INFLUENCER_CREATION_PROBABILITY = 0.004`.

### `ECHO_CHAMBER`

Representa uma bolha social que reforça a fake news localmente.

Efeitos:

- aumenta a probabilidade de propagação com `ECHO_CHAMBER_SPREAD_BONUS`;
- reduz a chance de um `SPREADER` virar `INACTIVE`;
- pode surgir em regiões com espalhadores;
- pode virar `INACTIVE` por decadência.

Parametros no codigo:

- `ECHO_CHAMBER_SPREAD_BONUS = 0.04`;
- `ECHO_CHAMBER_DECAY_PROBABILITY = 0.003`;
- `ECHO_CHAMBER_CREATION_PROBABILITY = 0.003`.

## Agentes de resistência e correção

### `GROK`

Representa uma IA verificadora.

Efeitos:

- reduz a probabilidade de propagação de células próximas pelo fator configurado em `SimulationConfig`;
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

- reduz a probabilidade de propagação local;
- pode neutralizar tentativa de convencimento;
- pode corrigir um `SPREADER`, transformando-o em `INACTIVE`;
- pode perder atuação e virar `INACTIVE`.

Parametros no codigo:

- `FACT_CHECKER_SPREAD_REDUCTION_FACTOR = 0.78`;
- `FACT_CHECKER_DECAY_PROBABILITY = 0.006`;
- `FACT_CHECKER_NEUTRALIZATION_PROBABILITY = 0.08`;
- `FACT_CHECKER_CORRECTION_PROBABILITY = 0.0025`.

### `JOURNALIST`

Representa um agente jornalístico local.

Efeitos:

- reduz a probabilidade de propagação local;
- pode corrigir um `SPREADER`, transformando-o em `INACTIVE`;
- pode perder atuação e virar `INACTIVE`.

Parametros no codigo:

- `JOURNALIST_SPREAD_REDUCTION_FACTOR = 0.72`;
- `JOURNALIST_DECAY_PROBABILITY = 0.004`;
- `JOURNALIST_CORRECTION_PROBABILITY = 0.004`.

## Cálculo da propagação

A probabilidade de propagação é calculada em `SimulationRules.spreadProbability(...)`.

Ela parte da probabilidade base de `SimulationConfig` e é ajustada por:

- bonus de `BOT`;
- bonus de `INFLUENCER`;
- bonus de `ECHO_CHAMBER`;
- redução por `GROK`;
- redução por `FACT_CHECKER`;
- redução por `JOURNALIST`.

A probabilidade final é limitada por `MAX_SPREAD_PROBABILITY = 0.27`.

## Aleatoriedade determinística

As regras usam `deterministicRandom(...)`, que combina:

- seed;
- geração;
- linha;
- coluna;
- identificador da regra.

Isso permite repetir os mesmos resultados quando a configuração e a matriz inicial são iguais. Também ajuda a preservar o comportamento entre execuções sequenciais, paralelas e distribuídas.

## Inicialização

`GridFactory.createInitialGrid(config)` cria a matriz inicial com base nas porcentagens de `SimulationConfig`:

- espalhadores iniciais;
- `GROK`;
- bots;
- influenciadores;
- bolhas;
- checadores;
- jornalistas.

As demais células ficam como `IGNORANT`.

## Estatísticas

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
- neutralizações por `GROK`.
