export interface UserProfile {
  id?: string; // keycloakId
  username: string;
  email: string;
  roles: string[];
}