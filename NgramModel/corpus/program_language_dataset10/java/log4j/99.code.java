package org.apache.log4j.helpers;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.Appender;
import java.util.Vector;
import java.util.Enumeration;
public class AppenderAttachableImpl implements AppenderAttachable {
  protected Vector  appenderList;
  public
  void addAppender(Appender newAppender) {
    if(newAppender == null)
      return;
    if(appenderList == null) {
      appenderList = new Vector(1);
    }
    if(!appenderList.contains(newAppender))
      appenderList.addElement(newAppender);
  }
  public
  int appendLoopOnAppenders(LoggingEvent event) {
    int size = 0;
    Appender appender;
    if(appenderList != null) {
      size = appenderList.size();
      for(int i = 0; i < size; i++) {
	appender = (Appender) appenderList.elementAt(i);
	appender.doAppend(event);
      }
    }    
    return size;
  }
  public
  Enumeration getAllAppenders() {
    if(appenderList == null)
      return null;
    else 
      return appenderList.elements();    
  }
  public
  Appender getAppender(String name) {
     if(appenderList == null || name == null)
      return null;
     int size = appenderList.size();
     Appender appender;
     for(int i = 0; i < size; i++) {
       appender = (Appender) appenderList.elementAt(i);
       if(name.equals(appender.getName()))
	  return appender;
     }
     return null;    
  }
  public 
  boolean isAttached(Appender appender) {
    if(appenderList == null || appender == null)
      return false;
     int size = appenderList.size();
     Appender a;
     for(int i = 0; i < size; i++) {
       a  = (Appender) appenderList.elementAt(i);
       if(a == appender)
	  return true;
     }
     return false;    
  }
  public
  void removeAllAppenders() {
    if(appenderList != null) {
      int len = appenderList.size();      
      for(int i = 0; i < len; i++) {
	Appender a = (Appender) appenderList.elementAt(i);
	a.close();
      }
      appenderList.removeAllElements();
      appenderList = null;      
    }
  }
  public
  void removeAppender(Appender appender) {
    if(appender == null || appenderList == null) 
      return;
    appenderList.removeElement(appender);    
  }
  public
  void removeAppender(String name) {
    if(name == null || appenderList == null) return;
    int size = appenderList.size();
    for(int i = 0; i < size; i++) {
      if(name.equals(((Appender)appenderList.elementAt(i)).getName())) {
	 appenderList.removeElementAt(i);
	 break;
      }
    }
  }
}