/* ********************************************************
 * This file automatically generated by StcResourceProfile.pl.
 *                   Do not edit.                         *
 **********************************************************/

package uk.ac.starlink.ast;


/**
 * Java interface to the AST StcResourceProfile class
 *  - correspond to the IVOA STCResourceProfile class. 
 * The StcResourceProfile class is a sub-class of Stc used to describe 
 * the coverage of the datasets contained in some VO resource.
 * <p>
 * See http://hea-www.harvard.edu/~arots/nvometa/STC.html
 * 
 * 
 * @see  <a href='http://star-www.rl.ac.uk/cgi-bin/htxserver/sun211.htx/?xref_StcResourceProfile'>AST StcResourceProfile</a>  
 */
public class StcResourceProfile extends Stc {

   /**
    * Constructs a new StcResourceProfile.
    *
    * @param   region  the encapsulated region
    * @param   coords  the AstroCoords elements associated with this Stc
    */
   public StcResourceProfile( Region region, AstroCoords[] coords ) {
       construct( region, astroCoordsToKeyMaps( coords ) );
   }
   private native void construct( Region region, KeyMap[] coordMaps );
}
