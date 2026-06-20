# Guia de Execucao

Este guia mostra como compilar e executar todas as partes do projeto em Windows e Linux.

## Pre-requisitos

Instale:

- JDK 17 ou superior;
- Maven;
- ambiente grafico ativo, caso queira executar a interface JavaFX no Linux.

Verifique:

```bash
java -version
mvn -version
```

## 1. Compilar o projeto

Windows PowerShell ou Linux:

```bash
mvn compile
```

As classes compiladas ficam em:

```text
target/classes
```

## 2. Executar a versao sequencial

```bash
java -cp target/classes app.Main sequential
```

## 3. Executar a versao paralela

Exemplo com 4 threads:

```bash
java -cp target/classes app.Main parallel 4
```

## 4. Executar a versao distribuida RMI manualmente

Abra um terminal para cada worker.

Worker 1:

```bash
java -cp target/classes distributed.WorkerServer 9100 worker
```

Worker 2:

```bash
java -cp target/classes distributed.WorkerServer 9101 worker
```

Em outro terminal, rode o coordenador:

```bash
java -cp target/classes app.Main distributed 127.0.0.1:9100:worker 127.0.0.1:9101:worker
```

Em maquinas diferentes, troque `127.0.0.1` pelo IP da maquina onde cada worker esta rodando. Verifique firewall e portas.

## 5. Executar benchmark rapido

Roda sequencial, paralela e distribuida com workers RMI locais automaticos:

```bash
java -cp target/classes app.BenchmarkRunner 4 2 9100
```

Argumentos:

```text
4    numero de threads
2    numero de workers RMI
9100 porta inicial dos workers
```

Gera:

```text
benchmark-results.csv
experimental-environment.txt
```

## 6. Executar benchmark em lote

Este e o modo mais importante para o trabalho academico, pois varia matriz, geracoes, espalhadores, threads e workers:

```bash
java -cp target/classes app.BenchmarkRunner batch 9100
```

Gera:

```text
benchmark-results.csv
experimental-environment.txt
```

O CSV contem:

- cenario;
- versao;
- tamanho da matriz;
- numero de geracoes;
- percentual inicial de espalhadores;
- unidades de execucao;
- tempo;
- speedup;
- eficiencia;
- contagens finais.

## 7. Executar interface grafica JavaFX

Use Maven:

```bash
mvn javafx:run
```

A interface permite visualizar:

- sequencial;
- paralela;
- distribuida RMI.

A interface e para demonstracao visual. Para resultados experimentais, use o `BenchmarkRunner`.

## 8. Execucao via IntelliJ

Abra o projeto como Maven.

Classes principais:

- `app.Main`;
- `app.BenchmarkRunner`;
- `app.SimulationViewer`;
- `distributed.WorkerServer`.

Para a interface grafica, tambem pode rodar pelo Maven tool window:

```text
Plugins > javafx > javafx:run
```

## Arquivos gerados

Estes arquivos sao gerados durante a execucao e nao precisam ser commitados:

```text
benchmark-results.csv
experimental-environment.txt
target/
out/
```

## Fluxo recomendado para apresentacao

1. Compilar:

```bash
mvn compile
```

2. Rodar sequencial:

```bash
java -cp target/classes app.Main sequential
```

3. Rodar paralela:

```bash
java -cp target/classes app.Main parallel 4
```

4. Rodar distribuida com dois workers RMI.

5. Gerar resultados:

```bash
java -cp target/classes app.BenchmarkRunner batch 9100
```

6. Abrir a visualizacao:

```bash
mvn javafx:run
```
