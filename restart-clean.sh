#!/bin/bash
set -e

echo "Building services..."
(cd user-service && mvn package -q -DskipTests)
(cd projection-service && mvn package -q -DskipTests)

echo "Restarting environment (clean)..."
docker-compose down -v
docker-compose up -d --build

echo "Done. Logs: docker-compose logs -f"
