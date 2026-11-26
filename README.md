# ShipTracargo - Shipment Tracking

Application de gestion et de suivi des expéditions. Elle permet de créer, consulter et mettre à jour des expéditions, de suivre leur statut et de recevoir des notifications en temps réel.

Le projet est composé de deux parties :

- `backend-shipment` : API Spring Boot (Java)
- `frontend-shipment` : interface Angular (TypeScript)

## Fonctionnalités

- Création d'une expédition
- Liste des expéditions
- Modification des informations et du statut
- Actualisation en temps réel de la liste après la création d'une expédition (WebSocket)
- Authentification JWT avec rôles
- Inscription des clients
- Récupération du mot de passe (simulée, voir la section Limites)
- Gestion des comptes par l'administrateur

## Technologies

- Backend : Java 21, Spring Boot, Spring Security, WebSocket, Maven
- Frontend : Angular, TypeScript
- Base de données : H2 (en mémoire, pour le développement)

## Structure du projet

```
backend-shipment/    backend Spring Boot
frontend-shipment/   frontend Angular
README.md
```

## Prérequis

- Java JDK 21 ou plus
- Node.js 22.22.3 ou plus, 24.15 ou plus, ou 26 ou plus, avec npm

Maven et Angular CLI n'ont pas besoin d'être installés globalement : le wrapper Maven et Angular CLI sont inclus dans le projet.

## Installation et lancement

### 1. Variables d'environnement

Avant le premier démarrage, il faut définir une clé JWT d'au moins 32 octets. On peut aussi définir un compte administrateur initial. Ce compte n'est créé que s'il n'existe pas encore. Sans ces variables, le backend démarre sans administrateur.

Sous Windows (PowerShell) :

```powershell
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 48
$rng.GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:SHIPMENT_ADMIN_EMAIL = "admin@example.com"
$env:SHIPMENT_ADMIN_PASSWORD = "mot-de-passe-long-et-unique"
```

Sous Linux ou macOS :

```bash
export JWT_SECRET=$(openssl rand -base64 48)
export SHIPMENT_ADMIN_EMAIL="admin@example.com"
export SHIPMENT_ADMIN_PASSWORD="mot-de-passe-long-et-unique"
```

Le mot de passe de l'administrateur doit contenir au moins 12 caractères.

### 2. Lancer le backend

```bash
cd backend-shipment
./mvnw spring-boot:run
```

Sous Windows : `.\mvnw.cmd spring-boot:run`

### 3. Lancer le frontend

Dans un second terminal :

```bash
cd frontend-shipment
npm install
npm start
```

Pour vérifier le projet :

```powershell
# Backend (depuis backend-shipment)
.\mvnw.cmd test

# Frontend (depuis frontend-shipment)
npm test -- --watch=false
npm run build
```

### 4. Accès

- Frontend : http://localhost:4200
- Backend : http://localhost:8080

Connectez-vous avec le compte administrateur, puis ouvrez la page Comptes pour créer d'autres utilisateurs.

## Authentification et rôles

L'API utilise des access tokens JWT de courte durée. L'access token reste en mémoire dans le navigateur. Le refresh token est un jeton opaque, renouvelé à chaque utilisation et stocké haché en base ; il est envoyé dans un cookie HttpOnly. Les requêtes d'écriture sont protégées contre le CSRF. Les mots de passe sont hachés avec BCrypt.

L'inscription publique crée uniquement des comptes CLIENT. Un administrateur peut créer des comptes avec les rôles ADMIN, MANAGER, DISPATCHER, DRIVER, CLIENT ou OPERATOR.

## Variables d'environnement

- `JWT_SECRET` (obligatoire) : clé de signature JWT, 32 octets minimum
- `SHIPMENT_ADMIN_EMAIL` : e-mail de l'administrateur initial
- `SHIPMENT_ADMIN_PASSWORD` : mot de passe de l'administrateur initial
- `PASSWORD_RESET_LOG_LINK` : mettre à `true` pour afficher le lien de réinitialisation dans les logs, uniquement en développement
- `REFRESH_COOKIE_SECURE` : mettre à `true` sous HTTPS
- `FRONTEND_ORIGIN` : origine exacte de l'application Angular, à définir en production

## Réinitialisation du mot de passe

La réinitialisation est simulée. Le lien à usage unique est valable 15 minutes et n'est pas écrit dans les logs par défaut. Pour la tester en local, démarrez le backend avec `PASSWORD_RESET_LOG_LINK=true`. Le lien contient un jeton temporaire : n'activez jamais cette option en production et ne conservez pas ces logs.
