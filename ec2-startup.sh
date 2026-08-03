#!/bin/bash
set -e

LOG_FILE="/var/log/dispute-setup.log"
exec > >(tee -a "$LOG_FILE") 2>&1
echo "=== Setup started at $(date) ==="

echo "Installing Java 19..."
sudo apt-get update -y
sudo apt-get install -y wget unzip jq curl

wget -q https://download.oracle.com/graalvm/19/latest/graalvm-jdk-19_linux-x64_bin.tar.gz
sudo mkdir -p /opt/java
sudo tar -xzf graalvm-jdk-19_linux-x64_bin.tar.gz -C /opt/java --strip-components=1
rm graalvm-jdk-19_linux-x64_bin.tar.gz

echo 'export JAVA_HOME=/opt/java' | sudo tee /etc/profile.d/java.sh
echo 'export PATH=$JAVA_HOME/bin:$PATH' | sudo tee -a /etc/profile.d/java.sh
source /etc/profile.d/java.sh

java -version

if ! command -v aws &> /dev/null; then
  echo "Installing AWS CLI..."
  curl -s "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
  unzip -q awscliv2.zip
  sudo ./aws/install
  rm -rf aws awscliv2.zip
fi

echo "Fetching secrets from Secrets Manager..."
REGION="us-east-1"
SECRETS=$(aws secretsmanager get-secret-value \
  --secret-id dispute-mgmt/app-secrets \
  --query SecretString --output text \
  --region $REGION)

APP_DIR="/opt/dispute-mgmt"
sudo mkdir -p $APP_DIR/config $APP_DIR/logs
sudo mkdir -p $APP_DIR/data/sources/issuer/{customer-comms,device,fraud-tools,identity,merchant,psp,shipping}
sudo mkdir -p $APP_DIR/data/sources/acquirer/{customer-comms,device,fraud-tools,identity,merchant,psp,shipping}

echo "Downloading Mastercard keystore..."
# Replace YOUR_ACCOUNT_ID with your actual AWS account ID
aws s3 cp s3://dispute-mgmt-config-YOUR_ACCOUNT_ID/keystore/signing.p12 \
  $APP_DIR/config/signing.p12 --region $REGION

cat > $APP_DIR/application-prod.properties << 'PROPS'
spring.application.name=mastercard-dispute-management

spring.datasource.url=jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
spring.datasource.username=${PGUSER}
spring.datasource.password=${PGPASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

spring.datasource.hikari.maximum-pool-size=15
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000

server.port=5000
server.compression.enabled=true
server.compression.mime-types=application/json,text/plain,text/html

mastercard.base-url=https://sandbox.api.mastercard.com
mastercard.consumer-key=${MASTERCARD_CONSUMER_KEY}
mastercard.keystore-password=${MASTERCARD_KEYSTORE_PASSWORD}
mastercard.keystore-path=/opt/dispute-mgmt/config/signing.p12

claim-detail.local-fallback.enabled=true
ingestion.scheduled.enabled=false
ingestion.scheduled.interval-ms=3600000
ingestion.scheduled.initial-delay-ms=60000
ingestion.max-new-claims=10

gemini.api-key=${GEMINI_API_KEY}

stripe.secret-key=${STRIPE_SECRET_KEY}
stripe.base-url=https://api.stripe.com/v1

ethoca.base-url=https://sandbox.api.ethocaweb.com
ethoca.api-key-id=${ETHOCA_API_KEY_ID}
ethoca.api-secret=${ETHOCA_API_SECRET}

spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB

logging.level.root=INFO
logging.level.com.opus.dispute.management=INFO
logging.file.name=/opt/dispute-mgmt/logs/application.log
logging.logback.rollingpolicy.max-file-size=50MB
logging.logback.rollingpolicy.max-history=30
PROPS

sudo cat > /etc/systemd/system/dispute-mgmt.service << 'SERVICE'
[Unit]
Description=Mastercard Dispute Management System
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
SERVICE

sudo chown -R ubuntu:ubuntu $APP_DIR
sudo systemctl daemon-reload
sudo systemctl enable dispute-mgmt

echo "=== Setup complete at $(date) ==="
echo "Next steps:"
echo "  1. Upload JAR to $APP_DIR/app.jar"
echo "  2. Upload evidence data to $APP_DIR/data/sources/"
echo "  3. Run: sudo systemctl start dispute-mgmt"
