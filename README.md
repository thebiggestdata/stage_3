# Stage 3 - Distributed Search Engine

Sistema de búsqueda distribuido usando Hazelcast, ActiveMQ y Docker.

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────────────┐
│                         HAZELCAST CLUSTER                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                 │
│  │ IMap:       │  │ IMap:       │  │ IMap:       │                 │
│  │ datalake    │  │ inverted-   │  │ processed-  │                 │
│  │ (books)     │  │ index       │  │ documents   │                 │
│  └─────────────┘  └─────────────┘  └─────────────┘                 │
└─────────────────────────────────────────────────────────────────────┘
         ▲                   ▲                   ▲
         │                   │                   │
    ┌────┴────┐         ┌────┴────┐         ┌────┴────┐
    │Ingestion│   ───►  │Indexing │   ◄───  │ Search  │
    │ Service │  AMQ    │ Service │         │ Service │
    │ :7001   │         │ :7002   │         │ :7003   │
    │ :5701   │         │ :5702   │         │ :5703   │
    └─────────┘         └─────────┘         └─────────┘
```

## 📋 Requisitos

- Docker + Docker Compose
- Maven 3.9+
- Java 17+
- Red local (mismo switch/VLAN)

## 🚀 Guía Rápida

### 1. Configurar IPs

Editar el archivo `.env` en TODOS los PCs con las IPs reales:

```bash
# Obtener IP en Windows:
ipconfig
# Buscar "IPv4 Address" (ej: 10.26.14.100)
```

```env
PC1_IP=10.26.14.100   # Master (ActiveMQ aquí)
PC2_IP=10.26.14.101   # Worker
PC3_IP=10.26.14.102   # Worker (opcional)
PC4_IP=10.26.14.103   # Worker (opcional)
```

### 2. Copiar proyecto a todos los PCs

Copiar toda la carpeta `stage_3` a cada PC del laboratorio.

### 3. Ejecutar en cada PC

#### Opción A: Solo 2 PCs (recomendado para empezar)

**PC1 (Master):**
```bash
docker-compose --env-file .env -f docker-compose-2pc-master.yml up --build
```

**PC2:**
```bash
docker-compose --env-file .env -f docker-compose-2pc-worker.yml up --build
```

#### Opción B: 4 PCs (configuración completa)

**PC1 (Master):**
```bash
docker-compose --env-file .env -f docker-compose-pc1.yml up --build
```

**PC2:**
```bash
docker-compose --env-file .env -f docker-compose-pc2.yml up --build
```

**PC3:**
```bash
docker-compose --env-file .env -f docker-compose-pc3.yml up --build
```

**PC4:**
```bash
docker-compose --env-file .env -f docker-compose-pc4.yml up --build
```

### 4. Verificar cluster

Esperar ~30 segundos y verificar:

```bash
# Desde cualquier PC:
curl http://localhost:7001/health
curl http://localhost:7002/health
curl http://localhost:7003/health
```

Deberías ver `clusterSize: N` donde N = número de nodos (3 servicios × número de PCs).

## 📚 Uso

### Ingestar libros

```bash
# Ingestar un libro
curl -X POST http://PC1_IP:7001/ingest/1

# Ingestar varios (batch)
curl -X POST "http://PC1_IP:7001/ingest/batch?start=1&end=100&threads=5"

# Ver estado
curl http://PC1_IP:7001/datalake/stats
```

### Buscar

```bash
# Búsqueda simple
curl "http://PC1_IP:7003/search?q=love"

# Búsqueda múltiples términos (AND)
curl "http://PC1_IP:7003/search?q=love+war&mode=and"

# Búsqueda OR
curl "http://PC1_IP:7003/search?q=love+war&mode=or"

# Ver estadísticas del índice
curl http://PC1_IP:7003/stats
```

## 🔧 Puertos

| Servicio | HTTP API | Hazelcast |
|----------|----------|-----------|
| Ingestion | 7001 | 5701 |
| Indexing | 7002 | 5702 |
| Search | 7003 | 5703 |
| ActiveMQ | 8161 (web) | 61616 (JMS) |
| Nginx LB | 80 | - |

## 🐛 Troubleshooting

### Los nodos no se ven entre sí

1. Verificar que las IPs en `.env` son correctas
2. Verificar que los puertos 5701-5703 están accesibles entre PCs
3. Verificar firewall desactivado
4. Ver logs: `docker logs ingestion-pc1`

### Error al ingestar

1. Verificar internet (se descarga de gutenberg.org)
2. Ver logs: `docker logs ingestion-pc1`

### ActiveMQ no conecta

1. Verificar que PC1 tiene ActiveMQ corriendo
2. Verificar que otros PCs pueden alcanzar PC1_IP:61616

### Limpiar y reiniciar

```bash
docker-compose --env-file .env -f docker-compose-pcX.yml down -v
docker system prune -f
```

## 📊 Métricas para el benchmark

- **Ingestion Rate**: `curl http://PC1_IP:7001/datalake/stats`
- **Index Size**: `curl http://PC1_IP:7003/stats`
- **Search Time**: Incluido en respuesta de `/search`

## 👥 Configuraciones de nodos (para el PDF)

El PDF pide probar con diferentes configuraciones:

1. **Config 1**: 2 PCs (6 nodos Hazelcast)
2. **Config 2**: 3 PCs (9 nodos Hazelcast)  
3. **Config 3**: 4 PCs (12 nodos Hazelcast)