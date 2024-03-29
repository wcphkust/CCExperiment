package org.apache.lucene.spatial.tier.projections;
public interface IProjector {
  public String coordsAsString(double latitude, double longitude);
  public double[] coords(double latitude, double longitude);
}