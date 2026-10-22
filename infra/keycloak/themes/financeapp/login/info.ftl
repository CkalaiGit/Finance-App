<#import "template.ftl" as layout>
<@layout.registrationLayout title="Information - FinanceApp"; section>
    <#if section = "header">Information
    <#elseif section = "form">
        <p class="form-instruction">${message.summary}</p>
        <div class="form-footer">
            <a href="${url.loginUrl}" class="link-secondary">Retour à la page de connexion</a>
        </div>
    </#if>
</@layout.registrationLayout>
