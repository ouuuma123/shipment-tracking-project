# Shipment Tracking Project
Application de gestion et de suivi des expéditions

## La description

Ce projet permet de créer, consulter et mettre à jour des shipments, avec un suivi de leur statut et des notifications en temps réel.

Le système est divisé en deux parties :
- un backend Spring Boot pour gérer les données et les API
- un frontend Angular pour l’interface utilisateur

## Les fonctionnalités

- création d’un shipment
- consultation de la liste des expéditions
- modification du statut
- mise à jour des informations
- notifications et actualisation en temps réel de la liste après création d’un shipment via WebSocket

## Technologies
  Java, Spring Boot, Maven, Angular, TypeScript, WebSocket

## Structure du projet

- backend-shipment/: backend du projet
- frontend-shipment/: interface utilisateur
- README.md: documentation du projet

## Prérequis

- Java JDK 17 ou plus
- Maven
- Node.js et npm
- Angular CLI

## Lancer le backend

```bash
cd backend-shipment
./mvnw spring-boot:run
```

## Lancer le frontend

```bash
cd frontend-shipment
npm install
ng serve
```

## Accès

- Frontend: http://localhost:4200
- Backend: http://localhost:8080

## Notes

Le projet a été conçu pour simuler un système simple de tracking de colis avec gestion des statuts et mise à jour en temps réel.
