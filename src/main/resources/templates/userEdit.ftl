<#import "parts/common.ftl" as c>

<@c.page>
    User editor
    <form method="post" action="/user">
        <input type="hidden" value="${user.id}" name="userId"/>
        <input type="text" value="${user.username}" name="username"/>
        <#list roles as role>
            <label><input type="checkbox" name="${role}" ${user.roles?seq_contains(role)?string("checked", "")}>${role}</label>
        </#list>
        <input type="hidden" name="_csrf" value="${_csrf.token}"/>
        <button type="submit">Save</button>
    </form>
</@c.page>