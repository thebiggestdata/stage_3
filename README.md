# TheBiggestData - Stage 3

Distributed full-text search engine for the Big Data course at Universidad de Las Palmas de Gran Canaria.

The project evolves the previous modular search engine into a fault-tolerant cluster with distributed ingestion, indexing and search. It uses Hazelcast as the in-memory data grid, ActiveMQ as the message broker, Nginx as the search load balancer and Docker Compose for deployment.

## Demonstration Video

Unlisted YouTube link:

```text
https://youtu.be/xy3evkU5xew
```

## Architecture

Each physical machine runs three services:

- `ingestion-service`: downloads books from Project Gutenberg, stores them in the datalake and publishes indexing events.
- `indexing-service`: consumes `documents.ingested` events, tokenizes books and updates the distributed inverted index.
- `search-service`: exposes the search API and reads the active Hazelcast index generation.

Shared infrastructure:

- `Hazelcast`: distributed datalake, inverted index, metadata, queues and coordination state.
- `ActiveMQ`: asynchronous communication between ingestion and indexing.
- `Nginx`: load balances `/search` and `/health` across search services.
- `Docker Compose`: starts the broker, services, load balancer and benchmark module.

The laboratory experiments use:

- `3 nodes`: 1 machine running ingestion + indexing + search.
- `6 nodes`: 2 machines running ingestion + indexing + search.
- `9 nodes`: 3 machines running ingestion + indexing + search.

## Requirements

- Java 17
- Maven 3.9+
- Docker and Docker Compose
- Git
- Apache JMeter 5.6.3 for the search load tests

## Build

From the repository root:

```bash
mvn clean package
```

To build with Docker Compose:

```bash
docker compose --profile backend --profile broker --profile loadbalancer build
```

## Local Single-Machine Run

Start ActiveMQ:

```bash
docker compose --profile broker up -d
```

Start the three backend services:

```bash
docker compose --profile backend up -d
```

Start Nginx:

```bash
docker compose --profile loadbalancer up -d
```

Useful URLs:

```text
ActiveMQ console: http://localhost:8161
Search through Nginx: http://localhost:8080/search?q=love
Search health: http://localhost:8080/health
Ingestion health: http://localhost:7001/health
Indexing health: http://localhost:7002/health
Search health direct: http://localhost:7003/health
```

Default ActiveMQ credentials:

```text
admin / admin
```

## Multi-Machine Laboratory Run

Use one machine as the master. The master runs ActiveMQ and acts as the Hazelcast seed member.

Create a `.env` file on every machine:

```env
MASTER_IP=<master-ip>
HZ_PUBLIC_ADDRESS=<this-machine-ip>
```

On the master:

```bash
docker compose --profile broker up -d
docker compose --profile backend up -d
docker compose --profile loadbalancer up -d
```

On each additional machine:

```bash
docker compose --profile backend up -d
```

If Nginx is used from the master, configure `nginx.conf` with the search service address of each active machine:

```nginx
upstream search_backend {
    least_conn;
    server <machine-1-ip>:7003 max_fails=10 fail_timeout=30s;
    server <machine-2-ip>:7003 max_fails=10 fail_timeout=30s;
    server <machine-3-ip>:7003 max_fails=10 fail_timeout=30s;
}
```

Reload Nginx after changing the configuration:

```bash
docker compose --profile loadbalancer restart nginx
```

## Main Configuration

| Variable | Default | Purpose |
|---|---:|---|
| `MASTER_IP` | service name locally | Host used for ActiveMQ and Hazelcast seed discovery |
| `HZ_PUBLIC_ADDRESS` | service name locally | Address advertised by the local Hazelcast member |
| `HAZELCAST_BACKUP_COUNT` | `2` | Synchronous backups for distributed data |
| `REPLICATION_FACTOR` | `2` | Number of disk replicas requested for each book |
| `INDEXING_BUFFER_FACTOR` | `20` | Backpressure threshold for ingestion |
| `INGESTION_WORKERS` | `4` | Scheduled ingestion workers |
| `INDEXING_CONSUMERS` | `2` | ActiveMQ consumers per indexing service |
| `INDEX_WRITERS` | `4` | Parallel writers for Hazelcast index updates |
| `ACTIVEMQ_PREFETCH` | `1` | Message prefetch per indexing consumer |
| `LAST_BOOK_ID` | `100000` | Upper book id seeded into the pending queue |

## REST API

### Ingestion Service

```text
POST /ingest/{book_id}
GET  /ingest/status/{book_id}
GET  /ingest/list
GET  /health
```

Example:

```bash
curl -X POST http://localhost:7001/ingest/1342
```

### Indexing Service

```text
POST /index/books/{book_id}
POST /index/rebuild
GET  /health
```

Example:

```bash
curl -X POST http://localhost:7002/index/rebuild
```

### Search Service

```text
GET /search?q=<query>
GET /search?q=<query>&mode=any
GET /search?q=<query>&author=<author>&language=<language>&year=<year>
GET /health
```

Example through Nginx:

```bash
curl "http://localhost:8080/search?q=love"
```

## Benchmarks

The project includes two benchmark paths:

- Internal benchmark module for ingestion, indexing and recovery metrics.
- Apache JMeter for concurrent search latency and throughput through Nginx.

### Internal Benchmark Module

Run from the repository root after the cluster is already running:

```bash
docker compose --profile benchmark run --rm benchmark
```

Select the metric with `BENCHMARK_MODE`:

```bash
BENCHMARK_MODE=ingestionrate docker compose --profile benchmark run --rm benchmark
BENCHMARK_MODE=indexingthroughput docker compose --profile benchmark run --rm benchmark
BENCHMARK_MODE=recoverytime docker compose --profile benchmark run --rm benchmark
```

On PowerShell:

```powershell
$env:BENCHMARK_MODE="ingestionrate"
docker compose --profile benchmark run --rm benchmark
```

Available modes:

- `ingestionrate`: documents downloaded per second.
- `indexingthroughput`: tokens indexed per second and indexed documents per second.
- `recoverytime`: time until the cluster becomes available and stable after a service failure.

### JMeter Search Benchmark

The JMeter benchmark sends requests to:

```text
http://localhost:8080/search
```

The workload used for the report contains five thread groups:

| Query group | Query | Users | Ramp-up | Requests |
|---|---|---:|---:|---:|
| Simple Keyword Query | `q=love` | 20 | 10 s | 20000 |
| Multiple Keywords Query | `q=love,death` | 15 | 15 s | 7500 |
| Filtered Query | `q=love&author=William Shakespeare&year=1994&language=English` | 15 | 15 s | 7500 |
| Year-Language Query | `q=love&year=1994&language=English` | 10 | 10 s | 5000 |
| Author Query | `q=beauty&author=Oscar Wilde` | 10 | 10 s | 5000 |

Total: 45,000 search requests per cluster size.

Run the same JMeter plan for 3, 6 and 9 nodes, exporting the CSV reports for each query group.

## Fault-Tolerance Test

For the recovery benchmark:

1. Start the full cluster.
2. Run:

```bash
BENCHMARK_MODE=recoverytime docker compose --profile benchmark run --rm benchmark
```

3. Wait until the benchmark prints that the cluster is available and stable.
4. Stop one service container.
5. Record the reported recovery time.

Example container stop:

```bash
docker compose stop ingestion-service
```

Restart it afterwards:

```bash
docker compose up -d ingestion-service
```

## Useful Docker Commands

Show running containers:

```bash
docker compose ps
```

View logs:

```bash
docker compose logs -f ingestion-service
docker compose logs -f indexing-service
docker compose logs -f search-service
docker compose logs -f nginx
docker compose logs -f activemq
```

Stop all project services:

```bash
docker compose --profile backend --profile broker --profile loadbalancer down
```

Remove containers and local volumes created by the compose project:

```bash
docker compose --profile backend --profile broker --profile loadbalancer down -v
```
