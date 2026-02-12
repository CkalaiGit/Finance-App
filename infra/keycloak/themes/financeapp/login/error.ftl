<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">Erreur
    <#elseif section = "form"><p>${message.summary}</p><a href="${url.loginUrl}">Retour</a></#if>
</@layout.registrationLayout>
