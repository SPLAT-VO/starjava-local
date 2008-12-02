package uk.ac.starlink.topcat.interop;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.JMenu;
import nom.tam.fits.FitsException;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.gui.CallActionManager;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.astrogrid.samp.gui.SendActionManager;
import org.astrogrid.samp.xmlrpc.HubMode;
import org.astrogrid.samp.xmlrpc.HubRunner;
import org.astrogrid.samp.xmlrpc.XmlRpcKit;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.SubsetWindow;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.plot.DensityWindow;

/**
 * TopcatCommunicator implementation based on SAMP.
 *
 * @author   Mark Taylor
 * @since    4 Sep 2008
 */
public class SampCommunicator implements TopcatCommunicator {

    private final GuiHubConnector hubConnector_;
    private final TopcatSampControl sampControl_;
    private final Transmitter tableTransmitter_;
    private int imageCount_;
    private static final Logger logger_ =
        Logger.getLogger( SampCommunicator.class.getName() );
    private static final HubMode INTERNAL_HUB_MODE = HubMode.NO_GUI;
    private static final HubMode EXTERNAL_HUB_MODE = HubMode.MESSAGE_GUI;

    /**
     * Constructor.
     *
     * @param   controlWindow   TOPCAT control window
     */
    public SampCommunicator( ControlWindow controlWindow ) throws IOException {
        hubConnector_ =
            new GuiHubConnector( TopcatServer.getInstance().getProfile() );
        sampControl_ = new TopcatSampControl( hubConnector_, controlWindow );
        tableTransmitter_ =
            adaptTransmitter( new TableSendActionManager( hubConnector_,
                                                          sampControl_ ) );
    }

    public String getProtocolName() {
        return "SAMP";
    }

    public boolean setActive() {
        hubConnector_.setActive( true );
        hubConnector_.setAutoconnect( 5 );
        try {
            return hubConnector_.getConnection() != null;
        }
        catch ( IOException e ) {
            logger_.warning( "SAMP connection attempt failed: " + e );
            return false;
        }
    }

    public Transmitter getTableTransmitter() {
        return tableTransmitter_;
    }

    public Transmitter createImageTransmitter( DensityWindow densWin ) {
        return adaptTransmitter( new DensityImageSendActionManager( densWin ) );
    }

    public Transmitter createSubsetTransmitter( TopcatModel tcModel,
                                                SubsetWindow subWin ) {
        return adaptTransmitter( new SubsetSendActionManager( tcModel,
                                                              subWin ) );
    }

    public SkyPointActivity createSkyPointActivity() {
        final SendManager pointSender =
            new SendManager( hubConnector_, "coord.pointAt.sky" );
        return new SkyPointActivity() {
            public ComboBoxModel getTargetSelector() {
                return pointSender.getComboBoxModel();
            }
            public void pointAtSky( double ra, double dec ) throws IOException {
                Message msg = new Message( "coord.pointAt.sky" );
                msg.addParam( "ra", SampUtils.encodeFloat( ra ) );
                msg.addParam( "dec", SampUtils.encodeFloat( dec ) );
                pointSender.notify( msg );
            }
        };
    }

    public RowActivity createRowActivity() {
        final SendManager rowSender =
            new SendManager( hubConnector_, "table.highlight.row" );
        return new RowActivity() {
            public ComboBoxModel getTargetSelector() {
                return rowSender.getComboBoxModel();
            }
            public void highlightRow( TopcatModel tcModel, long lrow )
                    throws IOException {
                Map msg = sampControl_.createRowMessage( tcModel, lrow );
                if ( msg != null ) {
                    rowSender.notify( msg );
                }
            }
        };
    }

    public ImageActivity createImageActivity() {
        return new SampImageActivity( hubConnector_ );
    }

    public Action[] getInteropActions() {
        return new Action[] {
            hubConnector_.getRegisterAction(),
            hubConnector_.getUnregisterAction(),
            hubConnector_.getShowMonitorAction(),
            hubConnector_.getHubAction( false, INTERNAL_HUB_MODE ),
            hubConnector_.getHubAction( true, EXTERNAL_HUB_MODE ),
        };
    }

    public void startHub( boolean external ) throws IOException {
        if ( external ) {
            HubRunner.runExternalHub( EXTERNAL_HUB_MODE );
        }
        else {
            HubRunner.runHub( INTERNAL_HUB_MODE, XmlRpcKit.INTERNAL );
        }
    }

    /**
     * Turns a SAMP action manager into a Transmitter.
     *
     * @param  sender  SAMP sender
     * @return  Transmitter facade
     */
    private static Transmitter
            adaptTransmitter( final CallActionManager sender ) {
        return new Transmitter() {
            public Action getBroadcastAction() {
                return sender.getBroadcastAction();
            }
            public JMenu createSendMenu() {
                return sender.createSendMenu();
            }
            public void setEnabled( boolean isEnabled ) {
                sender.setEnabled( isEnabled );
            }
        };
    }

    /**
     * SendActionManager for sending subsets as row selections from the 
     * subset window.
     */
    private class SubsetSendActionManager extends CallActionManager {
        private final TopcatModel tcModel_;
        private final SubsetWindow subWin_;

        /**
         * Constructor.
         *
         * @param   tcModel  table
         * @param   subWin   subset window
         */
        SubsetSendActionManager( TopcatModel tcModel, SubsetWindow subWin ) {
            super( subWin, hubConnector_, "table.select.rowList",
                   "Row Subset" );
            tcModel_ = tcModel;
            subWin_ = subWin;
        }

        protected Map createMessage() throws IOException {
            RowSubset rset = subWin_.getSelectedSubset();
            return sampControl_.createSubsetMessage( tcModel_, rset );
        }
    }

    /**
     * SendActionManager for sending FITS images from the density plot window.
     */
    private class DensityImageSendActionManager extends CallActionManager {
        private final DensityWindow densWin_;

        /**
         * Constructor.
         *
         * @param   densWin  density plot window
         */
        DensityImageSendActionManager( DensityWindow densWin ) {
            super( densWin, hubConnector_, "image.load.fits", "FITS Image" );
            densWin_ = densWin;
        }

        protected Map createMessage() throws IOException, FitsException {

            /* Write the FITS image to a byte array. */
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            BufferedOutputStream bufout = new BufferedOutputStream( bout );
            densWin_.exportFits( bufout );
            bufout.flush();
            final byte[] fitsdata = bout.toByteArray();
            ServerResource resource = new ServerResource() {
                public String getContentType() {
                    return "image/fits";
                }
                public long getContentLength() {
                    return fitsdata.length;
                }
                public void writeBody( OutputStream out ) throws IOException {
                    out.write( fitsdata );
                }
            };

            /* Make it available from the server.
             * But expire it after some amount of time, as this may be 
             * a large-ish amount of memory.
             * Is this a good solution? */
            final TopcatServer server = TopcatServer.getInstance();
            final URL iurl = server.addResource( "density.fits", resource );
            new Timer( true ).schedule( new TimerTask() {
                public void run() {
                    server.expireResource( iurl );
                }
            }, 60000 );

            /* Construct and return the message. */
            String iid = "density-" + ++imageCount_;
            return new Message( "image.load.fits" )
                  .addParam( "url", iurl.toString() )
                  .addParam( "image-id", iid );
        }
    }
}
