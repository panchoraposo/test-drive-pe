#!/usr/bin/env bash
# Publish software-template skeletons to Gitea for user3 (equivalent to Developer Hub scaffolder output).
set -euo pipefail

USER="${USER_NAME:-user3}"
ORG="ws-${USER}"
NS="${USER}-neuralbank"
DOMAIN="${CLUSTER_DOMAIN:-$(oc get ingresses.config cluster -o jsonpath='{.spec.domain}')}"
GITEA="${GITEA_URL:-http://gitea-http.gitea.svc.cluster.local:3000}"
GITEA_USER="${GITEA_ADMIN:-gitea_admin}"
GITEA_PASS="${GITEA_PASS:-openshift}"
REPO_ROOT="${REPO_ROOT:-/workspace}"
IMAGE_REG="image-registry.openshift-image-registry.svc:5000"

substitute() {
  local dir="$1"
  find "$dir" -type f ! -path '*/.git/*' -print0 | while IFS= read -r -d '' f; do
    sed -i \
      -e "s|\${{values.name}}|${NAME}|g" \
      -e "s|\${{values.uniqueName}}|${USER}-${NAME}|g" \
      -e "s|\${{values.owner}}|${USER}|g" \
      -e "s|\${{values.namespace}}|${NS}|g" \
      -e "s|\${{values.imageRegistry}}|${IMAGE_REG}|g" \
      -e "s|\${{values.clusterDomain}}|${DOMAIN}|g" \
      -e "s|\${{values.backendUniqueName}}|${USER}-neuralbank-backend|g" \
      -e "s|\${{values.backendApiName}}|${USER}-neuralbank-backend-api|g" \
      -e "s|\${{values.description}}|Neuralbank ${NAME} for ${USER}|g" \
      "$f"
  done
}

create_repo() {
  local name="$1"
  CODE=$(curl -sS -o /dev/null -w "%{http_code}" -u "${GITEA_USER}:${GITEA_PASS}" \
    -X POST "${GITEA}/api/v1/orgs/${ORG}/repos" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"${name}\",\"auto_init\":false,\"private\":false}" || true)
  echo "  create repo ${ORG}/${name}: HTTP ${CODE}"
}

push_repo() {
  local src="$1"
  local name="$2"
  local tmp
  tmp=$(mktemp -d)
  cp -a "$src/." "$tmp/"
  substitute "$tmp"
  cd "$tmp"
  git init -q
  git config user.email "${USER}@redhat.local"
  git config user.name "${USER}"
  git add -A
  git commit -q -m "Scaffold ${name} for ${USER} from software template"
  git branch -M main
  git remote add origin "http://${GITEA_USER}:${GITEA_PASS}@gitea-http.gitea.svc.cluster.local:3000/${ORG}/${name}.git"
  git push -f origin main
  echo "  pushed ${ORG}/${name}"
}

for NAME in neuralbank-backend neuralbank-frontend customer-service-mcp; do
  case "$NAME" in
    neuralbank-backend) SKEL="${REPO_ROOT}/examples/helm/software-templates/neuralbank-backend/skeleton" ;;
    neuralbank-frontend) SKEL="${REPO_ROOT}/examples/helm/software-templates/neuralbank-frontend/skeleton" ;;
    customer-service-mcp) SKEL="${REPO_ROOT}/examples/helm/software-templates/customer-service-mcp/skeleton" ;;
  esac
  echo "=== ${NAME} ==="
  create_repo "$NAME"
  push_repo "$SKEL" "$NAME"
done

echo "Done. ArgoCD ApplicationSet ws-${USER}-repos should pick up new repos."
