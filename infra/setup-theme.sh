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
    --accent-blue: #3b82f6;
    --accent-purple: #8b5cf6;
    --border-color: rgba(255, 255, 255, 0.1);
    --shadow-glow: 0 8px 24px rgba(59, 130, 246, 0.4);
}

body {
    font-family: 'Crimson Text', serif;
    background: radial-gradient(circle at top right, #1e293b, #0f172a);
    color: var(--text-primary);
    margin: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
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

h1 { font-family: 'Inter', sans-serif; font-size: 20px; text-align: center; margin-bottom: 30px; }

input.pf-c-form-control {
    width: 100%; padding: 12px; margin-top: 8px;
    background: rgba(0,0,0,0.2); border: 1px solid var(--border-color);
    border-radius: 8px; color: white; box-sizing: border-box;
}

input[type="submit"] {
    width: 100%; padding: 12px; margin-top: 20px;
    background: linear-gradient(135deg, var(--accent-blue), var(--accent-purple));
    border: none; border-radius: 8px; color: white; font-weight: bold;
    cursor: pointer; box-shadow: var(--shadow-glow);
}

.alert { padding: 10px; border-radius: 8px; margin-bottom: 15px; font-size: 14px; }
.alert-error { background: rgba(239, 68, 68, 0.2); border: 1px solid #ef4444; }
EOF

# 4. Création de template.ftl
cat <<EOF > "$THEME_DIR/template.ftl"
<#macro registrationLayout bodyClass="" displayMessage=true>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="https://fonts.googleapis.com/css2?family=Crimson+Text&family=Inter:wght@400;600&display=swap" rel="stylesheet">
    <#if properties.styles?has_content><#list properties.styles?split(' ') as style><link href="\${url.resourcesPath}/\${style}" rel="stylesheet" /></#list></#if>
</head>
<body>
    <div class="card-pf">
        <div class="logo-icon">Ƒ</div>
        <#if displayMessage && message?has_content>
            <div class="alert alert-\${message.type}">\${kcSanitize(message.summary)?no_esc}</div>
        </#if>
        <h1><#nested "header"></h1>
        <#nested "form">
    </div>
</body>
</html>
</#macro>
EOF

# 5. Création de login.ftl
cat <<EOF > "$THEME_DIR/login.ftl"
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">Connexion
    <#elseif section = "form">
        <form action="\${url.loginAction}" method="post">
            <label>Utilisateur</label>
            <input id="username" name="username" class="pf-c-form-control" type="text" autofocus />
            <label>Mot de passe</label>
            <input id="password" name="password" class="pf-c-form-control" type="password" />
            <input type="submit" value="Se connecter"/>
        </form>
        <div style="text-align:center; margin-top:15px">
            <a href="\${url.registrationUrl}" style="color:var(--accent-blue); font-size:14px">Créer un compte</a>
        </div>
    </#if>
</@layout.registrationLayout>
EOF

# 6. Création de register.ftl
cat <<EOF > "$THEME_DIR/register.ftl"
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">Inscription
    <#elseif section = "form">
        <form action="\${url.registrationAction}" method="post">
            <input type="text" name="firstName" class="pf-c-form-control" placeholder="Prénom" />
            <input type="text" name="lastName" class="pf-c-form-control" placeholder="Nom" />
            <input type="text" name="email" class="pf-c-form-control" placeholder="Email" />
            <input type="password" name="password" class="pf-c-form-control" placeholder="Mot de passe" />
            <input type="submit" value="S'inscrire"/>
        </form>
    </#if>
</@layout.registrationLayout>
EOF

# 7. Création de login-reset-password.ftl
cat <<EOF > "$THEME_DIR/login-reset-password.ftl"
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">Récupération
    <#elseif section = "form">
        <form action="\${url.loginAction}" method="post">
            <p style="font-size:14px; color:var(--text-secondary)">Entrez votre email pour réinitialiser.</p>
            <input type="text" name="username" class="pf-c-form-control" placeholder="Email" />
            <input type="submit" value="Envoyer"/>
        </form>
    </#if>
</@layout.registrationLayout>
EOF

# 8. Création de error.ftl et info.ftl
cat <<EOF > "$THEME_DIR/error.ftl"
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">Erreur
    <#elseif section = "form"><p>\${message.summary}</p><a href="\${url.loginUrl}">Retour</a></#if>
</@layout.registrationLayout>
EOF

cat <<EOF > "$THEME_DIR/info.ftl"
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">Information
    <#elseif section = "form"><p>\${message.summary}</p></#if>
</@layout.registrationLayout>
EOF

echo "✅ Thème généré avec succès dans $THEME_DIR"
