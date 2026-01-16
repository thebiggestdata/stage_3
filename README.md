# Stage 3 - Hazelcast Distributed System

Sistema distribuido de procesamiento de tareas usando **Hazelcast** para comunicación entre nodos master y workers.

## Características

✅ **Sin permisos de administrador** - No requiere permisos elevados  
✅ **TCP/IP directo** - Sin necesidad de multicast  
✅ **Escalable horizontalmente** - Añade workers dinámicamente  
✅ **Tolerante a fallos** - Si un worker falla, otro puede continuar  
✅ **Sin archivos externos** - Todo se comunica mediante Hazelcast en memoria

## Arquitectura

```
Master Node
    │
    ├── Distribuye tareas → IQueue (Cola distribuida)
    │                           │
    │                           ├── Worker 1 (procesa tareas)
    │                           ├── Worker 2 (procesa tareas)
    │                           └── Worker N (procesa tareas)
    │
    └── Recibe resultados ← IMap (Mapa distribuido)
```

## Requisitos

- Java 11 o superior
- Maven 3.6 o superior

## Compilación

```bash
# Compilar el proyecto
mvn clean package

# Esto generará target/stage3-hazelcast-1.0.0.jar
```

## Configuración de Red

### Para ejecución local (mismo equipo)

No se requiere configuración adicional. El sistema usa `127.0.0.1` por defecto.

### Para ejecución distribuida (múltiples máquinas)

Editar `HazelcastConfig.java` y modificar la lista de miembros:

```java
// En createConfig() o createLocalConfig()
tcpIpConfig.addMember("192.168.1.10:5701");  // IP del Master
tcpIpConfig.addMember("192.168.1.11:5701");  // IP del Worker 1
tcpIpConfig.addMember("192.168.1.12:5701");  // IP del Worker 2
```

**Importante:** Asegúrate de que el puerto 5701 (y siguientes) estén abiertos en el firewall.

## Uso

### 1. Ejecutar el Master

```bash
# Sintaxis básica
java -cp target/stage3-hazelcast-1.0.0.jar:~/.m2/repository/com/hazelcast/hazelcast/5.3.6/hazelcast-5.3.6.jar com.bigdata.stage3.Master [número_de_tareas]

# Ejemplo con 100 tareas (por defecto)
java -cp target/stage3-hazelcast-1.0.0.jar:~/.m2/repository/com/hazelcast/hazelcast/5.3.6/hazelcast-5.3.6.jar com.bigdata.stage3.Master

# Ejemplo con 500 tareas
java -cp target/stage3-hazelcast-1.0.0.jar:~/.m2/repository/com/hazelcast/hazelcast/5.3.6/hazelcast-5.3.6.jar com.bigdata.stage3.Master 500
```

### 2. Ejecutar Workers (en terminales separadas o diferentes máquinas)

```bash
# Sintaxis básica
java -cp target/stage3-hazelcast-1.0.0.jar:~/.m2/repository/com/hazelcast/hazelcast/5.3.6/hazelcast-5.3.6.jar com.bigdata.stage3.Worker [id_worker]

# Worker 1
java -cp target/stage3-hazelcast-1.0.0.jar:~/.m2/repository/com/hazelcast/hazelcast/5.3.6/hazelcast-5.3.6.jar com.bigdata.stage3.Worker WORKER-1

# Worker 2
java -cp target/stage3-hazelcast-1.0.0.jar:~/.m2/repository/com/hazelcast/hazelcast/5.3.6/hazelcast-5.3.6.jar com.bigdata.stage3.Worker WORKER-2

# Worker 3
java -cp target/stage3-hazelcast-1.0.0.jar:~/.m2/repository/com/hazelcast/hazelcast/5.3.6/hazelcast-5.3.6.jar com.bigdata.stage3.Worker WORKER-3
```

Si no se especifica un ID, se generará automáticamente.

## Ejemplo de Ejecución Completa

### Paso 1: Abrir Terminal 1 (Master)
```bash
cd /home/runner/work/stage_3/stage_3
mvn clean package
java -cp target/stage3-hazelcast-1.0.0.jar:~/.m2/repository/com/hazelcast/hazelcast/5.3.6/hazelcast-5.3.6.jar com.bigdata.stage3.Master 200
```

Verás:
```
=== MASTER NODE STARTED ===
Cluster: ACTIVE
Members: 1
Waiting for workers to connect...
Press ENTER when workers are ready, or wait 5 seconds...
```

### Paso 2: Abrir Terminal 2 (Worker 1)
```bash
java -cp target/stage3-hazelcast-1.0.0.jar:~/.m2/repository/com/hazelcast/hazelcast/5.3.6/hazelcast-5.3.6.jar com.bigdata.stage3.Worker WORKER-1
```

### Paso 3: Abrir Terminal 3 (Worker 2)
```bash
java -cp target/stage3-hazelcast-1.0.0.jar:~/.m2/repository/com/hazelcast/hazelcast/5.3.6/hazelcast-5.3.6.jar com.bigdata.stage3.Worker WORKER-2
```

### Paso 4: Iniciar Procesamiento

Volver a Terminal 1 (Master) y presionar ENTER. El Master distribuirá las tareas y verás:

```
=== DISTRIBUTING TASKS ===
Distributed 10 tasks...
Distributed 20 tasks...
...
Total tasks distributed: 200

=== MONITORING RESULTS ===
Progress: 50/200 (25.0%) - Throughput: 45.23 tasks/sec - Active Workers: 2
Progress: 100/200 (50.0%) - Throughput: 48.15 tasks/sec - Active Workers: 2
...
```

Los Workers mostrarán:
```
WORKER-1 completed TASK-1 (CALCULATE_PRIME) in 5ms
WORKER-2 completed TASK-2 (UPPERCASE) in 2ms
WORKER-1 processed 10 tasks
...
```

### Paso 5: Resultados Finales

Cuando todas las tareas terminen, el Master mostrará:
```
=== PROCESSING COMPLETE ===
Total tasks: 200
Total time: 4235 ms
Average throughput: 47.23 tasks/sec

=== RESULTS BY WORKER ===
WORKER-1: 98 tasks
WORKER-2: 102 tasks

=== SAMPLE RESULTS ===
Result{taskId='TASK-1', workerId='WORKER-1', result='Next prime after 100 is 101', processingTimeMs=5}
...

Success: 200, Failures: 0
```

## Verificar Conexión de Nodos

Los nodos se conectan automáticamente. Para verificar:

1. El Master mostrará el número de miembros del cluster
2. Cada Worker mostrará el número de miembros al iniciar
3. El Master muestra "Active Workers" durante el monitoreo

Ejemplo de salida de conexión correcta:
```
Members: 3  (1 Master + 2 Workers)
```

## Tipos de Tareas Soportadas

El sistema implementa tres tipos de operaciones:

1. **CALCULATE_PRIME**: Calcula el siguiente número primo
   - Entrada: número entero
   - Salida: siguiente número primo

2. **UPPERCASE**: Convierte texto a mayúsculas
   - Entrada: texto
   - Salida: texto en mayúsculas

3. **REVERSE**: Invierte una cadena de texto
   - Entrada: texto
   - Salida: texto invertido

## Estructura del Proyecto

```
src/
  └── main/
      └── java/
          └── com/
              └── bigdata/
                  └── stage3/
                      ├── Master.java          # Nodo master
                      ├── Worker.java          # Nodo worker
                      ├── Task.java            # Modelo de tarea
                      ├── Result.java          # Modelo de resultado
                      └── HazelcastConfig.java # Configuración compartida
pom.xml                                        # Dependencias Maven
README.md                                      # Este archivo
```

## Solución de Problemas

### Los Workers no se conectan al Master

- Verifica que todos los nodos tengan la misma configuración de red
- Asegúrate de que el puerto 5701 esté abierto
- Revisa que todos usen el mismo `CLUSTER_NAME` en `HazelcastConfig.java`

### "Address already in use"

- Ya hay una instancia corriendo en ese puerto
- Cambia el puerto en `NetworkConfig` o detén la instancia anterior
- El autoincremento de puerto está habilitado, debería asignar el siguiente disponible

### Workers no procesan tareas

- Verifica que el Master haya distribuido las tareas (revisa su salida)
- Asegúrate de que los Workers estén corriendo antes de iniciar el procesamiento
- Revisa los logs para errores de serialización

## Escalabilidad

- **Añadir más workers**: Simplemente ejecuta más instancias de Worker
- **Aumentar tareas**: Especifica un número mayor al ejecutar Master
- **Distribución geográfica**: Configura las IPs de los nodos remotos

## Licencia

Este proyecto es parte de Stage 3 - Big Data Processing