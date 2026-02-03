# Infrastructure Keycloak (Local Dev)

Ce dépôt contient la configuration Docker pour lancer un environnement Keycloak local prêt à l'emploi avec un realm de développement pré-configuré.

## ⚠️ Avertissement de Sécurité
**TOUS les mots de passe et secrets présents dans ce dépôt sont destinés UNIQUEMENT au développement local.** Ne jamais utiliser ces configurations en production.

## Prérequis
- Docker & Docker Compose

## Démarrage Rapide

1. (Optionnel) Copier la configuration d'exemple :
   ```bash
   cp .env.example .env
