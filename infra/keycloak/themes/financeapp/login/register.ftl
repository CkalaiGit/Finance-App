<#import "template.ftl" as layout>
<@layout.registrationLayout title="Inscription - FinanceApp"; section>
    <#if section = "header">Inscription
    <#elseif section = "form">
        <form action="${url.registrationAction}" method="post">
            <div class="form-group">
                <label for="firstName">Prénom</label>
                <input id="firstName" type="text" name="firstName" class="pf-c-form-control" autocomplete="given-name" required aria-required="true" />
            </div>
            <div class="form-group">
                <label for="lastName">Nom</label>
                <input id="lastName" type="text" name="lastName" class="pf-c-form-control" autocomplete="family-name" required aria-required="true" />
            </div>
            <div class="form-group">
                <label for="email">Adresse e-mail</label>
                <input id="email" type="email" name="email" class="pf-c-form-control" autocomplete="email" required aria-required="true" />
            </div>
            <div class="form-group">
                <label for="password">Mot de passe</label>
                <input id="password" type="password" name="password" class="pf-c-form-control" autocomplete="new-password" required aria-required="true" />
            </div>
            <button type="submit" class="btn-submit">S'inscrire</button>
        </form>
        <div class="form-footer">
            <a href="${url.loginUrl}" class="link-secondary">Déjà inscrit ? Se connecter</a>
        </div>
    </#if>
</@layout.registrationLayout>
