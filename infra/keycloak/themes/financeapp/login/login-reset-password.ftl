<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">Récupération
    <#elseif section = "form">
        <form action="${url.loginAction}" method="post">
            <p style="font-size:14px; color:var(--text-secondary)">Entrez votre email pour réinitialiser.</p>
            <input type="text" name="username" class="pf-c-form-control" placeholder="Email" />
            <input type="submit" value="Envoyer"/>
        </form>
    </#if>
</@layout.registrationLayout>
