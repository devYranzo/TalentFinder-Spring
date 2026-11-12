# Talent Finder

Plataforma multi-organización para reclutadores: sube CVs en PDF, y busca candidatos
con lenguaje natural mediante búsqueda semántica (RAG). Pensada para evolucionar a un
servicio SaaS.

> **Estado: en desarrollo.** El backend se está migrando de FastAPI a Spring Boot.
> Este documento distingue lo que ya funciona de lo que está planificado.

## Estado actual

| Módulo | Estado |
|---|---|
| Base de datos, migraciones (Flyway) | Hecho |
| Organizaciones y usuarios (roles `ADMIN` y `RECRUITER`) | Hecho |
| Autenticación (JWT en cookie `HttpOnly`, CSRF, Argon2) | Hecho |
| Registro de empresa y gestión de usuarios | Hecho |
| Registro de auditoría de eventos de seguridad | Hecho (sin purga por retención todavía) |
| Cambio de contraseña propio | Pendiente |
| Tests de integración | Pendiente |
| Gestor de CVs (subida segura de PDF) | Pendiente |
| Proyectos de selección | Pendiente |
| Emails (Resend) e invitaciones | Pendiente |
| Búsqueda semántica (RAG, PGVector) | Pendiente |
| Analíticas | Pendiente |
| Frontend | En rehacer |

## Stack

- **Backend:** Java, Spring Boot 4.1, Spring Security, Spring Data JPA, Flyway
- **Base de datos:** PostgreSQL 18 con la extensión pgvector
- **IA / RAG (planificado):** Spring AI, PGVector, lectura de PDF
- **Infraestructura local:** Docker Compose

## Estructura

```
.
├── backend/            Aplicación Spring Boot
├── frontend/           (pendiente)
├── secrets/            Secretos locales (NO se versiona)
├── docker-compose.yml  PostgreSQL + pgvector endurecido
├── Makefile            Atajos de desarrollo
└── .env                Variables no secretas de Docker (NO se versiona)
```

El código del backend se organiza **por funcionalidad** (`user/`, `organization/`,
`auth/`, `security/`, `audit/`...) y no por tipo técnico.

## Requisitos

- JDK compatible con el proyecto (`java.version` en `backend/pom.xml`)
- Docker Desktop (o Docker Engine)
- Para usar el Makefile: GNU Make y un shell tipo Unix (Git Bash o WSL) con `openssl`

## Puesta en marcha

Con Make:

```bash
make start     # genera secretos si faltan, levanta la BD y espera a que esté sana
make back      # arranca el backend
make help      # lista todos los comandos
```

Sin Make:

1. Crea `.env` en la raíz:
   ```
   POSTGRES_DB=talentfinder
   POSTGRES_USER=talentfinder
   ```
2. Crea `secrets/db_password.txt` (contraseña aleatoria) y `secrets/jwt_secret.txt`
   (48 bytes aleatorios en Base64). **Sin salto de línea al final.**
3. `docker compose up -d --wait`
4. `cd backend && ./mvnw spring-boot:run` (ejecútalo siempre desde `backend/`:
   los secretos se leen de `../secrets/`).

Comprobación: `GET http://localhost:8080/actuator/health` responde `200`.

## Configuración

- Los ajustes están en `backend/src/main/resources/application.properties`.
- **Ningún secreto va en el repositorio.** La contraseña de la base de datos y la clave
  de firma JWT se leen de los ficheros de `secrets/` mediante `spring.config.import=configtree`.
- `DB_URL` y `DB_USER` pueden sobrescribirse con variables de entorno.

## API (resumen)

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/auth/register-company` | Público |
| POST | `/auth/login` | Público |
| POST | `/auth/logout` | Autenticado |
| GET | `/auth/me` | Autenticado |
| GET / POST | `/users` | `ADMIN` |
| PATCH / DELETE | `/users/{id}` | `ADMIN` |
| GET | `/users/members` | `RECRUITER` o superior |
| GET | `/actuator/health` | Público |

La sesión viaja en la cookie `access_token` (`HttpOnly`, `SameSite=Strict`). Las peticiones
que modifican datos deben enviar la cabecera `X-XSRF-TOKEN` con el valor de la cookie
`XSRF-TOKEN`.

## Seguridad y privacidad

Decisiones de diseño ya aplicadas:

- **Aislamiento entre organizaciones:** el `organizationId` sale siempre del usuario
  autenticado, nunca de la petición, y toda consulta de negocio lo incluye. Un recurso de
  otra organización responde `404`.
- **Contraseñas** con Argon2; mínimo de 12 caracteres.
- **Login:** tiempo de respuesta homogéneo para no revelar qué emails existen, y bloqueo
  temporal tras intentos fallidos.
- **Revocación inmediata:** el usuario se recarga de la base de datos en cada petición,
  así que desactivar o borrar una cuenta corta el acceso al instante.
- **Una organización nunca se queda sin administrador activo.**
- **Auditoría** de login, logout y cambios de usuarios, guardando identificadores y no
  nombres ni emails.
- **Base de datos** en un contenedor endurecido (sistema de ficheros de solo lectura,
  capabilities mínimas, escucha solo en `127.0.0.1`, autenticación SCRAM).

Limitaciones conocidas, a resolver antes de ofrecerlo como servicio:

- El **registro de empresa es abierto**: no hay verificación de email ni límite por IP.
- El **límite de intentos de login es en memoria**: solo vale para una instancia.
- El **registro de auditoría no tiene aún política de retención** ni purga automática.
- La contraseña inicial de un usuario la fija el administrador que lo crea; hace falta el
  flujo de invitación por email y el cambio de contraseña propio.

## Hoja de ruta hacia SaaS

Técnico:

- Verificación de email y límites en el registro.
- Rate limiting compartido entre instancias (Redis o base de datos).
- Row Level Security en PostgreSQL como segunda barrera de aislamiento.
- Usuarios de base de datos separados: migraciones y aplicación, y esta última sin
  `UPDATE` ni `DELETE` sobre la auditoría.
- Cifrado en tránsito (TLS) y en reposo; copias de seguridad cifradas con retención definida.
- Planes, facturación y límites por organización (usuarios, CVs, consultas).
- Observabilidad y alertas sobre los eventos de seguridad.

Cumplimiento (requiere validación con asesoría legal y DPO, esto no es asesoramiento jurídico):

- **RGPD:** registro de actividades de tratamiento, evaluación de impacto (art. 35),
  contratos de encargado de tratamiento con proveedores (incluido el del modelo de lenguaje),
  derecho de supresión completo (CV, vectores, notas y copias de seguridad) y
  política de retención.
- **EU AI Act:** un sistema que filtra o clasifica candidatos es de alto riesgo
  (Anexo III, empleo). Según el Digital Omnibus, las obligaciones se aplican desde el
  2 de diciembre de 2027; conviene diseñar ya la supervisión humana (la IA recomienda,
  nunca descarta), el registro de cada consulta de IA, la documentación técnica y la
  minimización de datos enviados al modelo.
- **Transferencias internacionales:** si el modelo de lenguaje o los embeddings se
  consumen desde un proveedor fuera de la UE, hace falta base legal y garantías, o bien
  una región UE o un modelo local.

## Desarrollo

```bash
make test      # tests del backend
make psql      # consola SQL en el contenedor
make reset     # borra la base de datos y empieza de cero
```

Las migraciones están en `backend/src/main/resources/db/migration/`. Una migración
aplicada **no se edita**: los cambios de esquema van siempre en una nueva.

## Licencia

Copyright (c) 2026 devYranzo. Todos los derechos reservados.