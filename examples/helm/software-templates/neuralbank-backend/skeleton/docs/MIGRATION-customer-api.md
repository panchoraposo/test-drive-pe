# Migration: MCP `getCustomer` 404 fix

Workshop `customer-service-mcp` calls `GET /api/customers/{customerId}` on `neuralbank-backend`.
Older backend repos only exposed `GET /api/customers` (list) and returned **404** for `GET /api/customers/1`.

## New apps

Scaffold from the updated Backstage template; no extra steps.

## Existing Gitea repos (`ws-*/neuralbank-backend`)

1. **Add customer API** — copy from this template into your repo:
   - `src/main/java/com/neuralbank/api/CustomerResource.java`
   - `src/main/java/com/neuralbank/dto/**`
   - `src/main/java/com/neuralbank/enums/CustomerType.java`
2. **Remove duplicate list endpoint** — delete `listCustomers()` from `CreditResource.java` (customers are served by `CustomerResource`).
3. **Fix Service selector** (stops routing cluster traffic to the Istio gateway pod):
   - `manifests/gateway.yaml`: `app.kubernetes.io/name: <app>-gateway`
   - `manifests/deployment.yaml`: add label `app.kubernetes.io/component: application` on Deployment and pod template
   - `manifests/service.yaml`: add `app.kubernetes.io/component: application` to `spec.selector`
4. Commit and push; Tekton rebuilds and Argo CD rolls out.

Verify from any pod in the namespace:

```bash
curl -sS -H "x-api-key: devspaces" \
  "http://neuralbank-backend.<namespace>.svc.cluster.local:8080/api/customers/1"
```

Expect HTTP 200 and JSON with numeric `"id": 1`.

## Existing Gitea repos (`ws-*/customer-service-mcp`)

1. `src/main/resources/application.properties`:
   - `quarkus.rest-client."customerclient".headers."X-API-Key"=devspaces`
2. Apply the same gateway/service/component manifest changes as above.
3. Push and redeploy.

## Cluster-level auto-patch

The `gitea-webhook-sync` CronJob (connectivity-link-applicationsets) patches manifests and MCP
`application.properties` on a schedule. Java sources must still be merged manually or by re-scaffolding.
