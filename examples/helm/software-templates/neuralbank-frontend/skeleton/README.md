# ${{ values.name }}

${{ values.description }}

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | HTML5 / CSS3 / JavaScript |
| Fonts | Google Fonts (Red Hat Display) |
| Icons | Font Awesome |
| Runtime (dev) | Python 3 HTTP server |
| Runtime (prod) | Apache HTTPD (UBI8) |
| Container | `registry.access.redhat.com/ubi8/httpd-24` |
| CI/CD | Tekton Pipelines |
| GitOps | ArgoCD |
| IDE | Red Hat DevSpaces |

## Quick Start (Local Development)

```bash
# Clone the repository
git clone https://gitea-gitea.${{ values.clusterDomain | default('apps.cluster.example.com') }}/ws-${{ values.owner }}/${{ values.name }}.git
cd ${{ values.name }}

# Serve locally
python3 -m http.server 8080

# Open http://localhost:8080 in your browser
```

## Application

Static single-page application for the Neuralbank platform. Provides a credit management UI that consumes the `neuralbank-backend` REST API.

### Features

- Customer listing
- Credit application form
- Credit status dashboard
- Responsive design with Red Hat branding

### Backend Dependency

This frontend consumes the API from:

```
component:default/${{ values.owner }}-neuralbank-backend
```

## Project Structure

```
${{ values.name }}/
├── index.html                        # Main SPA page
├── Dockerfile                        # Production container (httpd)
├── manifests/
│   ├── deployment.yaml               # Kubernetes Deployment
│   ├── service.yaml                  # Kubernetes Service
│   ├── route.yaml                    # OpenShift Route
│   ├── pipeline.yaml                 # Tekton Pipeline
│   ├── pipelinerun.yaml              # Initial PipelineRun
│   ├── tasks.yaml                    # Tekton Tasks
│   ├── event-listener.yaml           # Tekton EventListener
│   ├── trigger-binding.yaml          # Tekton TriggerBinding
│   ├── trigger-template.yaml         # Tekton TriggerTemplate
│   └── trigger-rbac.yaml             # RBAC for triggers
├── argocd/
│   └── application.yaml              # ArgoCD Application
├── catalog-info.yaml                 # Backstage catalog entity
└── devfile.yaml                      # DevSpaces workspace config
```

## Backstage Labels

| Label/Annotation | Value |
|-----------------|-------|
| `backstage.io/kubernetes-id` | `${{ values.name }}` |
| `backstage.io/kubernetes-namespace` | `${{ values.namespace }}` |
| `argocd/app-name` | `${{ values.name }}` |
| `argocd/app-namespace` | `openshift-gitops` |
| `janus-idp.io/tekton` | `${{ values.name }}` |
| `tektonci/build-namespace` | `${{ values.namespace }}` |
| Tags | `frontend`, `html`, `neuralbank` |

## CI/CD Pipeline

The Tekton pipeline runs automatically on git push (via webhook):

1. **git-clone** - Clone source from Gitea
2. **buildah** - Build container image from `Dockerfile`
3. **deploy** - Rollout the new image

## Development with DevSpaces

Open directly in DevSpaces:

```
https://devspaces.${{ values.clusterDomain | default('apps.cluster.example.com') }}/#https://gitea-gitea.${{ values.clusterDomain | default('apps.cluster.example.com') }}/ws-${{ values.owner }}/${{ values.name }}
```

## Owner

- **User:** ${{ values.owner }}
- **Namespace:** ${{ values.namespace }}
- **System:** neuralbank
