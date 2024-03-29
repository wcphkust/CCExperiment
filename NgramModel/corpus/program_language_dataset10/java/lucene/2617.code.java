package org.apache.solr.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class VersionedFile 
{
  public static InputStream getLatestFile(String dirName, String fileName) throws FileNotFoundException 
  {
    Collection<File> oldFiles=null;
    final String prefix = fileName+'.';
    File f = new File(dirName, fileName);
    InputStream is = null;
    for (int retry=0; retry<10 && is==null; retry++) {
      try {
        if (!f.exists()) {
          File dir = new File(dirName);
          String[] names = dir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
              return name.startsWith(prefix);
            }
          });
          Arrays.sort(names);
          f = new File(dir, names[names.length-1]);
          oldFiles = new ArrayList<File>();
          for (int i=0; i<names.length-1; i++) {
            oldFiles.add(new File(dir, names[i]));
          }
        }
        is = new FileInputStream(f);
      } catch (Exception e) {
      }
    }
    if (is == null) {
      is = new FileInputStream(f);
    }
    if (oldFiles != null) {
      delete(oldFiles);
    }
    return is;
  }
  private static final Set<File> deleteList = new HashSet<File>();
  private static synchronized void delete(Collection<File> files) {
    synchronized (deleteList) {
      deleteList.addAll(files);
      List<File> deleted = new ArrayList<File>();
      for (File df : deleteList) {
        try {
          df.delete();
          deleted.add(df);
        } catch (SecurityException e) {
          if (!df.exists()) {
            deleted.add(df);
          }
        }
      }
      deleteList.removeAll(deleted);
    }
  }
}
