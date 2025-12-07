# Distributed Search Engine - Stage 3

A distributed search engine implementation using microservices architecture with Hazelcast, ActiveMQ, and Nginx load balancing.

## Architecture

```
                    ┌─────────────┐
                    │   Nginx LB  │ :80
                    └──────┬──────┘
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │  Ingestion  │ │  Indexing   │ │   Search    │
    │   :7001     │ │   :7002     │ │   :7003     │
    └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
           │               │               │
           ▼               ▼               ▼
    ┌─────────────────────────────────────────────┐
    │              ActiveMQ Broker                │
    │                 :61616                      │
    └─────────────────────────────────────────────┘
           │               │               │
           ▼               ▼               ▼
    ┌─────────────────────────────────────────────┐
    │         Hazelcast Cluster (3 nodes)         │
    │   hazelcast1:5701, hazelcast2, hazelcast3   │
    │   - datalake (MultiMap)                     │
    │   - inverted-index (MultiMap)               │
    └─────────────────────────────────────────────┘
           ▲
           │
    ┌──────┴──────┐
    │   Crawler   │
    │   :7000     │
    └─────────────┘
```

## Services

### Crawler Service (Port 7000)
Crawls web content from external sources and sends it to the ingestion service via ActiveMQ.

**Endpoints:**
- `POST /crawl?url={url}&bookId={id}` - Start crawling a URL
- `GET /crawl/status` - Get crawler status
- `GET /crawl/stats` - Get crawling statistics

### Ingestion Service (Port 7001)
Receives and stores documents in the distributed datalake using Hazelcast MultiMap.

**Endpoints:**
- `POST /ingest/{book_id}` - Ingest a document
- `GET /ingest/status/{book_id}` - Get document ingestion status
- `GET /ingest/list` - List all ingested documents

### Indexing Service (Port 7002)
Consumes ingested documents from ActiveMQ, tokenizes content, and builds an inverted index in Hazelcast.

**Endpoints:**
- `POST /index/{book_id}` - Manually trigger indexing for a book
- `GET /index/status` - Get indexing service status
- `GET /index/word/{word}` - Get book IDs containing a specific word

### Search Service (Port 7003)
Provides search functionality using the inverted index.

**Endpoints:**
- `GET /search?q={query}` - Search for documents containing the query term
- `GET /search/{book_id}` - Check if a book is indexed
- `GET /search/stats` - Get search service statistics

### Nginx Load Balancer (Port 80)
Routes requests to appropriate services with health checking and failover.

**Routes:**
- `/ingest/*` → ingestion-service
- `/index/*` → indexing-service
- `/search*` → search-service
- `/health` → Load balancer health check

## Technology Stack

- **Java 17** - Primary programming language
- **Javalin 6.1.3** - REST API framework
- **Gson 2.10.1** - JSON serialization
- **Hazelcast 5.6.0** - Distributed cache and data storage
- **ActiveMQ 6.1.6** - Message broker
- **Jakarta JMS API 3.1.0** - JMS messaging
- **Nginx** - Load balancer (alpine-based)
- **Docker & Docker Compose** - Container orchestration
- **Maven** - Build tool with shade plugin for fat JARs

## Building and Running

### Prerequisites
- Docker and Docker Compose installed
- Java 17 (for local development)
- Maven (for local development)

### Build and Run with Docker Compose

```bash
# Build all services
docker-compose build

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

### Individual Service Build (Local)

```bash
# Build ingestion service
cd ingestion-service
mvn clean package
java -jar target/ingestion-service-1.0.0.jar

# Build indexing service
cd indexing-service
mvn clean package
java -jar target/indexer-service-1.0-SNAPSHOT.jar

# Build search service
cd search-service
mvn clean package
java -jar target/search-service-1.0-SNAPSHOT.jar

# Build crawler service
cd crawler-service
mvn clean package
java -jar target/crawler-service-1.0.0.jar
```

## Configuration

### Environment Variables

**Ingestion Service:**
- `BROKER_URL` - ActiveMQ broker URL (default: `tcp://activemq:61616`)
- `REPLICATION_FACTOR` - Hazelcast replication factor (default: `3`)
- `DATALAKE_PATH` - Path for datalake storage (default: `datalake`)
- `DOWNLOAD_LOG_PATH` - Path for download log (default: `downloads.log`)

**Indexing Service:**
- `BROKER_URL` - ActiveMQ broker URL (default: `tcp://activemq:61616`)
- `HZ_CLUSTER_NAME` - Hazelcast cluster name (default: `SearchEngine`)
- `QUEUE_NAME` - ActiveMQ queue name (default: `ingested.documents`)
- `DATALAKE_PATH` - Path to datalake (default: `datalake`)

**Search Service:**
- `HZ_CLUSTER_NAME` - Hazelcast cluster name (default: `SearchEngine`)

**Crawler Service:**
- `BROKER_URL` - ActiveMQ broker URL (default: `tcp://activemq:61616`)
- `INGESTION_QUEUE` - Queue for ingestion messages (default: `books.to.ingest`)

## Testing Instructions

### 1. Test Crawler Service

```bash
# Crawl a sample text file from Project Gutenberg
curl -X POST "http://localhost:7000/crawl?url=https://www.gutenberg.org/files/1342/1342-0.txt&bookId=1342"

# Check crawler status
curl http://localhost:7000/crawl/status

# Get crawler statistics
curl http://localhost:7000/crawl/stats
```

### 2. Test Ingestion Service (via Load Balancer)

```bash
# Ingest a document directly
curl -X POST http://localhost/ingest/1342

# Check ingestion status
curl http://localhost/ingest/status/1342

# List all documents
curl http://localhost/ingest/list
```

### 3. Test Indexing Service (via Load Balancer)

```bash
# Check indexing status
curl http://localhost/index/status

# Query specific word
curl http://localhost/index/word/pride

# Manually trigger indexing
curl -X POST http://localhost/index/1342
```

### 4. Test Search Service (via Load Balancer)

```bash
# Search for a term
curl "http://localhost/search?q=pride"

# Check if a book is indexed
curl http://localhost/search/1342

# Get search statistics
curl http://localhost/search/stats
```

### 5. Test Load Balancer Health

```bash
curl http://localhost/health
```

## Fault Tolerance Features

1. **Hazelcast Data Replication**
   - Configurable replication factor (default: 3)
   - Automatic data redistribution on node failure
   - Data partitioning across cluster nodes

2. **ActiveMQ Message Persistence**
   - Guaranteed message delivery
   - Message persistence across broker restarts

3. **Nginx Health Checks**
   - Automatic backend removal on failure
   - Connection timeouts and retry logic
   - Load distribution across healthy instances

4. **Docker Restart Policies**
   - Services automatically restart on failure
   - Dependency management via `depends_on`

## Development

### Project Structure

```
stage_3/
├── crawler-service/        # Web crawler microservice
├── ingestion-service/      # Document ingestion service
├── indexing-service/       # Indexing and tokenization service
├── search-service/         # Search API service
├── nginx/                  # Nginx load balancer configuration
├── docker-compose.yml      # Docker Compose orchestration
└── README.md              # This file
```

### Architecture Patterns

- **Hexagonal Architecture**: Services follow ports-and-adapters pattern
- **SOLID Principles**: Single responsibility, dependency inversion
- **Microservices**: Independent, loosely-coupled services
- **Message-Driven**: Asynchronous communication via ActiveMQ
- **Distributed Cache**: Shared state via Hazelcast

## Monitoring

- **ActiveMQ Console**: http://localhost:8161 (admin/admin)
- **Hazelcast Management**: Connect to cluster via Management Center
- **Service Logs**: `docker-compose logs -f [service-name]`

## Troubleshooting

### Services won't start
```bash
# Check if ports are available
netstat -tulpn | grep -E ':(80|7000|7001|7002|7003|5701|61616|8161)'

# Rebuild without cache
docker-compose build --no-cache

# Remove all containers and volumes
docker-compose down -v
```

### Hazelcast connection issues
- Ensure all 3 Hazelcast nodes are running
- Check cluster name matches across services
- Verify network connectivity between containers

### ActiveMQ connection issues
- Verify broker is running: `docker-compose ps activemq`
- Check broker URL is correct in service configs
- Check ActiveMQ console for queue status

## License

This project is for educational purposes.
