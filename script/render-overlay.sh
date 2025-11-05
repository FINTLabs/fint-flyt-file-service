#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMPLATE_DIR="$ROOT/kustomize/templates"
BASE_TEMPLATE="$TEMPLATE_DIR/overlay.yaml.tpl"
ENV_TEMPLATE="$TEMPLATE_DIR/overlay-with-env.yaml.tpl"

while IFS= read -r file; do
  rel="${file#"$ROOT/kustomize/overlays/"}"
  dir="$(dirname "$rel")"

  namespace="${dir%%/*}"
  env_path="${dir#*/}"
  if [[ "$env_path" == "$namespace" ]]; then
    env_path=""
  fi

  export NAMESPACE="$namespace"
  export ORG_ID="${namespace//-/.}"
  export APP_INSTANCE="fint-flyt-file-service_${namespace}"
  export KAFKA_TOPIC="${namespace}.flyt.*"
  export BLOB_INSTANCE="fint-flyt-file-service-azure-blob-storage_${namespace//-/_}"
  export FINT_KAFKA_TOPIC_ORGID="$namespace"

  target_dir="$ROOT/kustomize/overlays/$dir"
  template="$BASE_TEMPLATE"

  tmp="$(mktemp)"
  envsubst < "$template" > "$tmp"
  mv "$tmp" "$target_dir/kustomization.yaml"
done < <(find "$ROOT/kustomize/overlays" -name kustomization.yaml -print | sort)
