# MS-ListaEspera
RedNorte Fullstack III
chupalo mati
rama de desarrollo con seguridad 
desactivada para testing local.
NO hacer merge a main.

## Docker

Este servicio está preparado para correr en Docker y conectarse a la MySQL ya creada por `ms-portal-pacientes` usando el puerto publicado `3306` del host (`host.docker.internal`).

### 1) Levantar el servicio en Docker

```bash
docker compose up -d --build
```

Quedará disponible en `http://localhost:8085`.

### 2) Subir imagen a Docker Hub (más adelante)

1. Cambiar la imagen en `docker-compose.yaml`:
	- `tu-usuario-dockerhub/ms-lista-espera:latest`
2. Construir imagen:
	- `docker build -t tu-usuario-dockerhub/ms-lista-espera:latest .`
3. Login en Docker Hub:
	- `docker login`
4. Publicar:
	- `docker push tu-usuario-dockerhub/ms-lista-espera:latest`
