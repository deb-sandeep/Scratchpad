package com.sandy.scratchpad.attendance;

import com.sandy.scratchpad.attendance.AttendanceModel.DayPoint;
import com.sandy.scratchpad.attendance.AttendanceModel.DayType;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Month grids from the start date to the end date. Left click flips a working
 * day between present and absent; right click flips a day between off and
 * working.
 */
public class CalendarPanel extends JPanel {

    static final Color PRESENT_BG   = new Color( 0xD8F0D8 ) ;
    static final Color ABSENT_BG    = new Color( 0xF6C6C6 ) ;
    static final Color OFF_BG       = new Color( 0xE4E4E4 ) ;
    static final Color HOLIDAY_BG   = new Color( 0xE6DDF2 ) ;
    static final Color OUT_BG       = new Color( 0xFAFAFA ) ;
    static final Color BELOW_FG     = new Color( 0xB00020 ) ;
    static final Color ABOVE_FG     = new Color( 0x1B6E20 ) ;
    static final Color OVERRIDE_DOT = new Color( 0x1E5AA8 ) ;
    static final Color MANDATORY_BORDER = new Color( 0xD32F2F ) ;

    private static final int COLUMNS = 3 ;

    private final AttendanceModel model ;

    public CalendarPanel( AttendanceModel model ) {
        this.model = model ;
        setLayout( new GridLayout( 0, COLUMNS, 12, 12 ) ) ;
        setBorder( BorderFactory.createEmptyBorder( 8, 8, 8, 8 ) ) ;
        rebuild() ;
    }

    /** Rebuilds the month grids; needed when the date range changes. */
    public void rebuild() {
        removeAll() ;
        YearMonth from = YearMonth.from( model.getStartDate() ) ;
        YearMonth to = YearMonth.from( model.getEndDate() ) ;
        for( YearMonth ym = from ; !ym.isAfter( to ) ; ym = ym.plusMonths( 1 ) ) {
            add( new MonthPanel( ym ) ) ;
        }
        revalidate() ;
        repaint() ;
    }

    private class MonthPanel extends JPanel {
        MonthPanel( YearMonth ym ) {
            super( new BorderLayout( 0, 4 ) ) ;
            JLabel title = new JLabel(
                    ym.getMonth().getDisplayName( TextStyle.FULL, Locale.ENGLISH ) + " " + ym.getYear(),
                    SwingConstants.CENTER ) ;
            title.setFont( title.getFont().deriveFont( Font.BOLD, 14f ) ) ;
            add( title, BorderLayout.NORTH ) ;

            JPanel grid = new JPanel( new GridLayout( 0, 7, 2, 2 ) ) ;
            for( DayOfWeek dow : DayOfWeek.values() ) {
                JLabel l = new JLabel( dow.getDisplayName( TextStyle.SHORT, Locale.ENGLISH ), SwingConstants.CENTER ) ;
                l.setFont( l.getFont().deriveFont( 11f ) ) ;
                grid.add( l ) ;
            }
            int lead = ym.atDay( 1 ).getDayOfWeek().getValue() - 1 ;
            for( int i = 0 ; i < lead ; i++ ) {
                grid.add( new JLabel() ) ;
            }
            for( int d = 1 ; d <= ym.lengthOfMonth() ; d++ ) {
                grid.add( new DayCell( ym.atDay( d ) ) ) ;
            }
            int cells = lead + ym.lengthOfMonth() ;
            for( int i = cells ; i < 42 ; i++ ) {
                grid.add( new JLabel() ) ;
            }
            add( grid, BorderLayout.CENTER ) ;
        }
    }

    /** Tooltip headed by the reason a day is a leave, off, mandatory or school day. */
    static String reasonTooltip( AttendanceModel model, LocalDate date ) {
        StringBuilder sb = new StringBuilder( "<html><b>" + date + " (" +
                date.getDayOfWeek().getDisplayName( TextStyle.SHORT, Locale.ENGLISH ) + ")</b>" ) ;

        List<String> reasons = model.getReasons( date ) ;
        String color = reasonColor( model, date ) ;
        sb.append( "<br><font color='" ).append( color ).append( "'><b>" )
          .append( escape( reasons.get( 0 ) ) ).append( "</b></font>" ) ;
        for( int i = 1 ; i < reasons.size() ; i++ ) {
            sb.append( "<br>" ).append( escape( reasons.get( i ) ) ) ;
        }

        DayPoint p = model.getPoint( date ) ;
        if( p != null ) {
            sb.append( String.format( "<br>After this day: %d / %d = %.2f%%",
                    p.getPresentCount(), p.getTotal(), p.getPct() ) ) ;
        }
        if( model.isInRange( date ) ) {
            sb.append( "<br><i>Click: present/absent &nbsp; Right-click: off/working</i>" ) ;
        }
        return sb.append( "</html>" ).toString() ;
    }

    private static String reasonColor( AttendanceModel model, LocalDate date ) {
        if( !model.isInRange( date ) ) return "#808080" ;
        DayType t = model.getDayType( date ) ;
        if( t == DayType.ABSENT ) return "#B00020" ;
        if( model.getMandatoryName( date ) != null ) return "#D32F2F" ;
        if( t == DayType.OFF ) return "#5E35B1" ;
        return "#1B6E20" ;
    }

    private static String escape( String s ) {
        return s.replace( "&", "&amp;" ).replace( "<", "&lt;" ).replace( ">", "&gt;" ) ;
    }

    private class DayCell extends JComponent {
        private final LocalDate date ;

        DayCell( LocalDate date ) {
            this.date = date ;
            setPreferredSize( new Dimension( 52, 40 ) ) ;
            setToolTipText( "" ) ;
            addMouseListener( new MouseAdapter() {
                @Override
                public void mouseClicked( MouseEvent e ) {
                    if( SwingUtilities.isRightMouseButton( e ) || e.isControlDown() ) {
                        model.toggleOff( date ) ;
                    }
                    else if( SwingUtilities.isLeftMouseButton( e ) ) {
                        model.toggleAttendance( date ) ;
                    }
                }
            } ) ;
        }

        @Override
        public String getToolTipText( MouseEvent e ) {
            return reasonTooltip( model, date ) ;
        }

        @Override
        protected void paintComponent( Graphics g0 ) {
            Graphics2D g = (Graphics2D)g0 ;
            g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON ) ;
            g.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON ) ;

            int w = getWidth() ;
            int h = getHeight() ;
            boolean inRange = model.isInRange( date ) ;
            DayType t = model.getDayType( date ) ;

            Color bg ;
            if( !inRange ) bg = OUT_BG ;
            else if( t == DayType.PRESENT ) bg = PRESENT_BG ;
            else if( t == DayType.ABSENT ) bg = ABSENT_BG ;
            else if( model.getHolidayName( date ) != null ) bg = HOLIDAY_BG ;
            else bg = OFF_BG ;

            g.setColor( bg ) ;
            g.fillRoundRect( 0, 0, w - 1, h - 1, 6, 6 ) ;

            if( inRange && model.getMandatoryName( date ) != null ) {
                g.setColor( MANDATORY_BORDER ) ;
                g.setStroke( new BasicStroke( 2.5f ) ) ;
                g.drawRoundRect( 1, 1, w - 3, h - 3, 6, 6 ) ;
            }
            if( date.equals( LocalDate.now() ) ) {
                g.setColor( Color.BLUE ) ;
                g.setStroke( new BasicStroke( 2f ) ) ;
                g.drawRoundRect( 3, 3, w - 7, h - 7, 6, 6 ) ;
            }

            g.setColor( inRange ? Color.DARK_GRAY : Color.LIGHT_GRAY ) ;
            g.setFont( getFont().deriveFont( Font.BOLD, 12f ) ) ;
            g.drawString( String.valueOf( date.getDayOfMonth() ), 4, 14 ) ;

            if( inRange && model.isOverridden( date ) ) {
                g.setColor( OVERRIDE_DOT ) ;
                g.fillOval( w - 9, 4, 5, 5 ) ;
            }

            DayPoint p = model.getPoint( date ) ;
            if( p != null ) {
                String s = String.format( "%.2f", p.getPct() ) ;
                g.setFont( getFont().deriveFont( 10f ) ) ;
                FontMetrics fm = g.getFontMetrics() ;
                g.setColor( p.getPct() + 1e-9 >= model.getConfig().getTargetPct() ? ABOVE_FG : BELOW_FG ) ;
                g.drawString( s, w - fm.stringWidth( s ) - 4, h - 5 ) ;
            }
            else if( inRange ) {
                String s = model.getHolidayName( date ) != null ? "holiday" : "off" ;
                g.setFont( getFont().deriveFont( Font.ITALIC, 9f ) ) ;
                FontMetrics fm = g.getFontMetrics() ;
                g.setColor( Color.GRAY ) ;
                g.drawString( s, w - fm.stringWidth( s ) - 4, h - 5 ) ;
            }
        }
    }
}
