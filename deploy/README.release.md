# Backend image release template

## 1) GitHub Actions setup

Workflow file: `.github/workflows/backend-image-release.yml`

Configure repository variables and secrets:

- Optional variable: `GHCR_IMAGE` (default: `ghcr.io/<owner>/chrome-extension-backend`)
- Optional variable: `DOCKERHUB_IMAGE` (example: `yourname/chrome-extension-backend`)
- Optional secret: `DOCKERHUB_USERNAME`
- Optional secret: `DOCKERHUB_TOKEN`

Release trigger options:

- Push a git tag like `v1.4.2`
- Or run `workflow_dispatch` and input `1.4.2`

The workflow only publishes immutable tags like `1.4.2` and `sha-xxxxxxx`.
It does not publish `latest`.

## 2) Server files

Use files in this directory:

- `docker-compose.release.yml`
- `.env` (copy from `.env.example`)
- `deploy-fixed-tag.sh`

## 3) Deploy a fixed version

```bash
cd deploy
cp .env.example .env
chmod +x deploy-fixed-tag.sh
./deploy-fixed-tag.sh 1.4.2
```

The script will:

1. Reject `latest`
2. Pull exactly `IMAGE_REPO:IMAGE_TAG`
3. Restart backend container
4. Check `/api/v1/health`
