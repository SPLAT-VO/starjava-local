package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.io.OutputStream;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.StreamStarTableWriter;
import uk.ac.starlink.table.Tables;

/**
 * A StarTableWriter that outputs text to a LaTeX document.
 * Depending on the value of the <code>standalone</code> attribute,
 * the output may either be a complete LaTeX document or just a
 * <tt>tabular</tt> environment suitable for inserting into an existing
 * document.
 *
 * @author   Mark Taylor (Starlnk)
 */
public class LatexTableWriter extends StreamStarTableWriter {

    private boolean standalone;

    /**
     * Constructs a new writer with default characteristics.
     */
    public LatexTableWriter() {
        this( false );
    }

    /**
     * Constructs a new writer indicating whether it will produce complete
     * or partial LaTeX documents.
     */
    public LatexTableWriter( boolean standalone ) {
        setStandalone( standalone );
    }

    /**
     * Sets whether output tables should be complete LaTeX documents.
     *
     * @param   standalone  true if the output document should be a
     *          complete LaTeX document
     */
    public void setStandalone( boolean standalone ) {
        this.standalone = standalone;
    }

    /**
     * Indicates whether output tables will be complete LaTeX documents.
     *
     * @return  true if the output documents will be complete LaTeX docs
     */
    public boolean isStandalone() {
        return standalone;
    }

    /**
     * Returns the string "LaTeX-document" or "LaTeX";
     */
    public String getFormatName() {
        return standalone ? "LaTeX-document" : "LaTeX";
    }

    public String getMimeType() {
        return "text/plain";
    }

    public boolean looksLikeFile( String location ) {
        return location.endsWith( ".tex" );
    }

    public void writeStarTable( StarTable startab, OutputStream ostrm ) 
            throws IOException {
 
        /* Work out the tabular format. */
        StringBuffer tfmt = new StringBuffer( "|" );
        ColumnInfo[] colinfos = Tables.getColumnInfos( startab );
        int ncol = colinfos.length;
        int[] maxWidths = new int[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            ColumnInfo colinfo = colinfos[ i ];
            Class clazz = colinfo.getContentClass();
            if ( Number.class.isAssignableFrom( clazz ) ) {
                tfmt.append( 'r' );
                maxWidths[ i ] = 32;
            }
            else {
                tfmt.append( 'l' );
                maxWidths[ i ] = 256;
            }
            tfmt.append( '|' );
        }

        /* Get an iterator over the table data. */
        RowSequence rseq = startab.getRowSequence();

        /* Get a stream for output. */
        try {

            /* Write the header information. */
            if ( standalone ) {
                printHeader( ostrm, startab );
            }
            printLine( ostrm, "\\begin{tabular}{" + tfmt + "}" );
            printLine( ostrm, "\\hline" );

            /* Write the column headings. */
            for ( int i = 0; i < ncol; i++ ) {
                boolean first = i == 0;
                boolean last = i == ncol - 1;
                ColumnInfo colinfo = colinfos[ i ];
                String line = new StringBuffer()
                             .append( "  \\multicolumn{1}{" )
                             .append( first ? "|" : "" )
                             .append( 'c' )
                             .append( "|" )
                             .append( "}{" )
                             .append( escape( colinfo.getName() ) )
                             .append( "} " )
                             .append( last ? "\\\\" : "&" )
                             .toString();
                printLine( ostrm, line );
            }
            printLine( ostrm, "\\hline" );

            /* Write the data. */
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                print( ostrm, "  " );
                for ( int i = 0; i < ncol; i++ ) {
                    String datum = colinfos[ i ]
                                  .formatValue( row[ i ], maxWidths[ i ] );
                    if ( i > 0 ) {
                        print( ostrm, " & " );
                    }
                    print( ostrm, escape( datum ) );
                }
                printLine( ostrm, "\\\\" );
            }

            /* Write footer information. */
            print( ostrm, "\\hline" );
            printLine( ostrm, "\\end{tabular}" );
            if ( standalone ) {
                printFooter( ostrm, startab );
            }
        }

        /* Close. */
        finally {
            rseq.close();
        }
    }

    /**
     * Returns a useful list of LatexTableWriters.
     *
     * @return  array containing one standalone and one tabular-only writer
     */
    public static StarTableWriter[] getStarTableWriters() {
        return new LatexTableWriter[] {
            new LatexTableWriter( false ),
            new LatexTableWriter( true ),
        };
    }

    /**
     * For standalone output, this method is invoked to output any text
     * preceding the <code>tabular</code> environment.  May be overridden to
     * modify the form of output documents.
     *
     * @param  ostrm  output stream
     * @param  startab  table for which header is required
     */
    protected void printHeader( OutputStream ostrm, StarTable startab ) 
            throws IOException {
        printLine( ostrm, "\\documentclass{article}" );
        printLine( ostrm, "\\begin{document}" );
        printLine( ostrm, "\\begin{table}" );
    }

    /**
     * For standalone output, this method is invoked to output any text
     * following the <code>tabular</code> environment.  May be overridden to
     * modify the form of output documents.
     *
     * @param   ostrm the stream to write to
     * @param   startab  the StarTable which the tabular will contain
     */
    protected void printFooter( OutputStream ostrm, StarTable startab ) 
            throws IOException {
        String tname = startab.getName();
        if ( tname != null && tname.trim().length() > 0 ) {
            printLine( ostrm, "\\caption{" + escape( tname ) + "}" );
        }
        printLine( ostrm, "\\end{table}" );
        printLine( ostrm, "\\end{document}" );
    }

    /**
     * Outputs a given string to a stream, with a newline character appended.
     *
     * @param  ostrm  the output stream
     * @param  line  the string to output
     */
    private void printLine( OutputStream ostrm, String line ) 
            throws IOException {
        print( ostrm, line );
        ostrm.write( (int) '\n' );
    }

    /**
     * Outputs a given string to a stream.
     *
     * @param  ostrm  the output stream
     * @param  str  the string to output
     */
    private void print( OutputStream ostrm, String str ) throws IOException {
        ostrm.write( str.getBytes() );
    }

    /**
     * Turns a Unicode string into an ASCII string suitable for inclusion
     * in LaTeX text - any special characters are escaped in LaTeX-friendly
     * ways.
     *
     * @param  line  the input string
     * @return  a LaTeX-friendly version of <tt>line</tt>
     */
    private String escape( String line ) {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < line.length(); i++ ) {
            char chr = line.charAt( i );
            switch ( chr ) {
                case '$':
                case '&':
                case '%':
                case '#':
                case '_':
                case '{':
                case '}':
                    sbuf.append( '\\' ).append( chr );
                    break;
                case '~':
                case '^':
                case '\\':
                    sbuf.append( "\\verb+" ).append( chr ).append( '+' );
                    break;
                default:
                    sbuf.append( ( chr > 0 && chr < 127 ) ? chr : '?' );
            }
        }
        return sbuf.toString();
    }
}
