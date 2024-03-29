package org.apache.solr.analysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public abstract class BaseTokenizerFactory extends BaseTokenStreamFactory implements TokenizerFactory {
  public static final Logger log = LoggerFactory.getLogger(BaseTokenizerFactory.class);
}
