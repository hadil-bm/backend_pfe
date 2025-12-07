# Utilise une image stable
FROM ghcr.io/cirruslabs/flutter:stable AS build

# Configurer le dossier
WORKDIR /app

# Copier JUSTE les fichiers de dépendances d'abord (Optimisation Cache)
COPY pubspec.* ./
RUN flutter pub get

# Copier tout le reste
COPY . .

# Build Web avec moins de "Workers" pour économiser la RAM
# --web-renderer html: Parfois plus stable que canvaskit sur petits serveurs
RUN flutter build web --release --web-renderer html

# Stage Nginx
FROM nginx:alpine
COPY --from=build /app/build/web /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]