#!/usr/bin/env bash
# Benchmark Gitea publish:gitea sub-steps from Backstage pod (or locally with curl).
set -euo pipefail

DOMAIN="${CLUSTER_DOMAIN:-$(oc get ingresses.config cluster -o jsonpath='{.spec.domain}')}"
POD="${BACKSTAGE_POD:-$(oc get pod -n developer-hub -l rhdh.redhat.com/app=backstage-developer-hub -o jsonpath='{.items[0].metadata.name}')}"
ORG="${GITEA_ORG:-ws-user1}"
REPO="perf-test-$(date +%s)"

run_in_pod() {
  oc exec -n developer-hub "$POD" -c backstage-backend -- bash -s <<SCRIPT
set -euo pipefail
BASE_INT=http://gitea-http.gitea.svc.cluster.local:3000
BASE_EXT=https://gitea-gitea.${DOMAIN}
ORG=${ORG}
REPO=${REPO}
AUTH='gitea_admin:openshift'

echo "=== org check (internal) ==="
curl -sS -o /dev/null -w "org internal: %{time_total}s code=%{http_code}\n" -u "\$AUTH" "\$BASE_INT/api/v1/orgs/\$ORG"

echo "=== create repo (internal API) ==="
curl -sS -o /tmp/create.json -w "create: %{time_total}s code=%{http_code}\n" -u "\$AUTH" \\
  -X POST "\$BASE_INT/api/v1/orgs/\$ORG/repos" \\
  -H "Content-Type: application/json" \\
  -d "{\"name\":\"\$REPO\",\"auto_init\":false,\"private\":false}"

TMP=/tmp/gperf-\$\$
mkdir -p "\$TMP" && cd "\$TMP"
git init -q
git config user.email t@t.com
git config user.name t
echo hi >f
git add .
git commit -q -m init
BR=\$(git branch --show-current)

echo "=== git push (internal remote) ==="
START=\$(date +%s%3N)
git push -q "http://\${AUTH}@\${BASE_INT#http://}/\$ORG/\$REPO.git" "\$BR" 2>&1
echo "push elapsed: \$(( \$(date +%s%3N) - START ))ms"

poll() {
  local base="\$1" label="\$2"
  local start=\$(date +%s)
  for i in \$(seq 1 20); do
    code=\$(curl -sS -o /dev/null -w "%{http_code}" "\$base/\$ORG/\$REPO/src/branch/\$BR" -u "\$AUTH" || echo 000)
    echo "  \$label attempt \$i: \$code (\$(( \$(date +%s) - start ))s elapsed)"
    [ "\$code" = "200" ] && return 0
    sleep 1
  done
  return 1
}

echo "=== availability poll (internal baseUrl) ==="
poll "\$BASE_INT" internal || true

echo "=== availability poll (external baseUrl) ==="
poll "\$BASE_EXT" external || true

curl -sS -o /dev/null -u "\$AUTH" -X DELETE "\$BASE_INT/api/v1/repos/\$ORG/\$REPO"
echo "cleaned up \$ORG/\$REPO"
SCRIPT
}

run_in_pod
