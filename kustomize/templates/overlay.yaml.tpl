apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: $NAMESPACE

resources:
  - ../../../base

patches:
  - patch: |-
      - op: replace
        path: "/metadata/labels/app.kubernetes.io~1instance"
        value: "$APP_INSTANCE"
      - op: replace
        path: "/metadata/labels/fintlabs.no~1org-id"
        value: "$ORG_ID"
      - op: replace
        path: "/spec/kafka/acls/0/topic"
        value: "$KAFKA_TOPIC"
      - op: replace
        path: "/spec/orgId"
        value: "$ORG_ID"
      - op: replace
        path: "/spec/env/1/value"
        value: "$FINT_KAFKA_TOPIC_ORGID"
    target:
      kind: Application
      name: fint-flyt-file-service

  - patch: |-
      - op: replace
        path: "/metadata/labels/app.kubernetes.io~1instance"
        value: "$BLOB_INSTANCE"
      - op: replace
        path: "/metadata/labels/fintlabs.no~1org-id"
        value: "$ORG_ID"
    target:
      kind: AzureBlobContainer
      name: fint-flyt-file-service-azure-blob-storage
