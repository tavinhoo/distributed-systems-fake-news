# 05 - Resultados

Este documento usa os resultados já produzidos pelo benchmark do projeto.

## Configuração experimental

- Coordenador: Windows 11, Ryzen 5 7600X, 32 GB RAM 6000 MHz.
- Worker distribuído: Fedora, Intel i5-12450H, 20 GB RAM 3200 MHz.
- Endereços RMI usados: `[127.0.0.1:9200:worker, 127.0.0.1:9201:worker, 192.168.18.76:9100:worker]`.
- Modo distribuído medido: RMI local + remoto.
- JVM: 25.0.3 - Eclipse Adoptium.
- Método: cada cenário executa primeiro a versão sequencial; speedup = tempo sequencial / tempo da versão; eficiência = speedup / unidades.
- Validação: as versões paralela e distribuída comparam a matriz final exata contra a sequencial quando a execução termina.

## Tabela comparativa

| Cenário | Modo | Status | Matriz | Gerações | Espalhadores iniciais | Unidades | Tempo (ms) | Speedup | Eficiência | Matriz final igual | Erro |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| base_80x80 | Sequencial | OK | 80x80 | 100 | 2.00% | 1 | 115.2270 | 1.0000 | 1.0000 | referencia |  |
| base_80x80 | Paralela | OK | 80x80 | 100 | 2.00% | 2 | 72.9713 | 1.5791 | 0.7895 | sim |  |
| base_80x80 | Paralela | OK | 80x80 | 100 | 2.00% | 4 | 60.0135 | 1.9200 | 0.4800 | sim |  |
| base_80x80 | Paralela | OK | 80x80 | 100 | 2.00% | 8 | 68.5138 | 1.6818 | 0.2102 | sim |  |
| base_80x80 | Distribuída | OK | 80x80 | 100 | 2.00% | 1 | 274.9376 | 0.4191 | 0.4191 | sim |  |
| base_80x80 | Distribuída | OK | 80x80 | 100 | 2.00% | 2 | 179.1209 | 0.6433 | 0.3216 | sim |  |
| base_80x80 | Distribuída | OK | 80x80 | 100 | 2.00% | 3 | 1945.8466 | 0.0592 | 0.0197 | sim |  |
| matriz_120x120 | Sequencial | OK | 120x120 | 100 | 2.00% | 1 | 203.4973 | 1.0000 | 1.0000 | referencia |  |
| matriz_120x120 | Paralela | OK | 120x120 | 100 | 2.00% | 2 | 151.9558 | 1.3392 | 0.6696 | sim |  |
| matriz_120x120 | Paralela | OK | 120x120 | 100 | 2.00% | 4 | 107.4190 | 1.8944 | 0.4736 | sim |  |
| matriz_120x120 | Paralela | OK | 120x120 | 100 | 2.00% | 8 | 128.3025 | 1.5861 | 0.1983 | sim |  |
| matriz_120x120 | Distribuída | OK | 120x120 | 100 | 2.00% | 1 | 342.5978 | 0.5940 | 0.5940 | sim |  |
| matriz_120x120 | Distribuída | OK | 120x120 | 100 | 2.00% | 2 | 228.3802 | 0.8910 | 0.4455 | sim |  |
| matriz_120x120 | Distribuída | OK | 120x120 | 100 | 2.00% | 3 | 2261.6192 | 0.0900 | 0.0300 | sim |  |
| matriz_180x180 | Sequencial | OK | 180x180 | 100 | 2.00% | 1 | 487.3202 | 1.0000 | 1.0000 | referencia |  |
| matriz_180x180 | Paralela | OK | 180x180 | 100 | 2.00% | 2 | 294.2699 | 1.6560 | 0.8280 | sim |  |
| matriz_180x180 | Paralela | OK | 180x180 | 100 | 2.00% | 4 | 258.3350 | 1.8864 | 0.4716 | sim |  |
| matriz_180x180 | Paralela | OK | 180x180 | 100 | 2.00% | 8 | 160.3651 | 3.0388 | 0.3799 | sim |  |
| matriz_180x180 | Distribuída | OK | 180x180 | 100 | 2.00% | 1 | 701.0726 | 0.6951 | 0.6951 | sim |  |
| matriz_180x180 | Distribuída | OK | 180x180 | 100 | 2.00% | 2 | 467.0216 | 1.0435 | 0.5217 | sim |  |
| matriz_180x180 | Distribuída | OK | 180x180 | 100 | 2.00% | 3 | 13886.8383 | 0.0351 | 0.0117 | sim |  |
| geracoes_200 | Sequencial | OK | 80x80 | 200 | 2.00% | 1 | 152.1517 | 1.0000 | 1.0000 | referencia |  |
| geracoes_200 | Paralela | OK | 80x80 | 200 | 2.00% | 2 | 122.1283 | 1.2458 | 0.6229 | sim |  |
| geracoes_200 | Paralela | OK | 80x80 | 200 | 2.00% | 4 | 111.4768 | 1.3649 | 0.3412 | sim |  |
| geracoes_200 | Paralela | OK | 80x80 | 200 | 2.00% | 8 | 145.4976 | 1.0457 | 0.1307 | sim |  |
| geracoes_200 | Distribuída | OK | 80x80 | 200 | 2.00% | 1 | 322.2518 | 0.4722 | 0.4722 | sim |  |
| geracoes_200 | Distribuída | OK | 80x80 | 200 | 2.00% | 2 | 230.2878 | 0.6607 | 0.3304 | sim |  |
| geracoes_200 | Distribuída | OK | 80x80 | 200 | 2.00% | 3 | 3914.8869 | 0.0389 | 0.0130 | sim |  |
| espalhadores_5pct | Sequencial | OK | 80x80 | 100 | 5.00% | 1 | 101.2597 | 1.0000 | 1.0000 | referencia |  |
| espalhadores_5pct | Paralela | OK | 80x80 | 100 | 5.00% | 2 | 79.2071 | 1.2784 | 0.6392 | sim |  |
| espalhadores_5pct | Paralela | OK | 80x80 | 100 | 5.00% | 4 | 64.7784 | 1.5632 | 0.3908 | sim |  |
| espalhadores_5pct | Paralela | OK | 80x80 | 100 | 5.00% | 8 | 78.7928 | 1.2851 | 0.1606 | sim |  |
| espalhadores_5pct | Distribuída | OK | 80x80 | 100 | 5.00% | 1 | 168.9380 | 0.5994 | 0.5994 | sim |  |
| espalhadores_5pct | Distribuída | OK | 80x80 | 100 | 5.00% | 2 | 130.6707 | 0.7749 | 0.3875 | sim |  |
| espalhadores_5pct | Distribuída | OK | 80x80 | 100 | 5.00% | 3 | 2102.7020 | 0.0482 | 0.0161 | sim |  |

## Análise de desempenho

- Melhor resultado paralela: cenário `matriz_180x180`, 8 unidade(s), 160.3651 ms, speedup 3.0388 e eficiência 0.3799.
- Melhor resultado distribuída: cenário `matriz_180x180`, 2 unidade(s), 467.0216 ms, speedup 1.0435 e eficiência 0.5217.

## Análise de gargalos

- A versão paralela cria e sincroniza threads a cada geração; em matrizes pequenas, esse custo compete com o processamento útil.
- A versão distribuída transfere blocos da matriz em todas as gerações, incluindo linhas fantasmas. Quando o tempo de rede e serialização domina, o speedup fica abaixo de 1.
- Resultados com eficiência baixa indicam que aumentar unidades não trouxe ganho proporcional para o tamanho de problema medido.

## Análise de sincronização

- A sequencial é a referência de corretude.
- A paralela usa `join()` como barreira ao fim de cada geração; nenhuma geração seguinte começa antes de todas as faixas terminarem.
- A distribuída espera todos os workers RMI devolverem seus blocos antes de montar a matriz da próxima geração.
- As execuções concluídas compararam a matriz final exata contra a sequencial; divergências aparecem na coluna `Matriz final igual`.

## Análise do custo de comunicação

- No RMI, cada geração envia uma fatia da matriz para cada worker e recebe outra fatia calculada. Esse custo cresce com a matriz, gerações e quantidade de workers.
- O uso de linhas fantasmas preserva a vizinhança entre fronteiras, mas aumenta o volume serializado.
- Com poucos workers ou matriz pequena, o custo fixo da chamada remota pode superar o ganho de processamento distribuído.

## Limitações observadas

- Nenhum teste registrou falha de execução.
- As medições foram feitas em uma única rodada por configuração; para estatística mais robusta, recomenda-se repetir cada cenário e reportar média e desvio padrão.
- O worker remoto disponível informa apenas um processo RMI quando usado sozinho; portanto, a variação distribuída fica limitada aos endereços informados ao runner.

## Arquivos relacionados

- `benchmark-results.csv`: dados brutos em CSV.
- `experimental-environment.txt`: ambiente de execução registrado.
- `RESULTADOS.md`: relatório gerado anteriormente.
- `app.BenchmarkRunner`: rotina automatizada de benchmark.
