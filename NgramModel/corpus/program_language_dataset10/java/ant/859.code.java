package org.apache.tools.tar;
public interface TarConstants {
    int    NAMELEN = 100;
    int    MODELEN = 8;
    int    UIDLEN = 8;
    int    GIDLEN = 8;
    int    CHKSUMLEN = 8;
    int    SIZELEN = 12;
    long   MAXSIZE = 077777777777L;
    int    MAGICLEN = 8;
    int    MODTIMELEN = 12;
    int    UNAMELEN = 32;
    int    GNAMELEN = 32;
    int    DEVLEN = 8;
    byte   LF_OLDNORM = 0;
    byte   LF_NORMAL = (byte) '0';
    byte   LF_LINK = (byte) '1';
    byte   LF_SYMLINK = (byte) '2';
    byte   LF_CHR = (byte) '3';
    byte   LF_BLK = (byte) '4';
    byte   LF_DIR = (byte) '5';
    byte   LF_FIFO = (byte) '6';
    byte   LF_CONTIG = (byte) '7';
    String TMAGIC = "ustar";
    String GNU_TMAGIC = "ustar  ";
    String GNU_LONGLINK = "././@LongLink";
    byte LF_GNUTYPE_LONGNAME = (byte) 'L';
}