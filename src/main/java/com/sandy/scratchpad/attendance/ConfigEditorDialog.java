package com.sandy.scratchpad.attendance;

import com.sandy.scratchpad.attendance.AttendanceConfig.ConfigException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * Raw text editor for attendance.properties, holidays.txt and mandatory.txt. The edited
 * texts are validated as you type; Save is enabled only when both are valid,
 * and writes the files and then reloads the app.
 */
public class ConfigEditorDialog extends JDialog {

    private static final Color OK_COLOR  = new Color( 0x1B6E20 ) ;
    private static final Color ERR_COLOR = new Color( 0xB00020 ) ;

    private final Runnable onSaved ;

    private final JTextArea configArea   = createEditor() ;
    private final JTextArea holidaysArea = createEditor() ;
    private final JTextArea mandatoryArea = createEditor() ;
    private final JTextArea statusArea   = new JTextArea( 5, 60 ) ;
    private final JButton saveBtn        = new JButton( "Save & Reload" ) ;
    private final Timer validateTimer    = new Timer( 300, e -> validateTexts() ) ;

    private boolean dirty = false ;
    private boolean valid = true ;

    public ConfigEditorDialog( JFrame owner, Runnable onSaved ) {
        super( owner, "Edit configuration — " + AttendanceConfig.APP_DIR, true ) ;
        this.onSaved = onSaved ;

        JTabbedPane tabs = new JTabbedPane() ;
        tabs.addTab( AttendanceConfig.CONFIG_FILE.getName(), new JScrollPane( configArea ) ) ;
        tabs.addTab( AttendanceConfig.HOLIDAYS_FILE.getName(), new JScrollPane( holidaysArea ) ) ;
        tabs.addTab( AttendanceConfig.MANDATORY_FILE.getName(), new JScrollPane( mandatoryArea ) ) ;

        statusArea.setEditable( false ) ;
        statusArea.setLineWrap( true ) ;
        statusArea.setWrapStyleWord( true ) ;
        statusArea.setFont( statusArea.getFont().deriveFont( 12f ) ) ;
        statusArea.setBackground( getBackground() ) ;
        JScrollPane statusScroll = new JScrollPane( statusArea ) ;
        statusScroll.setBorder( BorderFactory.createTitledBorder( "Validation" ) ) ;

        JButton revertBtn = new JButton( "Revert" ) ;
        revertBtn.setToolTipText( "Discard edits and re-read the files from disk" ) ;
        revertBtn.addActionListener( e -> {
            if( !dirty || confirmDiscard() ) loadTexts() ;
        } ) ;
        saveBtn.addActionListener( e -> save() ) ;
        JButton closeBtn = new JButton( "Close" ) ;
        closeBtn.addActionListener( e -> closeIfAllowed() ) ;

        JPanel buttons = new JPanel( new FlowLayout( FlowLayout.RIGHT ) ) ;
        buttons.add( revertBtn ) ;
        buttons.add( saveBtn ) ;
        buttons.add( closeBtn ) ;

        JPanel south = new JPanel( new BorderLayout() ) ;
        south.add( statusScroll, BorderLayout.CENTER ) ;
        south.add( buttons, BorderLayout.SOUTH ) ;

        getContentPane().setLayout( new BorderLayout( 0, 4 ) ) ;
        getContentPane().add( tabs, BorderLayout.CENTER ) ;
        getContentPane().add( south, BorderLayout.SOUTH ) ;

        validateTimer.setRepeats( false ) ;
        DocumentListener dl = new DocumentListener() {
            @Override public void insertUpdate( DocumentEvent e ) { onEdit() ; }
            @Override public void removeUpdate( DocumentEvent e ) { onEdit() ; }
            @Override public void changedUpdate( DocumentEvent e ) { onEdit() ; }
        } ;
        configArea.getDocument().addDocumentListener( dl ) ;
        holidaysArea.getDocument().addDocumentListener( dl ) ;
        mandatoryArea.getDocument().addDocumentListener( dl ) ;

        setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE ) ;
        addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing( WindowEvent e ) {
                closeIfAllowed() ;
            }
        } ) ;

        loadTexts() ;
        setSize( new Dimension( 760, 640 ) ) ;
        setLocationRelativeTo( owner ) ;
    }

    private static JTextArea createEditor() {
        JTextArea a = new JTextArea() ;
        a.setFont( new Font( Font.MONOSPACED, Font.PLAIN, 13 ) ) ;
        a.setTabSize( 4 ) ;
        a.setMargin( new java.awt.Insets( 6, 6, 6, 6 ) ) ;
        return a ;
    }

    private void loadTexts() {
        try {
            configArea.setText( AttendanceConfig.readConfigText() ) ;
            holidaysArea.setText( AttendanceConfig.readHolidaysText() ) ;
            mandatoryArea.setText( AttendanceConfig.readMandatoryText() ) ;
            configArea.setCaretPosition( 0 ) ;
            holidaysArea.setCaretPosition( 0 ) ;
            mandatoryArea.setCaretPosition( 0 ) ;
        }
        catch( IOException e ) {
            showStatus( "Could not read config files: " + e.getMessage(), ERR_COLOR ) ;
        }
        validateTimer.stop() ;
        validateTexts() ;
        setDirty( false ) ;
    }

    private void onEdit() {
        setDirty( true ) ;
        validateTimer.restart() ;
    }

    private void setDirty( boolean d ) {
        dirty = d ;
        saveBtn.setEnabled( dirty && valid ) ;
    }

    /** Returns true if both texts are valid. */
    private boolean validateTexts() {
        try {
            AttendanceConfig.validate( configArea.getText(), holidaysArea.getText(), mandatoryArea.getText() ) ;
            valid = true ;
            showStatus( "✓ Configuration is valid.", OK_COLOR ) ;
        }
        catch( ConfigException e ) {
            valid = false ;
            showStatus( e.getErrors().size() + " problem(s):\n• " +
                        String.join( "\n• ", e.getErrors() ), ERR_COLOR ) ;
        }
        saveBtn.setEnabled( dirty && valid ) ;
        return valid ;
    }

    private void showStatus( String msg, Color color ) {
        statusArea.setForeground( color ) ;
        statusArea.setText( msg ) ;
        statusArea.setCaretPosition( 0 ) ;
    }

    private void save() {
        validateTimer.stop() ;
        if( !validateTexts() ) return ;
        try {
            AttendanceConfig.writeRaw( configArea.getText(), holidaysArea.getText(), mandatoryArea.getText() ) ;
        }
        catch( IOException e ) {
            showStatus( "Could not save: " + e.getMessage(), ERR_COLOR ) ;
            return ;
        }
        setDirty( false ) ;
        showStatus( "✓ Saved and reloaded.", OK_COLOR ) ;
        onSaved.run() ;
    }

    private boolean confirmDiscard() {
        return JOptionPane.showConfirmDialog( this, "Discard unsaved edits?", "Unsaved changes",
                JOptionPane.OK_CANCEL_OPTION ) == JOptionPane.OK_OPTION ;
    }

    private void closeIfAllowed() {
        if( !dirty || confirmDiscard() ) {
            validateTimer.stop() ;
            dispose() ;
        }
    }
}
