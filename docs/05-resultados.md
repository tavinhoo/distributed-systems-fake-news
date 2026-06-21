# 05 - Resultados

Este documento usa os resultados ja produzidos pelo benchmark do projeto.

## Configuracao experimental

- Coordenador: Windows 11, Ryzen 5 7600X, 32 GB RAM 6000 MHz.
- Worker distribuido: Fedora, Intel i5-12450H, 20 GB RAM 3200 MHz.
- Enderecos RMI usados: `[127.0.0.1:9200:worker, 127.0.0.1:9201:worker, 192.168.18.76:9100:worker]`.
- Modo distribuido medido: RMI local + remoto.
- JVM: 25.0.3 - Eclipse Adoptium.
- Metodo: cada cenario executa primeiro a versao sequencial; speedup = tempo sequencial / tempo da versao; eficiencia = speedup / unidades.
- Validacao: as versoes paralela e distribuida comparam a matriz final exata contra a sequencial quando a execucao termina.

## Tabela comparativa

| Cenario | Modo | Status | Matriz | Geracoes | Espalhadores iniciais | Unidades | Tempo (ms) | Speedup | Eficiencia | Matriz final igual | Erro |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| base_80x80 | Sequencial | OK | 80x80 | 100 | 2.00% | 1 | 115.2270 | 1.0000 | 1.0000 | referencia |  |
| base_80x80 | Paralela | OK | 80x80 | 100 | 2.00% | 2 | 72.9713 | 1.5791 | 0.7895 | sim |  |
| base_80x80 | Paralela | OK | 80x80 | 100 | 2.00% | 4 | 60.0135 | 1.9200 | 0.4800 | sim |  |
| base_80x80 | Paralela | OK | 80x80 | 100 | 2.00% | 8 | 68.5138 | 1.6818 | 0.2102 | sim |  |
| base_80x80 | Distribuida | OK | 80x80 | 100 | 2.00% | 1 | 274.9376 | 0.4191 | 0.4191 | sim |  |
| base_80x80 | Distribuida | OK | 80x80 | 100 | 2.00% | 2 | 179.1209 | 0.6433 | 0.3216 | sim |  |
| base_80x80 | Distribuida | OK | 80x80 | 100 | 2.00% | 3 | 1945.8466 | 0.0592 | 0.0197 | sim |  |
| matriz_120x120 | Sequencial | OK | 120x120 | 100 | 2.00% | 1 | 203.4973 | 1.0000 | 1.0000 | referencia |  |
| matriz_120x120 | Paralela | OK | 120x120 | 100 | 2.00% | 2 | 151.9558 | 1.3392 | 0.6696 | sim |  |
| matriz_120x120 | Paralela | OK | 120x120 | 100 | 2.00% | 4 | 107.4190 | 1.8944 | 0.4736 | sim |  |
| matriz_120x120 | Paralela | OK | 120x120 | 100 | 2.00% | 8 | 128.3025 | 1.5861 | 0.1983 | sim |  |
| matriz_120x120 | Distribuida | OK | 120x120 | 100 | 2.00% | 1 | 342.5978 | 0.5940 | 0.5940 | sim |  |
| matriz_120x120 | Distribuida | OK | 120x120 | 100 | 2.00% | 2 | 228.3802 | 0.8910 | 0.4455 | sim |  |
| matriz_120x120 | Distribuida | OK | 120x120 | 100 | 2.00% | 3 | 2261.6192 | 0.0900 | 0.0300 | sim |  |
| matriz_180x180 | Sequencial | OK | 180x180 | 100 | 2.00% | 1 | 487.3202 | 1.0000 | 1.0000 | referencia |  |
| matriz_180x180 | Paralela | OK | 180x180 | 100 | 2.00% | 2 | 294.2699 | 1.6560 | 0.8280 | sim |  |
| matriz_180x180 | Paralela | OK | 180x180 | 100 | 2.00% | 4 | 258.3350 | 1.8864 | 0.4716 | sim |  |
| matriz_180x180 | Paralela | OK | 180x180 | 100 | 2.00% | 8 | 160.3651 | 3.0388 | 0.3799 | sim |  |
| matriz_180x180 | Distribuida | OK | 180x180 | 100 | 2.00% | 1 | 701.0726 | 0.6951 | 0.6951 | sim |  |
| matriz_180x180 | Distribuida | OK | 180x180 | 100 | 2.00% | 2 | 467.0216 | 1.0435 | 0.5217 | sim |  |
| matriz_180x180 | Distribuida | OK | 180x180 | 100 | 2.00% | 3 | 13886.8383 | 0.0351 | 0.0117 | sim |  |
| geracoes_200 | Sequencial | OK | 80x80 | 200 | 2.00% | 1 | 152.1517 | 1.0000 | 1.0000 | referencia |  |
| geracoes_200 | Paralela | OK | 80x80 | 200 | 2.00% | 2 | 122.1283 | 1.2458 | 0.6229 | sim |  |
| geracoes_200 | Paralela | OK | 80x80 | 200 | 2.00% | 4 | 111.4768 | 1.3649 | 0.3412 | sim |  |
| geracoes_200 | Paralela | OK | 80x80 | 200 | 2.00% | 8 | 145.4976 | 1.0457 | 0.1307 | sim |  |
| geracoes_200 | Distribuida | OK | 80x80 | 200 | 2.00% | 1 | 322.2518 | 0.4722 | 0.4722 | sim |  |
| geracoes_200 | Distribuida | OK | 80x80 | 200 | 2.00% | 2 | 230.2878 | 0.6607 | 0.3304 | sim |  |
| geracoes_200 | Distribuida | OK | 80x80 | 200 | 2.00% | 3 | 3914.8869 | 0.0389 | 0.0130 | sim |  |
| espalhadores_5pct | Sequencial | OK | 80x80 | 100 | 5.00% | 1 | 101.2597 | 1.0000 | 1.0000 | referencia |  |
| espalhadores_5pct | Paralela | OK | 80x80 | 100 | 5.00% | 2 | 79.2071 | 1.2784 | 0.6392 | sim |  |
| espalhadores_5pct | Paralela | OK | 80x80 | 100 | 5.00% | 4 | 64.7784 | 1.5632 | 0.3908 | sim |  |
| espalhadores_5pct | Paralela | OK | 80x80 | 100 | 5.00% | 8 | 78.7928 | 1.2851 | 0.1606 | sim |  |
| espalhadores_5pct | Distribuida | OK | 80x80 | 100 | 5.00% | 1 | 168.9380 | 0.5994 | 0.5994 | sim |  |
| espalhadores_5pct | Distribuida | OK | 80x80 | 100 | 5.00% | 2 | 130.6707 | 0.7749 | 0.3875 | sim |  |
| espalhadores_5pct | Distribuida | OK | 80x80 | 100 | 5.00% | 3 | 2102.7020 | 0.0482 | 0.0161 | sim |  |

## Analise de desempenho

- Melhor resultado paralela: cenario `matriz_180x180`, 8 unidade(s), 160.3651 ms, speedup 3.0388 e eficiencia 0.3799.
- Melhor resultado distribuida: cenario `matriz_180x180`, 2 unidade(s), 467.0216 ms, speedup 1.0435 e eficiencia 0.5217.

## Analise de gargalos

- A versao paralela cria e sincroniza threads a cada geracao; em matrizes pequenas, esse custo compete com o processamento util.
- A versao distribuida transfere blocos da matriz em todas as geracoes, incluindo linhas fantasmas. Quando o tempo de rede e serializacao domina, o speedup fica abaixo de 1.
- Resultados com eficiencia baixa indicam que aumentar unidades nao trouxe ganho proporcional para o tamanho de problema medido.

## Analise de sincronizacao

- A sequencial e a referencia de corretude.
- A paralela usa `join()` como barreira ao fim de cada geracao; nenhuma geracao seguinte comeca antes de todas as faixas terminarem.
- A distribuida espera todos os workers RMI devolverem seus blocos antes de montar a matriz da proxima geracao.
- As execucoes concluidas compararam a matriz final exata contra a sequencial; divergencias aparecem na coluna `Matriz final igual`.

## Analise do custo de comunicacao

- No RMI, cada geracao envia uma fatia da matriz para cada worker e recebe outra fatia calculada. Esse custo cresce com matriz, geracoes e quantidade de workers.
- O uso de linhas fantasmas preserva a vizinhanca entre fronteiras, mas aumenta o volume serializado.
- Com poucos workers ou matriz pequena, o custo fixo da chamada remota pode superar o ganho de processamento distribuido.

## Limitacoes observadas

- Nenhum teste registrou falha de execucao.
- As medicoes foram feitas em uma unica rodada por configuracao; para estatistica mais robusta, recomenda-se repetir cada cenario e reportar media e desvio padrao.
- O worker remoto disponivel informa apenas um processo RMI quando usado sozinho; portanto, a variacao distribuida fica limitada aos enderecos informados ao runner.

## Arquivos relacionados

- `benchmark-results.csv`: dados brutos em CSV.
- `experimental-environment.txt`: ambiente de execucao registrado.
- `RESULTADOS.md`: relatorio gerado anteriormente.
- `app.BenchmarkRunner`: rotina automatizada de benchmark.
