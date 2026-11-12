.DEFAULT_GOAL := help

SECRET_FILE := secrets/db_password.txt
ENV_FILE    := .env

.PHONY: help start stop restart status

help:
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  make %-10s %s\n", $$1, $$2}'

$(ENV_FILE):
	@echo "Creando $(ENV_FILE) con valores por defecto..."
	@printf 'POSTGRES_DB=talentfinder\nPOSTGRES_USER=talentfinder\n' > $(ENV_FILE)

$(SECRET_FILE):
	@echo "Generando contraseña de la base de datos en $(SECRET_FILE)..."
	@mkdir -p secrets
	@umask 077 && openssl rand -base64 32 | tr -d '\n' > $(SECRET_FILE)

start: $(ENV_FILE) $(SECRET_FILE)
	docker compose up -d --wait
	@echo ""
	@echo "Base de datos lista en 127.0.0.1:5432"

stop:
	docker compose down

restart: stop start

status:
	docker compose ps