package com.cairedine.finance.app.intelligence.domain.port;

import java.util.List;
import java.util.Map;

/**
 * Port d'accès à la base de données vectorielle (pgvector via Spring AI).
 */
public interface IVectorStorePort {

    /**
     * Stocke les chunks de texte vectorisés avec leurs métadonnées associées.
     *
     * @param ticker Symbole boursier de l'entreprise
     * @param accessionNumber Numéro d'accession unique SEC
     * @param chunks Liste des extraits de texte
     * @param metadata Métadonnées additionnelles à rattacher à chaque chunk
     */
    void storeChunks(String ticker, String accessionNumber, List<String> chunks, Map<String, Object> metadata);

    /**
     * Effectue une recherche par similarité vectorielle (Cosine Distance) pour un ticker donné.
     *
     * @param ticker Symbole boursier de l'entreprise
     * @param query Requête ou question en langage naturel
     * @param topK Nombre maximal de chunks les plus proches à retourner
     * @return Liste des extraits de texte les plus pertinents
     */
    List<String> searchSimilar(String ticker, String query, int topK);

    /**
     * Supprime tous les vecteurs associés à un ticker.
     *
     * @param ticker Symbole boursier de l'entreprise
     */
    void deleteByTicker(String ticker);

    /**
     * Indique si des documents/vecteurs existent déjà pour le ticker spécifié.
     *
     * @param ticker Symbole boursier de l'entreprise
     * @return true si des vecteurs sont présents dans le store
     */
    boolean hasDocuments(String ticker);
}
