package org.apache.lucene.spatial.geohash;
import static junit.framework.Assert.*;
import org.junit.Test;
public class TestGeoHashUtils {
  @Test
  public void testEncode() {
    String hash = GeoHashUtils.encode(42.6, -5.6);
    assertEquals("ezs42e44yx96", hash);
    hash = GeoHashUtils.encode(57.64911, 10.40744);
    assertEquals("u4pruydqqvj8", hash);
  }
  @Test
  public void testDecodePreciseLongitudeLatitude() {
    String hash = GeoHashUtils.encode(52.3738007, 4.8909347);
    double[] latitudeLongitude = GeoHashUtils.decode(hash);
    assertEquals(52.3738007, latitudeLongitude[0], 0.00001D);
    assertEquals(4.8909347, latitudeLongitude[1], 0.00001D);
  }
  @Test
  public void testDecodeImpreciseLongitudeLatitude() {
    String hash = GeoHashUtils.encode(84.6, 10.5);
    double[] latitudeLongitude = GeoHashUtils.decode(hash);
    assertEquals(84.6, latitudeLongitude[0], 0.00001D);
    assertEquals(10.5, latitudeLongitude[1], 0.00001D);
  }
  @Test
  public void testDecodeEncode() {
    String geoHash = "u173zq37x014";
    assertEquals(geoHash, GeoHashUtils.encode(52.3738007, 4.8909347));
    double[] decode = GeoHashUtils.decode(geoHash);
    assertEquals(52.37380061d, decode[0], 0.000001d);
    assertEquals(4.8909343d, decode[1], 0.000001d);
    assertEquals(geoHash, GeoHashUtils.encode(decode[0], decode[1]));
    geoHash = "u173";
    decode = GeoHashUtils.decode("u173");
    geoHash = GeoHashUtils.encode(decode[0], decode[1]);
    assertEquals(decode[0], GeoHashUtils.decode(geoHash)[0], 0.000001d);
    assertEquals(decode[1], GeoHashUtils.decode(geoHash)[1], 0.000001d);
  }
}
