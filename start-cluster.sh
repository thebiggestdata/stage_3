#!/bin/bash

# ========================================
# Stage 3 Cluster - Startup Script
# ========================================
# Usage: ./start-cluster.sh <pc1|pc2|pc3>
# Example: ./start-cluster.sh pc1

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
NC='\033[0m' # No Color

# Display banner
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  Stage 3 Distributed Cluster Startup  ${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# Validate arguments
if [ $# -ne 1 ]; then
    echo -e "${RED}ERROR: Node parameter required!${NC}"
    echo ""
    echo -e "${YELLOW}Usage: ./start-cluster.sh <pc1|pc2|pc3>${NC}"
    echo -e "${GRAY}Example: ./start-cluster.sh pc1${NC}"
    echo ""
    exit 1
fi

NODE=$1

# Validate node parameter
if [[ ! "$NODE" =~ ^(pc1|pc2|pc3)$ ]]; then
    echo -e "${RED}ERROR: Invalid node '$NODE'${NC}"
    echo -e "${YELLOW}Valid options: pc1, pc2, pc3${NC}"
    echo ""
    exit 1
fi

echo -e "${GREEN}Starting Node: $NODE${NC}"
echo ""

# Validate that .env file exists
if [ ! -f ".env" ]; then
    echo -e "${RED}ERROR: .env file not found!${NC}"
    echo ""
    echo -e "${YELLOW}Please follow these steps:${NC}"
    echo -e "${YELLOW}1. Copy .env.example to .env${NC}"
    echo -e "${GRAY}   cp .env.example .env${NC}"
    echo -e "${YELLOW}2. Edit .env and configure your PC IP addresses${NC}"
    echo -e "${YELLOW}3. Run this script again${NC}"
    echo ""
    exit 1
fi

# Display configuration info
echo -e "${GREEN}Configuration loaded from .env file${NC}"
echo ""

# Start the appropriate node
case $NODE in
    pc1)
        echo -e "${CYAN}Starting PC1 (Master Node)${NC}"
        echo -e "${GRAY}  - ActiveMQ Broker${NC}"
        echo -e "${GRAY}  - Hazelcast Node 1${NC}"
        echo -e "${GRAY}  - Ingestion Service 1${NC}"
        echo -e "${GRAY}  - Indexer Service 1${NC}"
        echo -e "${GRAY}  - Search Service 1${NC}"
        echo -e "${GRAY}  - Nginx Load Balancer${NC}"
        echo ""
        
        docker-compose -f docker-compose-pc1.yml --env-file .env up -d
        
        if [ $? -ne 0 ]; then
            echo ""
            echo -e "${RED}ERROR: Failed to start PC1 services!${NC}"
            echo -e "${YELLOW}Check docker-compose logs for details:${NC}"
            echo -e "${GRAY}  docker-compose -f docker-compose-pc1.yml logs${NC}"
            exit 1
        fi
        
        echo ""
        echo -e "${GREEN}PC1 Services Started!${NC}"
        echo ""
        echo -e "${YELLOW}Access points:${NC}"
        echo -e "${GRAY}  - ActiveMQ Console: http://localhost:8161 (admin/admin)${NC}"
        echo -e "${GRAY}  - Nginx Load Balancer: http://localhost${NC}"
        echo -e "${GRAY}  - Health Check: http://localhost/health${NC}"
        ;;
    pc2)
        echo -e "${CYAN}Starting PC2 (Worker Node)${NC}"
        echo -e "${GRAY}  - Hazelcast Node 2${NC}"
        echo -e "${GRAY}  - Ingestion Service 2${NC}"
        echo -e "${GRAY}  - Indexer Service 2${NC}"
        echo -e "${GRAY}  - Search Service 2${NC}"
        echo ""
        
        docker-compose -f docker-compose-pc2.yml --env-file .env up -d
        
        if [ $? -ne 0 ]; then
            echo ""
            echo -e "${RED}ERROR: Failed to start PC2 services!${NC}"
            echo -e "${YELLOW}Check docker-compose logs for details:${NC}"
            echo -e "${GRAY}  docker-compose -f docker-compose-pc2.yml logs${NC}"
            exit 1
        fi
        
        echo ""
        echo -e "${GREEN}PC2 Services Started!${NC}"
        ;;
    pc3)
        echo -e "${CYAN}Starting PC3 (Worker Node)${NC}"
        echo -e "${GRAY}  - Hazelcast Node 3${NC}"
        echo -e "${GRAY}  - Ingestion Service 3${NC}"
        echo -e "${GRAY}  - Indexer Service 3${NC}"
        echo -e "${GRAY}  - Search Service 3${NC}"
        echo ""
        
        docker-compose -f docker-compose-pc3.yml --env-file .env up -d
        
        if [ $? -ne 0 ]; then
            echo ""
            echo -e "${RED}ERROR: Failed to start PC3 services!${NC}"
            echo -e "${YELLOW}Check docker-compose logs for details:${NC}"
            echo -e "${GRAY}  docker-compose -f docker-compose-pc3.yml logs${NC}"
            exit 1
        fi
        
        echo ""
        echo -e "${GREEN}PC3 Services Started!${NC}"
        ;;
esac

echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${YELLOW}Useful Commands:${NC}"
echo -e "${GRAY}  Check status:    docker ps${NC}"
echo -e "${GRAY}  View logs:       docker-compose -f docker-compose-$NODE.yml logs -f${NC}"
echo -e "${GRAY}  Stop cluster:    docker-compose -f docker-compose-$NODE.yml down${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""
