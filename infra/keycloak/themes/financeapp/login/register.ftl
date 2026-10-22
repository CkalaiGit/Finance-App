<#import "template.ftl" as layout>
<@layout.registrationLayout title="Inscription - FinanceApp"; section>
    <#if section = "header">Inscription
    <#elseif section = "form">
        <form action="${url.registrationAction}" method="post">
            <div class="form-group">
                <label for="firstName">Prénom</label>
                <input id="firstName" type="text" name="firstName" class="pf-c-form-control" autocomplete="given-name" value="${(register.formData.firstName)!''}" required aria-required="true" />
            </div>
            <div class="form-group">
                <label for="lastName">Nom</label>
                <input id="lastName" type="text" name="lastName" class="pf-c-form-control" autocomplete="family-name" value="${(register.formData.lastName)!''}" required aria-required="true" />
            </div>
            <div class="form-group">
                <label for="username">Nom d'utilisateur</label>
                <input id="username" type="text" name="username" class="pf-c-form-control" autocomplete="username" value="${(register.formData.username)!''}" required aria-required="true" />
            </div>
            <div class="form-group">
                <label for="email">Adresse e-mail</label>
                <input id="email" type="email" name="email" class="pf-c-form-control" autocomplete="email" value="${(register.formData.email)!''}" required aria-required="true" />
            </div>
            <div class="form-group">
                <label for="password">Mot de passe</label>
                <input id="password" type="password" name="password" class="pf-c-form-control" autocomplete="new-password" required aria-required="true" />
            </div>
            <div class="form-group">
                <label for="password-confirm">Confirmer le mot de passe</label>
                <input id="password-confirm" type="password" name="password-confirm" class="pf-c-form-control" autocomplete="new-password" required aria-required="true" />
            </div>
            <button type="submit" class="btn-submit">S'inscrire</button>
        </form>
        <div class="form-footer">
            <a href="${url.loginUrl}" class="link-secondary">Déjà inscrit ? Se connecter</a>
        </div>
    </#if>
</@layout.registrationLayout>
