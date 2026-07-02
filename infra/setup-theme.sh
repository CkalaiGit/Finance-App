#!/bin/bash

# Définition du chemin de base
THEME_DIR="keycloak/themes/financeapp/login"

echo "🚀 Création du thème Keycloak FinanceApp..."

# 1. Création des dossiers
mkdir -p "$THEME_DIR/resources/css"

# 2. Création de theme.properties
cat <<EOF > "$THEME_DIR/theme.properties"
parent=keycloak
import=common/keycloak
styles=css/financeapp.css
EOF

# 3. Création du CSS (Inspiré du mockup HTML)
cat <<EOF > "$THEME_DIR/resources/css/financeapp.css"
:root {
    --bg-primary: #0f172a;
    --bg-secondary: #1e293b;
    --bg-card: rgba(255, 255, 255, 0.03);
    --text-primary: #f8fafc;
    --text-secondary: #cbd5e1;
    --text-muted: #94a3b8;
    --link-color: #60a5fa; /* Contrast > 7:1 on dark bg */
    --btn-bg-start: #2563eb; /* Darker blue for contrast with white text */
    --btn-bg-end: #7c3aed; /* Darker purple for contrast with white text */
    --accent-blue: #3b82f6;
    --accent-purple: #8b5cf6;
    --border-color: rgba(255, 255, 255, 0.1);
    --shadow-glow: 0 8px 24px rgba(37, 99, 235, 0.4);
}

body {
    font-family: 'Inter', sans-serif;
    background: radial-gradient(circle at top right, #1e293b, #0f172a);
    color: var(--text-primary);
    margin: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
}

/* Keyboard focus visibility (RGAA 12.1) */
:focus {
    outline: 3px solid var(--link-color);
    outline-offset: 2px;
}
:focus:not(:focus-visible) {
    outline: none;
}

/* Skip link style (RGAA 12.7) */
.skip-link {
    position: absolute;
    top: -100px;
    left: 20px;
    background: var(--btn-bg-start);
    color: white;
    padding: 10px 20px;
    border-radius: 8px;
    z-index: 100;
    text-decoration: none;
    font-weight: bold;
    transition: top 0.2s ease;
}
.skip-link:focus {
    top: 20px;
    outline: 3px solid white;
}

.card-pf {
    background: var(--bg-card);
    backdrop-filter: blur(20px);
    border: 1px solid var(--border-color);
    border-radius: 24px;
    padding: 40px;
    width: 100%;
    max-width: 400px;
    box-shadow: 0 20px 50px rgba(0,0,0,0.3);
}

.logo-icon {
    width: 40px; height: 40px;
    background: linear-gradient(135deg, var(--accent-blue), var(--accent-purple));
    border-radius: 10px;
    display: flex; align-items: center; justify-content: center;
    font-weight: bold; margin: 0 auto 15px;
}

h1 { font-family: 'Inter', sans-serif; font-size: 22px; text-align: center; margin-bottom: 30px; font-weight: 600; }

.form-group {
    margin-bottom: 20px;
    text-align: left;
}

label {
    display: block;
    font-family: 'Inter', sans-serif;
    font-size: 14px;
    color: var(--text-secondary);
    margin-top: 20px;
    margin-bottom: 6px;
    text-align: left;
    padding-left: 1rem;
}
.form-group label {
    margin-top: 0;
}

input.pf-c-form-control {
    width: 100%; padding: 12px;
    background: rgba(0,0,0,0.25); border: 1px solid var(--border-color);
    border-radius: 8px; color: white; box-sizing: border-box;
    font-family: 'Inter', sans-serif;
    font-size: 14px;
}

button.btn-submit {
    width: 100%; padding: 12px; margin-top: 20px;
    background: linear-gradient(135deg, var(--btn-bg-start), var(--btn-bg-end));
    border: none; border-radius: 8px; color: white; font-weight: bold;
    cursor: pointer; box-shadow: var(--shadow-glow);
    font-family: 'Inter', sans-serif;
    font-size: 16px;
    transition: opacity 0.2s ease;
}
button.btn-submit:hover {
    opacity: 0.9;
}

.form-footer {
    text-align: center;
    margin-top: 20px;
}

.link-secondary {
    color: var(--link-color);
    font-size: 14px;
    text-decoration: none;
}
.link-secondary:hover {
    text-decoration: underline;
}

.form-instruction {
    font-size: 14px;
    color: var(--text-secondary);
    text-align: center;
    margin-bottom: 20px;
    line-height: 1.5;
}

.alert { padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; font-size: 14px; line-height: 1.4; text-align: left; }
.alert-error { background: rgba(239, 68, 68, 0.15); border: 1px solid #ef4444; color: #fca5a5; }
EOF

# 4. Création de template.ftl
cat <<EOF > "$THEME_DIR/template.ftl"
<#macro registrationLayout bodyClass="" displayMessage=true title="FinanceApp">
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <title>\${title}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="https://fonts.googleapis.com/css2?family=Crimson+Text&family=Inter:wght@400;600&display=swap" rel="stylesheet">
    <#if properties.styles?has_content><#list properties.styles?split(' ') as style><link href="\${url.resourcesPath}/\${style}" rel="stylesheet" /></#list></#if>
</head>
<body>
    <a href="#main-content" class="skip-link">Aller au contenu principal</a>
    <main id="main-content" class="card-pf">
        <div class="logo-icon" aria-hidden="true">Ƒ</div>
        <#if displayMessage && message?has_content>
            <div class="alert alert-\${message.type}" role="alert">\${kcSanitize(message.summary)?no_esc}</div>
        </#if>
        <h1><#nested "header"></h1>
        <#nested "form">
    </main>
</body>
</html>
</#macro>
EOF

# 5. Création de login.ftl
cat <<EOF > "$THEME_DIR/login.ftl"
<#import "template.ftl" as layout>
<@layout.registrationLayout title="Connexion - FinanceApp"; section>
    <#if section = "header">Connexion
    <#elseif section = "form">
        <form action="\${url.loginAction}" method="post">
            <div class="form-group">
                <label for="username">Nom d'utilisateur ou adresse e-mail</label>
                <input id="username" name="username" class="pf-c-form-control" type="text" autocomplete="username" required aria-required="true" autofocus />
            </div>
            <div class="form-group">
                <label for="password">Mot de passe</label>
                <input id="password" name="password" class="pf-c-form-control" type="password" autocomplete="current-password" required aria-required="true" />
            </div>
            <button type="submit" class="btn-submit">Se connecter</button>
        </form>
        <div class="form-footer">
            <a href="\${url.registrationUrl}" class="link-secondary">Créer un compte</a>
        </div>
    </#if>
</@layout.registrationLayout>
EOF

# 6. Création de register.ftl
cat <<EOF > "$THEME_DIR/register.ftl"
<#import "template.ftl" as layout>
<@layout.registrationLayout title="Inscription - FinanceApp"; section>
    <#if section = "header">Inscription
    <#elseif section = "form">
        <form action="\${url.registrationAction}" method="post">
            <div class="form-group">
                <label for="firstName">Prénom</label>
                <input id="firstName" type="text" name="firstName" class="pf-c-form-control" autocomplete="given-name" required aria-required="true" />
            </div>
            <div class="form-group">
                <label for="lastName">Nom</label>
                <input id="lastName" type="text" name="lastName" class="pf-c-form-control" autocomplete="family-name" required aria-required="true" />
            </div>
            <div class="form-group">
                <label for="email">Adresse e-mail</label>
                <input id="email" type="email" name="email" class="pf-c-form-control" autocomplete="email" required aria-required="true" />
            </div>
            <div class="form-group">
                <label for="password">Mot de passe</label>
                <input id="password" type="password" name="password" class="pf-c-form-control" autocomplete="new-password" required aria-required="true" />
            </div>
            <button type="submit" class="btn-submit">S'inscrire</button>
        </form>
        <div class="form-footer">
            <a href="\${url.loginUrl}" class="link-secondary">Déjà inscrit ? Se connecter</a>
        </div>
    </#if>
</@layout.registrationLayout>
EOF

# 7. Création de login-reset-password.ftl
cat <<EOF > "$THEME_DIR/login-reset-password.ftl"
<#import "template.ftl" as layout>
<@layout.registrationLayout title="Réinitialisation du mot de passe - FinanceApp"; section>
    <#if section = "header">Récupération
    <#elseif section = "form">
        <form action="\${url.loginAction}" method="post">
            <p class="form-instruction">Entrez votre adresse e-mail pour réinitialiser votre mot de passe.</p>
            <div class="form-group">
                <label for="username">Adresse e-mail</label>
                <input id="username" type="email" name="username" class="pf-c-form-control" autocomplete="email" required aria-required="true" autofocus />
            </div>
            <button type="submit" class="btn-submit">Envoyer</button>
        </form>
        <div class="form-footer">
            <a href="\${url.loginUrl}" class="link-secondary">Retour à la connexion</a>
        </div>
    </#if>
</@layout.registrationLayout>
EOF

# 8. Création de error.ftl et info.ftl
cat <<EOF > "$THEME_DIR/error.ftl"
<#import "template.ftl" as layout>
<@layout.registrationLayout title="Erreur - FinanceApp"; section>
    <#if section = "header">Erreur
    <#elseif section = "form">
        <p class="form-instruction" role="alert">\${message.summary}</p>
        <div class="form-footer">
            <a href="\${url.loginUrl}" class="link-secondary">Retour à la page de connexion</a>
        </div>
    </#if>
</@layout.registrationLayout>
EOF

cat <<EOF > "$THEME_DIR/info.ftl"
<#import "template.ftl" as layout>
<@layout.registrationLayout title="Information - FinanceApp"; section>
    <#if section = "header">Information
    <#elseif section = "form">
        <p class="form-instruction">\${message.summary}</p>
        <div class="form-footer">
            <a href="\${url.loginUrl}" class="link-secondary">Retour à la page de connexion</a>
        </div>
    </#if>
</@layout.registrationLayout>
EOF

echo "✅ Thème généré avec succès dans $THEME_DIR"
