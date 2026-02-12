<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">Information
    <#elseif section = "form"><p>${message.summary}</p></#if>
</@layout.registrationLayout>
