# Stage 3 - Distributed Search Engine

A distributed search engine system designed to run across multiple physical computers using Hazelcast for distributed caching, ActiveMQ for message brokering, and Docker for containerization.

## Architecture Overview

This system implements a multi-PC distributed architecture where services are deployed across 3 physical nodes:

- **PC1 (Master Node)**: Runs ActiveMQ broker, Hazelcast node, all services, and Nginx load balancer
- **PC2 (Worker Node)**: Runs Hazelcast node and all services
- **PC3 (Worker Node)**: Runs Hazelcast node and all services

```
┌──────────────────────────────────────────────────────────────┐
│                         PC1 (Master)                          │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────────┐ │
│  │  ActiveMQ   │  │ Hazelcast-1 │  │  Services (1,1,1)    │ │
│  │  :61616     │  │   :5701     │  │  7001, 7002, 7003    │ │
│  └─────────────┘  └─────────────┘  └──────────────────────┘ │
│  ┌─────────────┐                                             │
│  │    Nginx    │  Load Balancer                              │
│  │     :80     │                                             │
│  └─────────────┘                                             │
└───────────────────────┬──────────────────────────────────────┘
                        │ Network
    ┌───────────────────┴───────────────────┐
    │                                        │
┌───▼──────────────┐                  ┌─────▼─────────────┐
│   PC2 (Worker)   │                  │   PC3 (Worker)    │
│ ┌──────────────┐ │                  │ ┌──────────────┐  │
│ │ Hazelcast-2  │ │                  │ │ Hazelcast-3  │  │
│ │   :5701      │ │                  │ │   :5701      │  │
│ └──────────────┘ │                  │ └──────────────┘  │
│ ┌──────────────┐ │                  │ ┌──────────────┐  │
│ │  Services    │ │                  │ │  Services    │  │
│ │  (2,2,2)     │ │                  │ │  (3,3,3)     │  │
│ └──────────────┘ │                  │ └──────────────┘  │
└──────────────────┘                  └──────────────────┘
```

## Components

### Services

1. **Ingestion Service** (Port 7001)
   - Handles document ingestion
   - Stores documents in distributed datalake
   - Replicates data across nodes
   - Publishes events to ActiveMQ

2. **Indexer Service** (Port 7002)
   - Consumes documents from ActiveMQ
   - Creates inverted index in Hazelcast
   - Stores metadata in SQLite datamart

3. **Search Service** (Port 7003)
   - Handles search queries
   - Queries distributed inverted index
   - Returns ranked results

### Infrastructure

- **Hazelcast**: Distributed in-memory data grid for caching and data sharing
- **ActiveMQ**: Message broker for asynchronous processing
- **Nginx**: Load balancer for distributing traffic across nodes (dynamically configured with PC IPs from .env)
- **Docker**: Container runtime for all services

**Note**: The Nginx configuration automatically substitutes PC2 and PC3 IP addresses from the .env file at container startup time, enabling proper load balancing across all nodes.

## Network Requirements

### Firewall Ports

All PCs must be on the same network with the following ports open:

#### PC1 (Master Node)
- `61616`: ActiveMQ (required by all nodes)
- `8161`: ActiveMQ Web Console
- `5672`: ActiveMQ AMQP
- `5701`: Hazelcast (required by all nodes)
- `7001`: Ingestion Service
- `7002`: Indexer Service
- `7003`: Search Service
- `80`: Nginx Load Balancer (public access)

#### PC2 and PC3 (Worker Nodes)
- `5701`: Hazelcast (required by all nodes)
- `7001`: Ingestion Service
- `7002`: Indexer Service
- `7003`: Search Service

### Network Configuration

All nodes must:
- Be on the same subnet (e.g., 192.168.1.x)
- Have static IPs or DHCP reservations
- Allow bidirectional traffic on the ports above
- Have Docker and Docker Compose installed

## Installation

### Prerequisites

1. **Docker**: Version 20.10 or higher
2. **Docker Compose**: Version 2.0 or higher
3. **Network Access**: All nodes on same network
4. **Operating System**: Windows 10/11 or Linux (Ubuntu 20.04+)

### Step 1: Clone Repository

On each PC, clone the repository:

```bash
git clone https://github.com/thebiggestdata/stage_3.git
cd stage_3
```

### Step 2: Configure Environment

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and set the actual IP addresses of your PCs:
   ```bash
   PC1_IP=192.168.1.100    # Replace with actual PC1 IP
   PC2_IP=192.168.1.101    # Replace with actual PC2 IP
   PC3_IP=192.168.1.102    # Replace with actual PC3 IP
   ```

3. (Optional) Change ActiveMQ credentials:
   ```bash
   ACTIVEMQ_ADMIN_LOGIN=admin
   ACTIVEMQ_ADMIN_PASSWORD=your_secure_password
   ```

**Important**: The `.env` file must be identical on all PCs.

### Step 3: Configure Firewall

#### Windows (PowerShell as Administrator)

```powershell
# On PC1 (Master)
New-NetFirewallRule -DisplayName "Stage3-ActiveMQ" -Direction Inbound -Protocol TCP -LocalPort 61616,8161,5672 -Action Allow
New-NetFirewallRule -DisplayName "Stage3-Hazelcast" -Direction Inbound -Protocol TCP -LocalPort 5701 -Action Allow
New-NetFirewallRule -DisplayName "Stage3-Services" -Direction Inbound -Protocol TCP -LocalPort 7001,7002,7003 -Action Allow
New-NetFirewallRule -DisplayName "Stage3-Nginx" -Direction Inbound -Protocol TCP -LocalPort 80 -Action Allow

# On PC2 and PC3 (Workers)
New-NetFirewallRule -DisplayName "Stage3-Hazelcast" -Direction Inbound -Protocol TCP -LocalPort 5701 -Action Allow
New-NetFirewallRule -DisplayName "Stage3-Services" -Direction Inbound -Protocol TCP -LocalPort 7001,7002,7003 -Action Allow
```

#### Linux (Ubuntu/Debian)

```bash
# On PC1 (Master)
sudo ufw allow 61616/tcp
sudo ufw allow 8161/tcp
sudo ufw allow 5672/tcp
sudo ufw allow 5701/tcp
sudo ufw allow 7001:7003/tcp
sudo ufw allow 80/tcp

# On PC2 and PC3 (Workers)
sudo ufw allow 5701/tcp
sudo ufw allow 7001:7003/tcp

# Enable firewall if not already enabled
sudo ufw enable
```

## Deployment

### Starting the Cluster

Start nodes in order: PC1 first, then PC2 and PC3.

#### Windows (PowerShell)

```powershell
# On PC1
.\start-cluster.ps1 -Node pc1

# On PC2
.\start-cluster.ps1 -Node pc2

# On PC3
.\start-cluster.ps1 -Node pc3
```

#### Linux

```bash
# On PC1
./start-cluster.sh pc1

# On PC2
./start-cluster.sh pc2

# On PC3
./start-cluster.sh pc3
```

### Verifying Deployment

1. **Check running containers** on each PC:
   ```bash
   docker ps
   ```

2. **Check Hazelcast cluster** (should show 3 members):
   ```bash
   # On any PC
   docker logs hazelcast-pc1 2>&1 | grep "Members \["
   ```
   Expected output: `Members [3]`

3. **Check ActiveMQ** (PC1 only):
   - Open browser: `http://PC1_IP:8161`
   - Login: admin/admin (or your configured credentials)
   - Verify connections from all nodes

4. **Test health endpoint**:
   ```bash
   curl http://PC1_IP/health
   ```
   Expected: `Stage 3 Cluster OK`

### Monitoring Logs

View logs for all services on a node:

```bash
# Windows
docker-compose -f docker-compose-pc1.yml logs -f

# Linux
docker-compose -f docker-compose-pc1.yml logs -f
```

View logs for a specific service:

```bash
docker logs -f hazelcast-pc1
docker logs -f ingestion-service-pc1
docker logs -f indexer-service-pc1
docker logs -f search-service-pc1
```

## Usage

### Ingesting Documents

Send documents via the Nginx load balancer on PC1:

```bash
curl -X POST http://PC1_IP/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "id": "doc1",
    "title": "Sample Document",
    "content": "This is a sample document for testing"
  }'
```

### Searching Documents

Query the search service via Nginx:

```bash
curl "http://PC1_IP/search?q=sample"
```

### Accessing ActiveMQ Console

1. Open browser: `http://PC1_IP:8161`
2. Login with configured credentials (default: admin/admin)
3. Monitor queues, topics, and connections

## Troubleshooting

### Hazelcast Connection Issues

**Symptom**: Hazelcast shows `Members [1]` instead of `Members [3]`

**Solutions**:
1. Verify all PCs are on the same network
2. Check firewall rules allow port 5701
3. Verify IP addresses in `.env` are correct
4. Check Hazelcast logs for connection errors:
   ```bash
   docker logs hazelcast-pc1
   ```

### ActiveMQ Connection Issues

**Symptom**: Services can't connect to ActiveMQ broker

**Solutions**:
1. Verify PC1 firewall allows port 61616
2. Check ActiveMQ is running on PC1:
   ```bash
   docker ps | grep activemq
   ```
3. Test connection from worker nodes:
   ```bash
   telnet PC1_IP 61616
   ```

### Service Not Starting

**Symptom**: Service container exits immediately

**Solutions**:
1. Check logs for the service:
   ```bash
   docker logs <container-name>
   ```
2. Verify environment variables in docker-compose file
3. Ensure dependencies (Hazelcast, ActiveMQ) are running
4. Check if port is already in use:
   ```bash
   # Windows
   netstat -ano | findstr :7001
   
   # Linux
   netstat -tlnp | grep 7001
   ```

### Data Replication Issues

**Symptom**: Documents not appearing on all nodes

**Solutions**:
1. Verify Hazelcast cluster has 3 members
2. Check `DATALAKE_REPLICATION_FACTOR=3` in environment
3. Check `DATALAKE_PEERS` includes all node IPs
4. Review ingestion service logs

## Stopping the Cluster

Stop nodes in reverse order: PC3, PC2, then PC1.

```bash
# Windows
docker-compose -f docker-compose-pc1.yml down

# Linux
docker-compose -f docker-compose-pc1.yml down
```

To also remove volumes (delete all data):

```bash
docker-compose -f docker-compose-pc1.yml down -v
```

## Maintenance

### Updating Services

1. Pull latest code:
   ```bash
   git pull origin main
   ```

2. Rebuild containers:
   ```bash
   docker-compose -f docker-compose-pc1.yml build
   ```

3. Restart services:
   ```bash
   docker-compose -f docker-compose-pc1.yml up -d
   ```

### Backup Data

Backup data directories on PC1:

```bash
# Create backup directory
mkdir -p backups/$(date +%Y%m%d)

# Backup datalake
cp -r datalake/ backups/$(date +%Y%m%d)/

# Backup datamart
cp -r datamart/ backups/$(date +%Y%m%d)/

# Backup ActiveMQ data (optional)
docker cp activemq-pc1:/opt/activemq/data backups/$(date +%Y%m%d)/activemq-data
```

### Scaling

To add more worker nodes (PC4, PC5, etc.):

1. Copy `docker-compose-pc3.yml` to `docker-compose-pc4.yml`
2. Update container names (e.g., `hazelcast-pc4`)
3. Add PC4_IP to `.env` file
4. Update `HZ_NETWORK_JOIN_TCPIP_MEMBERS` to include new node
5. Update `DATALAKE_PEERS` and `HAZELCAST_MEMBERS` in all services
6. Update Nginx configuration to include new node in upstreams

## Performance Tuning

### Hazelcast Memory

Adjust in docker-compose files:

```yaml
JAVA_OPTS=-Xms512m -Xmx1024m -Dhazelcast.local.publicAddress=${PC1_IP}:5701
```

### ActiveMQ Memory

Add environment variable in PC1 docker-compose:

```yaml
environment:
  - ACTIVEMQ_OPTS_MEMORY=-Xms512m -Xmx1024m
```

### Connection Pooling

Adjust `worker_connections` in `nginx.conf`:

```nginx
events {
    worker_connections 2048;  # Increase for high traffic
}
```

## Development

### Building Services Locally

```bash
# Build ingestion service
cd ingestion-service
mvn clean package
cd ..

# Build indexer service
cd indexing-service
mvn clean package
cd ..

# Build search service
cd search-service
mvn clean package
cd ..
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific service tests
cd indexing-service
mvn test
```

## License

[Add your license information here]

## Contributing

[Add contribution guidelines here]

## Support

For issues and questions:
- GitHub Issues: [repository URL]/issues
- Documentation: [repository URL]/wiki

## References

Based on the successful architecture from:
- https://github.com/DREAM-TEAM-ULPGC/stage_3
