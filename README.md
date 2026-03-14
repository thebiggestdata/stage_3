# Distributed Search Engine – Stage 3

A 3-stage distributed search engine built with **Java 17**, **Maven** (multi-module), **Javalin** (REST), and **Hazelcast** (in-memory grid).

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Hazelcast Cluster                    │
│        (auto-discovery via multicast 224.2.2.3)        │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Ingestion   │  │   Indexing   │  │    Search    │  │
│  │  Service     │  │   Service    │  │   Service    │  │
│  │  :7001       │  │   :7002      │  │   :7003      │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │  book-downloaded │  MultiMap query │          │
│         │  (HZ Topic)      │                 │          │
│         └──────────────────┘                 │          │
│                    inverted-index (MultiMap) ─┘          │
└─────────────────────────────────────────────────────────┘
                          ▲
               ┌──────────┴──────────┐
               │   Control Module    │
               │       :7000         │
               └─────────────────────┘
```

## Modules

| Module             | Port | Description |
|--------------------|------|-------------|
| `shared`           | –    | Common classes: `HazelcastFactory`, `Constants`, DTOs |
| `ingestion-service`| 7001 | Downloads books from Project Gutenberg, stores locally, replicates |
| `indexing-service` | 7002 | Listens on HZ topic, builds distributed inverted index |
| `search-service`   | 7003 | Queries distributed index, returns ranked results |
| `control-module`   | 7000 | Orchestrates the workflow; exposes high-level REST API |

## Datalake Structure

```
datalake/
└── YYYYMMDD/
    └── HH/
        ├── <bookId>.header.txt
        └── <bookId>.body.txt
```

## Build

```bash
mvn clean package -DskipTests
```

Produces fat JARs in each module's `target/` directory.

## Running Locally (single machine)

Start each service in a separate terminal:

```bash
# Terminal 1 – Control Module
java -jar control-module/target/control-module-1.0-SNAPSHOT.jar

# Terminal 2 – Ingestion Service
java -jar ingestion-service/target/ingestion-service-1.0-SNAPSHOT.jar

# Terminal 3 – Indexing Service
java -jar indexing-service/target/indexing-service-1.0-SNAPSHOT.jar

# Terminal 4 – Search Service
java -jar search-service/target/search-service-1.0-SNAPSHOT.jar
```

All four processes join the same Hazelcast cluster automatically via multicast.

## Running in a University Lab (multiple machines)

Run any subset of the services on each machine. They will discover each other via multicast on the same LAN.

```bash
# On Machine A
java -jar ingestion-service/target/ingestion-service-1.0-SNAPSHOT.jar 7001

# On Machine B
java -jar indexing-service/target/indexing-service-1.0-SNAPSHOT.jar 7002
java -jar search-service/target/search-service-1.0-SNAPSHOT.jar 7003
```

## API Reference

### Control Module (`:7000`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/cluster/status` | Members + indexed term count |
| POST | `/api/workflow/ingest/{bookId}?host=&port=` | Trigger ingestion on a node |
| POST | `/api/workflow/ingest/{bookId}/replicated?host=&port=&replicaHost=&replicaPort=` | Ingest + replicate |

### Ingestion Service (`:7001`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/health` | Health check |
| POST | `/api/books/{id}` | Download a book by Gutenberg ID |
| POST | `/api/books/{id}/replicate?host=&port=` | Download + send copy to peer |
| POST | `/api/replicate` | Receive a replicated file (peer-to-peer) |

### Indexing Service (`:7002`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/health` | Health check |
| POST | `/api/index?path=` | Manually trigger indexing of a body file |

### Search Service (`:7003`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/search?q=term1+term2` | Search (space-separated terms, ranked results) |
| GET | `/api/index/size` | Number of indexed terms |

## Example Workflow

```bash
# 1. Download Pride and Prejudice (book 1342)
curl -X POST http://localhost:7001/api/books/1342

# 2. Search for a term
curl "http://localhost:7003/api/search?q=darcy+elizabeth"

# 3. Check cluster health
curl http://localhost:7000/api/cluster/status
```

## Fault Tolerance

- The Hazelcast inverted index is stored **in-memory across all cluster members** — if one node goes down, the data is still accessible from the remaining members.
- File replication (R=2) ensures each book's files exist on at least two nodes' local storage.
- Any node can accept search queries and will get the same results from the distributed MultiMap.
