package com.sandy.scratchpad.geogebra.cmdleech;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.util.*;

public class GeogebraMkSite {
    
    private static final Logger log = Logger.getLogger( GeogebraMkSite.class ) ;
    
    public static void main( String[] args ) throws Exception {
        GeogebraMkSite app = new GeogebraMkSite() ;
        app.generateSite() ;
    }
    
    public static Map<String, Command> CMD_MAP = new HashMap<>() ;
    
    private final TemplateEngine te ;
    private final ArrayList<CommandGroup> commandGroups = new ArrayList<>() ;
    private final ArrayList<Command> allCommands = new ArrayList<>() ;
    
    private final Map<String, Command> cmdMap = new HashMap<>() ;
    private final Map<String, CommandGroup> commandGroupMap = new HashMap<>() ;
    
    public GeogebraMkSite() {
        this.te = new TemplateEngine() ;
    }
    
    private void generateSite() throws Exception {
        
        parseContent() ;
        
        copyStaticFiles() ;
        generateAllCommandsIndex() ;
        generateAllCommandGroupIndex() ;
        generateAllCommandHelpFiles() ;
    }
    
    private void parseContent() throws Exception {
        File[] directories = GeogebraCommandLeecher.BASE_DIR.listFiles() ;
        assert directories != null;
        for ( File dir : directories ) {
            if ( dir.isDirectory() ) {
                CommandGroup cmdGroup = new CommandGroup( dir ) ;
                allCommands.addAll( cmdGroup.getCommands() ) ;
                commandGroups.add( cmdGroup ) ;
                commandGroupMap.put( cmdGroup.getGroupName(), cmdGroup ) ;
            }
        }
        allCommands.sort( Comparator.comparing( c -> c.cmdName ) ) ;
        allCommands.forEach( cmd -> cmdMap.put( cmd.cmdName, cmd ) ) ;
    }
    
    private void copyStaticFiles() throws Exception{
        log.debug( "Copying static files" ) ;
        
        te.copyDirectory( "images", "images" );
        te.copyStatic( "templates/style.css", "style.css" ) ;
        te.copyStatic( "templates/site.css", "site.css" ) ;
        te.processTemplate( "templates/index.ftlh", "index.html" ) ;
        te.processTemplate( "templates/cmd-group.ftlh", "cmd-group.html" ) ;
    }
    
    private void generateAllCommandsIndex() throws Exception {
        log.debug( "Generating all commands index" ) ;
        
        Map<String, Object> model = te.getBaseModel( "All Commands" ) ;
        model.put( "allCommands", new LinkedHashSet<>( allCommands ) ) ;
        
        te.processTemplate( "templates/all-commands.ftlh", "commands/all-commands.html", model ) ;
    }
    
    private void generateAllCommandGroupIndex() throws Exception {
        log.debug( "Generating all command groups index" ) ;
        
        Map<String, Object> model ;
        
        for( CommandGroup cmdGroup : commandGroups ) {
            log.debug( "  Generating command group index for : " + cmdGroup.getGroupName() ) ;
            
            List<Command> commands = cmdGroup.getCommands() ;
            commands.sort( Comparator.comparing( c -> c.cmdName ) ) ;
            
            model = te.getBaseModel( cmdGroup.getGroupName() ) ;
            model.put( "commands", commands ) ;
            
            te.processTemplate( "templates/cmd-group-index.ftlh",
                                "commands/" + cmdGroup.getGroupName() + "/index.html", model ) ;
        }
    }
    
    private void generateAllCommandHelpFiles() throws Exception {
        
        log.debug( "Generating all command files" ) ;
        
        Map<String, Object> model ;
        
        for( CommandGroup cmdGroup : commandGroups ) {
            log.debug( "  Generating command files for group : " + cmdGroup.getGroupName() ) ;
            
            for( Command command : cmdGroup.getCommands() ) {
                log.debug( "    Generating command file for : " + command.getCmdName() ) ;
                
                model = te.getBaseModel( command.getCmdName() ) ;
                model.put( "command", command ) ;
                
                String targetPath = "commands/" + cmdGroup.getGroupName() + "/" + command.getCmdName() + ".html" ;
                
                File targetFile = te.processTemplate( "templates/cmd.ftlh", targetPath, model ) ;
                postProcessCommandFile( command, targetFile ) ;
            }
        }
    }
    
    private void postProcessCommandFile( Command command, File htmlFile ) throws Exception {
        
        String fileContents = FileUtils.readFileToString( htmlFile, "UTF-8" ) ;
        Document doc = Jsoup.parse( fileContents ) ;
        
        rewriteImgSources( doc ) ;
        rewriteLinkRefs( command, doc ) ;
        
        FileUtils.writeStringToFile( htmlFile, doc.outerHtml(), "UTF-8" ) ;
    }
    
    private void rewriteImgSources( Document doc ) {
        doc.select( "img[src]" ).forEach( img -> {
            String imgSrc = img.attr( "src" ) ;
            if( imgSrc.startsWith( "../../_images/" ) ) {
                img.attr( "src", imgSrc.replace( "_images", "images" ) ) ;
            }
            else if( imgSrc.startsWith( "../_images/" ) ) {
                img.attr( "src", imgSrc.replace( "_images", "../images" ) ) ;
            }
            else {
                log.error( imgSrc ) ;
            }
        }) ;
    }
    
    private void rewriteLinkRefs( Command currentCmd, Document doc ) {
        
        doc.select( "a[href]" ).forEach( a -> {
            String href = a.attr( "href" ) ;
            if( !href.startsWith( "https://" ) &&
                !href.startsWith( "#" ) &&
                !href.startsWith( "http://" ) ) {
                
                if( href.startsWith( "../../tools/" ) ) {
                    a.attr( "href", href.replace( "../../tools", "https://geogebra.github.io/docs/manual/en/tools" ) ) ;
                }
                else if( href.startsWith( "../../" ) ) {
                    a.attr( "href", href.replace( "../../", "https://geogebra.github.io/docs/manual/en/" ) ) ;
                }
                else if( href.startsWith( "../" ) && href.endsWith( "/" ) ) {
                    
                    String cmdName = href.substring( 3, href.length() - 1 ) ;
                    Command cmd = cmdMap.get( cmdName ) ;
                    if( cmd != null ) {
                        if( cmd.groupName.equals( currentCmd.groupName ) ) {
                            a.attr( "href", cmd.getFileName() ) ;
                        }
                        else {
                            a.attr( "href", "../" + cmd.groupName + "/" + cmd.getFileName() ) ;
                        }
                    }
                    else {
                        // Check if it is a command group
                        String groupName = cmdName.replaceAll( "_", " " ) ;
                        if( commandGroupMap.containsKey( groupName ) ) {
                            CommandGroup cmdGroup = commandGroupMap.get( groupName ) ;
                            a.attr( "href", "../" + cmdGroup.getGroupName() + "/index.html" ) ;
                            a.attr( "target", "group-cmds-frame" ) ;
                        }
                        else {
                            a.attr( "href", "https://geogebra.github.io/docs/manual/en/" + cmdName + "/" ) ;
                        }
                    }
                }
                else {
                    log.error( "ERROR: Unclassified link - " + href );
                }
            }
        }) ;
    }
}
