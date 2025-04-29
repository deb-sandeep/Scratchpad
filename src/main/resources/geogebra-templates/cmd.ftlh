<!DOCTYPE html>
<html lang="en">

<head>
<#include "/templates/google-tag.ftlh">
    <meta charset="UTF-8">
    <title>${title}</title>
    <link rel="stylesheet" href="../../site.css">
<#include "/templates/mathjax.ftlh">
</head>

<body>
<#include "/raw-content/" + command.groupName + "/" + command.fileName>
</body>

</html>