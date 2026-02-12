<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">Inscription
    <#elseif section = "form">
        <form action="${url.registrationAction}" method="post">
            <input type="text" name="firstName" class="pf-c-form-control" placeholder="Prénom" />
            <input type="text" name="lastName" class="pf-c-form-control" placeholder="Nom" />
            <input type="text" name="email" class="pf-c-form-control" placeholder="Email" />
            <input type="password" name="password" class="pf-c-form-control" placeholder="Mot de passe" />
            <input type="submit" value="S'inscrire"/>
        </form>
    </#if>
</@layout.registrationLayout>
