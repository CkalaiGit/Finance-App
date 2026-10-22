<#import "template.ftl" as layout>
<@layout.registrationLayout title="Erreur - FinanceApp"; section>
    <#if section = "header">Erreur
    <#elseif section = "form">
        <p class="form-instruction" role="alert">${message.summary}</p>
        <div class="form-footer">
            <a href="${url.loginUrl}" class="link-secondary">Retour à la page de connexion</a>
        </div>
    </#if>
</@layout.registrationLayout>
