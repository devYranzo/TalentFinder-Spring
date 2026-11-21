# Talent Finder - entorno de desarrollo.
.DEFAULT_GOAL := help

SECRETS_DIR := secrets
ENV_FILE    := .env
BUCKET      := cvs

DB_SECRET        := $(SECRETS_DIR)/db_password.txt
JWT_SECRET       := $(SECRETS_DIR)/jwt_secret.txt
DEV_USER_SECRET  := $(SECRETS_DIR)/dev_user_password.txt
STORAGE_ADMIN_AK := $(SECRETS_DIR)/storage_admin_access_key.txt
STORAGE_ADMIN_SK := $(SECRETS_DIR)/storage_admin_secret_key.txt
STORAGE_AK       := $(SECRETS_DIR)/storage_access_key.txt
STORAGE_SK       := $(SECRETS_DIR)/storage_secret_key.txt
STORAGE_JSON     := $(SECRETS_DIR)/seaweedfs_s3.json

# Cliente de AWS CLI (contenedor) con las credenciales de administración del almacén.
AWS = AWS_ACCESS_KEY_ID="$$(cat $(STORAGE_ADMIN_AK))" AWS_SECRET_ACCESS_KEY="$$(cat $(STORAGE_ADMIN_SK))" docker compose --profile tools run --rm -T aws

.PHONY: help prepare-secrets start-dev run-dev stop psql test

help: _header
	${info }
	@echo Opciones:
	@echo ------------------------------------------
	@echo start-dev
	@echo run-dev
	@echo stop
	@echo ------------------------------------------
	@echo psql
	@echo test
	@echo ------------------------------------------

_header:
	${info }
	@echo Talent Finder
	@echo -------------

# ---------------------------------------------------------------------- secretos
$(SECRETS_DIR):
	@mkdir -p $@

$(ENV_FILE):
	@echo "Creando $(ENV_FILE)..."
	@printf 'POSTGRES_DB=talentfinder\nPOSTGRES_USER=talentfinder\n' > $@

$(DB_SECRET): | $(SECRETS_DIR)
	@echo "Generando la contraseña de la base de datos..."
	@openssl rand -base64 32 | tr -d '\n' > $@
	@chmod 644 $@

$(JWT_SECRET): | $(SECRETS_DIR)
	@echo "Generando el secreto de firma JWT..."
	@openssl rand -base64 48 | tr -d '\n' > $@
	@chmod 600 $@

$(DEV_USER_SECRET): | $(SECRETS_DIR)
	@echo "Generando la contraseña de los usuarios de desarrollo..."
	@openssl rand -base64 24 | tr -d '\n' > $@
	@chmod 600 $@

$(STORAGE_ADMIN_AK): | $(SECRETS_DIR)
	@openssl rand -base64 32 | tr -d '+/=\n' | cut -c1-20 | tr -d '\n' > $@
	@chmod 600 $@

$(STORAGE_ADMIN_SK): | $(SECRETS_DIR)
	@openssl rand -base64 32 | tr -d '+/=\n' > $@
	@chmod 600 $@

$(STORAGE_AK): | $(SECRETS_DIR)
	@openssl rand -base64 32 | tr -d '+/=\n' | cut -c1-20 | tr -d '\n' > $@
	@chmod 600 $@

$(STORAGE_SK): | $(SECRETS_DIR)
	@openssl rand -base64 32 | tr -d '+/=\n' > $@
	@chmod 600 $@

# Configuración de identidades de SeaweedFS, construida a partir de las claves anteriores:
$(STORAGE_JSON): $(STORAGE_ADMIN_AK) $(STORAGE_ADMIN_SK) $(STORAGE_AK) $(STORAGE_SK)
	@echo "Generando la configuración del almacén..."
	@printf '{"identities":[{"name":"setup-admin","credentials":[{"accessKey":"%s","secretKey":"%s"}],"actions":["Admin"]},{"name":"backend","credentials":[{"accessKey":"%s","secretKey":"%s"}],"actions":["Read:$(BUCKET)","Write:$(BUCKET)","List:$(BUCKET)"]}]}' "$$(cat $(STORAGE_ADMIN_AK))" "$$(cat $(STORAGE_ADMIN_SK))" "$$(cat $(STORAGE_AK))" "$$(cat $(STORAGE_SK))" > $@
	@chmod 644 $@

prepare-secrets: $(ENV_FILE) $(DB_SECRET) $(JWT_SECRET) $(DEV_USER_SECRET) $(STORAGE_JSON)
	@chmod 700 $(SECRETS_DIR)

# ---------------------------------------------------------------------- entorno
start-dev: prepare-secrets
	docker compose up -d --wait db storage
	@echo "Esperando al almacén..."
	@i=0; until $(AWS) s3 ls >/dev/null 2>&1; do i=$$((i+1)); [ $$i -ge 30 ] && { echo "El almacén no responde"; exit 1; }; sleep 2; done
	@$(AWS) s3 ls s3://$(BUCKET) >/dev/null 2>&1 || $(AWS) s3 mb s3://$(BUCKET)
	@echo ""
	@echo "Entorno de desarrollo listo:"
	@echo "  PostgreSQL   127.0.0.1:5432"
	@echo "  Almacén S3   127.0.0.1:8333 (bucket $(BUCKET))"
	@echo "  Usuarios     admin@talentfinder.test y recruiter@talentfinder.test"
	@echo "  Contraseña en $(DEV_USER_SECRET)"

run-dev: start-dev
	cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

stop:
	docker compose down

psql:
	docker compose exec db sh -c 'psql -U "$$POSTGRES_USER" -d "$$POSTGRES_DB"'

test:
	cd backend && ./mvnw test