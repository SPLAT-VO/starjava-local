package uk.ac.starlink.votable;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;

/**
 * Field or Param value restriction set represented by a VALUES element
 * in a VOTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ValuesElement extends VOElement {

    private String minimum;
    private String maximum;
    private String[] options;
    private String blank;
    private String type;
    private boolean isInvalid;

    /**
     * Constructs a ValuesElement object from a VALUES element.
     *
     * @param  el  a VALUES element
     * @param  systemId  document system ID
     */
    public ValuesElement( Element el, String systemId,
                          VOElementFactory factory ) {
        super( el, systemId, "VALUES", factory );
        blank = getAttribute( "null" );
        type = getAttribute( "type" );
        isInvalid = getAttribute( "invalid" ) == "yes";
        if ( type == null ) {
            type = "legal";
        }

        VOElement[] children = getChildren();
        List opts = new ArrayList();
        for ( int i = 0; i < children.length; i++ ) {
            VOElement child = children[ i ];
            if ( child.getTagName().equals( "MAX" ) ) {
                maximum = child.getAttribute( "value" );
            }
            else if ( child.getTagName().equals( "MIN" ) ) {
                minimum = child.getAttribute( "value" );
            }
            else if ( child.getTagName().equals( "OPTION" ) ) {
                opts.add( child.getAttribute( "name" ) );
            }
        }
        options = (String[]) opts.toArray( new String[ 0 ] );
    }

    /**
     * Returns the specified maximum value for this ValuesElement object
     * (the value of any Maximum child).
     *
     * @return  maximum value, or <tt>null</tt> if none specified
     */
    public String getMaximum() {
        return maximum;
    }

    /**
     * Returns the specified minimum value for this ValuesElement object
     * (the value of any MINIMUM child).
     *
     * @return  minimum value, or <tt>null</tt> if none specified
     */
    public String getMinimum() {
        return minimum;
    }

    /**
     * Returns the specified option values for this ValuesElement object.
     *
     * @return  an array of option strings ('value' attributes
     *          of OPTION children)
     */
    public String[] getOptions() {
        return (String[]) options.clone();
    }

    /**
     * Returns the 'null' value for this ValuesElement object, that is the
     * value which represents an undefined data value.  This is
     * the value of the 'null' attribute of the VALUES element, 
     * but does not have anything to do with the Java language
     * <tt>null</tt> value.
     *
     * @return   the 'null' value for this ValuesElement object or, confusingly,
     *           <tt>null</tt> if none is defined
     */
    public String getNull() {
        return blank;
    }

    /**
     * Returns the supplied or implied value of the 'type' attribute of this
     * ValuesElement object. 
     *
     * @return  one of the strings 'actual' or 'legal'
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the sense of the supplied or implied 'invalid' attribute 
     * of this ValuesElement object.  I don't know what the semantics of this
     * is supposed to be though, I can't find it referenced 
     * in the VOTable document.
     *
     * @return  is the 'invalid' attribute present and equal to "yes"?
     */
    public boolean isInvalid() {
        return isInvalid;
    }
}
