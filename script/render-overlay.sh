#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMPLATE_DIR="$ROOT/kustomize/templates"
BASE_TEMPLATE="$TEMPLATE_DIR/overlay.yaml.tpl"

OTEL_ENDPOINT_BETA="http://alloy.flais-system.svc.cluster.local:4318"

# Base-URL uten /v1/traces: telemetry-starter legger på signal-stien selv.
# Settes manuelt inntil flaiserator har støtte for det, og kun i beta der Alloy kjører.
build_otel_env_patch() {
  local environment="$1"

  if [[ "$environment" != "beta" ]]; then
    return
  fi

  printf '\n      - op: add\n        path: "/spec/env/-"\n        value:\n          name: "OTEL_EXPORTER_OTLP_ENDPOINT"\n          value: "%s"' \
    "$OTEL_ENDPOINT_BETA"
}

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
  export OTEL_ENV_PATCH="$(build_otel_env_patch "$env_path")"

  target_dir="$ROOT/kustomize/overlays/$dir"
  template="$BASE_TEMPLATE"

  tmp="$(mktemp)"
  envsubst < "$template" > "$tmp"
  mv "$tmp" "$target_dir/kustomization.yaml"
done < <(find "$ROOT/kustomize/overlays" -name kustomization.yaml -print | sort)
