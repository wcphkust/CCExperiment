package org.apache.solr.util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.solr.common.util.NamedList;
@Deprecated
public class DisMaxParams extends CommonParams implements org.apache.solr.common.params.DisMaxParams {
  public static Logger log = LoggerFactory.getLogger(DisMaxParams.class);
  public static String FQ = "fq";
  public float tiebreaker = 0.0f;
  public String qf = null;
  public String pf = null;
  public String mm = "100%";
  public int pslop = 0;
  public String bq = null;
  public String bf = null;
  public String fq = null;
  public void setValues(NamedList args) {
    super.setValues(args);
    Object tmp;
    tmp = args.get(TIE);
    if (null != tmp) {
      if (tmp instanceof Float) {
        tiebreaker = ((Float)tmp).floatValue();
      } else {
        log.error("init param is not a float: " + TIE);
      }
    }
    tmp = args.get(QF);
    if (null != tmp) {
      if (tmp instanceof String) {
        qf = tmp.toString();
      } else {
        log.error("init param is not a str: " + QF);
      }
    }
    tmp = args.get(PF);
    if (null != tmp) {
      if (tmp instanceof String) {
        pf = tmp.toString();
      } else {
        log.error("init param is not a str: " + PF);
      }
    }
    tmp = args.get(MM);
    if (null != tmp) {
      if (tmp instanceof String) {
        mm = tmp.toString();
      } else {
        log.error("init param is not a str: " + MM);
      }
    }
    tmp = args.get(PS);
    if (null != tmp) {
      if (tmp instanceof Integer) {
        pslop = ((Integer)tmp).intValue();
      } else {
        log.error("init param is not an int: " + PS);
      }
    }
    tmp = args.get(BQ);
    if (null != tmp) {
      if (tmp instanceof String) {
        bq = tmp.toString();
      } else {
        log.error("init param is not a str: " + BQ);
      }
    }
    tmp = args.get(BF);
    if (null != tmp) {
      if (tmp instanceof String) {
        bf = tmp.toString();
      } else {
        log.error("init param is not a str: " + BF);
      }
    }
    tmp = args.get(FQ);
    if (null != tmp) {
      if (tmp instanceof String) {
        fq = tmp.toString();
      } else {
        log.error("init param is not a str: " + FQ);
      }
    }
  }
}
