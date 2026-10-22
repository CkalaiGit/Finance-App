package com.cairedine.finance.app.webclient.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/**
 * Response DTO pour les données de profil d'entreprise.
 * Propose une vue complète des informations statiques d'une entreprise
 * sans les métriques dynamiques (évite le surcharger de données).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompanyProfileResponse(
        String symbol,
        String companyName,
        String industry,
        String description,
        String imageUrl,
        BigDecimal price,
        BigDecimal beta,
        BigDecimal marketCap
) {}

