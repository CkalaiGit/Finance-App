<#import "template.ftl" as layout>
<@layout.registrationLayout title="Réinitialisation du mot de passe - FinanceApp"; section>
    <#if section = "header">Récupération
    <#elseif section = "form">
        <form action="${url.loginAction}" method="post">
            <p class="form-instruction">Entrez votre adresse e-mail pour réinitialiser votre mot de passe.</p>
            <div class="form-group">
                <label for="username">Adresse e-mail</label>
                <input id="username" type="email" name="username" class="pf-c-form-control" autocomplete="email" required aria-required="true" autofocus />
            </div>
            <button type="submit" class="btn-submit">Envoyer</button>
        </form>
        <div class="form-footer">
            <a href="${url.loginUrl}" class="link-secondary">Retour à la connexion</a>
        </div>
    </#if>
</@layout.registrationLayout>
