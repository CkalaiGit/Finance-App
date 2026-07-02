<#import "template.ftl" as layout>
<@layout.registrationLayout title="Connexion - FinanceApp"; section>
    <#if section = "header">Connexion
    <#elseif section = "form">
        <form action="${url.loginAction}" method="post">
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
            <a href="${url.registrationUrl}" class="link-secondary">Créer un compte</a>
        </div>
    </#if>
</@layout.registrationLayout>
