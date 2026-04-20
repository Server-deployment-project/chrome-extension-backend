#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

usage() {
  cat <<EOF
Usage:
  ./deploy-fixed-tag.sh <version-tag>

Example:
  ./deploy-fixed-tag.sh 1.4.2
EOF
}

if [[ $# -ne 1 ]]; then
  usage
  exit 1
fi

IMAGE_TAG="$1"
if [[ "${IMAGE_TAG}" == "latest" ]]; then
  echo "Refuse to deploy latest. Use an immutable version tag."
  exit 1
fi

if ! [[ "${IMAGE_TAG}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z]+)*$ ]]; then
  echo "Tag format is invalid. Example: 1.2.3 or 1.2.3-rc1"
  exit 1
fi

if [[ ! -f ".env" ]]; then
  echo ".env is required in ${SCRIPT_DIR}"
  exit 1
fi

set -a
source ".env"
set +a

if [[ -z "${IMAGE_REPO:-}" ]]; then
  echo "IMAGE_REPO is required in .env"
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "docker compose or docker-compose is required"
  exit 1
fi

if [[ -n "${REGISTRY_USERNAME:-}" && -n "${REGISTRY_TOKEN:-}" ]]; then
  REGISTRY_HOST="${REGISTRY_HOST:-}"
  if [[ -n "${REGISTRY_HOST}" ]]; then
    echo "${REGISTRY_TOKEN}" | docker login "${REGISTRY_HOST}" -u "${REGISTRY_USERNAME}" --password-stdin
  else
    echo "${REGISTRY_TOKEN}" | docker login -u "${REGISTRY_USERNAME}" --password-stdin
  fi
fi

export IMAGE_TAG

echo "Deploying ${IMAGE_REPO}:${IMAGE_TAG}"
"${COMPOSE_CMD[@]}" -f docker-compose.release.yml pull backend
"${COMPOSE_CMD[@]}" -f docker-compose.release.yml up -d backend --remove-orphans

APP_PORT_VALUE="${APP_PORT:-8001}"
echo "Waiting for health endpoint on port ${APP_PORT_VALUE} ..."
for _ in {1..30}; do
  if curl -fsS "http://127.0.0.1:${APP_PORT_VALUE}/api/v1/health" >/dev/null 2>&1; then
    echo "Deployment succeeded: ${IMAGE_REPO}:${IMAGE_TAG}"
    exit 0
  fi
  sleep 2
done

echo "Health check failed. Recent backend logs:"
"${COMPOSE_CMD[@]}" -f docker-compose.release.yml logs --tail=100 backend || true
exit 1
