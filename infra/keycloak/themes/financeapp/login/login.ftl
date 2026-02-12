<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">Connexion
    <#elseif section = "form">
        <form action="${url.loginAction}" method="post">
            <label>Utilisateur</label>
            <input id="username" name="username" class="pf-c-form-control" type="text" autofocus />
            <label>Mot de passe</label>
            <input id="password" name="password" class="pf-c-form-control" type="password" />
            <input type="submit" value="Se connecter"/>
        </form>
        <div style="text-align:center; margin-top:15px">
            <a href="${url.registrationUrl}" style="color:var(--accent-blue); font-size:14px">Créer un compte</a>
        </div>
    </#if>
</@layout.registrationLayout>
