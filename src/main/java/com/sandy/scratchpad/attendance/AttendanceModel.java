package com.sandy.scratchpad.attendance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Holds the simulated attendance from the day after the baseline date up to
 * the end date. Every working day is assumed present unless the user marks it
 * absent. Days can also be flipped between working and off. Only deviations
 * from the defaults are persisted in {@link #STATE_FILE}.
 */
@Slf4j
public class AttendanceModel {

    public static final File STATE_FILE = new File( AttendanceConfig.APP_DIR, "state.json" ) ;

    public enum DayType { PRESENT, ABSENT, OFF }

    /** Cumulative attendance at the end of a working day. */
    @Getter
    public static class DayPoint {
        private final LocalDate date ;
        private final boolean present ;
        private final int total ;
        private final int presentCount ;

        DayPoint( LocalDate date, boolean present, int total, int presentCount ) {
            this.date = date ;
            this.present = present ;
            this.total = total ;
            this.presentCount = presentCount ;
        }

        public double getPct() {
            return 100.0 * presentCount / total ;
        }
    }

    @Getter
    private final AttendanceConfig config ;
    private final Map<LocalDate, DayType> overrides = new TreeMap<>() ;
    private final List<Runnable> listeners = new ArrayList<>() ;
    private final ObjectMapper mapper = new ObjectMapper().enable( SerializationFeature.INDENT_OUTPUT ) ;

    private List<DayPoint> series = new ArrayList<>() ;
    private Map<LocalDate, DayPoint> pointByDate = new HashMap<>() ;

    public AttendanceModel( AttendanceConfig config ) {
        this.config = config ;
    }

    public void addListener( Runnable l ) {
        listeners.add( l ) ;
    }

    public LocalDate getStartDate() {
        return config.getAsOfDate().plusDays( 1 ) ;
    }

    public LocalDate getEndDate() {
        return config.getEndDate() ;
    }

    public boolean isInRange( LocalDate d ) {
        return !d.isBefore( getStartDate() ) && !d.isAfter( getEndDate() ) ;
    }

    public boolean isDefaultOff( LocalDate d ) {
        if( config.getMandatoryDays().containsKey( d ) ) return false ;
        return config.getWeeklyOffs().contains( d.getDayOfWeek() ) ||
               config.getHolidays().containsKey( d ) ;
    }

    public String getHolidayName( LocalDate d ) {
        return config.getHolidays().get( d ) ;
    }

    public String getMandatoryName( LocalDate d ) {
        return config.getMandatoryDays().get( d ) ;
    }

    /**
     * Human readable explanation of why a day is a leave, an off day, a
     * mandatory day or a regular school day. The first line is the headline.
     */
    public List<String> getReasons( LocalDate d ) {
        List<String> reasons = new ArrayList<>() ;
        String holiday   = getHolidayName( d ) ;
        String mandatory = getMandatoryName( d ) ;
        boolean weeklyOff = config.getWeeklyOffs().contains( d.getDayOfWeek() ) ;
        String dow = d.getDayOfWeek().getDisplayName( TextStyle.FULL, Locale.ENGLISH ) ;

        String normally ;
        if( mandatory != null ) normally = "Mandatory: " + mandatory + ( weeklyOff ? " (on a " + dow + ")" : "" ) ;
        else if( holiday != null ) normally = "Holiday: " + holiday ;
        else if( weeklyOff ) normally = "Weekly off (" + dow + ")" ;
        else normally = "School day" ;

        if( !isInRange( d ) ) {
            reasons.add( normally ) ;
            reasons.add( d.isAfter( getEndDate() ) ? "After the end date; not counted"
                                                   : "Already counted in the baseline" ) ;
            return reasons ;
        }

        DayType t = getDayType( d ) ;
        if( !isOverridden( d ) ) {
            reasons.add( normally ) ;
        }
        else if( t == DayType.ABSENT ) {
            reasons.add( mandatory != null ? "Leave on a MANDATORY day (" + mandatory + ")"
                                           : "Leave — marked absent by you" ) ;
        }
        else if( t == DayType.OFF ) {
            reasons.add( "Off — marked by you" ) ;
            reasons.add( "Normally: " + normally ) ;
        }
        else {
            reasons.add( "School day — marked working by you" ) ;
            reasons.add( "Normally: " + normally ) ;
        }
        return reasons ;
    }

    public boolean isOverridden( LocalDate d ) {
        return overrides.containsKey( d ) ;
    }

    public DayType getDayType( LocalDate d ) {
        DayType t = overrides.get( d ) ;
        if( t != null ) return t ;
        return isDefaultOff( d ) ? DayType.OFF : DayType.PRESENT ;
    }

    /** Flips a working day between present and absent. Off days are ignored. */
    public void toggleAttendance( LocalDate d ) {
        DayType t = getDayType( d ) ;
        if( !isInRange( d ) || t == DayType.OFF ) return ;
        setDayType( d, t == DayType.PRESENT ? DayType.ABSENT : DayType.PRESENT ) ;
    }

    /** Flips a day between off and working (present). */
    public void toggleOff( LocalDate d ) {
        if( !isInRange( d ) ) return ;
        setDayType( d, getDayType( d ) == DayType.OFF ? DayType.PRESENT : DayType.OFF ) ;
    }

    public void clearOverrides() {
        overrides.clear() ;
        saveState() ;
        refresh() ;
    }

    private void setDayType( LocalDate d, DayType t ) {
        DayType def = isDefaultOff( d ) ? DayType.OFF : DayType.PRESENT ;
        if( t == def ) {
            overrides.remove( d ) ;
        }
        else {
            overrides.put( d, t ) ;
        }
        saveState() ;
        refresh() ;
    }

    /** Recomputes the series and notifies listeners. */
    public void refresh() {
        List<DayPoint> newSeries = new ArrayList<>() ;
        Map<LocalDate, DayPoint> newMap = new HashMap<>() ;

        int total = config.getBaseTotal() ;
        int present = config.getBasePresent() ;
        for( LocalDate d = getStartDate() ; !d.isAfter( getEndDate() ) ; d = d.plusDays( 1 ) ) {
            DayType t = getDayType( d ) ;
            if( t == DayType.OFF ) continue ;
            total++ ;
            if( t == DayType.PRESENT ) present++ ;
            DayPoint p = new DayPoint( d, t == DayType.PRESENT, total, present ) ;
            newSeries.add( p ) ;
            newMap.put( d, p ) ;
        }
        series = newSeries ;
        pointByDate = newMap ;
        listeners.forEach( Runnable::run ) ;
    }

    public List<DayPoint> getSeries() {
        return Collections.unmodifiableList( series ) ;
    }

    public DayPoint getPoint( LocalDate d ) {
        return pointByDate.get( d ) ;
    }

    public int getWorkingDays() {
        return series.size() ;
    }

    public int getPlannedLeaves() {
        return (int)series.stream().filter( p -> !p.isPresent() ).count() ;
    }

    public int getFinalTotal() {
        return config.getBaseTotal() + series.size() ;
    }

    public int getFinalPresent() {
        return config.getBasePresent() + series.size() - getPlannedLeaves() ;
    }

    public double getFinalPct() {
        int total = getFinalTotal() ;
        return total == 0 ? 0 : 100.0 * getFinalPresent() / total ;
    }

    /**
     * Maximum number of leaves over the remaining working days such that the
     * final percentage stays at or above target. Negative if the target can't
     * be reached even with full attendance.
     */
    public int getMaxLeaves() {
        double needed = config.getTargetPct() / 100.0 * getFinalTotal() ;
        double attainable = config.getBasePresent() + series.size() ;
        return (int)Math.floor( attainable - needed + 1e-9 ) ;
    }

    // ------------------------------------------------------------------ persistence

    public void loadState() {
        overrides.clear() ;
        if( !STATE_FILE.exists() ) return ;
        try {
            Map<String, String> raw = mapper.readValue( STATE_FILE, new TypeReference<Map<String, String>>(){} ) ;
            raw.forEach( ( k, v ) -> overrides.put( LocalDate.parse( k ), DayType.valueOf( v ) ) ) ;
        }
        catch( Exception e ) {
            log.error( "Could not read state from {}", STATE_FILE, e ) ;
        }
    }

    private void saveState() {
        Map<String, String> raw = new LinkedHashMap<>() ;
        overrides.forEach( ( k, v ) -> raw.put( k.toString(), v.name() ) ) ;
        try {
            AttendanceConfig.APP_DIR.mkdirs() ;
            mapper.writeValue( STATE_FILE, raw ) ;
        }
        catch( IOException e ) {
            log.error( "Could not save state to {}", STATE_FILE, e ) ;
        }
    }
}
