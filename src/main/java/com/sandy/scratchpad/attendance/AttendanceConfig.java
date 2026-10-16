package com.sandy.scratchpad.attendance;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Settings (baseline attendance, target, end date, weekly offs) are read from
 * {@code ~/.attendance/attendance.properties}. School off days are read from
 * {@code ~/.attendance/holidays.txt} and days on which attendance is mandatory
 * (exams etc.) from {@code ~/.attendance/mandatory.txt}. Any file that is
 * absent (e.g. on a fresh install) is bootstrapped from the copy bundled under
 * {@link #BOOTSTRAP_RESOURCE_DIR}.
 */
@Slf4j
@Getter
@Setter
public class AttendanceConfig {

    public static final File APP_DIR = new File( System.getProperty( "user.home" ), ".attendance" ) ;
    public static final File CONFIG_FILE = new File( APP_DIR, "attendance.properties" ) ;
    public static final File HOLIDAYS_FILE = new File( APP_DIR, "holidays.txt" ) ;
    public static final File MANDATORY_FILE = new File( APP_DIR, "mandatory.txt" ) ;

    /** Classpath folder holding the initial versions of the config files. */
    public static final String BOOTSTRAP_RESOURCE_DIR = "/attendance/bootstrap/" ;

    private static final String K_AS_OF   = "baseline.asOfDate" ;
    private static final String K_TOTAL   = "baseline.totalDays" ;
    private static final String K_PRESENT = "baseline.presentDays" ;
    private static final String K_TARGET  = "target.percent" ;
    private static final String K_END     = "end.date" ;
    private static final String K_OFFS    = "weekly.offDays" ;
    private static final Set<String> KNOWN_KEYS =
            Set.of( K_AS_OF, K_TOTAL, K_PRESENT, K_TARGET, K_END, K_OFFS ) ;

    /** Thrown when configuration text fails validation; carries every problem found. */
    @Getter
    public static class ConfigException extends Exception {
        private final List<String> errors ;

        ConfigException( List<String> errors ) {
            super( String.join( "\n", errors ) ) ;
            this.errors = errors ;
        }
    }

    /** Date up to (and including) which the baseline numbers are counted. */
    private LocalDate asOfDate = LocalDate.of( 2026, 10, 16 ) ;
    private int baseTotal = 116 ;
    private int basePresent = 85 ;
    private double targetPct = 75.0 ;
    private LocalDate endDate = LocalDate.of( 2027, 1, 31 ) ;
    private Set<DayOfWeek> weeklyOffs = EnumSet.of( DayOfWeek.SATURDAY, DayOfWeek.SUNDAY ) ;

    @Setter( lombok.AccessLevel.NONE )
    private Map<LocalDate, String> holidays = new TreeMap<>() ;

    @Setter( lombok.AccessLevel.NONE )
    private Map<LocalDate, String> mandatoryDays = new TreeMap<>() ;

    /** Bootstraps any absent file from the bundled copy, then parses them. */
    public void load() throws IOException, ConfigException {
        APP_DIR.mkdirs() ;
        bootstrapIfAbsent( CONFIG_FILE ) ;
        bootstrapIfAbsent( HOLIDAYS_FILE ) ;
        bootstrapIfAbsent( MANDATORY_FILE ) ;
        parse( readConfigText(), readHolidaysText(), readMandatoryText() ) ;
    }

    private static void bootstrapIfAbsent( File file ) throws IOException {
        if( file.exists() ) return ;
        String resource = BOOTSTRAP_RESOURCE_DIR + file.getName() ;
        try( InputStream in = AttendanceConfig.class.getResourceAsStream( resource ) ) {
            if( in == null ) {
                throw new IOException( "Bootstrap resource " + resource + " not found on classpath" ) ;
            }
            Files.copy( in, file.toPath() ) ;
        }
        log.info( "Bootstrapped {} from {}", file, resource ) ;
    }

    public static String readConfigText() throws IOException {
        return Files.readString( CONFIG_FILE.toPath(), StandardCharsets.UTF_8 ) ;
    }

    public static String readHolidaysText() throws IOException {
        return Files.readString( HOLIDAYS_FILE.toPath(), StandardCharsets.UTF_8 ) ;
    }

    public static String readMandatoryText() throws IOException {
        return Files.readString( MANDATORY_FILE.toPath(), StandardCharsets.UTF_8 ) ;
    }

    /** Validates the given raw file contents without touching any state. */
    public static void validate( String configText, String holidaysText, String mandatoryText )
            throws ConfigException {
        new AttendanceConfig().parse( configText, holidaysText, mandatoryText ) ;
    }

    /** Writes the raw texts to the config files. Callers must validate first. */
    public static void writeRaw( String configText, String holidaysText, String mandatoryText )
            throws IOException {
        APP_DIR.mkdirs() ;
        Files.writeString( CONFIG_FILE.toPath(), configText, StandardCharsets.UTF_8 ) ;
        Files.writeString( HOLIDAYS_FILE.toPath(), holidaysText, StandardCharsets.UTF_8 ) ;
        Files.writeString( MANDATORY_FILE.toPath(), mandatoryText, StandardCharsets.UTF_8 ) ;
    }

    /**
     * Parses both texts. Fields are updated only if no errors are found;
     * otherwise a {@link ConfigException} listing all problems is thrown.
     */
    private void parse( String configText, String holidaysText, String mandatoryText )
            throws ConfigException {
        List<String> errors = new ArrayList<>() ;

        Properties p = new Properties() ;
        try {
            p.load( new StringReader( configText ) ) ;
        }
        catch( IOException | IllegalArgumentException e ) {
            errors.add( "attendance.properties: " + e.getMessage() ) ;
        }

        for( String key : p.stringPropertyNames() ) {
            if( !KNOWN_KEYS.contains( key ) ) {
                errors.add( "attendance.properties: unknown key '" + key + "'" ) ;
            }
        }

        LocalDate asOf     = parseDate( p, K_AS_OF, errors ) ;
        Integer total      = parseInt( p, K_TOTAL, errors ) ;
        Integer present    = parseInt( p, K_PRESENT, errors ) ;
        Double target      = parseDouble( p, K_TARGET, errors ) ;
        LocalDate end      = parseDate( p, K_END, errors ) ;
        Set<DayOfWeek> offs = parseOffs( p, errors ) ;

        if( total != null && total < 0 ) {
            errors.add( "attendance.properties: " + K_TOTAL + " must not be negative" ) ;
        }
        if( present != null && present < 0 ) {
            errors.add( "attendance.properties: " + K_PRESENT + " must not be negative" ) ;
        }
        if( total != null && present != null && present > total ) {
            errors.add( "attendance.properties: " + K_PRESENT + " (" + present +
                        ") exceeds " + K_TOTAL + " (" + total + ")" ) ;
        }
        if( target != null && ( target <= 0 || target > 100 ) ) {
            errors.add( "attendance.properties: " + K_TARGET + " must be in (0, 100]" ) ;
        }
        if( asOf != null && end != null && !end.isAfter( asOf ) ) {
            errors.add( "attendance.properties: " + K_END + " (" + end +
                        ") must be after " + K_AS_OF + " (" + asOf + ")" ) ;
        }
        if( offs != null && offs.size() == 7 ) {
            errors.add( "attendance.properties: " + K_OFFS + " cannot cover every day of the week" ) ;
        }

        Map<LocalDate, String> hols = parseDateFile( holidaysText, HOLIDAYS_FILE.getName(), "Holiday", errors ) ;
        Map<LocalDate, String> mand = parseDateFile( mandatoryText, MANDATORY_FILE.getName(), "Mandatory", errors ) ;
        for( LocalDate d : mand.keySet() ) {
            if( hols.containsKey( d ) ) {
                errors.add( MANDATORY_FILE.getName() + ": " + d + " (" + mand.get( d ) +
                            ") is also a holiday in " + HOLIDAYS_FILE.getName() + " (" + hols.get( d ) + ")" ) ;
            }
        }

        if( !errors.isEmpty() ) {
            throw new ConfigException( errors ) ;
        }
        asOfDate    = asOf ;
        baseTotal   = total ;
        basePresent = present ;
        targetPct   = target ;
        endDate     = end ;
        weeklyOffs  = offs ;
        holidays    = hols ;
        mandatoryDays = mand ;
    }

    private static String required( Properties p, String key, List<String> errors ) {
        String v = p.getProperty( key ) ;
        if( v == null || v.isBlank() ) {
            errors.add( "attendance.properties: missing value for '" + key + "'" ) ;
            return null ;
        }
        return v.trim() ;
    }

    private static LocalDate parseDate( Properties p, String key, List<String> errors ) {
        String v = required( p, key, errors ) ;
        if( v == null ) return null ;
        try {
            return LocalDate.parse( v ) ;
        }
        catch( DateTimeParseException e ) {
            errors.add( "attendance.properties: " + key + " '" + v + "' is not a yyyy-MM-dd date" ) ;
            return null ;
        }
    }

    private static Integer parseInt( Properties p, String key, List<String> errors ) {
        String v = required( p, key, errors ) ;
        if( v == null ) return null ;
        try {
            return Integer.parseInt( v ) ;
        }
        catch( NumberFormatException e ) {
            errors.add( "attendance.properties: " + key + " '" + v + "' is not a whole number" ) ;
            return null ;
        }
    }

    private static Double parseDouble( Properties p, String key, List<String> errors ) {
        String v = required( p, key, errors ) ;
        if( v == null ) return null ;
        try {
            return Double.parseDouble( v ) ;
        }
        catch( NumberFormatException e ) {
            errors.add( "attendance.properties: " + key + " '" + v + "' is not a number" ) ;
            return null ;
        }
    }

    private static Set<DayOfWeek> parseOffs( Properties p, List<String> errors ) {
        Set<DayOfWeek> offs = EnumSet.noneOf( DayOfWeek.class ) ;
        String v = p.getProperty( K_OFFS, "" ).trim() ;
        boolean ok = true ;
        for( String s : v.split( "," ) ) {
            if( s.isBlank() ) continue ;
            try {
                offs.add( DayOfWeek.valueOf( s.trim().toUpperCase() ) ) ;
            }
            catch( IllegalArgumentException e ) {
                errors.add( "attendance.properties: " + K_OFFS + " has invalid day '" + s.trim() +
                            "' (use MONDAY..SUNDAY)" ) ;
                ok = false ;
            }
        }
        return ok ? offs : null ;
    }

    /**
     * Each non-comment line is either
     * <pre>
     *   yyyy-MM-dd  Description
     *   yyyy-MM-dd..yyyy-MM-dd  Description   (inclusive range, e.g. vacations)
     * </pre>
     */
    private static Map<LocalDate, String> parseDateFile( String text, String fileName,
                                                         String defaultDesc, List<String> errors ) {
        Map<LocalDate, String> hols = new TreeMap<>() ;
        String[] lines = text.split( "\\R", -1 ) ;
        for( int i = 0 ; i < lines.length ; i++ ) {
            String line = lines[i].trim() ;
            if( line.isEmpty() || line.startsWith( "#" ) ) continue ;

            String where = fileName + " line " + ( i + 1 ) + ": " ;
            String[] parts = line.split( "\\s+", 2 ) ;
            String desc = parts.length > 1 ? parts[1].trim() : defaultDesc ;
            try {
                if( parts[0].contains( ".." ) ) {
                    String[] range = parts[0].split( "\\.\\.", -1 ) ;
                    if( range.length != 2 ) {
                        errors.add( where + "bad range '" + parts[0] + "' (use yyyy-MM-dd..yyyy-MM-dd)" ) ;
                        continue ;
                    }
                    LocalDate from = LocalDate.parse( range[0] ) ;
                    LocalDate to = LocalDate.parse( range[1] ) ;
                    if( to.isBefore( from ) ) {
                        errors.add( where + "range end " + to + " is before start " + from ) ;
                        continue ;
                    }
                    for( LocalDate d = from ; !d.isAfter( to ) ; d = d.plusDays( 1 ) ) {
                        hols.put( d, desc ) ;
                    }
                }
                else {
                    hols.put( LocalDate.parse( parts[0] ), desc ) ;
                }
            }
            catch( DateTimeParseException e ) {
                errors.add( where + "'" + e.getParsedString() + "' is not a yyyy-MM-dd date" ) ;
            }
        }
        return hols ;
    }

    public void save() throws IOException {
        APP_DIR.mkdirs() ;
        String offs = weeklyOffs.stream().map( Enum::name ).collect( Collectors.joining( "," ) ) ;
        String content = "# Attendance simulator settings\n" +
                "# Attendance as recorded by school up to and including baseline.asOfDate\n" +
                K_AS_OF + "=" + asOfDate + "\n" +
                K_TOTAL + "=" + baseTotal + "\n" +
                K_PRESENT + "=" + basePresent + "\n" +
                "\n" +
                "# Qualifying percentage\n" +
                K_TARGET + "=" + targetPct + "\n" +
                "\n" +
                "# Last date (inclusive) up to which attendance is counted\n" +
                K_END + "=" + endDate + "\n" +
                "\n" +
                "# Comma separated days of week on which school is off\n" +
                K_OFFS + "=" + offs + "\n" ;
        Files.writeString( CONFIG_FILE.toPath(), content, StandardCharsets.UTF_8 ) ;
    }

    public Map<LocalDate, String> getMandatoryDays() {
        return Collections.unmodifiableMap( mandatoryDays ) ;
    }

    public Map<LocalDate, String> getHolidays() {
        return Collections.unmodifiableMap( holidays ) ;
    }

    public double getBasePct() {
        return baseTotal == 0 ? 0 : 100.0 * basePresent / baseTotal ;
    }
}
