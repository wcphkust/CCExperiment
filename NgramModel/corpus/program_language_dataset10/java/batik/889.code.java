package org.apache.batik.ext.awt.image.rendered;
import java.awt.Point;
import java.awt.image.Raster;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import org.apache.batik.util.CleanerThread;
import org.apache.batik.util.HaltingThread;
public class TileMap implements TileStore {
    private static final boolean DEBUG = false;
    private static final boolean COUNT = false;
    private HashMap rasters=new HashMap();
    static class TileMapLRUMember extends TileLRUMember {
        public Point   pt;
        public SoftReference parent;
        class RasterSoftRef extends CleanerThread.SoftReferenceCleared {
            RasterSoftRef(Object o) { super(o); }
            public void cleared() {
                if (DEBUG) System.err.println("Cleaned: " + this);
                TileMap tm = (TileMap)parent.get();
                if (tm != null)
                    tm.rasters.remove(pt);
            }
        }
        TileMapLRUMember(TileMap parent, Point pt, Raster ras) {
            super(ras);
            this.parent = new SoftReference(parent);
            this.pt     = pt;
        }
        public void setRaster(Raster ras) {
            hRaster = ras;
            wRaster = new RasterSoftRef(ras);
        }
    }
    private TileGenerator source = null;
    private LRUCache      cache = null;
    public TileMap(TileGenerator source,
                   LRUCache cache) {
        this.cache    = cache;
        this.source   = source;
    }
    public void setTile(int x, int y, Raster ras) {
        Point pt = new Point(x, y);
        if (ras == null) {
            Object o = rasters.remove(pt);
            if (o != null)
                cache.remove((TileMapLRUMember)o);
            return;
        }
        Object o = rasters.get(pt);
        TileMapLRUMember item;
        if (o == null) {
            item = new TileMapLRUMember(this, pt, ras);
            rasters.put(pt, item);
        } else {
            item = (TileMapLRUMember)o;
            item.setRaster(ras);
        }
        cache.add(item);
        if (DEBUG) System.out.println("Setting: (" + x + ", " + y + ')' );
    }
    public Raster getTileNoCompute(int x, int y) {
        Point pt = new Point(x, y);
        Object o = rasters.get(pt);
        if (o == null)
            return null;
        TileMapLRUMember item = (TileMapLRUMember)o;
        Raster ret = item.retrieveRaster();
        if (ret != null)
            cache.add(item);
        return ret;
    }
    public Raster getTile(int x, int y) {
        if (DEBUG) System.out.println("Fetching: (" + (x) + ", " +
                                      (y) + ')' );
        if (COUNT) synchronized (TileMap.class) { requests++; }
        Raster       ras  = null;
        Point pt = new Point(x, y);
        Object o = rasters.get(pt);
        TileMapLRUMember item = null;
        if (o != null) {
            item = (TileMapLRUMember)o;
            ras = item.retrieveRaster();
        }
        if (ras == null) {
            if (DEBUG) System.out.println("Generating: ("+(x)+", "+
                                          (y) + ")");
            if (COUNT) synchronized (TileMap.class) { misses++; }
            ras = source.genTile(x, y);
            if (HaltingThread.hasBeenHalted())
                return ras;
            if (item != null)
                item.setRaster(ras);
            else  {
                item = new TileMapLRUMember(this, pt, ras);
                rasters.put(pt, item);
            }
        }
        cache.add(item);
        return ras;
    }
    static int requests;
    static int misses;
}