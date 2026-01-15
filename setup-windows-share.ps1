param(
    [Parameter(Mandatory=$true)]
    [string]$Role  # "master" o "worker"
)

$SharedPath = "C:\shared\datalake"

if ($Role -eq "master") {
    Write-Host "📁 Configurando MASTER..."

    # Crear directorio
    New-Item -Path $SharedPath -ItemType Directory -Force

    # Compartir
    try {
        New-SmbShare -Name "datalake" -Path $SharedPath -FullAccess "Everyone" -ErrorAction Stop
        Write-Host "✅ Carpeta compartida creada: \\$env:COMPUTERNAME\datalake"
    } catch {
        Write-Host "⚠️  La carpeta ya está compartida."
    }

    # Permisos
    icacls $SharedPath /grant Everyone:"(OI)(CI)F" /T

    Write-Host "`n✅ MASTER configurado!"
    Write-Host "🔗 Ruta de red: \\$env:COMPUTERNAME\datalake"

} elseif ($Role -eq "worker") {
    $MasterIP = Read-Host "Ingrese la IP del MASTER"

    Write-Host "📁 Configurando WORKER..."

    # Montar unidad de red
    net use Z: "\\$MasterIP\datalake" /persistent:yes

    Write-Host "✅ WORKER configurado!"
    Write-Host "📂 Carpeta montada en: Z:\"

} else {
    Write-Host "❌ Rol inválido. Use 'master' o 'worker'"
}