#!/bin/bash

# ========================================
# NFS SETUP SCRIPT
# ========================================
# Ejecutar en MASTER (192.168.1.10)
# ========================================

set -e

echo "========================================="
echo "   NFS Server Setup (Master Node)"
echo "========================================="

# Instalar NFS Server
sudo apt update
sudo apt install -y nfs-kernel-server

# Crear directorio compartido
sudo mkdir -p /shared/datalake
sudo chown -R nobody:nogroup /shared/datalake
sudo chmod 777 /shared/datalake

# Configurar exports
echo "/shared/datalake 192.168.1.0/24(rw,sync,no_subtree_check,no_root_squash)" | sudo tee -a /etc/exports

# Aplicar configuración
sudo exportfs -a
sudo systemctl restart nfs-kernel-server

echo "✅ NFS Server configured successfully!"
echo "Shared path: /shared/datalake"
echo ""
echo "========================================="
echo "   Run on WORKER nodes:"
echo "========================================="
echo "sudo apt install -y nfs-common"
echo "sudo mkdir -p /shared/datalake"
echo "sudo mount -t nfs 192.168.1.10:/shared/datalake /shared/datalake"
echo "# Add to /etc/fstab for persistence:"
echo "192.168.1.10:/shared/datalake /shared/datalake nfs defaults 0 0"