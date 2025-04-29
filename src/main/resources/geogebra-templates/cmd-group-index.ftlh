<!DOCTYPE html>
<html lang="en">

<head>
<#include "/templates/google-tag.ftlh">
    <meta charset="UTF-8">
    <title>${title}</title>
    <link rel="stylesheet" href="../../style.css">
</head>

<body>
    <h3>${title}</h3>
    <ul>
        <#list commands as command>
        <li><a href="${command.fileName}" target="cmd-detail-frame">${command.cmdName}</a></li>  
        </#list>
    </ul>
</body>

</html>