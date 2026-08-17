package com.cairedine.finance.app.intelligence.infrastructure.sec;

import com.cairedine.finance.app.intelligence.domain.model.SecFiling;
import com.cairedine.finance.app.intelligence.domain.port.IDocumentSourcePort;
import com.cairedine.finance.app.intelligence.infrastructure.sec.dto.SecFilingDocDto;
import com.cairedine.finance.app.intelligence.infrastructure.sec.dto.SecFilingIndexDto;
import com.cairedine.finance.app.intelligence.infrastructure.sec.dto.SecRecentFilingsDto;
import com.cairedine.finance.app.intelligence.infrastructure.sec.dto.SecSubmissionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adaptateur d'infrastructure pour l'extraction et le téléchargement des rapports officiels SEC EDGAR (10-K, 10-Q).
 */
@Component
@Slf4j
public class SecEdgarAdapter implements IDocumentSourcePort {

    private final RestClient secDataRestClient;
    private final RestClient secWwwRestClient;
    private final CikResolverService cikResolver;

    public SecEdgarAdapter(
            @Qualifier("secDataRestClient") RestClient secDataRestClient,
            @Qualifier("secWwwRestClient") RestClient secWwwRestClient,
            CikResolverService cikResolver) {
        this.secDataRestClient = secDataRestClient;
        this.secWwwRestClient = secWwwRestClient;
        this.cikResolver = cikResolver;
    }

    @Override
    public List<SecFiling> fetchFilings(String ticker, String formType, int limit) {
        log.info("Récupération des rapports SEC type='{}' (limit={}) pour le ticker '{}'", formType, limit, ticker);
        String cik = cikResolver.resolve(ticker);

        SecSubmissionDto submission;
        try {
            submission = secDataRestClient.get()
                    .uri("/submissions/CIK{cik}.json", cik)
                    .retrieve()
                    .body(SecSubmissionDto.class);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des soumissions SEC pour CIK {}: {}", cik, e.getMessage());
            return Collections.emptyList();
        }

        if (submission == null || submission.filings() == null || submission.filings().recent() == null) {
            log.warn("Aucune soumission récente trouvée pour CIK {}", cik);
            return Collections.emptyList();
        }

        SecRecentFilingsDto recent = submission.filings().recent();
        List<String> forms = recent.form();
        List<String> accessionNumbers = recent.accessionNumber();
        List<String> filingDates = recent.filingDate();
        List<String> primaryDocuments = recent.primaryDocument();

        if (forms == null || accessionNumbers == null) {
            return Collections.emptyList();
        }

        List<SecFiling> result = new ArrayList<>();
        int count = 0;

        for (int i = 0; i < forms.size() && count < limit; i++) {
            String form = forms.get(i);
            if (form != null && form.equalsIgnoreCase(formType)) {
                String accessionNumber = accessionNumbers.get(i);
                String filingDate = (filingDates != null && filingDates.size() > i) ? filingDates.get(i) : "UNKNOWN";
                String primaryDoc = (primaryDocuments != null && primaryDocuments.size() > i) ? primaryDocuments.get(i) : null;

                try {
                    String content = downloadFilingContent(cik, accessionNumber, primaryDoc);
                    if (content != null && !content.isBlank()) {
                        result.add(new SecFiling(
                                accessionNumber,
                                formType.toUpperCase(),
                                filingDate,
                                content,
                                Instant.now()
                        ));
                        count++;
                        log.info("Document SEC téléchargé avec succès: form='{}', accession='{}', date='{}'", formType, accessionNumber, filingDate);
                    }
                } catch (Exception e) {
                    log.warn("Échec du téléchargement du document SEC accession='{}': {}", accessionNumber, e.getMessage());
                }
            }
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean isAvailable(String ticker) {
        return cikResolver.isKnown(ticker);
    }

    /**
     * Télécharge le contenu brut (HTML / Texte) du document primaire associé à un filing SEC.
     */
    private String downloadFilingContent(String cik, String accessionNumber, String primaryDoc) {
        String cikRaw = String.valueOf(Long.parseLong(cik));
        String cleanAccession = accessionNumber.replace("-", "");

        if (primaryDoc != null && !primaryDoc.isBlank()) {
            String docUri = "/Archives/edgar/data/" + cikRaw + "/" + cleanAccession + "/" + primaryDoc;
            return secWwwRestClient.get()
                    .uri(docUri)
                    .retrieve()
                    .body(String.class);
        }

        // Fallback via index.json si le primaryDocument n'est pas renseigné
        String indexUri = "/Archives/edgar/data/" + cikRaw + "/" + cleanAccession + "/" + accessionNumber + "-index.json";
        try {
            SecFilingIndexDto indexDto = secWwwRestClient.get()
                    .uri(indexUri)
                    .retrieve()
                    .body(SecFilingIndexDto.class);

            if (indexDto != null && indexDto.directory() != null && indexDto.directory().item() != null) {
                for (SecFilingDocDto doc : indexDto.directory().item()) {
                    if (doc.name() != null && (doc.name().endsWith(".htm") || doc.name().endsWith(".html") || doc.name().endsWith(".txt"))) {
                        String docUri = "/Archives/edgar/data/" + cikRaw + "/" + cleanAccession + "/" + doc.name();
                        return secWwwRestClient.get()
                                .uri(docUri)
                                .retrieve()
                                .body(String.class);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Fallback index.json impossible pour accession {}: {}", accessionNumber, e.getMessage());
        }

        return null;
    }
}