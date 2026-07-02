<#macro registrationLayout bodyClass="" displayMessage=true title="FinanceApp">
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <title>${title}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="https://fonts.googleapis.com/css2?family=Crimson+Text&family=Inter:wght@400;600&display=swap" rel="stylesheet">
    <#if properties.styles?has_content><#list properties.styles?split(' ') as style><link href="${url.resourcesPath}/${style}" rel="stylesheet" /></#list></#if>
</head>
<body>
    <a href="#main-content" class="skip-link">Aller au contenu principal</a>
    <main id="main-content" class="card-pf">
        <div class="logo-icon" aria-hidden="true">Ƒ</div>
        <#if displayMessage && message?has_content>
            <div class="alert alert-${message.type}" role="alert">${kcSanitize(message.summary)?no_esc}</div>
        </#if>
        <h1><#nested "header"></h1>
        <#nested "form">
    </main>
</body>
</html>
</#macro>
