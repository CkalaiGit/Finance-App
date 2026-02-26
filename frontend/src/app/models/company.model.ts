/**
 * Modèle métier représentant une entreprise cotée
 * 
 * @description
 * Interface métier SANS pollution par des données UI (pas de color).
 * Représente les données réelles d'une entreprise.
 */
export interface Company {
  ticker: string;
  
  name: string;
  
  industry: string;
  
  marketCap?: number;
  
  currentPrice?: number;
}

/**
 * DTO pour l'affichage dans la Landing Page
 * 
 * @description
 * Étend Company avec des métadonnées UI nécessaires au composant.
 * Sépare clairement la logique métier de la présentation.
 */
export interface CompanyDisplayDTO extends Company {
  /** 
   * Variante de couleur pour le badge UI
   * Déterminée par le service selon la logique d'affichage
   */
  colorVariant: 'blue' | 'green' | 'purple' | 'cyan' | 'red';
}