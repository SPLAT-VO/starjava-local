package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * Displays a legend for an auxiliary (colour) axis.
 * 
 * @author   Mark Taylor
 * @since    14 Jun 2007
 */
public class AuxLegend extends JComponent {

    private final boolean horizontal_;
    private final int iconDepth_;
    private int preLength_;
    private int postLength_;
    private Shader shader_;
    private int prefDepth_;
    private AxisLabeller labeller_;
    private boolean logFlag_;
    private boolean flipFlag_;
    private double lo_;
    private double hi_;

    /**
     * Constructor.
     *
     * @param   horizontal  true for a bar that runs horizontally,
     *                      false for a bar that runs vertically
     * @param   iconDepth  preferred transverse size of the legend colour band
     */
    public AuxLegend( boolean horizontal, int iconDepth ) {
        horizontal_ = horizontal;
        iconDepth_ = iconDepth;
    }

    /**
     * Configures the amount of padding left before and after the bar 
     * which can be used to carry half-labels etc.
     * 
     * @param   preLength   number of padding pixels blank before the bar run
     * @param   postLength  number of padding pixels blank after the bar run
     */
    public void setLengthPadding( int preLength, int postLength ) {
        preLength_ = preLength;
        postLength_ = postLength;
    }

    /**
     * Configures this legend according to a given plot state.
     *
     * @param   state  plot state
     * @param   iaux   index of auxiliary axis to use
     */
    public void configure( PlotState state, int iaux ) {
        shader_ = state.getValid() && state.getShaders().length > iaux
               ? state.getShaders()[ iaux ]
               : null;
        if ( shader_ != null ) {
            int idim = state.getMainNdim() + iaux;
            logFlag_ = state.getLogFlags()[ idim ];
            flipFlag_ = state.getFlipFlags()[ idim ];
            lo_ = state.getRanges()[ idim ][ 0 ];
            hi_ = state.getRanges()[ idim ][ 1 ];
            String label = state.getAxisLabels()[ idim ];
            labeller_ =
                new AxisLabeller( label, lo_, hi_, 200, logFlag_, ! flipFlag_, 
                                  getFontMetrics( getFont() ),
                                  horizontal_ ? AxisLabeller.X
                                              : AxisLabeller.ANTI_Y,
                                  6, preLength_, postLength_ );
        }
        else {
            labeller_ = null;
        }
        fitToSize();
        revalidate();
        repaint();
    }

    /**
     * Configures the internal arrangement of this legend according to
     * the current size of this component.  Should be called prior to
     * drawing and possibly before validation.
     */
    private void fitToSize() {
        if ( labeller_ != null ) {
            Insets insets = getInsets();
            int xadd = insets.left + insets.right;
            int yadd = insets.top + insets.bottom;
            int npix = ( horizontal_ ? ( getWidth() - xadd )
                                     : ( getHeight() - yadd ) )
                     - preLength_ - postLength_;
                   
            labeller_.setNpix( npix );
            prefDepth_ = iconDepth_ + labeller_.getAnnotationHeight()
                       + ( horizontal_ ? yadd : xadd );
        }
        else {
            prefDepth_ = 0;
        }
    }

    protected void paintComponent( Graphics g ) {

        /* JComponent boilerplate. */
        if ( isOpaque() ) {
            Color col = g.getColor();
            g.setColor( getBackground() );
            g.fillRect( 0, 0, getWidth(), getHeight() );
            g.setColor( col );
        }

        /* Draw the bar and annotations if not blank. */
        if ( labeller_ != null ) {
            fitToSize();

            /* Work out geometry. */
            Insets insets = getInsets();
            int txtDepth = labeller_.getAnnotationHeight();
            int iconDepth =
                ( horizontal_ ? ( getHeight() - insets.top - insets.bottom )
                              : ( getWidth() - insets.left - insets.right ) )
                - txtDepth;
            int xIcon = insets.left + ( horizontal_ ? preLength_ : 0 );
            int yIcon = insets.top + ( horizontal_ ? 0 : preLength_ );
            int xpix = horizontal_ ? labeller_.getNpix() : iconDepth;
            int ypix = horizontal_ ? iconDepth : labeller_.getNpix();
            int xLabel = xIcon + ( horizontal_ ? 0 : xpix );
            int yLabel = yIcon + ( horizontal_ ? ypix : 0 );

            /* Draw the numerical annotations. */
            Graphics2D g2 = (Graphics2D) g;
            AffineTransform transform = g2.getTransform();
            if ( horizontal_ ) {
                g2.translate( xLabel, yLabel );
                labeller_.annotateAxis( g );
            }
            else {
                g2.translate( xLabel, yLabel );
                g2.rotate( Math.PI / 2 );
                labeller_.annotateAxis( g2 );
            }
            g2.setTransform( transform );

            /* Draw the colour bar itself. */
            Shaders.invert( shader_ )
                   .createIcon( horizontal_, xpix, ypix, 0, 0 )
                   .paintIcon( this, g, xIcon, yIcon );

            /* Draw a surrounding rectangle. */
            g.drawRect( xIcon, yIcon, xpix, ypix - 1 );
        }
    }

    public Dimension getPreferredSize() {
        return getSize( 200 );
    }

    public Dimension getMaximumSize() {
        return getSize( Integer.MAX_VALUE );
    }

    public Dimension getMinimumSize() {
        Insets insets = getInsets();
        return getSize( 32 + ( horizontal_ ? insets.left + insets.right
                                           : insets.top + insets.bottom ) );
    }

    /**
     * Returns a dimension fixed along the preferred depth of this component
     * with a length as specified.
     *
     * @param   length  length
     * @return  dimension
     */
    private Dimension getSize( int length ) {
        return horizontal_ ? new Dimension( length, prefDepth_ )
                           : new Dimension( prefDepth_, length );
    }

    /**
     * Converts a fractional value (where zero corresponds to current 
     * lower bound and 1 corresponds to current upper bound) into
     * a corresponding data value.
     *
     * @param  frac  input fractional value
     * @return   corresponding data value
     */
    private double fractionToData( double frac ) {
        double f = flipFlag_ ? 1.0 - frac : frac;
        return logFlag_
             ? Math.exp( f * Math.log( hi_ / lo_ ) ) * lo_
             : f * ( hi_ - lo_ ) + lo_;
    }

    /**
     * ZoomRegion implementation suitable for use with this legend.
     */
    public abstract class ZoomRegion extends AxisZoomRegion {

        /**
         * Constructor.
         */
        public ZoomRegion() {
            super( horizontal_ );
        }

        /**
         * Called when a zoom has taken place.
         *
         * @param  lo  new requested lower data bound
         * @param  hi  new requested upper data bound
         */
        protected abstract void dataZoomed( double lo, double hi );

        public Rectangle getDisplay() {
            Rectangle bounds = new Rectangle( getBounds() );
            Insets insets = getInsets();
            bounds.x += insets.left;
            bounds.y += insets.top;
            bounds.width -= insets.left + insets.right;
            bounds.height -= insets.top + insets.bottom;
            return bounds;
        }

        public Rectangle getTarget() {
            Rectangle bounds = getDisplay();
            if ( horizontal_ ) {
                bounds.x += preLength_;
                bounds.width -= preLength_ + postLength_;
            }
            else {
                bounds.y += preLength_;
                bounds.height -= preLength_ + postLength_;
            }
            return bounds;
        }

        public void setDisplay( Rectangle display ) {
            throw new UnsupportedOperationException();
        }

        public void setTarget( Rectangle target ) {
            throw new UnsupportedOperationException();
        }

        public void zoomed( double[][] bounds ) {
            double lo = fractionToData( bounds[ 0 ][ flipFlag_ ? 1 : 0 ] );
            double hi = fractionToData( bounds[ 0 ][ flipFlag_ ? 0 : 1 ] );
            dataZoomed( lo, hi );
        }
    }
}
