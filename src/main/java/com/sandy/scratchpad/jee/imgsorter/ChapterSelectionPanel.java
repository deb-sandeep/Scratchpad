package com.sandy.scratchpad.jee.imgsorter;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent ;
import java.awt.event.ActionListener ;
import java.io.File ;
import java.io.FileFilter ;
import java.io.IOException ;
import java.util.ArrayList ;
import java.util.Arrays ;
import java.util.Comparator ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;
import java.util.regex.Matcher ;
import java.util.regex.Pattern ;

@SuppressWarnings( "serial" )
public class ChapterSelectionPanel extends JPanel
    implements ActionListener {

    static final Logger log = Logger.getLogger( ImageSelectionPanel.class ) ;

    private static final Pattern TRAILING_NUM_PATTERN =
            Pattern.compile( "(\\d+)(?=\\.[^.]+$)" ) ;

    private QBankImageSorter parent          = null ;
    private JButton       removeChapterBtn= new JButton( "X" ) ;

    private DefaultListModel<String> listModel   = new DefaultListModel<>() ;
    private JList<String>            chapterList = new JList<>( listModel ) ;

    private File rootFolder = null ;

    public ChapterSelectionPanel( QBankImageSorter parent ) {
        this.parent = parent ;
        setUpUI() ;
    }

    private void setUpUI() {
        setLayout( new BorderLayout() ) ;

        chapterList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION ) ;

        JScrollPane sp = new JScrollPane( chapterList ) ;
        JPanel btnPanel = new JPanel( new BorderLayout() ) ;
        btnPanel.add( new JLabel( "Chapters" ), BorderLayout.CENTER ) ;
        btnPanel.add( removeChapterBtn, BorderLayout.EAST ) ;

        add( btnPanel, BorderLayout.NORTH ) ;
        add( sp, BorderLayout.CENTER ) ;

        setUpEventListeners() ;
    }

    private void setUpEventListeners() {
        removeChapterBtn.addActionListener( this ) ;
    }

    @Override
    public void actionPerformed( ActionEvent e ) {

        if( e.getSource() == removeChapterBtn ) {
            int selectedIndex = chapterList.getSelectedIndex() ;
            if( selectedIndex != -1 ) {
                listModel.remove( selectedIndex ) ;
                selectIndexOrNearest( selectedIndex ) ;
            }
        }
    }

    public void loadChapters( File root ) {

        this.rootFolder = root ;
        listModel.clear() ;

        File[] files = root.listFiles( new FileFilter() {
            public boolean accept( File file ) {
                return file.isDirectory() &&
                       new File( file, "pages" ).isDirectory() ;
            }
        } ) ;

        if( files != null && files.length > 0 ) {
            Arrays.sort( files, new Comparator<File>() {
                public int compare( File f1, File f2 ) {
                    return f1.getName().compareTo( f2.getName() ) ;
                }
            } ) ;

            for( File file : files ) {
                listModel.addElement( file.getName() ) ;
            }
            chapterList.setSelectedIndex( 0 ) ;
        }
    }

    public String acceptFiles( List<File> srcFiles )
        throws IOException {

        String selectedChapter = chapterList.getSelectedValue() ;

        if( selectedChapter == null ) {
            return "Please select a target chapter." ;
        }

        File chapterFolder = new File( this.rootFolder, selectedChapter ) ;
        File pageFolder     = new File( chapterFolder, "pages" ) ;

        Map<File, File> srcToDest = new HashMap<>() ;
        List<String> unparseableNames = new ArrayList<>() ;
        List<String> collisions       = new ArrayList<>() ;

        for( File srcFile : srcFiles ) {

            String srcName = srcFile.getName() ;
            Matcher matcher = TRAILING_NUM_PATTERN.matcher( srcName ) ;

            if( !matcher.find() ) {
                unparseableNames.add( srcName ) ;
                continue ;
            }

            int pageNum = Integer.parseInt( matcher.group( 1 ) ) ;
            String ext = srcName.substring( srcName.lastIndexOf( '.' ) + 1 ) ;
            String destFileName = selectedChapter + "-" +
                    String.format( "%03d", pageNum ) + "." + ext ;

            File destFile = new File( pageFolder, destFileName ) ;
            if( destFile.exists() ) {
                collisions.add( destFileName ) ;
                continue ;
            }

            srcToDest.put( srcFile, destFile ) ;
        }

        if( !unparseableNames.isEmpty() || !collisions.isEmpty() ) {
            StringBuilder msg = new StringBuilder() ;
            if( !unparseableNames.isEmpty() ) {
                msg.append( "Could not determine page number for: " )
                   .append( String.join( ", ", unparseableNames ) )
                   .append( ". " ) ;
            }
            if( !collisions.isEmpty() ) {
                msg.append( "Destination already exists for: " )
                   .append( String.join( ", ", collisions ) )
                   .append( "." ) ;
            }
            return msg.toString() ;
        }

        for( Map.Entry<File, File> entry : srcToDest.entrySet() ) {
            FileUtils.moveFile( entry.getKey(), entry.getValue() ) ;
        }

        int selectedIndex = chapterList.getSelectedIndex() ;
        listModel.removeElement( selectedChapter ) ;
        selectIndexOrNearest( selectedIndex ) ;

        return null ;
    }

    private void selectIndexOrNearest( int index ) {
        if( !listModel.isEmpty() ) {
            int nextIndex = Math.min( index, listModel.getSize() - 1 ) ;
            chapterList.setSelectedIndex( nextIndex ) ;
        }
    }
}
