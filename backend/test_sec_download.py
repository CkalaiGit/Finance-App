import os
import sys
import requests

# CONSTANTES & HEADERS
USER_AGENT = "CairedineFinance contact@cairedine.com"
HEADERS = {"User-Agent": USER_AGENT, "Accept-Encoding": "gzip, deflate"}


def get_cik(ticker: str) -> str:
    """Récupère le CIK 10 chiffres pour un ticker donné."""
    url = "https://www.sec.gov/files/company_tickers.json"
    response = requests.get(url, headers=HEADERS)
    response.raise_for_status()

    data = response.json()
    ticker_upper = ticker.upper()

    for item in data.values():
        if item["ticker"] == ticker_upper:
            # Formatage CIK sur 10 chiffres avec zéros devant
            return f"{item['cik_str']:010d}"

    raise ValueError(f"Ticker introuvable sur la SEC : {ticker}")


def get_latest_filing_metadata(cik_10d: str, form_type: str = "10-K") -> dict:
    """Récupère les métadonnées du dernier rapport du type demandé (10-K, 10-Q, etc.)."""
    url = f"https://data.sec.gov/submissions/CIK{cik_10d}.json"
    response = requests.get(url, headers=HEADERS)
    response.raise_for_status()

    recent = response.json().get("filings", {}).get("recent", {})
    forms = recent.get("form", [])
    accession_numbers = recent.get("accessionNumber", [])
    filing_dates = recent.get("filingDate", [])
    primary_docs = recent.get("primaryDocument", [])

    for i, form in enumerate(forms):
        if form.upper() == form_type.upper():
            return {
                "accessionNumber": accession_numbers[i],
                "filingDate": filing_dates[i],
                "primaryDocument": primary_docs[i],
                "form": form,
            }

    raise ValueError(f"Aucun rapport de type {form_type} trouvé pour le CIK {cik_10d}")


def download_filing(
    ticker: str, form_type: str = "10-K", output_dir: str = "./downloads"
):
    """Orchestre la récupération et le téléchargement du document."""
    os.makedirs(output_dir, exist_ok=True)

    print(f"[*] Recherche du CIK pour {ticker}...")
    cik_10d = get_cik(ticker)
    cik_raw = str(int(cik_10d))  # CIK sans zéros superflus pour le path
    print(f"[+] CIK trouvé : {cik_10d} (ID brut : {cik_raw})")

    print(f"[*] Recherche du dernier rapport {form_type}...")
    metadata = get_latest_filing_metadata(cik_10d, form_type)
    accession = metadata["accessionNumber"]
    clean_accession = accession.replace("-", "")
    primary_doc = metadata["primaryDocument"]

    print(f"[+] Rapport identifié :")
    print(f"    - Date        : {metadata['filingDate']}")
    print(f"    - Accession N°: {accession}")
    print(f"    - Fichier     : {primary_doc}")

    # Construction de l'URL directe du fichier HTML principal
    doc_url = f"https://www.sec.gov/Archives/edgar/data/{cik_raw}/{clean_accession}/{primary_doc}"
    print(f"[*] Téléchargement depuis : {doc_url}")

    res = requests.get(doc_url, headers=HEADERS)
    res.raise_for_status()

    output_filename = os.path.join(
        output_dir, f"{ticker.upper()}_{form_type}_{metadata['filingDate']}.htm"
    )
    with open(output_filename, "wb") as f:
        f.write(res.content)

    file_size_mb = os.path.getsize(output_filename) / (1024 * 1024)
    print(
        f"[OK] Document téléchargé avec succès : {output_filename} ({file_size_mb:.2f} MB)"
    )


if __name__ == "__main__":
    # Test AAPL 10-K
    print("=== TEST 1 : AAPL (10-K) ===")
    download_filing(ticker="AAPL", form_type="10-K")

    # Test NVDA 10-Q
    print("\n=== TEST 2 : NVDA (10-Q) ===")
    download_filing(ticker="NVDA", form_type="10-Q")
