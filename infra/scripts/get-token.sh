#!/bin/bash

# Script utilitaire pour récupérer un token ACCESS (Dev Only)
# Prérequis : curl, jq (optionnel pour affichage propre, sinon affichage brut)

KEYCLOAK_URL="http://localhost:8080"
REALM="dev-realm"
CLIENT_ID="frontend-client"
USER="alice"
PASS="password"

echo "Récupération du token pour l'utilisateur : $USER sur $REALM..."

RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "username=$USER" \
  -d "password=$PASS" \
  -d "grant_type=password")

# Vérifie si jq est installé pour un joli affichage, sinon affiche le brut
if command -v jq &> /dev/null; then
    echo "---------------------------------------------------"
    echo "Access Token décodé (payload partiel) :"
    echo "$RESPONSE" | jq -r '.access_token | split(".")[1] | @base64d | fromjson'
    echo "---------------------------------------------------"
    echo "Réponse complète :"
    echo "$RESPONSE" | jq .
else
    echo "---------------------------------------------------"
    echo "Réponse (installer 'jq' pour un meilleur formatage) :"
    echo "$RESPONSE"
fi
