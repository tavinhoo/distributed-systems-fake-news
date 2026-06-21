# 05 - Resultados

Este documento usa os resultados mais recentes produzidos pelo benchmark do projeto.

Gerado automaticamente em 2026-06-21 17:36:45.

## Configuração experimental

- Coordenador: Windows 11, Ryzen 5 7600X, 32 GB RAM 6000 MHz.
- Workers distribuídos: Fedora, Intel i5-12450H, 20 GB RAM 3200 MHz, Windows 10, Ryzen 3 2200g, 16 GB RAM 2666 MHz.
- Endereços RMI usados: `[192.168.18.76:9100:worker, 192.168.18.52:9100:worker]`.
- Modo distribuído medido: RMI remoto.
- JVM: 25.0.3 - Eclipse Adoptium.
- Método: cada cenário executa primeiro a versão sequencial; speedup = tempo sequencial / tempo da versão; eficiência = speedup / unidades.
- Validação: as versões paralela e distribuída comparam a matriz final exata contra a sequencial quando a execução termina.

## Tabela comparativa

| Cenário | Modo | Status | Matriz | Gerações | Espalhadores iniciais | Unidades | Tempo (ms) | Speedup | Eficiência | Matriz final igual | Erro |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| base_80x80 | Sequencial | OK | 80x80 | 100 | 2.00% | 1 | 114.5554 | 1.0000 | 1.0000 | referencia |  |
| base_80x80 | Paralela | OK | 80x80 | 100 | 2.00% | 2 | 73.5675 | 1.5571 | 0.7786 | sim |  |
| base_80x80 | Paralela | OK | 80x80 | 100 | 2.00% | 4 | 60.9309 | 1.8801 | 0.4700 | sim |  |
| base_80x80 | Paralela | OK | 80x80 | 100 | 2.00% | 8 | 70.4160 | 1.6268 | 0.2034 | sim |  |
| base_80x80 | Distribuída | OK | 80x80 | 100 | 2.00% | 1 | 2257.4783 | 0.0507 | 0.0507 | sim |  |
| base_80x80 | Distribuída | OK | 80x80 | 100 | 2.00% | 2 | 4183.1906 | 0.0274 | 0.0137 | sim |  |
| matriz_120x120 | Sequencial | OK | 120x120 | 100 | 2.00% | 1 | 241.8502 | 1.0000 | 1.0000 | referencia |  |
| matriz_120x120 | Paralela | OK | 120x120 | 100 | 2.00% | 2 | 144.2339 | 1.6768 | 0.8384 | sim |  |
| matriz_120x120 | Paralela | OK | 120x120 | 100 | 2.00% | 4 | 98.8463 | 2.4467 | 0.6117 | sim |  |
| matriz_120x120 | Paralela | OK | 120x120 | 100 | 2.00% | 8 | 93.8000 | 2.5784 | 0.3223 | sim |  |
| matriz_120x120 | Distribuída | OK | 120x120 | 100 | 2.00% | 1 | 6989.8809 | 0.0346 | 0.0346 | sim |  |
| matriz_120x120 | Distribuída | OK | 120x120 | 100 | 2.00% | 2 | 2681.9769 | 0.0902 | 0.0451 | sim |  |
| matriz_180x180 | Sequencial | OK | 180x180 | 100 | 2.00% | 1 | 430.2283 | 1.0000 | 1.0000 | referencia |  |
| matriz_180x180 | Paralela | OK | 180x180 | 100 | 2.00% | 2 | 254.0993 | 1.6932 | 0.8466 | sim |  |
| matriz_180x180 | Paralela | OK | 180x180 | 100 | 2.00% | 4 | 192.4040 | 2.2361 | 0.5590 | sim |  |
| matriz_180x180 | Paralela | OK | 180x180 | 100 | 2.00% | 8 | 181.2767 | 2.3733 | 0.2967 | sim |  |
| matriz_180x180 | Distribuída | OK | 180x180 | 100 | 2.00% | 1 | 12921.0157 | 0.0333 | 0.0333 | sim |  |
| matriz_180x180 | Distribuída | OK | 180x180 | 100 | 2.00% | 2 | 9454.1049 | 0.0455 | 0.0228 | sim |  |
| matriz_1000x1000 | Sequencial | OK | 1000x1000 | 100 | 2.00% | 1 | 15444.8307 | 1.0000 | 1.0000 | referencia |  |
| matriz_1000x1000 | Paralela | OK | 1000x1000 | 100 | 2.00% | 2 | 8307.5807 | 1.8591 | 0.9296 | sim |  |
| matriz_1000x1000 | Paralela | OK | 1000x1000 | 100 | 2.00% | 4 | 4529.8397 | 3.4096 | 0.8524 | sim |  |
| matriz_1000x1000 | Paralela | OK | 1000x1000 | 100 | 2.00% | 8 | 2649.9970 | 5.8282 | 0.7285 | sim |  |
| matriz_1000x1000 | Distribuída | OK | 1000x1000 | 100 | 2.00% | 1 | 208072.2570 | 0.0742 | 0.0742 | sim |  |
| matriz_1000x1000 | Distribuída | OK | 1000x1000 | 100 | 2.00% | 2 | 94595.8323 | 0.1633 | 0.0816 | sim |  |
| geracoes_200 | Sequencial | OK | 80x80 | 200 | 2.00% | 1 | 150.1432 | 1.0000 | 1.0000 | referencia |  |
| geracoes_200 | Paralela | OK | 80x80 | 200 | 2.00% | 2 | 128.5494 | 1.1680 | 0.5840 | sim |  |
| geracoes_200 | Paralela | OK | 80x80 | 200 | 2.00% | 4 | 110.6583 | 1.3568 | 0.3392 | sim |  |
| geracoes_200 | Paralela | OK | 80x80 | 200 | 2.00% | 8 | 157.1883 | 0.9552 | 0.1194 | sim |  |
| geracoes_200 | Distribuída | OK | 80x80 | 200 | 2.00% | 1 | 6372.9406 | 0.0236 | 0.0236 | sim |  |
| geracoes_200 | Distribuída | OK | 80x80 | 200 | 2.00% | 2 | 4508.0906 | 0.0333 | 0.0167 | sim |  |
| espalhadores_5pct | Sequencial | OK | 80x80 | 100 | 5.00% | 1 | 86.3302 | 1.0000 | 1.0000 | referencia |  |
| espalhadores_5pct | Paralela | OK | 80x80 | 100 | 5.00% | 2 | 62.8213 | 1.3742 | 0.6871 | sim |  |
| espalhadores_5pct | Paralela | OK | 80x80 | 100 | 5.00% | 4 | 54.7731 | 1.5761 | 0.3940 | sim |  |
| espalhadores_5pct | Paralela | OK | 80x80 | 100 | 5.00% | 8 | 62.3392 | 1.3848 | 0.1731 | sim |  |
| espalhadores_5pct | Distribuída | OK | 80x80 | 100 | 5.00% | 1 | 2998.9840 | 0.0288 | 0.0288 | sim |  |
| espalhadores_5pct | Distribuída | OK | 80x80 | 100 | 5.00% | 2 | 3366.2570 | 0.0256 | 0.0128 | sim |  |

## Análise de desempenho

- Melhor resultado paralela: cenário `matriz_1000x1000`, 8 unidade(s), 2649.9970 ms, speedup 5.8282 e eficiência 0.7285.
- Melhor resultado distribuída: cenário `matriz_1000x1000`, 2 unidade(s), 94595.8323 ms, speedup 0.1633 e eficiência 0.0816.

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

- No RMI, cada geração envia uma fatia da matriz para cada worker e recebe outra fatia calculada. Esse custo cresce com matriz, gerações e quantidade de workers.
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
