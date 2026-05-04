# COPO — Production Deployment Guide

> **Course Outcome Process Outcome** — Spring Boot 3.x, Java 17, MySQL 8

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Environment Variables](#environment-variables)
3. [Deploy with Docker Compose (Recommended)](#deploy-with-docker-compose)
4. [Deploy to a Cloud Server (VPS/EC2)](#deploy-to-a-vps--aws-ec2)
5. [Deploy to Railway](#deploy-to-railway)
6. [Deploy to Render](#deploy-to-render)
7. [Database Setup](#database-setup)
8. [Health Check & Monitoring](#health-check--monitoring)
9. [Security Checklist](#security-checklist)

---

## Prerequisites

| Tool       | Version     |
|------------|-------------|
| Java       | 17+         |
| Maven      | 3.9+        |
| Docker     | 24+         |
| Docker Compose | v2+     |
| MySQL      | 8.0         |

---

## Environment Variables

Copy `.env.example` to `.env` and fill in all values before deploying:

```bash
cp .env.example .env
```

| Variable               | Description                              | Example                              |
|------------------------|------------------------------------------|--------------------------------------|
| `SPRING_PROFILES_ACTIVE` | Spring profile to activate             | `prod`                               |
| `PORT`                 | Application port                         | `9091`                               |
| `DATABASE_URL`         | Full JDBC URL to your MySQL instance      | `jdbc:mysql://mysql:3306/copo3?...`  |
| `DB_USERNAME`          | MySQL username                           | `root`                               |
| `DB_PASSWORD`          | MySQL password                           | `YourStrongPass!`                    |
| `ADMIN_USERNAME`       | Spring Security actuator admin user      | `admin`                              |
| `ADMIN_PASSWORD`       | Spring Security actuator admin password  | `Admin@COPO#2025!`                   |
| `LOG_LEVEL`            | Root log level                           | `WARN`                               |

> ⚠️ **Never commit `.env` to Git!** It is already listed in `.gitignore`.

---

## Deploy with Docker Compose

This is the recommended approach for self-hosted/VPS deployments.

```bash
# 1. Clone the repository
git clone <your-repo-url> copo && cd copo/copov2

# 2. Create your .env file
cp .env.example .env
# Edit .env with your real values

# 3. Build and start all services
docker compose --env-file .env up -d --build

# 4. View logs
docker compose logs -f app

# 5. Stop
docker compose down
```

The app will be available at: `http://<server-ip>:9091`

### Database auto-import
On **first run**, Docker will auto-import all SQL files from `SQL Dump/copo3/` into the MySQL container. No manual import needed.

---

## Deploy to a VPS / AWS EC2

```bash
# On your server:
sudo apt-get update && sudo apt-get install -y docker.io docker-compose-plugin

# Copy project files to server (or clone git repo)
scp -r ./copov2 user@<server-ip>:/opt/copo

# SSH in and run
ssh user@<server-ip>
cd /opt/copo
cp .env.example .env && nano .env  # fill in values
docker compose --env-file .env up -d --build
```

**Nginx reverse proxy (recommended):**
```nginx
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://localhost:9091;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60;
        proxy_connect_timeout 60;
    }
}
```

---

## Deploy to Railway

1. Push code to GitHub
2. Go to [railway.app](https://railway.app) → **New Project** → **Deploy from GitHub Repo**
3. Add a **MySQL** plugin to the project
4. Go to your service → **Variables** tab → add all env vars from the `.env` file
5. Set `DATABASE_URL` to Railway's MySQL connection string
6. Set `SPRING_PROFILES_ACTIVE=prod`
7. Railway auto-detects the `Dockerfile` and deploys

---

## Deploy to Render

1. Push code to GitHub
2. Go to [render.com](https://render.com) → **New Web Service** → connect GitHub repo
3. Set **Build Command**: `./mvnw clean package -DskipTests`
4. Set **Start Command**: `java -Dspring.profiles.active=prod -jar target/*.jar`
5. Add all env vars in the **Environment** tab
6. Render provides a PostgreSQL/MySQL add-on, or use PlanetScale/ClearDB

---

## Database Setup

The SQL dump files are in `SQL Dump/copo3/`. Import order matters:

```bash
# Manual import (if not using Docker auto-import)
mysql -u root -p copo3 < "SQL Dump/copo3/copo3_departments.sql"
mysql -u root -p copo3 < "SQL Dump/copo3/copo3_batches.sql"
mysql -u root -p copo3 < "SQL Dump/copo3/copo3_faculty.sql"
mysql -u root -p copo3 < "SQL Dump/copo3/copo3_subjects.sql"
mysql -u root -p copo3 < "SQL Dump/copo3/copo3_students.sql"
mysql -u root -p copo3 < "SQL Dump/copo3/copo3_questions.sql"
mysql -u root -p copo3 < "SQL Dump/copo3/copo3_student_marks.sql"
mysql -u root -p copo3 < "SQL Dump/copo3/copo3_faculty_subjects_handled.sql"
```

---

## Health Check & Monitoring

After deployment, verify the app is running:

```bash
# Health endpoint
curl http://localhost:9091/actuator/health

# Expected response:
# {"status":"UP"}
```

Prometheus metrics (if configured):
```
GET /actuator/metrics
GET /actuator/info
```

---

## Security Checklist

- [x] `.env` is in `.gitignore` — secrets never committed
- [x] Production profile disables SQL logging (`show-sql=false`)
- [x] Actuator endpoints restricted to `health`, `info`, `metrics` only
- [x] Stack traces NOT exposed in HTTP error responses
- [x] Non-root Docker user (`copouser`)
- [x] HikariCP connection pool configured
- [x] Session cookies set to `HttpOnly` and `SameSite=Strict`
- [x] `ddl-auto=validate` in production (schema not modified at runtime)
- [x] `System.out.println` replaced with SLF4J throughout
- [ ] Enable HTTPS / TLS via Nginx reverse proxy or cloud load balancer
- [ ] Change default `DB_PASSWORD` and `ADMIN_PASSWORD` before going live
- [ ] Set up database backups (cron + `mysqldump`)
