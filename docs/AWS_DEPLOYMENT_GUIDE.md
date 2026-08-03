# AWS Deployment Guide — Opus Dispute Hub (Complete System)

This guide covers deploying the **full system** — the Spring Boot backend API, the React+Vite frontend with its proxy layer, PostgreSQL database, and all the networking required to connect them on AWS.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [System Components Summary](#2-system-components-summary)
3. [Prerequisites](#3-prerequisites)
4. [Step 1: Set Up the VPC & Network](#4-step-1-set-up-the-vpc--network)
5. [Step 2: Create the RDS PostgreSQL Database](#5-step-2-create-the-rds-postgresql-database)
6. [Step 3: Store Secrets in AWS Secrets Manager](#6-step-3-store-secrets-in-aws-secrets-manager)
7. [Step 4: Deploy the Backend on EC2](#7-step-4-deploy-the-backend-on-ec2)
8. [Step 5: Set Up the Backend Application Load Balancer (HTTPS)](#8-step-5-set-up-the-backend-application-load-balancer-https)
9. [Step 6: Deploy the Frontend](#9-step-6-deploy-the-frontend)
10. [Step 7: Domain & DNS Setup](#10-step-7-domain--dns-setup)
11. [Step 8: Frontend ↔ Backend Connection](#11-step-8-frontend--backend-connection)
12. [Configuration Files](#12-configuration-files)
13. [Production Hardening Checklist](#13-production-hardening-checklist)
14. [Monitoring & Logging](#14-monitoring--logging)
15. [Backup & Recovery](#15-backup--recovery)
16. [Scaling Considerations](#16-scaling-considerations)
17. [Cost Estimate](#17-cost-estimate)
18. [Troubleshooting](#18-troubleshooting)
19. [Quick Start Summary](#19-quick-start-summary)

---

## 1. Architecture Overview

```
                         ┌──────────────────────────────────────────────────────────┐
                         │                       AWS VPC                            │
                         │                                                          │
                         │  ┌─────────────── Public Subnets ─────────────────┐      │
  Users ──HTTPS──▶       │  │                                                │      │
     app.yourdomain.com  │  │  ┌──────────────────┐  ┌──────────────────┐   │      │
     api.yourdomain.com  │  │  │  ALB (Backend)   │  │  ALB (Frontend)  │   │      │
                         │  │  │  Port 443        │  │  Port 443        │   │      │
                         │  │  └────────┬─────────┘  └────────┬─────────┘   │      │
                         │  │           │                      │             │      │
                         │  └───────────┼──────────────────────┼─────────────┘      │
                         │              │                      │                    │
                         │  ┌───────────┼── Private Subnets ───┼────────────┐       │
                         │  │           │                      │            │       │
                         │  │  ┌────────▼─────────┐  ┌────────▼─────────┐  │       │
                         │  │  │  EC2 (Backend)   │  │  EC2 (Frontend)  │  │       │
                         │  │  │  Spring Boot API │  │  Node.js / Vite  │  │       │
                         │  │  │  Port 5000       │  │  Port 4173       │  │       │
                         │  │  └────────┬─────────┘  └────────┬─────────┘  │       │
                         │  │           │                      │            │       │
                         │  │  ┌────────▼─────────┐            │            │       │
                         │  │  │  RDS PostgreSQL  │◀───────────┘            │       │
                         │  │  │  Port 5432       │  (frontend proxies      │       │
                         │  │  └──────────────────┘   /api/* to backend)    │       │
                         │  │                                               │       │
                         │  └───────────────────────────────────────────────┘       │
                         │              │                                           │
                         │              │ outbound (NAT Gateway)                    │
                         │              ▼                                           │
                         │  External APIs:                                          │
                         │  - Mastercard Mastercom v6                               │
                         │  - Google Gemini AI (gemini-2.5-flash)                    │
                         │  - Stripe                                                │
                         │  - Ethoca                                                │
                         └──────────────────────────────────────────────────────────┘
```

### Alternative: Static Frontend (S3 + CloudFront)

If you strip out the Vite proxy layer and have the frontend call the backend API directly, you can deploy the frontend as static files to S3 + CloudFront (cheaper, simpler). See [Section 9 Option B](#option-b-static-deployment-s3--cloudfront) for this approach.

---

## 2. System Components Summary

### Backend (Spring Boot)

| Property | Value |
|----------|-------|
| Runtime | Java 19 (GraalVM) |
| Framework | Spring Boot |
| Port | 5000 |
| Health check | `GET /api/mastercard/test` |
| Database | PostgreSQL 16 |
| External APIs | Mastercard Mastercom v6, Google Gemini, Stripe, Ethoca |

### Frontend (React + Vite)

| Property | Value |
|----------|-------|
| Runtime | Node.js 20+ |
| Framework | React 18 + Vite 6 + TailwindCSS 4 |
| Port | 4173 (production preview mode) |
| Proxy layer | Vite plugin (`sb-proxy-plugin.ts`) — runs in both dev and preview mode |
| Build output | `artifacts/dispute-dashboard/dist/public/` |

### The Proxy Layer (Critical)

The frontend is **not a simple static SPA**. It includes a Vite server-side proxy plugin (`server/sb-proxy-plugin.ts`) that:

1. **Intercepts all `/api/*` requests** from the browser
2. **Normalizes dispute data** — maps backend statuses (e.g., `REBUTTAL_READY` → `open`, `SECOND_PRESENTMENT_SUBMITTED` → `represented`) to frontend-friendly values
3. **Hydrates enrichment percentages** — fetches completeness scores from evidence maps for enriched disputes
4. **Caches dispute lists** (30-second TTL) to reduce backend load
5. **Implements pagination, sorting, filtering** on the normalized data
6. **Computes aggregate stats** (total disputes, enrichment averages, status counts)
7. **Manages keep-alive for long-running agent calls** — prevents timeouts on enrichment/rebuttal generation (which can take 60–180 seconds)
8. **Proxies all other `/api/*` requests** directly to Spring Boot (agents, evidence maps, file uploads, etc.)
9. **Handles local config** — pipeline automation rules are stored in a local JSON file

**This means**: In production, the frontend must run as a Node.js process (Vite preview mode), NOT as static files served from S3/CloudFront — unless you refactor the proxy logic into a separate service or move it to the backend.

### Key Environment Variables

| Variable | Used By | Description |
|----------|---------|-------------|
| `SPRINGBOOT_URL` | Frontend proxy | Backend API URL (e.g., `https://api.yourdomain.com`) |
| `PORT` | Frontend | Port the Vite server binds to (default: 4173) |
| `BASE_PATH` | Frontend | URL base path (set to `/` for standalone deployment) |
| `PGHOST` | Backend | RDS PostgreSQL endpoint |
| `PGPORT` | Backend | PostgreSQL port (5432) |
| `PGDATABASE` | Backend | Database name |
| `PGUSER` | Backend | Database username |
| `PGPASSWORD` | Backend | Database password |
| `GEMINI_API_KEY` | Backend | Google Gemini API key |
| `STRIPE_SECRET_KEY` | Backend | Stripe secret key |
| `MASTERCARD_CONSUMER_KEY` | Backend | Mastercard API consumer key |
| `MASTERCARD_KEYSTORE_PASSWORD` | Backend | Mastercard .p12 keystore password |
| `ETHOCA_API_KEY_ID` | Backend | Ethoca API key ID |
| `ETHOCA_API_SECRET` | Backend | Ethoca API secret |

---

## 3. Prerequisites

Before starting, ensure you have:

- An AWS account with admin access
- AWS CLI installed and configured (`aws configure`)
- A registered domain name (recommended for HTTPS)
- The project source code (this repository)
- The Mastercard `.p12` signing keystore file
- Node.js 20+ and pnpm installed locally (for building the frontend)
- All API keys ready:
  - Gemini API Key
  - Stripe Secret Key
  - Mastercard consumer key and keystore password
  - Ethoca API key ID and secret (if using Ethoca)

---

## 4. Step 1: Set Up the VPC & Network

### 4.1 Create a VPC

```bash
aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=dispute-mgmt-vpc}]'

# Note the VPC ID from the output (e.g., vpc-0abc123def456)
```

### 4.2 Create Subnets

You need at least 2 subnets in different availability zones (required for ALB and RDS).

```bash
# Public Subnet 1 (for ALBs)
aws ec2 create-subnet \
  --vpc-id vpc-YOUR_VPC_ID \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=dispute-public-1}]'

# Public Subnet 2 (for ALBs - second AZ)
aws ec2 create-subnet \
  --vpc-id vpc-YOUR_VPC_ID \
  --cidr-block 10.0.2.0/24 \
  --availability-zone us-east-1b \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=dispute-public-2}]'

# Private Subnet 1 (for EC2 backend + frontend)
aws ec2 create-subnet \
  --vpc-id vpc-YOUR_VPC_ID \
  --cidr-block 10.0.3.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=dispute-private-1}]'

# Private Subnet 2 (for RDS + HA)
aws ec2 create-subnet \
  --vpc-id vpc-YOUR_VPC_ID \
  --cidr-block 10.0.4.0/24 \
  --availability-zone us-east-1b \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=dispute-private-2}]'
```

### 4.3 Create Internet Gateway (for public subnets)

```bash
aws ec2 create-internet-gateway \
  --tag-specifications 'ResourceType=internet-gateway,Tags=[{Key=Name,Value=dispute-igw}]'

aws ec2 attach-internet-gateway \
  --internet-gateway-id igw-YOUR_IGW_ID \
  --vpc-id vpc-YOUR_VPC_ID
```

### 4.4 Create NAT Gateway (for private subnet internet access)

Both backend and frontend need outbound internet — backend for external APIs, frontend for proxying to backend (if on separate instances).

```bash
aws ec2 allocate-address --domain vpc

aws ec2 create-nat-gateway \
  --subnet-id subnet-YOUR_PUBLIC_SUBNET_1_ID \
  --allocation-id eipalloc-YOUR_EIP_ID \
  --tag-specifications 'ResourceType=natgateway,Tags=[{Key=Name,Value=dispute-nat}]'
```

### 4.5 Configure Route Tables

```bash
# Public route table (routes to Internet Gateway)
aws ec2 create-route-table --vpc-id vpc-YOUR_VPC_ID \
  --tag-specifications 'ResourceType=route-table,Tags=[{Key=Name,Value=dispute-public-rt}]'

aws ec2 create-route \
  --route-table-id rtb-YOUR_PUBLIC_RT_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id igw-YOUR_IGW_ID

aws ec2 associate-route-table --route-table-id rtb-YOUR_PUBLIC_RT_ID --subnet-id subnet-YOUR_PUBLIC_1_ID
aws ec2 associate-route-table --route-table-id rtb-YOUR_PUBLIC_RT_ID --subnet-id subnet-YOUR_PUBLIC_2_ID

# Private route table (routes to NAT Gateway)
aws ec2 create-route-table --vpc-id vpc-YOUR_VPC_ID \
  --tag-specifications 'ResourceType=route-table,Tags=[{Key=Name,Value=dispute-private-rt}]'

aws ec2 create-route \
  --route-table-id rtb-YOUR_PRIVATE_RT_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --nat-gateway-id nat-YOUR_NAT_ID

aws ec2 associate-route-table --route-table-id rtb-YOUR_PRIVATE_RT_ID --subnet-id subnet-YOUR_PRIVATE_1_ID
aws ec2 associate-route-table --route-table-id rtb-YOUR_PRIVATE_RT_ID --subnet-id subnet-YOUR_PRIVATE_2_ID
```

### 4.6 Create Security Groups

```bash
# ALB Security Group — allows HTTPS from anywhere
aws ec2 create-security-group \
  --group-name dispute-alb-sg \
  --description "ALB - allows HTTPS inbound" \
  --vpc-id vpc-YOUR_VPC_ID

aws ec2 authorize-security-group-ingress \
  --group-id sg-YOUR_ALB_SG_ID \
  --protocol tcp --port 443 --cidr 0.0.0.0/0

aws ec2 authorize-security-group-ingress \
  --group-id sg-YOUR_ALB_SG_ID \
  --protocol tcp --port 80 --cidr 0.0.0.0/0

# Backend Security Group — allows traffic from ALB + frontend EC2
aws ec2 create-security-group \
  --group-name dispute-backend-sg \
  --description "Backend - allows port 5000 from ALB and frontend" \
  --vpc-id vpc-YOUR_VPC_ID

aws ec2 authorize-security-group-ingress \
  --group-id sg-YOUR_BACKEND_SG_ID \
  --protocol tcp --port 5000 \
  --source-group sg-YOUR_ALB_SG_ID

# Frontend Security Group — allows traffic from ALB
aws ec2 create-security-group \
  --group-name dispute-frontend-sg \
  --description "Frontend - allows port 4173 from ALB" \
  --vpc-id vpc-YOUR_VPC_ID

aws ec2 authorize-security-group-ingress \
  --group-id sg-YOUR_FRONTEND_SG_ID \
  --protocol tcp --port 4173 \
  --source-group sg-YOUR_ALB_SG_ID

# Allow frontend to reach backend on port 5000 (for proxy)
aws ec2 authorize-security-group-ingress \
  --group-id sg-YOUR_BACKEND_SG_ID \
  --protocol tcp --port 5000 \
  --source-group sg-YOUR_FRONTEND_SG_ID

# SSH access (restrict to your IP, remove after setup)
aws ec2 authorize-security-group-ingress \
  --group-id sg-YOUR_BACKEND_SG_ID \
  --protocol tcp --port 22 --cidr YOUR_IP/32

aws ec2 authorize-security-group-ingress \
  --group-id sg-YOUR_FRONTEND_SG_ID \
  --protocol tcp --port 22 --cidr YOUR_IP/32

# Database Security Group — allows PostgreSQL from backend only
aws ec2 create-security-group \
  --group-name dispute-db-sg \
  --description "RDS - allows port 5432 from backend" \
  --vpc-id vpc-YOUR_VPC_ID

aws ec2 authorize-security-group-ingress \
  --group-id sg-YOUR_DB_SG_ID \
  --protocol tcp --port 5432 \
  --source-group sg-YOUR_BACKEND_SG_ID
```

---

## 5. Step 2: Create the RDS PostgreSQL Database

### 5.1 Create DB Subnet Group

```bash
aws rds create-db-subnet-group \
  --db-subnet-group-name dispute-db-subnets \
  --db-subnet-group-description "Subnets for dispute DB" \
  --subnet-ids subnet-YOUR_PRIVATE_1_ID subnet-YOUR_PRIVATE_2_ID
```

### 5.2 Create the Database

```bash
aws rds create-db-instance \
  --db-instance-identifier dispute-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 16.4 \
  --master-username dispute_admin \
  --master-user-password YOUR_STRONG_DB_PASSWORD \
  --allocated-storage 20 \
  --storage-type gp3 \
  --db-name dispute_db \
  --vpc-security-group-ids sg-YOUR_DB_SG_ID \
  --db-subnet-group-name dispute-db-subnets \
  --backup-retention-period 7 \
  --storage-encrypted \
  --no-publicly-accessible \
  --tags Key=Name,Value=dispute-db
```

Wait for the instance to become available:

```bash
aws rds wait db-instance-available --db-instance-identifier dispute-db
```

Get the endpoint:

```bash
aws rds describe-db-instances \
  --db-instance-identifier dispute-db \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text
```

Note this endpoint — you'll need it as `PGHOST`.

---

## 6. Step 3: Store Secrets in AWS Secrets Manager

```bash
aws secretsmanager create-secret \
  --name dispute-mgmt/app-secrets \
  --description "Dispute Management System secrets" \
  --secret-string '{
    "PGHOST": "YOUR_RDS_ENDPOINT",
    "PGPORT": "5432",
    "PGDATABASE": "dispute_db",
    "PGUSER": "dispute_admin",
    "PGPASSWORD": "YOUR_STRONG_DB_PASSWORD",
    "GEMINI_API_KEY": "YOUR_GEMINI_API_KEY",
    "STRIPE_SECRET_KEY": "YOUR_STRIPE_SECRET_KEY",
    "MASTERCARD_CONSUMER_KEY": "YOUR_MASTERCARD_CONSUMER_KEY",
    "MASTERCARD_KEYSTORE_PASSWORD": "YOUR_KEYSTORE_PASSWORD",
    "ETHOCA_API_KEY_ID": "YOUR_ETHOCA_KEY_ID",
    "ETHOCA_API_SECRET": "YOUR_ETHOCA_SECRET"
  }'
```

### Upload the Mastercard .p12 keystore to S3

```bash
aws s3 mb s3://dispute-mgmt-config-YOUR_ACCOUNT_ID --region us-east-1

aws s3 cp "Opus Dispute Management System-sandbox-signing.p12" \
  s3://dispute-mgmt-config-YOUR_ACCOUNT_ID/keystore/signing.p12 \
  --sse AES256
```

---

## 7. Step 4: Deploy the Backend on EC2

### 7.1 Create an IAM Role for EC2

```bash
aws iam create-role \
  --role-name dispute-backend-role \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ec2.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }'

aws iam put-role-policy \
  --role-name dispute-backend-role \
  --policy-name dispute-secrets-access \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": ["secretsmanager:GetSecretValue"],
        "Resource": "arn:aws:secretsmanager:us-east-1:YOUR_ACCOUNT_ID:secret:dispute-mgmt/app-secrets*"
      },
      {
        "Effect": "Allow",
        "Action": ["s3:GetObject"],
        "Resource": "arn:aws:s3:::dispute-mgmt-config-YOUR_ACCOUNT_ID/keystore/*"
      }
    ]
  }'

aws iam create-instance-profile --instance-profile-name dispute-backend-profile
aws iam add-role-to-instance-profile \
  --instance-profile-name dispute-backend-profile \
  --role-name dispute-backend-role
```

### 7.2 Launch the EC2 Instance

```bash
aws ec2 run-instances \
  --image-id ami-0c7217cdde317cfec \
  --instance-type t3.medium \
  --key-name YOUR_KEY_PAIR_NAME \
  --security-group-ids sg-YOUR_BACKEND_SG_ID \
  --subnet-id subnet-YOUR_PRIVATE_1_ID \
  --iam-instance-profile Name=dispute-backend-profile \
  --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":30,"VolumeType":"gp3"}}]' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=dispute-backend}]' \
  --user-data file://ec2-backend-startup.sh
```

### 7.3 EC2 Backend Startup Script (`ec2-backend-startup.sh`)

```bash
#!/bin/bash
set -e

LOG_FILE="/var/log/dispute-setup.log"
exec > >(tee -a "$LOG_FILE") 2>&1
echo "=== Backend setup started at $(date) ==="

# --- 1. Install Java 19 ---
echo "Installing Java 19..."
sudo apt-get update -y
sudo apt-get install -y wget unzip jq

wget -q https://download.oracle.com/graalvm/19/latest/graalvm-jdk-19_linux-x64_bin.tar.gz
sudo mkdir -p /opt/java
sudo tar -xzf graalvm-jdk-19_linux-x64_bin.tar.gz -C /opt/java --strip-components=1
rm graalvm-jdk-19_linux-x64_bin.tar.gz

echo 'export JAVA_HOME=/opt/java' | sudo tee /etc/profile.d/java.sh
echo 'export PATH=$JAVA_HOME/bin:$PATH' | sudo tee -a /etc/profile.d/java.sh
source /etc/profile.d/java.sh

java -version

# --- 2. Install AWS CLI (if not present) ---
if ! command -v aws &> /dev/null; then
  echo "Installing AWS CLI..."
  curl -s "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
  unzip -q awscliv2.zip
  sudo ./aws/install
  rm -rf aws awscliv2.zip
fi

# --- 3. Fetch secrets ---
echo "Fetching secrets from Secrets Manager..."
SECRETS=$(aws secretsmanager get-secret-value \
  --secret-id dispute-mgmt/app-secrets \
  --query SecretString --output text \
  --region us-east-1)

export PGHOST=$(echo $SECRETS | jq -r '.PGHOST')
export PGPORT=$(echo $SECRETS | jq -r '.PGPORT')
export PGDATABASE=$(echo $SECRETS | jq -r '.PGDATABASE')
export PGUSER=$(echo $SECRETS | jq -r '.PGUSER')
export PGPASSWORD=$(echo $SECRETS | jq -r '.PGPASSWORD')
export GEMINI_API_KEY=$(echo $SECRETS | jq -r '.GEMINI_API_KEY')
export STRIPE_SECRET_KEY=$(echo $SECRETS | jq -r '.STRIPE_SECRET_KEY')
export MASTERCARD_CONSUMER_KEY=$(echo $SECRETS | jq -r '.MASTERCARD_CONSUMER_KEY')
export MASTERCARD_KEYSTORE_PASSWORD=$(echo $SECRETS | jq -r '.MASTERCARD_KEYSTORE_PASSWORD')
export ETHOCA_API_KEY_ID=$(echo $SECRETS | jq -r '.ETHOCA_API_KEY_ID')
export ETHOCA_API_SECRET=$(echo $SECRETS | jq -r '.ETHOCA_API_SECRET')

echo "Secrets loaded."

# --- 4. Download keystore from S3 ---
echo "Downloading Mastercard keystore..."
APP_DIR="/opt/dispute-mgmt"
sudo mkdir -p $APP_DIR/config
aws s3 cp s3://dispute-mgmt-config-YOUR_ACCOUNT_ID/keystore/signing.p12 \
  $APP_DIR/config/signing.p12 --region us-east-1

# --- 5. Deploy the application ---
echo "Setting up application directory..."
sudo mkdir -p $APP_DIR/data/sources/issuer
sudo mkdir -p $APP_DIR/data/sources/acquirer/{customer-comms,device,fraud-tools,identity,merchant,psp,shipping}
sudo mkdir -p $APP_DIR/logs

echo "Setup complete at $(date)"
echo "Upload the JAR to $APP_DIR/app.jar and start the service."
```

### 7.4 Build and Upload the JAR

On your local machine (or in a CI/CD pipeline):

```bash
./mvnw clean package -DskipTests

scp target/mastercard-dispute-management-0.0.1-SNAPSHOT.jar \
  ec2-user@YOUR_EC2_IP:/opt/dispute-mgmt/app.jar

scp -r src/data/sources/ ec2-user@YOUR_EC2_IP:/opt/dispute-mgmt/data/sources/
scp src/data/reason-code-rules.json ec2-user@YOUR_EC2_IP:/opt/dispute-mgmt/data/
```

### 7.5 Create the Production Properties File

SSH into the EC2 instance and create `/opt/dispute-mgmt/application-prod.properties`:

```properties
spring.application.name=mastercard-dispute-management

# Database
spring.datasource.url=jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
spring.datasource.username=${PGUSER}
spring.datasource.password=${PGPASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Connection pool
spring.datasource.hikari.maximum-pool-size=15
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000

# Server
server.port=5000

# CORS — allow frontend origin (update after DNS setup)
# This is configured in WebConfig.java — see Section 11

# Mastercard API
mastercard.base-url=https://sandbox.api.mastercard.com
mastercard.consumer-key=${MASTERCARD_CONSUMER_KEY}
mastercard.keystore-password=${MASTERCARD_KEYSTORE_PASSWORD}
mastercard.keystore-path=/opt/dispute-mgmt/config/signing.p12

# Gemini AI
gemini.api-key=${GEMINI_API_KEY}

# Stripe
stripe.secret-key=${STRIPE_SECRET_KEY}
stripe.base-url=https://api.stripe.com/v1

# Ethoca
ethoca.base-url=https://sandbox.api.ethocaweb.com
ethoca.api-key-id=${ETHOCA_API_KEY_ID}
ethoca.api-secret=${ETHOCA_API_SECRET}

# Ingestion
claim-detail.local-fallback.enabled=true
ingestion.scheduled.enabled=false
ingestion.scheduled.interval-ms=3600000
ingestion.scheduled.initial-delay-ms=60000
ingestion.max-new-claims=10

# Urgency (backend defaults: buffer=0, critical=2, warning=5)
urgency.buffer-days=0
urgency.critical-days=2
urgency.warning-days=5

# File uploads
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB

# Logging
logging.level.root=INFO
logging.level.com.opus.dispute.management=INFO
logging.file.name=/opt/dispute-mgmt/logs/application.log
logging.logback.rollingpolicy.max-file-size=50MB
logging.logback.rollingpolicy.max-history=30

# Compression
server.compression.enabled=true
server.compression.mime-types=application/json,text/html,text/plain
server.compression.min-response-size=1024
```

### 7.6 Create a systemd Service

SSH into the EC2 and create `/etc/systemd/system/dispute-mgmt.service`:

```ini
[Unit]
Description=Mastercard Dispute Management System — Backend
After=network.target

[Service]
Type=simple
User=ubuntu
Group=ubuntu
WorkingDirectory=/opt/dispute-mgmt

ExecStartPre=/bin/bash -c '\
  SECRETS=$(aws secretsmanager get-secret-value \
    --secret-id dispute-mgmt/app-secrets \
    --query SecretString --output text \
    --region us-east-1) && \
  echo "PGHOST=$(echo $SECRETS | jq -r .PGHOST)" > /opt/dispute-mgmt/.env && \
  echo "PGPORT=$(echo $SECRETS | jq -r .PGPORT)" >> /opt/dispute-mgmt/.env && \
  echo "PGDATABASE=$(echo $SECRETS | jq -r .PGDATABASE)" >> /opt/dispute-mgmt/.env && \
  echo "PGUSER=$(echo $SECRETS | jq -r .PGUSER)" >> /opt/dispute-mgmt/.env && \
  echo "PGPASSWORD=$(echo $SECRETS | jq -r .PGPASSWORD)" >> /opt/dispute-mgmt/.env && \
  echo "GEMINI_API_KEY=$(echo $SECRETS | jq -r .GEMINI_API_KEY)" >> /opt/dispute-mgmt/.env && \
  echo "STRIPE_SECRET_KEY=$(echo $SECRETS | jq -r .STRIPE_SECRET_KEY)" >> /opt/dispute-mgmt/.env && \
  echo "MASTERCARD_CONSUMER_KEY=$(echo $SECRETS | jq -r .MASTERCARD_CONSUMER_KEY)" >> /opt/dispute-mgmt/.env && \
  echo "MASTERCARD_KEYSTORE_PASSWORD=$(echo $SECRETS | jq -r .MASTERCARD_KEYSTORE_PASSWORD)" >> /opt/dispute-mgmt/.env && \
  echo "ETHOCA_API_KEY_ID=$(echo $SECRETS | jq -r .ETHOCA_API_KEY_ID)" >> /opt/dispute-mgmt/.env && \
  echo "ETHOCA_API_SECRET=$(echo $SECRETS | jq -r .ETHOCA_API_SECRET)" >> /opt/dispute-mgmt/.env && \
  chmod 600 /opt/dispute-mgmt/.env'

EnvironmentFile=/opt/dispute-mgmt/.env

ExecStart=/opt/java/bin/java \
  -Xms512m -Xmx1536m \
  -Dspring.profiles.active=prod \
  -Dspring.config.additional-location=file:/opt/dispute-mgmt/application-prod.properties \
  -jar /opt/dispute-mgmt/app.jar

Restart=always
RestartSec=10
StandardOutput=append:/opt/dispute-mgmt/logs/stdout.log
StandardError=append:/opt/dispute-mgmt/logs/stderr.log

NoNewPrivileges=true
ProtectSystem=strict
ReadWritePaths=/opt/dispute-mgmt

[Install]
WantedBy=multi-user.target
```

### 7.7 Start the Service

```bash
sudo chown -R ubuntu:ubuntu /opt/dispute-mgmt
sudo systemctl daemon-reload
sudo systemctl enable dispute-mgmt
sudo systemctl start dispute-mgmt

# Verify
sudo systemctl status dispute-mgmt
curl -s http://localhost:5000/api/mastercard/test
```

---

## 8. Step 5: Set Up the Backend Application Load Balancer (HTTPS)

### 8.1 Request an SSL Certificate

```bash
aws acm request-certificate \
  --domain-name api.yourdomain.com \
  --validation-method DNS \
  --region us-east-1
```

### 8.2 Create the ALB

```bash
aws elbv2 create-load-balancer \
  --name dispute-backend-alb \
  --subnets subnet-YOUR_PUBLIC_1_ID subnet-YOUR_PUBLIC_2_ID \
  --security-groups sg-YOUR_ALB_SG_ID \
  --scheme internet-facing \
  --type application

aws elbv2 create-target-group \
  --name dispute-backend-tg \
  --protocol HTTP \
  --port 5000 \
  --vpc-id vpc-YOUR_VPC_ID \
  --target-type instance \
  --health-check-path /api/mastercard/test \
  --health-check-interval-seconds 30 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3

aws elbv2 register-targets \
  --target-group-arn arn:aws:elasticloadbalancing:...:targetgroup/dispute-backend-tg/... \
  --targets Id=i-YOUR_BACKEND_EC2_ID

aws elbv2 create-listener \
  --load-balancer-arn arn:aws:elasticloadbalancing:...:loadbalancer/app/dispute-backend-alb/... \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=arn:aws:acm:...:certificate/YOUR_CERT_ID \
  --default-actions Type=forward,TargetGroupArn=arn:aws:elasticloadbalancing:...:targetgroup/dispute-backend-tg/...

aws elbv2 create-listener \
  --load-balancer-arn arn:aws:elasticloadbalancing:...:loadbalancer/app/dispute-backend-alb/... \
  --protocol HTTP \
  --port 80 \
  --default-actions Type=redirect,RedirectConfig='{Protocol=HTTPS,Port=443,StatusCode=HTTP_301}'
```

Your backend API will now be accessible at `https://api.yourdomain.com`.

**Verify:**

```bash
curl -s https://api.yourdomain.com/api/mastercard/test
curl -s https://api.yourdomain.com/api/disputes | jq length
```

---

## 9. Step 6: Deploy the Frontend

### Option A: EC2 with Vite Preview (Recommended)

This is the recommended approach because it preserves the full proxy layer — status normalization, enrichment hydration, caching, keep-alive for long agent calls, and local pipeline config.

#### 9A.1 Build the Frontend Locally

```bash
# From the project root
cd artifacts/dispute-dashboard

# Install dependencies
pnpm install

# Build (produces dist/public/ with static assets)
PORT=4173 BASE_PATH="/" pnpm run build
```

#### 9A.2 Prepare the Frontend Deployment Package

The frontend needs the built assets PLUS the server-side proxy code:

```bash
# Create deployment package
mkdir -p /tmp/dispute-frontend-deploy

# Copy the build output
cp -r artifacts/dispute-dashboard/dist /tmp/dispute-frontend-deploy/

# Copy the server proxy plugin (needed at runtime for vite preview)
cp -r artifacts/dispute-dashboard/server /tmp/dispute-frontend-deploy/

# Copy vite config (needed for vite preview)
cp artifacts/dispute-dashboard/vite.config.ts /tmp/dispute-frontend-deploy/

# Copy package.json and lockfile
cp artifacts/dispute-dashboard/package.json /tmp/dispute-frontend-deploy/
cp pnpm-lock.yaml /tmp/dispute-frontend-deploy/

# Copy tsconfig files
cp artifacts/dispute-dashboard/tsconfig.json /tmp/dispute-frontend-deploy/
cp artifacts/dispute-dashboard/tsconfig.node.json /tmp/dispute-frontend-deploy/ 2>/dev/null || true

# Create the pipeline-config.json (default empty config)
echo '{"enabled":false,"rules":[]}' > /tmp/dispute-frontend-deploy/pipeline-config.json

# Create a tar
cd /tmp && tar czf dispute-frontend.tar.gz dispute-frontend-deploy/
```

#### 9A.3 Launch the Frontend EC2 Instance

```bash
aws ec2 run-instances \
  --image-id ami-0c7217cdde317cfec \
  --instance-type t3.small \
  --key-name YOUR_KEY_PAIR_NAME \
  --security-group-ids sg-YOUR_FRONTEND_SG_ID \
  --subnet-id subnet-YOUR_PRIVATE_1_ID \
  --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":20,"VolumeType":"gp3"}}]' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=dispute-frontend}]' \
  --user-data file://ec2-frontend-startup.sh
```

#### 9A.4 EC2 Frontend Startup Script (`ec2-frontend-startup.sh`)

```bash
#!/bin/bash
set -e

LOG_FILE="/var/log/dispute-frontend-setup.log"
exec > >(tee -a "$LOG_FILE") 2>&1
echo "=== Frontend setup started at $(date) ==="

# --- 1. Install Node.js 20 ---
echo "Installing Node.js 20..."
sudo apt-get update -y
sudo apt-get install -y curl
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# --- 2. Install pnpm ---
echo "Installing pnpm..."
sudo npm install -g pnpm@9

# --- 3. Set up application directory ---
APP_DIR="/opt/dispute-frontend"
sudo mkdir -p $APP_DIR
sudo chown -R ubuntu:ubuntu $APP_DIR

echo "Frontend setup complete at $(date)"
echo "Upload the deployment package and start the service."
```

#### 9A.5 Upload and Install

```bash
# Upload the deployment package
scp /tmp/dispute-frontend.tar.gz ubuntu@YOUR_FRONTEND_EC2_IP:/opt/dispute-frontend/

# SSH into the frontend EC2
ssh ubuntu@YOUR_FRONTEND_EC2_IP

# Extract
cd /opt/dispute-frontend
tar xzf dispute-frontend.tar.gz --strip-components=1
rm dispute-frontend.tar.gz

# Install production dependencies
pnpm install --prod=false
```

#### 9A.6 Modify vite.config.ts for Standalone AWS Deployment

On the frontend EC2, edit `/opt/dispute-frontend/vite.config.ts` to remove Replit-specific plugins:

```typescript
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "path";
import { sbProxyPlugin } from "./server/sb-proxy-plugin";

const port = Number(process.env.PORT || "4173");
const basePath = process.env.BASE_PATH || "/";

export default defineConfig({
  base: basePath,
  plugins: [
    sbProxyPlugin(),
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      "@": path.resolve(import.meta.dirname, "src"),
    },
    dedupe: ["react", "react-dom"],
  },
  root: path.resolve(import.meta.dirname),
  build: {
    outDir: path.resolve(import.meta.dirname, "dist/public"),
    emptyOutDir: true,
  },
  server: {
    port,
    host: "0.0.0.0",
    allowedHosts: true,
  },
  preview: {
    port,
    host: "0.0.0.0",
    allowedHosts: true,
  },
});
```

#### 9A.7 Create a systemd Service for the Frontend

Create `/etc/systemd/system/dispute-frontend.service`:

```ini
[Unit]
Description=Opus Dispute Hub — Frontend (Vite Preview + Proxy)
After=network.target

[Service]
Type=simple
User=ubuntu
Group=ubuntu
WorkingDirectory=/opt/dispute-frontend

Environment=NODE_ENV=production
Environment=PORT=4173
Environment=BASE_PATH=/
Environment=SPRINGBOOT_URL=http://BACKEND_EC2_PRIVATE_IP:5000

ExecStart=/usr/bin/npx vite preview --config vite.config.ts --host 0.0.0.0

Restart=always
RestartSec=5
StandardOutput=append:/opt/dispute-frontend/logs/stdout.log
StandardError=append:/opt/dispute-frontend/logs/stderr.log

NoNewPrivileges=true

[Install]
WantedBy=multi-user.target
```

> **IMPORTANT**: Set `SPRINGBOOT_URL` to the backend's **private IP** within the VPC (e.g., `http://10.0.3.15:5000`). Since both EC2s are in the same VPC, the frontend proxy can reach the backend directly without going through the ALB. This is more efficient and avoids double TLS termination.
>
> Alternatively, use the backend ALB domain: `SPRINGBOOT_URL=https://api.yourdomain.com`

#### 9A.8 Start the Frontend Service

```bash
sudo mkdir -p /opt/dispute-frontend/logs
sudo chown -R ubuntu:ubuntu /opt/dispute-frontend

sudo systemctl daemon-reload
sudo systemctl enable dispute-frontend
sudo systemctl start dispute-frontend

# Verify
curl -s http://localhost:4173/ | head -20
curl -s http://localhost:4173/api/disputes | jq '.total'
```

#### 9A.9 Set Up the Frontend ALB

```bash
# Request SSL certificate for the frontend domain
aws acm request-certificate \
  --domain-name app.yourdomain.com \
  --validation-method DNS \
  --region us-east-1

# Create ALB for frontend
aws elbv2 create-load-balancer \
  --name dispute-frontend-alb \
  --subnets subnet-YOUR_PUBLIC_1_ID subnet-YOUR_PUBLIC_2_ID \
  --security-groups sg-YOUR_ALB_SG_ID \
  --scheme internet-facing \
  --type application

# Create target group
aws elbv2 create-target-group \
  --name dispute-frontend-tg \
  --protocol HTTP \
  --port 4173 \
  --vpc-id vpc-YOUR_VPC_ID \
  --target-type instance \
  --health-check-path / \
  --health-check-interval-seconds 30

# Register frontend EC2
aws elbv2 register-targets \
  --target-group-arn arn:aws:elasticloadbalancing:...:targetgroup/dispute-frontend-tg/... \
  --targets Id=i-YOUR_FRONTEND_EC2_ID

# Create HTTPS listener
aws elbv2 create-listener \
  --load-balancer-arn arn:aws:elasticloadbalancing:...:loadbalancer/app/dispute-frontend-alb/... \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=arn:aws:acm:...:certificate/YOUR_FRONTEND_CERT_ID \
  --default-actions Type=forward,TargetGroupArn=arn:aws:elasticloadbalancing:...:targetgroup/dispute-frontend-tg/...

# HTTP → HTTPS redirect
aws elbv2 create-listener \
  --load-balancer-arn arn:aws:elasticloadbalancing:...:loadbalancer/app/dispute-frontend-alb/... \
  --protocol HTTP \
  --port 80 \
  --default-actions Type=redirect,RedirectConfig='{Protocol=HTTPS,Port=443,StatusCode=HTTP_301}'
```

#### 9A.10 Configure ALB Idle Timeout

Agent calls (enrichment, rebuttal generation) can take 60–180 seconds. Increase the ALB idle timeout:

```bash
aws elbv2 modify-load-balancer-attributes \
  --load-balancer-arn arn:aws:elasticloadbalancing:...:loadbalancer/app/dispute-frontend-alb/... \
  --attributes Key=idle_timeout.timeout_seconds,Value=300

# Do the same for the backend ALB
aws elbv2 modify-load-balancer-attributes \
  --load-balancer-arn arn:aws:elasticloadbalancing:...:loadbalancer/app/dispute-backend-alb/... \
  --attributes Key=idle_timeout.timeout_seconds,Value=300
```

---

### Option B: Static Deployment (S3 + CloudFront)

Use this approach **only if** you refactor the proxy logic out of the frontend. This means:

1. The frontend calls the backend API directly (no proxy)
2. You lose: status normalization, enrichment hydration, caching, pagination server-side, keep-alive for long agent calls, local pipeline config
3. You would need to either:
   - Move the proxy logic into the Spring Boot backend (add a new controller)
   - Create a lightweight Express/Fastify middleware service
   - Accept the raw backend data format in the frontend

If you go this route:

#### 9B.1 Refactor Frontend API Calls

Change `enrichment-api.ts` to call the backend directly:

```typescript
// Instead of:
const BASE = import.meta.env.BASE_URL || "/";
function apiUrl(path: string): string {
  return `${BASE}api/enrichment${path}`;
}

// Change to:
const API_BASE = import.meta.env.VITE_API_URL || "https://api.yourdomain.com";
function apiUrl(path: string): string {
  return `${API_BASE}/api/enrichment${path}`;
}
```

#### 9B.2 Build and Upload to S3

```bash
# Build with API URL set
VITE_API_URL=https://api.yourdomain.com PORT=4173 BASE_PATH="/" pnpm run build

# Create S3 bucket
aws s3 mb s3://dispute-mgmt-frontend-YOUR_ACCOUNT_ID --region us-east-1

aws s3 website s3://dispute-mgmt-frontend-YOUR_ACCOUNT_ID \
  --index-document index.html \
  --error-document index.html

# Upload — cache static assets aggressively, never cache index.html
aws s3 sync dist/public/ s3://dispute-mgmt-frontend-YOUR_ACCOUNT_ID/ \
  --delete \
  --cache-control "max-age=31536000" \
  --exclude "index.html"

aws s3 cp dist/public/index.html s3://dispute-mgmt-frontend-YOUR_ACCOUNT_ID/index.html \
  --cache-control "no-cache"
```

#### 9B.3 Create CloudFront Distribution

```bash
aws cloudfront create-distribution \
  --distribution-config '{
    "CallerReference": "dispute-frontend-v1",
    "Comment": "Opus Dispute Hub Frontend",
    "DefaultCacheBehavior": {
      "TargetOriginId": "S3-dispute-frontend",
      "ViewerProtocolPolicy": "redirect-to-https",
      "AllowedMethods": {"Quantity": 2, "Items": ["GET", "HEAD"]},
      "ForwardedValues": {"QueryString": false, "Cookies": {"Forward": "none"}},
      "MinTTL": 0, "DefaultTTL": 86400, "MaxTTL": 31536000
    },
    "Origins": {
      "Quantity": 1,
      "Items": [{
        "Id": "S3-dispute-frontend",
        "DomainName": "dispute-mgmt-frontend-YOUR_ACCOUNT_ID.s3.amazonaws.com",
        "S3OriginConfig": {"OriginAccessIdentity": ""}
      }]
    },
    "Enabled": true,
    "DefaultRootObject": "index.html",
    "CustomErrorResponses": {
      "Quantity": 1,
      "Items": [{
        "ErrorCode": 404,
        "ResponsePagePath": "/index.html",
        "ResponseCode": "200",
        "ErrorCachingMinTTL": 300
      }]
    }
  }'
```

---

## 10. Step 7: Domain & DNS Setup

### 10.1 Route 53 DNS Records

```bash
# API subdomain → Backend ALB
aws route53 change-resource-record-sets \
  --hosted-zone-id YOUR_HOSTED_ZONE_ID \
  --change-batch '{
    "Changes": [{
      "Action": "CREATE",
      "ResourceRecordSet": {
        "Name": "api.yourdomain.com",
        "Type": "A",
        "AliasTarget": {
          "HostedZoneId": "Z35SXDOTRQ7X7K",
          "DNSName": "dispute-backend-alb-XXXXX.us-east-1.elb.amazonaws.com",
          "EvaluateTargetHealth": true
        }
      }
    }]
  }'

# Frontend subdomain → Frontend ALB (Option A) or CloudFront (Option B)
aws route53 change-resource-record-sets \
  --hosted-zone-id YOUR_HOSTED_ZONE_ID \
  --change-batch '{
    "Changes": [{
      "Action": "CREATE",
      "ResourceRecordSet": {
        "Name": "app.yourdomain.com",
        "Type": "A",
        "AliasTarget": {
          "HostedZoneId": "Z35SXDOTRQ7X7K",
          "DNSName": "dispute-frontend-alb-XXXXX.us-east-1.elb.amazonaws.com",
          "EvaluateTargetHealth": true
        }
      }
    }]
  }'
```

---

## 11. Step 8: Frontend ↔ Backend Connection

This is the most critical section — ensuring the frontend proxy correctly reaches the backend.

### 11.1 Connection Flow

```
User Browser
    │
    │  HTTPS (port 443)
    ▼
Frontend ALB (app.yourdomain.com)
    │
    │  HTTP (port 4173)
    ▼
Frontend EC2 (Vite Preview Server)
    │
    ├── Static assets (JS/CSS/HTML) → served from dist/public/
    │
    ├── /api/disputes (GET) → Proxy fetches from backend, normalizes, caches, paginates
    ├── /api/disputes/stats (GET) → Proxy computes aggregates from cached data
    ├── /api/disputes/:id (GET) → Proxy returns from cache
    ├── /api/config/pipeline (GET/PUT) → Proxy reads/writes local pipeline-config.json
    │
    └── All other /api/* → Proxy forwards directly to backend
            │
            │  HTTP (port 5000) via VPC private IP
            ▼
        Backend EC2 (Spring Boot)
            │
            ├── /api/disputes/* → CRUD operations
            ├── /api/agents/* → AI agent pipeline (enrichment, rebuttal, etc.)
            ├── /api/mastercard/* → Mastercard Mastercom API
            └── /api/evidence/* → Evidence file management
```

### 11.2 The `SPRINGBOOT_URL` Environment Variable

This is the single most important configuration for connecting frontend to backend.

In the frontend's systemd service (`dispute-frontend.service`), set:

```ini
# Option 1: Direct VPC connection (recommended — lower latency, no extra TLS)
Environment=SPRINGBOOT_URL=http://10.0.3.XX:5000

# Option 2: Through backend ALB (use if backend is behind multiple instances)
Environment=SPRINGBOOT_URL=https://api.yourdomain.com
```

The proxy plugin reads this on startup:

```typescript
const SB_URL = process.env.SPRINGBOOT_URL || "https://default-backend-url";
```

### 11.3 Backend CORS Configuration

Update `WebConfig.java` in the Spring Boot backend to allow the frontend origin:

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
            .allowedOrigins(
                "https://app.yourdomain.com",
                "http://localhost:3000",
                "http://localhost:4173"
            )
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*");
}
```

> **Note**: If the frontend proxy reaches the backend via VPC private IP (Option 1 above), CORS is less critical since the browser never directly calls the backend. But it's still good practice to configure it for debugging scenarios where you bypass the proxy.

### 11.4 Status Mapping Reference

The proxy normalizes backend statuses to frontend-friendly values:

| Backend Status | Frontend Status | Tab |
|---|---|---|
| `NEW` | `open` | Opened |
| `INITIATED` | `open` | Opened |
| `OPEN` | `open` | Opened |
| `ENRICHED` | `open` | Opened |
| `REBUTTAL_READY` | `open` | Opened |
| `scheduled` / `SCHEDULED` | `scheduled` | All |
| `REPRESENTED` | `represented` | Represented |
| `SECOND_PRESENTMENT_SUBMITTED` | `represented` | Represented |
| `NOT_REPRESENTED` | `not_represented` | Not Represented |
| `ACCEPTED` | `not_represented` | Not Represented |
| `MISSING_DATA` | `missing_data` | All |

### 11.5 Proxy Caching Behavior

- **Cache TTL**: 30 seconds for the dispute list
- **Cache invalidation**: Automatically cleared on any `POST`, `PATCH`, or `DELETE` to `/api/disputes/*`, `/api/agents/*`, or `/api/disputes/:id/accept|undo-accept|second-presentment`
- **Enrichment hydration**: For disputes with `ENRICHED`/`REPRESENTED`/etc. status but no enrichment percentage, the proxy fetches `GET /api/agents/evidence-map/:id` and extracts `completenessScore` from the annotated map

### 11.6 Agent Keep-Alive

Agent calls (enrichment, rebuttal generation, reassessment) can take 60–180 seconds. The proxy sends keep-alive heartbeats every 10 seconds to prevent ALB/browser timeouts:

```
Content-Type: application/x-ndjson
Transfer-Encoding: chunked

{"keepAlive":true}
{"keepAlive":true}
{"keepAlive":true}
{...actual response...}
```

The frontend client strips keep-alive lines and processes only the final response.

### 11.7 Verifying the Connection

After both services are running:

```bash
# 1. Verify backend is healthy
curl -s https://api.yourdomain.com/api/mastercard/test

# 2. Verify frontend serves the SPA
curl -s https://app.yourdomain.com/ | grep "Opus Dispute Hub"

# 3. Verify frontend proxy reaches backend
curl -s https://app.yourdomain.com/api/disputes | jq '.total'

# 4. Verify stats endpoint
curl -s https://app.yourdomain.com/api/disputes/stats | jq

# 5. Verify agent endpoint (GET evidence map)
curl -s https://app.yourdomain.com/api/agents/evidence-map/1 | jq '.enrichmentStatus'
```

### 11.8 Common Connection Issues

| Symptom | Cause | Fix |
|---|---|---|
| Frontend loads but API calls fail with 502 | `SPRINGBOOT_URL` is wrong or backend is down | Check `SPRINGBOOT_URL` env var; verify backend with `curl http://BACKEND_IP:5000/api/mastercard/test` |
| Frontend loads but shows 0 disputes | Proxy can't reach backend (network/security group) | Ensure frontend security group can reach backend on port 5000 |
| Agent calls timeout after 60s | ALB idle timeout too low | Increase ALB idle timeout to 300s (see Section 9A.10) |
| CORS errors in browser console | Browser directly calling backend (proxy bypass) | Ensure all API calls go through the frontend proxy, not directly to `api.yourdomain.com` |
| Enrichment shows 0% for enriched disputes | Evidence map endpoint failing | Check `curl https://app.yourdomain.com/api/agents/evidence-map/:id` |
| "Backend server is temporarily unavailable" | Backend returned 502/503/504 | Check backend health; may need to restart the Spring Boot service |

---

## 12. Configuration Files

### 12.1 Backend Dockerfile (Alternative to EC2)

If you prefer container deployment (ECS Fargate):

```dockerfile
FROM maven:3.9-eclipse-temurin-19 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:19-jre
WORKDIR /app
COPY --from=build /app/target/mastercard-dispute-management-0.0.1-SNAPSHOT.jar app.jar
COPY src/data ./data
RUN mkdir -p /app/logs /app/config \
    /app/data/sources/acquirer/{customer-comms,device,fraud-tools,identity,merchant,psp,shipping} \
    /app/data/sources/issuer/{customer-comms,device,fraud-tools,identity,merchant,psp,shipping}
EXPOSE 5000
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:5000/api/mastercard/test || exit 1
ENTRYPOINT ["java", "-Xms512m", "-Xmx1536m", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
```

### 12.2 Frontend Dockerfile

```dockerfile
FROM node:20-slim AS build
WORKDIR /app
RUN npm install -g pnpm@9

COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN PORT=4173 BASE_PATH="/" pnpm run build

FROM node:20-slim
WORKDIR /app
RUN npm install -g pnpm@9

COPY --from=build /app/dist ./dist
COPY --from=build /app/server ./server
COPY --from=build /app/vite.config.ts ./
COPY --from=build /app/package.json ./
COPY --from=build /app/node_modules ./node_modules

RUN echo '{"enabled":false,"rules":[]}' > /app/pipeline-config.json

ENV NODE_ENV=production
ENV PORT=4173
ENV BASE_PATH=/

EXPOSE 4173

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:4173/ || exit 1

CMD ["npx", "vite", "preview", "--config", "vite.config.ts", "--host", "0.0.0.0"]
```

Build and push:

```bash
docker build -t dispute-frontend:latest -f Dockerfile.frontend .
aws ecr create-repository --repository-name dispute-frontend
docker tag dispute-frontend:latest YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/dispute-frontend:latest
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com
docker push YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/dispute-frontend:latest
```

### 12.3 Docker Compose (Full Stack Local Testing)

```yaml
version: "3.8"

services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: dispute_db
      POSTGRES_USER: dispute_admin
      POSTGRES_PASSWORD: localdevpassword
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  backend:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "5000:5000"
    environment:
      PGHOST: db
      PGPORT: "5432"
      PGDATABASE: dispute_db
      PGUSER: dispute_admin
      PGPASSWORD: localdevpassword
      GEMINI_API_KEY: ${GEMINI_API_KEY}
      STRIPE_SECRET_KEY: ${STRIPE_SECRET_KEY}
    depends_on:
      - db
    volumes:
      - ./src/data:/app/data

  frontend:
    build:
      context: ../dispute-dashboard
      dockerfile: Dockerfile.frontend
    ports:
      - "4173:4173"
    environment:
      SPRINGBOOT_URL: http://backend:5000
      PORT: "4173"
      BASE_PATH: /
    depends_on:
      - backend

volumes:
  pgdata:
```

**Usage:**

```bash
docker-compose up -d
# Frontend: http://localhost:4173
# Backend API: http://localhost:5000
```

### 12.4 GitHub Actions CI/CD Pipeline (Full Stack)

```yaml
name: Build & Deploy — Full Stack

on:
  push:
    branches: [main]

env:
  AWS_REGION: us-east-1

jobs:
  deploy-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 19
        uses: actions/setup-java@v4
        with:
          distribution: oracle
          java-version: 19

      - name: Build JAR
        run: ./mvnw clean package -DskipTests -B

      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Upload JAR to S3
        run: |
          aws s3 cp target/mastercard-dispute-management-0.0.1-SNAPSHOT.jar \
            s3://dispute-mgmt-deployments/app.jar

      - name: Deploy to Backend EC2
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.BACKEND_EC2_HOST }}
          username: ubuntu
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            sudo systemctl stop dispute-mgmt
            aws s3 cp s3://dispute-mgmt-deployments/app.jar /opt/dispute-mgmt/app.jar
            sudo systemctl start dispute-mgmt
            sleep 10
            curl -sf http://localhost:5000/api/mastercard/test || exit 1

  deploy-frontend:
    runs-on: ubuntu-latest
    needs: deploy-backend
    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 20

      - name: Install pnpm
        run: npm install -g pnpm@9

      - name: Install dependencies
        working-directory: artifacts/dispute-dashboard
        run: pnpm install --frozen-lockfile

      - name: Build frontend
        working-directory: artifacts/dispute-dashboard
        env:
          PORT: "4173"
          BASE_PATH: "/"
        run: pnpm run build

      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Package frontend
        run: |
          mkdir -p /tmp/frontend-deploy
          cp -r artifacts/dispute-dashboard/dist /tmp/frontend-deploy/
          cp -r artifacts/dispute-dashboard/server /tmp/frontend-deploy/
          cp artifacts/dispute-dashboard/vite.config.ts /tmp/frontend-deploy/
          cp artifacts/dispute-dashboard/package.json /tmp/frontend-deploy/
          tar czf /tmp/frontend-deploy.tar.gz -C /tmp frontend-deploy

      - name: Upload to S3
        run: |
          aws s3 cp /tmp/frontend-deploy.tar.gz \
            s3://dispute-mgmt-deployments/frontend-deploy.tar.gz

      - name: Deploy to Frontend EC2
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.FRONTEND_EC2_HOST }}
          username: ubuntu
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            sudo systemctl stop dispute-frontend
            cd /opt/dispute-frontend
            aws s3 cp s3://dispute-mgmt-deployments/frontend-deploy.tar.gz .
            tar xzf frontend-deploy.tar.gz --strip-components=1
            rm frontend-deploy.tar.gz
            pnpm install --prod=false
            sudo systemctl start dispute-frontend
            sleep 5
            curl -sf http://localhost:4173/ || exit 1
```

---

## 13. Production Hardening Checklist

### Security

- [ ] Switch Mastercard API from `sandbox.api.mastercard.com` to production URL
- [ ] Switch Ethoca API from `sandbox.api.ethocaweb.com` to production URL
- [ ] Restrict CORS to `https://app.yourdomain.com` only
- [ ] Set `spring.jpa.show-sql=false` in production
- [ ] Remove SSH access security group rule after initial setup
- [ ] Enable RDS encryption at rest (done in the create command above)
- [ ] Enable RDS SSL connections
- [ ] Set the `.env` file permissions to `600` (owner-only read)
- [ ] Use AWS WAF on both ALBs to protect against common attacks
- [ ] Review and rotate all API keys periodically
- [ ] Set `NODE_ENV=production` on frontend EC2
- [ ] Remove Replit-specific plugins from vite.config.ts (cartographer, dev-banner, runtime-error-overlay)
- [ ] Consider adding rate limiting on the frontend ALB for `/api/agents/*` endpoints

### Performance

- [ ] Set JVM memory: `-Xms512m -Xmx1536m` for t3.medium (2GB RAM)
- [ ] Tune Hikari connection pool (15 max connections for db.t3.micro)
- [ ] Enable GZIP compression in Spring Boot: `server.compression.enabled=true`
- [ ] Set `ingestion.max-new-claims` to an appropriate value for production load
- [ ] Set ALB idle timeout to 300s for both ALBs (agent calls can be slow)
- [ ] Consider increasing proxy cache TTL from 30s to 60s for lower backend load

### Reliability

- [ ] Enable RDS automated backups (7 days minimum)
- [ ] Set RDS to Multi-AZ for high availability (adds cost)
- [ ] Configure ALB health checks: backend → `/api/mastercard/test`, frontend → `/`
- [ ] Set systemd `Restart=always` (already configured above)
- [ ] Set up CloudWatch alarms for CPU, memory, and disk
- [ ] Monitor Gemini API quota — token overflow (>1M tokens) causes agent failures

---

## 14. Monitoring & Logging

### CloudWatch Agent Setup

```bash
sudo apt-get install -y amazon-cloudwatch-agent

sudo tee /opt/aws/amazon-cloudwatch-agent/etc/config.json << 'EOF'
{
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/opt/dispute-mgmt/logs/application.log",
            "log_group_name": "/dispute-mgmt/backend",
            "log_stream_name": "{instance_id}",
            "timezone": "UTC"
          },
          {
            "file_path": "/opt/dispute-mgmt/logs/stdout.log",
            "log_group_name": "/dispute-mgmt/backend-stdout",
            "log_stream_name": "{instance_id}",
            "timezone": "UTC"
          },
          {
            "file_path": "/opt/dispute-frontend/logs/stdout.log",
            "log_group_name": "/dispute-mgmt/frontend",
            "log_stream_name": "{instance_id}",
            "timezone": "UTC"
          }
        ]
      }
    }
  },
  "metrics": {
    "namespace": "DisputeMgmt",
    "metrics_collected": {
      "mem": { "measurement": ["mem_used_percent"] },
      "disk": { "measurement": ["disk_used_percent"], "resources": ["*"] }
    }
  }
}
EOF

sudo amazon-cloudwatch-agent-ctl -a start
```

### CloudWatch Alarms

```bash
# High CPU — Backend
aws cloudwatch put-metric-alarm \
  --alarm-name dispute-backend-high-cpu \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=InstanceId,Value=i-YOUR_BACKEND_EC2_ID \
  --alarm-actions arn:aws:sns:us-east-1:YOUR_ACCOUNT_ID:dispute-alerts

# High CPU — Frontend
aws cloudwatch put-metric-alarm \
  --alarm-name dispute-frontend-high-cpu \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=InstanceId,Value=i-YOUR_FRONTEND_EC2_ID \
  --alarm-actions arn:aws:sns:us-east-1:YOUR_ACCOUNT_ID:dispute-alerts

# Database connections
aws cloudwatch put-metric-alarm \
  --alarm-name dispute-db-connections \
  --metric-name DatabaseConnections \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --threshold 50 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=DBInstanceIdentifier,Value=dispute-db \
  --alarm-actions arn:aws:sns:us-east-1:YOUR_ACCOUNT_ID:dispute-alerts

# RDS free storage
aws cloudwatch put-metric-alarm \
  --alarm-name dispute-db-storage \
  --metric-name FreeStorageSpace \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --threshold 2000000000 \
  --comparison-operator LessThanThreshold \
  --evaluation-periods 1 \
  --dimensions Name=DBInstanceIdentifier,Value=dispute-db \
  --alarm-actions arn:aws:sns:us-east-1:YOUR_ACCOUNT_ID:dispute-alerts

# ALB 5xx errors — Backend
aws cloudwatch put-metric-alarm \
  --alarm-name dispute-backend-5xx \
  --metric-name HTTPCode_Target_5XX_Count \
  --namespace AWS/ApplicationELB \
  --statistic Sum \
  --period 300 \
  --threshold 10 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --dimensions Name=LoadBalancer,Value=app/dispute-backend-alb/YOUR_ALB_ID \
  --alarm-actions arn:aws:sns:us-east-1:YOUR_ACCOUNT_ID:dispute-alerts
```

---

## 15. Backup & Recovery

### Database Backups

RDS automated backups are already enabled (7-day retention). For manual snapshots:

```bash
aws rds create-db-snapshot \
  --db-instance-identifier dispute-db \
  --db-snapshot-identifier dispute-db-$(date +%Y%m%d)

aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier dispute-db-restored \
  --db-snapshot-identifier dispute-db-20260408
```

### Evidence Files Backup

```bash
# Sync evidence files to S3 daily (add to crontab)
# crontab -e
0 2 * * * aws s3 sync /opt/dispute-mgmt/data/sources/ s3://dispute-mgmt-config-YOUR_ACCOUNT_ID/evidence-backup/ --delete
```

### Application Backup

```bash
# Backend JAR
aws s3 cp /opt/dispute-mgmt/app.jar \
  s3://dispute-mgmt-config-YOUR_ACCOUNT_ID/jar-backups/app-$(date +%Y%m%d%H%M).jar

# Frontend build
tar czf /tmp/frontend-$(date +%Y%m%d%H%M).tar.gz -C /opt/dispute-frontend dist server vite.config.ts
aws s3 cp /tmp/frontend-$(date +%Y%m%d%H%M).tar.gz \
  s3://dispute-mgmt-config-YOUR_ACCOUNT_ID/frontend-backups/

# Pipeline config
aws s3 cp /opt/dispute-frontend/pipeline-config.json \
  s3://dispute-mgmt-config-YOUR_ACCOUNT_ID/config-backups/pipeline-config-$(date +%Y%m%d%H%M).json
```

---

## 16. Scaling Considerations

### When to Scale

| Indicator | Action |
|-----------|--------|
| Backend CPU consistently > 70% | Upgrade EC2 to t3.large or add instances behind ALB |
| Backend memory consistently > 80% | Increase `-Xmx` or upgrade instance type |
| Frontend CPU consistently > 50% | Upgrade to t3.medium (proxy is lightweight) |
| DB connections > 80% of max | Upgrade RDS instance class |
| DB storage > 80% used | Enable RDS auto-scaling storage |
| Evidence files > 10GB | Move to S3 with code changes |
| > 50 concurrent users | Add second backend EC2 behind ALB |
| Agent calls causing timeouts | Consider async job queue (SQS) |

### Scaling the Backend (Multiple Instances)

1. Create an AMI from your configured EC2 instance
2. Launch additional instances from the AMI
3. Register all instances with the ALB target group
4. Evidence files must move to S3 (shared storage) or EFS

### Scaling the Frontend

The frontend is lightweight (Vite preview + proxy). A single `t3.small` should handle 100+ concurrent users. If you need to scale:

1. The proxy cache is per-instance — multiple instances will each maintain their own 30s cache
2. Pipeline config (`pipeline-config.json`) is local — move to a shared store (S3 or DynamoDB) if running multiple instances

### Scaling the Database

```bash
aws rds modify-db-instance \
  --db-instance-identifier dispute-db \
  --db-instance-class db.t3.small \
  --apply-immediately

aws rds modify-db-instance \
  --db-instance-identifier dispute-db \
  --max-allocated-storage 100
```

---

## 17. Cost Estimate

### Starter (Single Instance)

| Component | Service | Monthly Cost |
|-----------|---------|-------------|
| Backend | EC2 t3.medium (2 vCPU, 4GB) | ~$30 |
| Frontend | EC2 t3.small (2 vCPU, 2GB) | ~$15 |
| Database | RDS db.t3.micro (2 vCPU, 1GB) | ~$15 |
| Load Balancers | 2x ALB | ~$32 |
| NAT Gateway | NAT + data | ~$32 |
| Secrets | Secrets Manager | ~$1 |
| SSL | ACM Certificate | Free |
| **Total** | | **~$125/month** |

### Production (High Availability)

| Component | Service | Monthly Cost |
|-----------|---------|-------------|
| Backend | 2x EC2 t3.medium | ~$60 |
| Frontend | 2x EC2 t3.small | ~$30 |
| Database | RDS db.t3.medium Multi-AZ | ~$70 |
| Load Balancers | 2x ALB | ~$32 |
| NAT Gateway | 2x NAT (HA) | ~$64 |
| Monitoring | CloudWatch | ~$10 |
| **Total** | | **~$266/month** |

---

## 18. Troubleshooting

### Backend won't start

```bash
sudo systemctl status dispute-mgmt
tail -100 /opt/dispute-mgmt/logs/application.log
sudo lsof -i :5000
/opt/java/bin/java -version
psql -h YOUR_RDS_ENDPOINT -U dispute_admin -d dispute_db -c "SELECT 1"
```

### Frontend won't start

```bash
sudo systemctl status dispute-frontend
tail -100 /opt/dispute-frontend/logs/stdout.log

# Common issues:
# 1. Missing node_modules — run `pnpm install` in /opt/dispute-frontend
# 2. Missing dist/public — rebuild with `PORT=4173 BASE_PATH="/" pnpm run build`
# 3. Vite config referencing Replit plugins — see Section 9A.6

# Check if port is in use
sudo lsof -i :4173

# Verify Node.js
node --version
```

### Frontend loads but API calls fail

```bash
# Test backend directly from frontend EC2
curl -s http://BACKEND_PRIVATE_IP:5000/api/mastercard/test

# Test the proxy
curl -s http://localhost:4173/api/disputes | jq '.total'

# Check SPRINGBOOT_URL is set correctly
sudo systemctl show dispute-frontend | grep -i spring

# Check security groups allow frontend → backend on port 5000
```

### Health check failing on ALB

```bash
# Backend
curl -s http://localhost:5000/api/mastercard/test
sudo ss -tlnp | grep 5000

# Frontend
curl -s http://localhost:4173/
sudo ss -tlnp | grep 4173
```

### Database connection errors

```bash
nc -zv YOUR_RDS_ENDPOINT 5432

aws rds describe-db-instances --db-instance-identifier dispute-db \
  --query 'DBInstances[0].DBInstanceStatus'
```

### Gemini API errors

```bash
grep -i "gemini\|GeminiService\|429\|quota\|token" /opt/dispute-mgmt/logs/application.log | tail -20
```

> **Known issue**: The backend proactively trims context to 800,000 characters (`MAX_TOTAL_CHARS` in `GeminiService.java`) before sending to Gemini. Claims with very large evidence sets may have context truncated, which can reduce agent accuracy. Monitor for `429` (rate limit) errors or warnings about trimmed context in the application logs.

### Evidence files not found

```bash
ls -la /opt/dispute-mgmt/data/sources/acquirer/
ls -la /opt/dispute-mgmt/data/sources/issuer/
stat /opt/dispute-mgmt/data/sources/
```

### Pipeline config not persisting

```bash
# Check the config file exists and is writable
ls -la /opt/dispute-frontend/pipeline-config.json

# If missing, create it
echo '{"enabled":false,"rules":[]}' > /opt/dispute-frontend/pipeline-config.json
chown ubuntu:ubuntu /opt/dispute-frontend/pipeline-config.json
```

---

## 19. Quick Start Summary

For someone who wants to get the full system running on AWS as fast as possible:

### Backend (15 minutes)

1. Create an RDS PostgreSQL instance
2. Launch an EC2 t3.medium with Java 19
3. Upload the built JAR and evidence files
4. Set environment variables (DB credentials, API keys)
5. Update `mastercard.keystore-path` to point to the `.p12` file
6. Run `java -jar app.jar` and verify at `http://EC2_IP:5000/api/mastercard/test`
7. Put an ALB in front with an SSL certificate for HTTPS

### Frontend (10 minutes)

1. Build the frontend locally: `PORT=4173 BASE_PATH="/" pnpm run build`
2. Launch an EC2 t3.small with Node.js 20
3. Upload: build output (`dist/`), proxy code (`server/`), `vite.config.ts`, `package.json`
4. Install dependencies: `pnpm install`
5. Remove Replit-specific plugins from `vite.config.ts` (see Section 9A.6)
6. Set `SPRINGBOOT_URL` to the backend's private IP or ALB domain
7. Run `npx vite preview --config vite.config.ts --host 0.0.0.0`
8. Put an ALB in front with an SSL certificate

### Connect (5 minutes)

1. Point `api.yourdomain.com` → Backend ALB
2. Point `app.yourdomain.com` → Frontend ALB
3. Set `SPRINGBOOT_URL=http://BACKEND_PRIVATE_IP:5000` in frontend service
4. Update CORS in backend `WebConfig.java` to allow `https://app.yourdomain.com`
5. Set ALB idle timeouts to 300s on both ALBs
6. Verify: `curl https://app.yourdomain.com/api/disputes | jq '.total'`
