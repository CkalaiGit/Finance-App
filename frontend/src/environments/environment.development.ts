export const environment = {
  production: false,
  keycloak: {
    url: 'http://localhost:8080', // Ton Keycloak Docker
    realm: 'dev-realm',
    clientId: 'frontend-client'
  }
};