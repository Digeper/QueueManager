# QueueManager

## What it does

Manages music playback queues for users. Handles song requests, downloads, queue operations, and serves audio files. Coordinates with other services via Kafka to process song downloads.

## Local Setup

1. Ensure PostgreSQL is running on `localhost:5432`
2. Create database `postgres` (or update `application.properties`)
3. Update `application.properties` with database credentials
4. Ensure Kafka is running on `localhost:9092`
5. Configure file storage paths in `application.properties`
6. Run: `mvn spring-boot:run`
7. Service starts on port `8090`

## Deployment

Deploy to Kubernetes namespace `muzika`:
```bash
kubectl apply -k k8s/
```

Image: `${ACR_NAME}.azurecr.io/muzika/queuemanager:latest`

Requires: PostgreSQL database, Kafka cluster, Azure File Shares (database, downloads, incomplete), Azure Key Vault secrets, ConfigMap
