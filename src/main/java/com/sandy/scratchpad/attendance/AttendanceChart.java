package com.sandy.scratchpad.attendance;

import com.sandy.scratchpad.attendance.AttendanceModel.DayPoint;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Cumulative attendance percentage after each working day. Index 0 is the
 * baseline; index i is the i-th working day after it. Clicking a point
 * toggles that day's attendance.
 */
public class AttendanceChart extends JComponent {

    private static final int ML = 56, MR = 24, MT = 20, MB = 44 ;

    private static final Color LINE_COLOR   = new Color( 0x1E5AA8 ) ;
    private static final Color TARGET_COLOR = new Color( 0xC62828 ) ;
    private static final Color GRID_COLOR   = new Color( 0xE6E6E6 ) ;
    private static final Color ABOVE_FILL   = new Color( 0x2E7D32 ) ;
    private static final Color ABSENT_FILL  = new Color( 0xD32F2F ) ;

    private final AttendanceModel model ;

    private double yLo, yHi ;
    private int hoverIdx = -1 ;

    public AttendanceChart( AttendanceModel model ) {
        this.model = model ;
        setPreferredSize( new Dimension( 900, 280 ) ) ;
        setToolTipText( "" ) ;
        setOpaque( true ) ;

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved( MouseEvent e ) {
                int idx = indexAt( e.getX() ) ;
                if( idx != hoverIdx ) {
                    hoverIdx = idx ;
                    repaint() ;
                }
            }

            @Override
            public void mouseExited( MouseEvent e ) {
                hoverIdx = -1 ;
                repaint() ;
            }

            @Override
            public void mouseClicked( MouseEvent e ) {
                int idx = indexAt( e.getX() ) ;
                if( idx >= 1 ) {
                    model.toggleAttendance( model.getSeries().get( idx - 1 ).getDate() ) ;
                }
            }
        } ;
        addMouseListener( ma ) ;
        addMouseMotionListener( ma ) ;
    }

    private int pointCount() {
        return model.getSeries().size() + 1 ;
    }

    private double xFor( int idx ) {
        int n = pointCount() ;
        double plotW = getWidth() - ML - MR ;
        return n <= 1 ? ML : ML + plotW * idx / ( n - 1 ) ;
    }

    private double yFor( double pct ) {
        double plotH = getHeight() - MT - MB ;
        return MT + plotH * ( yHi - pct ) / ( yHi - yLo ) ;
    }

    private int indexAt( int x ) {
        int n = pointCount() ;
        if( n <= 1 ) return -1 ;
        double plotW = getWidth() - ML - MR ;
        int idx = (int)Math.round( ( x - ML ) * ( n - 1 ) / plotW ) ;
        return idx < 0 || idx >= n ? -1 : idx ;
    }

    private double pctAt( int idx ) {
        return idx == 0 ? model.getConfig().getBasePct() : model.getSeries().get( idx - 1 ).getPct() ;
    }

    @Override
    public String getToolTipText( MouseEvent e ) {
        int idx = indexAt( e.getX() ) ;
        if( idx < 0 ) return null ;
        AttendanceConfig cfg = model.getConfig() ;
        if( idx == 0 ) {
            return String.format( "<html><b>Baseline %s</b><br>%d / %d = %.2f%%</html>",
                    cfg.getAsOfDate(), cfg.getBasePresent(), cfg.getBaseTotal(), cfg.getBasePct() ) ;
        }
        return CalendarPanel.reasonTooltip( model, model.getSeries().get( idx - 1 ).getDate() ) ;
    }

    private void computeRange() {
        double target = model.getConfig().getTargetPct() ;
        double lo = Math.min( target, model.getConfig().getBasePct() ) ;
        double hi = Math.max( target, model.getConfig().getBasePct() ) ;
        for( DayPoint p : model.getSeries() ) {
            lo = Math.min( lo, p.getPct() ) ;
            hi = Math.max( hi, p.getPct() ) ;
        }
        yLo = Math.floor( lo - 0.5 ) ;
        yHi = Math.ceil( hi + 0.5 ) ;
    }

    @Override
    protected void paintComponent( Graphics g0 ) {
        Graphics2D g = (Graphics2D)g0 ;
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON ) ;
        g.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON ) ;
        g.setColor( Color.WHITE ) ;
        g.fillRect( 0, 0, getWidth(), getHeight() ) ;

        computeRange() ;
        List<DayPoint> series = model.getSeries() ;
        int n = pointCount() ;
        int right = getWidth() - MR ;
        int bottom = getHeight() - MB ;

        g.setFont( getFont().deriveFont( 11f ) ) ;
        FontMetrics fm = g.getFontMetrics() ;

        // Horizontal grid + y labels
        double span = yHi - yLo ;
        double step = span <= 4 ? 0.5 : span <= 10 ? 1 : span <= 25 ? 2 : 5 ;
        for( double v = Math.ceil( yLo / step ) * step ; v <= yHi + 1e-9 ; v += step ) {
            int y = (int)yFor( v ) ;
            g.setColor( GRID_COLOR ) ;
            g.drawLine( ML, y, right, y ) ;
            g.setColor( Color.DARK_GRAY ) ;
            String s = step < 1 ? String.format( "%.1f%%", v ) : String.format( "%.0f%%", v ) ;
            g.drawString( s, ML - fm.stringWidth( s ) - 6, y + fm.getAscent() / 2 - 1 ) ;
        }

        // Month boundaries on x axis
        g.setColor( Color.DARK_GRAY ) ;
        g.drawLine( ML, bottom, right, bottom ) ;
        g.drawLine( ML, MT, ML, bottom ) ;
        int prevMonth = model.getConfig().getAsOfDate().getMonthValue() ;
        for( int i = 1 ; i < n ; i++ ) {
            DayPoint p = series.get( i - 1 ) ;
            if( p.getDate().getMonthValue() != prevMonth ) {
                prevMonth = p.getDate().getMonthValue() ;
                int x = (int)xFor( i ) ;
                g.setColor( GRID_COLOR ) ;
                g.drawLine( x, MT, x, bottom ) ;
                g.setColor( Color.DARK_GRAY ) ;
                String s = p.getDate().getMonth().getDisplayName( TextStyle.SHORT, Locale.ENGLISH ) ;
                g.drawString( s, x + 3, bottom + fm.getAscent() + 4 ) ;
            }
        }
        g.drawString( "Working days →", right - fm.stringWidth( "Working days →" ), bottom + 2 * fm.getHeight() + 4 ) ;

        // Target line
        double target = model.getConfig().getTargetPct() ;
        int ty = (int)yFor( target ) ;
        Stroke old = g.getStroke() ;
        g.setColor( TARGET_COLOR ) ;
        g.setStroke( new BasicStroke( 1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{ 6f, 4f }, 0f ) ) ;
        g.drawLine( ML, ty, right, ty ) ;
        g.setStroke( old ) ;
        g.setFont( getFont().deriveFont( Font.BOLD, 11f ) ) ;
        g.drawString( String.format( "Target %.1f%%", target ), ML + 4, ty - 4 ) ;

        if( n <= 1 ) return ;

        // Line
        Path2D path = new Path2D.Double() ;
        path.moveTo( xFor( 0 ), yFor( pctAt( 0 ) ) ) ;
        for( int i = 1 ; i < n ; i++ ) {
            path.lineTo( xFor( i ), yFor( pctAt( i ) ) ) ;
        }
        g.setColor( LINE_COLOR ) ;
        g.setStroke( new BasicStroke( 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ) ) ;
        g.draw( path ) ;
        g.setStroke( old ) ;

        // Points: absent days are always marked; present days only if there's room
        boolean showAll = ( right - ML ) / (double)n >= 6 ;
        for( int i = 0 ; i < n ; i++ ) {
            boolean absent = i > 0 && !series.get( i - 1 ).isPresent() ;
            if( !absent && !showAll && i != hoverIdx ) continue ;
            int r = absent ? 4 : 3 ;
            int x = (int)xFor( i ) ;
            int y = (int)yFor( pctAt( i ) ) ;
            g.setColor( absent ? ABSENT_FILL : pctAt( i ) + 1e-9 >= target ? ABOVE_FILL : LINE_COLOR ) ;
            g.fillOval( x - r, y - r, 2 * r, 2 * r ) ;
        }

        // Hover marker
        if( hoverIdx >= 0 && hoverIdx < n ) {
            int x = (int)xFor( hoverIdx ) ;
            int y = (int)yFor( pctAt( hoverIdx ) ) ;
            g.setColor( new Color( 0, 0, 0, 60 ) ) ;
            g.drawLine( x, MT, x, bottom ) ;
            g.setColor( Color.BLACK ) ;
            g.drawOval( x - 6, y - 6, 12, 12 ) ;
        }

        // Final value label
        double fin = pctAt( n - 1 ) ;
        String s = String.format( "%.2f%%", fin ) ;
        g.setFont( getFont().deriveFont( Font.BOLD, 12f ) ) ;
        g.setColor( fin + 1e-9 >= target ? ABOVE_FILL : ABSENT_FILL ) ;
        int fx = (int)xFor( n - 1 ) - g.getFontMetrics().stringWidth( s ) ;
        int fy = (int)yFor( fin ) ;
        g.drawString( s, fx, fy < MT + 16 ? fy + 18 : fy - 8 ) ;
    }
}
