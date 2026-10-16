package com.sandy.scratchpad.attendance;

import com.sandy.scratchpad.attendance.AttendanceConfig.ConfigException;
import lombok.extern.slf4j.Slf4j;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;

/**
 * Simulates the final attendance percentage for a configurable set of future
 * leave days. See {@link AttendanceConfig} for the configuration files.
 */
@Slf4j
public class AttendanceSimulator extends JFrame {

    private final AttendanceConfig config ;
    private final AttendanceModel model ;

    private final CalendarPanel calendarPanel ;
    private final AttendanceChart chart ;

    private final JTextField asOfField    = new JTextField( 9 ) ;
    private final JTextField totalField   = new JTextField( 4 ) ;
    private final JTextField presentField = new JTextField( 4 ) ;
    private final JTextField targetField  = new JTextField( 4 ) ;
    private final JTextField endField     = new JTextField( 9 ) ;

    private final JLabel finalPctLabel = new JLabel() ;
    private final JLabel detailLabel   = new JLabel() ;

    public AttendanceSimulator( AttendanceConfig config ) {
        super( "Attendance Simulator" ) ;
        this.config = config ;
        this.model = new AttendanceModel( config ) ;
        this.model.loadState() ;
        this.model.refresh() ;

        calendarPanel = new CalendarPanel( model ) ;
        chart = new AttendanceChart( model ) ;
        model.addListener( this::onModelChange ) ;

        JPanel north = new JPanel() ;
        north.setLayout( new BoxLayout( north, BoxLayout.Y_AXIS ) ) ;
        north.add( buildSettingsPanel() ) ;
        north.add( buildSummaryPanel() ) ;

        JScrollPane calScroll = new JScrollPane( calendarPanel ) ;
        calScroll.getVerticalScrollBar().setUnitIncrement( 16 ) ;

        JSplitPane split = new JSplitPane( JSplitPane.VERTICAL_SPLIT, calScroll, chart ) ;
        split.setResizeWeight( 0.6 ) ;

        getContentPane().setLayout( new BorderLayout() ) ;
        getContentPane().add( north, BorderLayout.NORTH ) ;
        getContentPane().add( split, BorderLayout.CENTER ) ;

        populateSettings() ;
        onModelChange() ;

        setDefaultCloseOperation( EXIT_ON_CLOSE ) ;
        setSize( 1280, 940 ) ;
        setLocationRelativeTo( null ) ;
        SwingUtilities.invokeLater( () -> split.setDividerLocation( 0.6 ) ) ;
    }

    private JPanel buildSettingsPanel() {
        JPanel p = new JPanel( new FlowLayout( FlowLayout.LEFT, 6, 4 ) ) ;
        p.setBorder( BorderFactory.createTitledBorder( "Baseline & settings  (" + AttendanceConfig.APP_DIR + ")" ) ) ;

        p.add( new JLabel( "As of" ) ) ;
        p.add( asOfField ) ;
        p.add( new JLabel( "Total days" ) ) ;
        p.add( totalField ) ;
        p.add( new JLabel( "Present" ) ) ;
        p.add( presentField ) ;
        p.add( Box.createHorizontalStrut( 10 ) ) ;
        p.add( new JLabel( "Target %" ) ) ;
        p.add( targetField ) ;
        p.add( new JLabel( "Till" ) ) ;
        p.add( endField ) ;

        JButton apply = new JButton( "Apply" ) ;
        apply.addActionListener( e -> applySettings() ) ;
        p.add( apply ) ;

        JButton edit = new JButton( "Edit config…" ) ;
        edit.setToolTipText( "Edit attendance.properties, holidays.txt and mandatory.txt" ) ;
        edit.addActionListener( e -> openConfigEditor() ) ;
        p.add( edit ) ;

        JButton reload = new JButton( "Reload config" ) ;
        reload.setToolTipText( "Re-read attendance.properties, holidays.txt and mandatory.txt" ) ;
        reload.addActionListener( e -> reloadConfig() ) ;
        p.add( reload ) ;

        JButton reset = new JButton( "Clear marks" ) ;
        reset.setToolTipText( "Revert every day to its default (present on working days)" ) ;
        reset.addActionListener( e -> {
            int ans = JOptionPane.showConfirmDialog( this, "Clear all absent/off marks?",
                    "Clear marks", JOptionPane.OK_CANCEL_OPTION ) ;
            if( ans == JOptionPane.OK_OPTION ) model.clearOverrides() ;
        } ) ;
        p.add( reset ) ;

        return p ;
    }

    private JPanel buildSummaryPanel() {
        JPanel p = new JPanel( new BorderLayout( 16, 0 ) ) ;
        p.setBorder( BorderFactory.createEmptyBorder( 4, 12, 6, 12 ) ) ;
        finalPctLabel.setFont( finalPctLabel.getFont().deriveFont( Font.BOLD, 28f ) ) ;
        detailLabel.setFont( detailLabel.getFont().deriveFont( 13f ) ) ;
        p.add( finalPctLabel, BorderLayout.WEST ) ;
        p.add( detailLabel, BorderLayout.CENTER ) ;

        JLabel legend = new JLabel( "<html>Click a day: present ⇄ absent<br>" +
                "Right-click: off ⇄ working</html>" ) ;
        legend.setForeground( Color.GRAY ) ;
        p.add( legend, BorderLayout.EAST ) ;
        return p ;
    }

    private void populateSettings() {
        asOfField.setText( config.getAsOfDate().toString() ) ;
        totalField.setText( String.valueOf( config.getBaseTotal() ) ) ;
        presentField.setText( String.valueOf( config.getBasePresent() ) ) ;
        targetField.setText( String.valueOf( config.getTargetPct() ) ) ;
        endField.setText( config.getEndDate().toString() ) ;
    }

    private void applySettings() {
        try {
            LocalDate asOf = LocalDate.parse( asOfField.getText().trim() ) ;
            int total = Integer.parseInt( totalField.getText().trim() ) ;
            int present = Integer.parseInt( presentField.getText().trim() ) ;
            double target = Double.parseDouble( targetField.getText().trim() ) ;
            LocalDate end = LocalDate.parse( endField.getText().trim() ) ;
            if( present > total || total < 0 || present < 0 ) {
                throw new IllegalArgumentException( "Present days must be between 0 and total days" ) ;
            }
            if( end.isBefore( asOf.plusDays( 1 ) ) ) {
                throw new IllegalArgumentException( "End date must be after the as-of date" ) ;
            }
            config.setAsOfDate( asOf ) ;
            config.setBaseTotal( total ) ;
            config.setBasePresent( present ) ;
            config.setTargetPct( target ) ;
            config.setEndDate( end ) ;
            config.save() ;
            calendarPanel.rebuild() ;
            model.refresh() ;
        }
        catch( Exception ex ) {
            JOptionPane.showMessageDialog( this, "Invalid settings: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE ) ;
        }
    }

    void openConfigEditor() {
        new ConfigEditorDialog( this, this::reloadConfig ).setVisible( true ) ;
    }

    private void reloadConfig() {
        try {
            config.load() ;
            populateSettings() ;
            calendarPanel.rebuild() ;
            model.refresh() ;
        }
        catch( ConfigException ex ) {
            JOptionPane.showMessageDialog( this, "Config not reloaded; fix these problems:\n• " +
                    String.join( "\n• ", ex.getErrors() ), "Invalid config", JOptionPane.ERROR_MESSAGE ) ;
        }
        catch( Exception ex ) {
            log.error( "Could not reload config", ex ) ;
            JOptionPane.showMessageDialog( this, "Could not reload config: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE ) ;
        }
    }

    private void onModelChange() {
        double target = config.getTargetPct() ;
        double fin = model.getFinalPct() ;
        boolean ok = fin + 1e-9 >= target ;

        finalPctLabel.setText( String.format( "%.2f%%", fin ) ) ;
        finalPctLabel.setForeground( ok ? CalendarPanel.ABOVE_FG : CalendarPanel.BELOW_FG ) ;

        int maxLeaves = model.getMaxLeaves() ;
        int planned = model.getPlannedLeaves() ;
        String verdict ;
        if( maxLeaves < 0 ) {
            verdict = String.format( "<font color='#B00020'>Target unreachable: short by %d day(s) even with full attendance</font>", -maxLeaves ) ;
        }
        else if( planned <= maxLeaves ) {
            verdict = String.format( "<font color='#1B6E20'>Spare leaves: <b>%d</b></font>", maxLeaves - planned ) ;
        }
        else {
            verdict = String.format( "<font color='#B00020'>Over by <b>%d</b> leave(s)</font>", planned - maxLeaves ) ;
        }

        detailLabel.setText( String.format( "<html>" +
                "Baseline: %d / %d = %.2f%% &nbsp;|&nbsp; Working days ahead: <b>%d</b> &nbsp;|&nbsp; " +
                "Final: %d / %d<br>" +
                "Planned leaves: <b>%d</b> &nbsp;|&nbsp; Max leaves for %.1f%%: <b>%d</b> &nbsp;|&nbsp; %s" +
                "</html>",
                config.getBasePresent(), config.getBaseTotal(), config.getBasePct(),
                model.getWorkingDays(), model.getFinalPresent(), model.getFinalTotal(),
                planned, target, Math.max( maxLeaves, 0 ), verdict ) ) ;

        calendarPanel.repaint() ;
        chart.repaint() ;
    }

    public static void main( String[] args ) throws Exception {
        try {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() ) ;
        }
        catch( Exception e ) {
            log.warn( "Could not set system look and feel", e ) ;
        }
        ToolTipManager.sharedInstance().setInitialDelay( 200 ) ;
        ToolTipManager.sharedInstance().setDismissDelay( 20000 ) ;

        AttendanceConfig config = new AttendanceConfig() ;
        ConfigException startupError = null ;
        try {
            config.load() ;
        }
        catch( ConfigException e ) {
            startupError = e ;  // config keeps its defaults; let the user fix the files
        }
        final ConfigException err = startupError ;
        SwingUtilities.invokeLater( () -> {
            AttendanceSimulator app = new AttendanceSimulator( config ) ;
            app.setVisible( true ) ;
            if( err != null ) {
                JOptionPane.showMessageDialog( app, "Config files are invalid; showing defaults.\n• " +
                        String.join( "\n• ", err.getErrors() ), "Invalid config", JOptionPane.ERROR_MESSAGE ) ;
                app.openConfigEditor() ;
            }
        } ) ;
    }
}
