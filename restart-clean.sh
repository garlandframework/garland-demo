#!/bin/bash
set -e

SOFT=false
if [[ "$1" == "--soft" ]]; then
  SOFT=true
fi

if [ "$SOFT" = true ]; then
  echo "Restarting environment (keeping data)..."
  docker-compose down
  docker-compose up -d --build
else
  echo "Restarting environment (wiping data)..."
  docker-compose down -v
  docker-compose up -d --build
fi

echo "Done. Logs: docker-compose logs -f"
