package org.apache.cassandra.gms;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.*;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.utils.BoundedStatsDeque;
import org.apache.cassandra.utils.FBUtilities;
public class FailureDetector implements IFailureDetector, FailureDetectorMBean
{
    public static final IFailureDetector instance = new FailureDetector();
    private static Logger logger_ = LoggerFactory.getLogger(FailureDetector.class);
    private static final int sampleSize_ = 1000;
    private static int phiConvictThreshold_;
    private Map<InetAddress, ArrivalWindow> arrivalSamples_ = new Hashtable<InetAddress, ArrivalWindow>();
    private List<IFailureDetectionEventListener> fdEvntListeners_ = new ArrayList<IFailureDetectionEventListener>();
    public FailureDetector()
    {
        phiConvictThreshold_ = DatabaseDescriptor.getPhiConvictThreshold();
        try
        {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.registerMBean(this, new ObjectName("org.apache.cassandra.net:type=FailureDetector"));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    public String getAllEndpointStates()
    {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<InetAddress, EndpointState> entry : Gossiper.instance.endpointStateMap_.entrySet())
        {
            sb.append(entry.getKey()).append("\n");
            for (Map.Entry<ApplicationState, VersionedValue> state : entry.getValue().applicationState_.entrySet())
                sb.append("  ").append(state.getKey()).append(":").append(state.getValue().value).append("\n");
        }
        return sb.toString();
    }
    public void dumpInterArrivalTimes()
    {
        OutputStream os = null;
        try
        {
            File file = File.createTempFile("failuredetector-", ".dat");
            os = new BufferedOutputStream(new FileOutputStream(file, true));
            os.write(toString().getBytes());
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
        finally
        {
            FileUtils.closeQuietly(os);
        }
    }
    public void setPhiConvictThreshold(int phi)
    {
        phiConvictThreshold_ = phi;
    }
    public int getPhiConvictThreshold()
    {
        return phiConvictThreshold_;
    }
    public boolean isAlive(InetAddress ep)
    {
        if (ep.equals(FBUtilities.getLocalAddress()))
            return true;
        EndpointState epState = Gossiper.instance.getEndpointStateForEndpoint(ep);
        if (epState == null)
            logger_.error("unknown endpoint " + ep);
        return epState != null && epState.isAlive();
    }
    public void report(InetAddress ep)
    {
        if (logger_.isTraceEnabled())
            logger_.trace("reporting {}", ep);
        long now = System.currentTimeMillis();
        ArrivalWindow heartbeatWindow = arrivalSamples_.get(ep);
        if ( heartbeatWindow == null )
        {
            heartbeatWindow = new ArrivalWindow(sampleSize_);
            arrivalSamples_.put(ep, heartbeatWindow);
        }
        heartbeatWindow.add(now);
    }
    public void interpret(InetAddress ep)
    {
        ArrivalWindow hbWnd = arrivalSamples_.get(ep);
        if ( hbWnd == null )
        {            
            return;
        }
        long now = System.currentTimeMillis();
        double phi = hbWnd.phi(now);
        if (logger_.isTraceEnabled())
            logger_.trace("PHI for " + ep + " : " + phi);
        if ( phi > phiConvictThreshold_ )
        {     
            for ( IFailureDetectionEventListener listener : fdEvntListeners_ )
            {
                listener.convict(ep);
            }
        }        
    }
    public void remove(InetAddress ep)
    {
        arrivalSamples_.remove(ep);
    }
    public void registerFailureDetectionEventListener(IFailureDetectionEventListener listener)
    {
        fdEvntListeners_.add(listener);
    }
    public void unregisterFailureDetectionEventListener(IFailureDetectionEventListener listener)
    {
        fdEvntListeners_.remove(listener);
    }
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        Set<InetAddress> eps = arrivalSamples_.keySet();
        sb.append("-----------------------------------------------------------------------");
        for ( InetAddress ep : eps )
        {
            ArrivalWindow hWnd = arrivalSamples_.get(ep);
            sb.append(ep + " : ");
            sb.append(hWnd.toString());
            sb.append( System.getProperty("line.separator") );
        }
        sb.append("-----------------------------------------------------------------------");
        return sb.toString();
    }
    public static void main(String[] args) throws Throwable
    {           
    }
}
class ArrivalWindow
{
    private static Logger logger_ = LoggerFactory.getLogger(ArrivalWindow.class);
    private double tLast_ = 0L;
    private BoundedStatsDeque arrivalIntervals_;
    ArrivalWindow(int size)
    {
        arrivalIntervals_ = new BoundedStatsDeque(size);
    }
    synchronized void add(double value)
    {
        double interArrivalTime;
        if ( tLast_ > 0L )
        {                        
            interArrivalTime = (value - tLast_);            
        }
        else
        {
            interArrivalTime = Gossiper.intervalInMillis_ / 2;
        }
        tLast_ = value;            
        arrivalIntervals_.add(interArrivalTime);        
    }
    synchronized double sum()
    {
        return arrivalIntervals_.sum();
    }
    synchronized double sumOfDeviations()
    {
        return arrivalIntervals_.sumOfDeviations();
    }
    synchronized double mean()
    {
        return arrivalIntervals_.mean();
    }
    synchronized double variance()
    {
        return arrivalIntervals_.variance();
    }
    double stdev()
    {
        return arrivalIntervals_.stdev();
    }
    void clear()
    {
        arrivalIntervals_.clear();
    }
    double p(double t)
    {
        double mean = mean();
        double exponent = (-1)*(t)/mean;
        return Math.pow(Math.E, exponent);
    }
    double phi(long tnow)
    {            
        int size = arrivalIntervals_.size();
        double log = 0d;
        if ( size > 0 )
        {
            double t = tnow - tLast_;                
            double probability = p(t);       
            log = (-1) * Math.log10( probability );                                 
        }
        return log;           
    } 
    public String toString()
    {
        return StringUtils.join(arrivalIntervals_.iterator(), " ");
    }
}
