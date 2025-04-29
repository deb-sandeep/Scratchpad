<!DOCTYPE html>
<html lang="en">

<head>
<#include "/templates/google-tag.ftlh">
    <meta charset="UTF-8">
    <title>${title}</title>
    <link rel="stylesheet" href="../style.css">
    <script>
    function searchText() {

        const text = document.getElementById( 'search-input' ).value ;
        const listItems = document.body.getElementsByTagName( "li" ) ;

        for( let i=0; i<listItems.length; i++ ) {
            let li = listItems[i] ;
            let cmdName = li.getElementsByTagName( "a" )[0].innerHTML ;

            if( cmdName.toUpperCase().indexOf( text.toUpperCase() ) == -1 ) {
                li.hidden = true ;
            }
            else {
                li.hidden = false ;
            }
        }
    }
    </script>
</head>

<body>
    <h3>All Commands</h3>
    <input type="text" id="search-input" onkeyup="searchText()" />
    <ul>
        <#list allCommands as command>
        <li><a href="${command.groupName}/${command.fileName}" target="cmd-detail-frame">${command.cmdName}</a> <span class="sm-grp-name">[${command.smallGroupName}]</span></li>  
        </#list>
    </ul>
</body>

</html>