# Internal Benchmark Module Raw Results

These are the final console summaries used in the report.

## 3 Nodes - IngestionRate

```text
IngestionRate: 0.040 +/- 0.120 docs/s
(min=0.000, avg=0.040, max=0.400)
```

## 3 Nodes - IndexingThroughput

```text
IndexingThroughput: 5401.543 +/- 8979.735 tokens/s
(min=0.000, avg=5401.543, max=26587.093)

IndexedDocuments: 0.050 +/- 0.067 docs/s
(min=0.000, avg=0.050, max=0.199)
```

## 6 Nodes - IngestionRate

```text
IngestionRate: 0.070 +/- 0.142 docs/s
(min=0.000, avg=0.070, max=0.400)
```

## 6 Nodes - IndexingThroughput

```text
IndexingThroughput: 2487.841 +/- 4459.476 tokens/s
(min=0.000, avg=2487.841, max=15214.476)

IndexedDocuments: 0.070 +/- 0.078 docs/s
(min=0.000, avg=0.070, max=0.199)
```

## 9 Nodes - IngestionRate

```text
IngestionRate: 0.120 +/- 0.359 docs/s
(min=0.000, avg=0.120, max=1.198)
```

## 9 Nodes - IndexingThroughput

```text
IndexingThroughput: 4891.949 +/- 8876.548 tokens/s
(min=0.000, avg=4891.949, max=30723.828)

IndexedDocuments: 0.109 +/- 0.069 docs/s
(min=0.000, avg=0.109, max=0.198)
```

## 9 Nodes - RecoveryTime

```text
RECOVERY TIME: 12.712 s (12712 ms)
```

The later `0.000 s` line observed in the console was treated as a duplicate monitor event after the benchmark updated its baseline member count.
