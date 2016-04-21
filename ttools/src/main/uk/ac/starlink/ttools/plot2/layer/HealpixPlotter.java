package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Scaler;
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ComboBoxSpecifier;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
import uk.ac.starlink.ttools.plot2.config.SkySysConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.IntegerCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.Rotation;
import uk.ac.starlink.ttools.plot2.geom.SkySurface;
import uk.ac.starlink.ttools.plot2.geom.SkySurfaceFactory;
import uk.ac.starlink.ttools.plot2.geom.SkySys;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plotter for plotting lists of HEALPix tiles.
 *
 * @author   Mark Taylor
 * @since    31 Mar 2016
 */
public class HealpixPlotter
        extends AbstractPlotter<HealpixPlotter.HealpixStyle> {

    private final boolean transparent_;
    private final boolean reportAuxKeys_;
    private final int icHealpix_;
    private final int icValue_;

    /** Maximum HEALPix level supported by this plotter. */
    public static final int MAX_LEVEL = 13;

    /** Coordinate for HEALPix index. */
    public static final IntegerCoord HEALPIX_COORD =
        new IntegerCoord(
            new InputMeta( "healpix", "HEALPix index" )
           .setShortDescription( "HEALPix index" )
           .setXmlDescription( new String[] {
                "<p>HEALPix index indicating the sky position of the tile",
                "whose value is plotted.",
                "If not supplied, the assumption is that the supplied table",
                "contains one row for each HEALPix tile at a given level,",
                "in ascending order.",
                "</p>",
            } )
        , false, IntegerCoord.IntType.INT );

    /** Coordinate for value determining tile colours. */
    public static final FloatingCoord VALUE_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "value", "Value" )
           .setShortDescription( "Tile value" )
           .setXmlDescription( new String[] {
                "<p>Value of HEALPix tile, determining the colour",
                "which will be plotted.",
                "</p>",
            } )
        , true );


    /** ConfigKey for HEALPix level corresponding to data HEALPix indices. */
    public static final ConfigKey<Integer> DATALEVEL_KEY = createDataLevelKey();

    /** ConfigKey for Sky System corresponding to data HEALPix indices. */
    public static final ConfigKey<SkySys> DATASYS_KEY =
        new SkySysConfigKey(
            new ConfigMeta( "datasys", "Data Sky System" )
           .setShortDescription( "Sky system of HEALPix grid" )
           .setXmlDescription( new String[] {
                "<p>The sky coordinate system to which the HEALPix grid",
                "used by the input pixel file refers.",
                "</p>",
            } )
        , false );

    private static final AuxScale SCALE = AuxScale.COLOR;
    private static final ConfigKey<Double> TRANSPARENCY_KEY =
        StyleKeys.TRANSPARENCY;
    private static final RampKeySet RAMP_KEYS = StyleKeys.AUX_RAMP;
    private static final ConfigKey<SkySys> VIEWSYS_KEY =
        SkySurfaceFactory.VIEWSYS_KEY;

    /**
     * Constructor.
     *
     * @param  transparent  if true, there will be a config option for
     *                      setting the alpha value of the whole layer
     */
    public HealpixPlotter( boolean transparent ) {
        super( "Healpix", ResourceIcon.FORM_SKYDENSITY,
               CoordGroup.createCoordGroup( 0, new Coord[] { HEALPIX_COORD,
                                                             VALUE_COORD } ),
               false );
        icHealpix_ = 0;
        icValue_ = 1;
        transparent_ = transparent;
        reportAuxKeys_ = false;
    }

    public String getPlotterDescription() {
        StringBuffer sbuf = new StringBuffer()
            .append( "<p>Plots a table representing HEALPix pixels " )
            .append( "on the sky.\n" )
            .append( "Each row represents a single HEALPix tile,\n" )
            .append( "and a value from that row is used to colour\n" )
            .append( "the corresponding region of the sky plot.\n" )
            .append( "</p>\n" );
        sbuf.append( "<p>" );
        if ( reportAuxKeys_ ) {
            sbuf.append( "There are additional options to adjust\n" )
                .append( "the way data values are mapped to colours.\n" );
        }
        else {
            sbuf.append( "The way that data values are mapped\n" )
                .append( "to colours is usually controlled by options\n" )
                .append( "at the level of the plot itself,\n" )
                .append( "rather than by per-layer configuration.\n" );
        }
        sbuf.append( "</p>\n" );
        return sbuf.toString();
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> keyList = new ArrayList<ConfigKey>();
        keyList.add( DATALEVEL_KEY );
        keyList.add( DATASYS_KEY );
        keyList.add( VIEWSYS_KEY );
        if ( reportAuxKeys_ ) {
            keyList.addAll( Arrays.asList( RAMP_KEYS.getKeys() ) );
        }
        if ( transparent_ ) {
            keyList.add( TRANSPARENCY_KEY );
        }
        return keyList.toArray( new ConfigKey[ 0 ] );
    }

    public HealpixStyle createStyle( ConfigMap config ) {
        RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
        int dataLevel = config.get( DATALEVEL_KEY );
        SkySys dataSys = config.get( DATASYS_KEY );
        SkySys viewSys = config.get( VIEWSYS_KEY );
        Rotation rotation = Rotation.createRotation( dataSys, viewSys );
        Scaling scaling = ramp.getScaling();
        float scaleAlpha = 1f - config.get( TRANSPARENCY_KEY ).floatValue();
        Shader shader = Shaders.fade( ramp.getShader(), scaleAlpha );
        return new HealpixStyle( dataLevel, rotation, scaling, shader );
    }

    public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                  HealpixStyle style ) {
        final int level;
        if ( style.dataLevel_ >= 0 ) {
            level = style.dataLevel_;
        }
        else {
            long nrow = dataSpec.getSourceTable().getRowCount();
            if ( nrow > 0 ) {
                double rowLevel = Math.log( ( nrow / 12 ) ) / Math.log( 4 );
                if ( rowLevel == (int) rowLevel ) {
                    level = (int) rowLevel;
                }
                else {
                    level = -1;
                }
            }
            else {
                level = -1;
            }
        }
        if ( level >= 0 ) {
            IndexReader rdr =
                  dataSpec.isCoordBlank( icHealpix_ )
                ? new IndexReader() {
                      public long getHealpixIndex( TupleSequence tseq ) {
                          return tseq.getRowIndex();
                      }
                  }
                : new IndexReader() {
                      public long getHealpixIndex( TupleSequence tseq ) {
                          return HEALPIX_COORD.readIntCoord( tseq, icHealpix_ );
                      }
                  };
            return new HealpixLayer( geom, dataSpec, style, level, rdr );
        }

        /* Can't determine or guess HEALPix level.
         * We have no choice but to refuse to plot.
         * Unfortunately this doesn't give much useful user feedback. */
        else {
            return null;
        }
    }

    /**
     * Constructs the config key for supplying HEALPix level at which
     * index coordinate values must be interpreted.
     *
     * @return  HEALPix data level key
     */
    private static ConfigKey<Integer> createDataLevelKey() {
        ConfigMeta meta = new ConfigMeta( "datalevel", "HEALPix Data Level" );
        meta.setShortDescription( "HEALPix level of tile index" );
        meta.setXmlDescription( new String[] {
            "<p>HEALPix level of the (implicit or explicit) tile indices.",
            "Legal values are between 0 (12 pixels) and",
            Integer.toString( MAX_LEVEL ),
            "(" + Long.toString( 12 * (long) Math.pow( 4, MAX_LEVEL ) )
                + " pixels).",
            "If a negative value is supplied (the default),",
            "then an attempt is made to determine the correct level",
            "from the data.",
            "</p>",
        } );
        final Collection<Integer> levelOptions = new ArrayList<Integer>();
        levelOptions.add( new Integer( -1 ) );
        for ( int i = 0; i <= MAX_LEVEL; i++ ) {
            levelOptions.add( new Integer( i ) );
        }
        ConfigKey<Integer> key = new IntegerConfigKey( meta, -1 ) {
            public Specifier<Integer> createSpecifier() {
                return new ComboBoxSpecifier( levelOptions );
            }
        };
        return key;
    }

    /**
     * Style for configuring the HEALPix plot.
     */
    public static class HealpixStyle implements Style {
        private final int dataLevel_;
        private final Rotation rotation_;
        private final Scaling scaling_;
        private final Shader shader_;

        /**
         * Constructor.
         *
         * @param   dataLevel HEALPix level at which the pixel index coordinates
         *                    must be interpreted; if negative, automatic
         *                    detection will be used
         * @param   rotation  sky rotation to be applied before plotting
         * @param   scaling   scaling function for mapping densities to
         *                    colour map entries
         * @param   shader   colour map
         */
        public HealpixStyle( int dataLevel, Rotation rotation,
                             Scaling scaling, Shader shader ) {
            dataLevel_ = dataLevel;
            rotation_ = rotation;
            scaling_ = scaling;
            shader_ = shader;
        }

        /**
         * Indicates whether this style has any transparency.
         *
         * @return   if true, the colours painted by this shader within
         *           the plot's geometric region of validity (that is,
         *           on the sky) are guaranteed always to have an alpha
         *           value of 1
         */
        boolean isOpaque() {
            return ! Shaders.isTransparent( shader_ );
        }

        public Icon getLegendIcon() {
            return createHealpixIcon( shader_, 18, 16, 1, 1 );
        }

        @Override
        public int hashCode() {
            int code = 553227;
            code = 23 * code + dataLevel_;
            code = 23 * code + rotation_.hashCode();
            code = 23 * code + scaling_.hashCode();
            code = 23 * code + shader_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof HealpixStyle ) {
                HealpixStyle other = (HealpixStyle) o;
                return this.dataLevel_ == other.dataLevel_
                    && this.rotation_.equals( other.rotation_ )
                    && this.scaling_.equals( other.scaling_ )
                    && this.shader_.equals( other.shader_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * PlotLayer implementation for HEALPix plotter.
     */
    private class HealpixLayer extends AbstractPlotLayer {

        private final HealpixStyle hstyle_;
        private final int level_;
        private final IndexReader indexReader_;

        /**
         * Constructor.
         *
         * @param  geom   data geom
         * @param  dataSpec   data specification
         * @param  hstyle   style
         * @param  level   definite HEALPix level of data tiles
         * @param  indexReader   determines pixel index from data
         */
        HealpixLayer( DataGeom geom, DataSpec dataSpec, HealpixStyle hstyle,
                      int level, IndexReader indexReader ) {
            super( HealpixPlotter.this, geom, dataSpec, hstyle,
                   hstyle.isOpaque() ? LayerOpt.OPAQUE : LayerOpt.NO_SPECIAL );
            hstyle_ = hstyle;
            level_ = level;
            indexReader_ = indexReader;
        }

        public Map<AuxScale,AuxReader> getAuxRangers() {
            Map<AuxScale,AuxReader> map = new HashMap<AuxScale,AuxReader>();
            map.put( SCALE, new AuxReader() {
                public int getCoordIndex() {
                    return icValue_;
                }
                public void adjustAuxRange( Surface surface, TupleSequence tseq,
                                            Range range ) {
                    SkySurfaceTiler tiler = createTiler( (SkySurface) surface );
                    while ( tseq.next() ) {
                        long hpx = indexReader_.getHealpixIndex( tseq );
                        if ( tiler.isCenterVisible( hpx ) ) {
                            double value =
                                VALUE_COORD.readDoubleCoord( tseq, icValue_ );
                            range.submit( value );
                        }
                    }
                }
            } );
            return map;
        }

        public Drawing createDrawing( Surface surf,
                                      Map<AuxScale,Range> auxRanges,
                                      final PaperType paperType ) {
            final SkySurface ssurf = (SkySurface) surf;
            final Scaler scaler =
                Scaling.createRangeScaler( hstyle_.scaling_,
                                           auxRanges.get( SCALE ) );
            final Shader shader = hstyle_.shader_;
            final SkySurfaceTiler tiler = createTiler( ssurf );
            return new UnplannedDrawing() {
                protected void paintData( Paper paper,
                                          final DataStore dataStore ) {
                    paperType.placeDecal( paper, new Decal() {
                        public void paintDecal( Graphics g ) {
                            TupleSequence tseq =
                                dataStore.getTupleSequence( getDataSpec() );
                            paintTiles( g, tseq, tiler, scaler, shader );
                        }
                        public boolean isOpaque() {
                            return hstyle_.isOpaque();
                        }
                    } );
                }
            };
        }

        /**
         * Returns a SkySurfaceTiler for a given sky surface.
         *
         * @param  surf  sky surface
         * @retrun   tiler
         */
        private SkySurfaceTiler createTiler( SkySurface surf ) {
            return new SkySurfaceTiler( surf, level_, hstyle_.rotation_ );
        }

        /**
         * Paints HEALPix pixels on a graphics context.
         *
         * @param  g   graphics context
         * @param  tseq  tuple sequence yielding pixel indices and data values
         * @param  tiler  handles HEALPix tile geometry on the plotting surface
         * @param  scaler  scales data values to unit interval
         * @param  shader  determines colours from unit interval
         */
        private void paintTiles( Graphics g, TupleSequence tseq,
                                 SkySurfaceTiler tiler,
                                 Scaler scaler, Shader shader ) {
            Color color0 = g.getColor();
            float[] rgba = new float[ 4 ];
            while ( tseq.next() ) {
                double value = tseq.getDoubleValue( icValue_ );
                if ( ! Double.isNaN( value ) ) {
                    long hpx = indexReader_.getHealpixIndex( tseq );
                    Shape shape = tiler.getTileShape( hpx );
                    if ( shape != null ) {
                        rgba[ 0 ] = 0.5f;
                        rgba[ 1 ] = 0.5f;
                        rgba[ 2 ] = 0.5f;
                        rgba[ 3 ] = 1.0f;
                        float sval = (float) scaler.scaleValue( value );
                        shader.adjustRgba( rgba, sval );
                        g.setColor( new Color( rgba[ 0 ], rgba[ 1 ],
                                               rgba[ 2 ], rgba[ 3 ] ) );
                        tiler.fillTile( g, shape );
                    }
                }
            }
            g.setColor( color0 );
        }
    }

    /**
     * Returns an icon suitable for use in a legend that represents
     * painting HEALPix tiles.
     *
     * @param  shader   shader
     * @param  width    total icon width in pixels
     * @param  height   total icon height in pixels
     * @param  xpad     internal padding in the X direction in pixels
     * @param  ypad     internal padding in the Y direction in pixels
     * @return   legend icon
     */
    private static Icon createHealpixIcon( final Shader shader,
                                           final int width, final int height,
                                           final int xpad, final int ypad ) {
        final double xd = ( width - 2 * xpad ) * 0.25;
        final double yd = ( width - 2 * ypad ) * 0.25;
        return new Icon() {
            public int getIconWidth() {
                return width;
            }
            public int getIconHeight() {
                return height;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                int xoff = x + xpad;
                int yoff = y + ypad;
                g.translate( xoff, yoff );
                Color color0 = g.getColor();
                paintDiamond( g, 1./8., 2, 0 );
                paintDiamond( g, 5./8., 1, 1 );
                paintDiamond( g, 7./8., 3, 1 );
                paintDiamond( g, 3./8., 2, 2 );
                g.setColor( color0 );
                g.translate( -xoff, -yoff );
            }
            private void paintDiamond( Graphics g, double shade,
                                       int ix, int iy ) {
                float[] rgba = new float[] { 0.5f, 0.5f, 0.5f, 1.0f };
                shader.adjustRgba( rgba, (float) shade );
                g.setColor( new Color( rgba[ 0 ], rgba[ 1 ],
                                       rgba[ 2 ], rgba[ 3 ] ) );
                int[] xs = new int[] {
                    (int) xd * ix,
                    (int) xd * ( ix - 1 ),
                    (int) xd * ix,
                    (int) xd * ( ix + 1 ),
                };
                int[] ys = new int[] {
                    (int) yd * iy,
                    (int) yd * ( iy + 1 ),
                    (int) yd * ( iy + 2 ),
                    (int) yd * ( iy + 1 ),
                };
                g.fillPolygon( xs, ys, 4 );
            }
        };
    }

    /**
     * Defines how pixel index is acquired from a tuple sequence.
     */
    private interface IndexReader {

        /**
         * Acquires the HEALPix index corresponding to the current row of
         * a tuple sequence.
         *
         * @param  tseq  tuple sequence positioned at row of interest
         * @param  healpix index at current sequence position
         */
        long getHealpixIndex( TupleSequence tseq );
    }
}