# ========================================
# Stage 3 Cluster - Startup Script
# ========================================
# Usage: .\start-cluster.ps1 -Node <pc1|pc2|pc3>
# Example: .\start-cluster.ps1 -Node pc1

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("pc1", "pc2", "pc3")]
    [string]$Node
)

# Display banner
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Stage 3 Distributed Cluster Startup  " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Starting Node: $Node" -ForegroundColor Green
Write-Host ""

# Validate that .env file exists
if (-not (Test-Path ".env")) {
    Write-Host "ERROR: .env file not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please follow these steps:" -ForegroundColor Yellow
    Write-Host "1. Copy .env.example to .env" -ForegroundColor Yellow
    Write-Host "   cp .env.example .env" -ForegroundColor Gray
    Write-Host "2. Edit .env and configure your PC IP addresses" -ForegroundColor Yellow
    Write-Host "3. Run this script again" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

# Display configuration info
Write-Host "Configuration loaded from .env file" -ForegroundColor Green
Write-Host ""

# Start the appropriate node
switch ($Node) {
    "pc1" {
        Write-Host "Starting PC1 (Master Node)" -ForegroundColor Cyan
        Write-Host "  - ActiveMQ Broker" -ForegroundColor Gray
        Write-Host "  - Hazelcast Node 1" -ForegroundColor Gray
        Write-Host "  - Ingestion Service 1" -ForegroundColor Gray
        Write-Host "  - Indexer Service 1" -ForegroundColor Gray
        Write-Host "  - Search Service 1" -ForegroundColor Gray
        Write-Host "  - Nginx Load Balancer" -ForegroundColor Gray
        Write-Host ""
        
        docker-compose -f docker-compose-pc1.yml --env-file .env up -d
        
        Write-Host ""
        Write-Host "PC1 Services Started!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Access points:" -ForegroundColor Yellow
        Write-Host "  - ActiveMQ Console: http://localhost:8161 (admin/admin)" -ForegroundColor Gray
        Write-Host "  - Nginx Load Balancer: http://localhost" -ForegroundColor Gray
        Write-Host "  - Health Check: http://localhost/health" -ForegroundColor Gray
    }
    "pc2" {
        Write-Host "Starting PC2 (Worker Node)" -ForegroundColor Cyan
        Write-Host "  - Hazelcast Node 2" -ForegroundColor Gray
        Write-Host "  - Ingestion Service 2" -ForegroundColor Gray
        Write-Host "  - Indexer Service 2" -ForegroundColor Gray
        Write-Host "  - Search Service 2" -ForegroundColor Gray
        Write-Host ""
        
        docker-compose -f docker-compose-pc2.yml --env-file .env up -d
        
        Write-Host ""
        Write-Host "PC2 Services Started!" -ForegroundColor Green
    }
    "pc3" {
        Write-Host "Starting PC3 (Worker Node)" -ForegroundColor Cyan
        Write-Host "  - Hazelcast Node 3" -ForegroundColor Gray
        Write-Host "  - Ingestion Service 3" -ForegroundColor Gray
        Write-Host "  - Indexer Service 3" -ForegroundColor Gray
        Write-Host "  - Search Service 3" -ForegroundColor Gray
        Write-Host ""
        
        docker-compose -f docker-compose-pc3.yml --env-file .env up -d
        
        Write-Host ""
        Write-Host "PC3 Services Started!" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Useful Commands:" -ForegroundColor Yellow
Write-Host "  Check status:    docker ps" -ForegroundColor Gray
Write-Host "  View logs:       docker-compose -f docker-compose-$Node.yml logs -f" -ForegroundColor Gray
Write-Host "  Stop cluster:    docker-compose -f docker-compose-$Node.yml down" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
