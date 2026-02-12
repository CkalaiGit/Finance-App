<#macro registrationLayout bodyClass="" displayMessage=true>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="https://fonts.googleapis.com/css2?family=Crimson+Text&family=Inter:wght@400;600&display=swap" rel="stylesheet">
    <#if properties.styles?has_content><#list properties.styles?split(' ') as style><link href="${url.resourcesPath}/${style}" rel="stylesheet" /></#list></#if>
</head>
<body>
    <div class="card-pf">
        <div class="logo-icon">Ƒ</div>
        <#if displayMessage && message?has_content>
            <div class="alert alert-${message.type}">${kcSanitize(message.summary)?no_esc}</div>
        </#if>
        <h1><#nested "header"></h1>
        <#nested "form">
    </div>
</body>
</html>
</#macro>
