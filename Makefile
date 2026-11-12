.DEFAULT_GOAL := help

SECRETS_DIR := secrets
DB_SECRET   := $(SECRETS_DIR)/db_password.txt
JWT_SECRET  := $(SECRETS_DIR)/jwt_secret.txt
ENV_FILE    := .env

.PHONY: help start stop restart status logs psql reset

help: ## Muestra esta ayuda
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  make %-10s %s\n", $$1, $$2}'

# ------------------------------------------------------------ preparación
$(ENV_FILE):
	@echo "Creando $(ENV_FILE) con valores por defecto..."
	@printf 'POSTGRES_DB=talentfinder\nPOSTGRES_USER=talentfinder\n' > $(ENV_FILE)

$(DB_SECRET):
	@echo "Generando contraseña de la base de datos..."
	@mkdir -p $(SECRETS_DIR)
	@umask 077 && openssl rand -base64 32 | tr -d '\n' > $(DB_SECRET)

$(JWT_SECRET):
	@echo "Generando secreto de firma JWT..."
	@mkdir -p $(SECRETS_DIR)
	@umask 077 && openssl rand -base64 48 | tr -d '\n' > $(JWT_SECRET)

# ------------------------------------------------------------ Contenedores
start: $(ENV_FILE) $(DB_SECRET) $(JWT_SECRET) ## Levanta los contenedores y espera a que esté lista
	docker compose up -d --wait
	@echo ""
	@echo "Base de datos lista en 127.0.0.1:5432"

stop: ## Para los contenedores (conserva los datos)
	docker compose down

restart: stop start ## Reinicia los contenedores

status: ## Estado de los contenedores
	docker compose ps

logs: ## Sigue los logs de los contenedores
	docker compose logs -f

psql: ## Abre una consola psql dentro del contenedor
	docker compose exec db sh -c 'psql -U "$$POSTGRES_USER" -d "$$POSTGRES_DB"'

reset: ## BORRA la base de datos y su contraseña (el secreto JWT se conserva)
	@printf "Esto elimina TODOS los datos de la BD. Escribe 'si' para continuar: " && read ans && [ "$$ans" = "si" ]
	docker compose down -v
	rm -f $(DB_SECRET)