package org.apache.xerces.impl.dtd.models;
public class CMStateSet
{
    public CMStateSet(int bitCount) 
    {
        fBitCount = bitCount;
        if (fBitCount < 0)
            throw new RuntimeException("ImplementationMessages.VAL_CMSI");
        if (fBitCount > 64)
        {
            fByteCount = fBitCount / 8;
            if (fBitCount % 8 != 0)
                fByteCount++;
            fByteArray = new byte[fByteCount];
        }
        zeroBits();
    }
    public String toString()
    {
        StringBuffer strRet = new StringBuffer();
        try
        {
            strRet.append('{');
            for (int index = 0; index < fBitCount; index++) {
                if (getBit(index)) {
                    strRet.append(' ').append(index);
                }
            }
            strRet.append(" }");
        }
        catch(RuntimeException exToCatch)
        {
        }
        return strRet.toString();
    }
    public final void intersection(CMStateSet setToAnd)
    {
        if (fBitCount < 65)
        {
            fBits1 &= setToAnd.fBits1;
            fBits2 &= setToAnd.fBits2;
        }
         else
        {
            for (int index = fByteCount - 1; index >= 0; index--)
                fByteArray[index] &= setToAnd.fByteArray[index];
        }
    }
    public final boolean getBit(int bitToGet) 
    {
        if (bitToGet >= fBitCount)
            throw new RuntimeException("ImplementationMessages.VAL_CMSI");
        if (fBitCount < 65)
        {
            final int mask = (0x1 << (bitToGet % 32));
            if (bitToGet < 32)
                return (fBits1 & mask) != 0;
            else
                return (fBits2 & mask) != 0;
        }
         else
        {
            final byte mask = (byte)(0x1 << (bitToGet % 8));
            final int ofs = bitToGet >> 3;
            return ((fByteArray[ofs] & mask) != 0);
        }
    }
    public final boolean isEmpty()
    {
        if (fBitCount < 65)
        {
            return ((fBits1 == 0) && (fBits2 == 0));
        }
         else
        {
            for (int index = fByteCount - 1; index >= 0; index--)
            {
                if (fByteArray[index] != 0)
                    return false;
            }
        }
        return true;
    }
    final boolean isSameSet(CMStateSet setToCompare)
    {
        if (fBitCount != setToCompare.fBitCount)
            return false;
        if (fBitCount < 65)
        {
            return ((fBits1 == setToCompare.fBits1)
            &&      (fBits2 == setToCompare.fBits2));
        }
        for (int index = fByteCount - 1; index >= 0; index--)
        {
            if (fByteArray[index] != setToCompare.fByteArray[index])
                return false;
        }
        return true;
    }
    public final void union(CMStateSet setToOr)
    {
        if (fBitCount < 65)
        {
            fBits1 |= setToOr.fBits1;
            fBits2 |= setToOr.fBits2;
        }
         else
        {
            for (int index = fByteCount - 1; index >= 0; index--)
                fByteArray[index] |= setToOr.fByteArray[index];
        }
    }
    public final void setBit(int bitToSet) 
    {
        if (bitToSet >= fBitCount)
            throw new RuntimeException("ImplementationMessages.VAL_CMSI");
        if (fBitCount < 65)
        {
            final int mask = (0x1 << (bitToSet % 32));
            if (bitToSet < 32)
            {
                fBits1 &= ~mask;
                fBits1 |= mask;
            }
             else
            {
                fBits2 &= ~mask;
                fBits2 |= mask;
            }
        }
         else
        {
            final byte mask = (byte)(0x1 << (bitToSet % 8));
            final int ofs = bitToSet >> 3;
            fByteArray[ofs] &= ~mask;
            fByteArray[ofs] |= mask;
        }
    }
    public final void setTo(CMStateSet srcSet) 
    {
        if (fBitCount != srcSet.fBitCount)
            throw new RuntimeException("ImplementationMessages.VAL_CMSI");
        if (fBitCount < 65)
        {
            fBits1 = srcSet.fBits1;
            fBits2 = srcSet.fBits2;
        }
         else
        {
            for (int index = fByteCount - 1; index >= 0; index--)
                fByteArray[index] = srcSet.fByteArray[index];
        }
    }
    public final void zeroBits()
    {
        if (fBitCount < 65)
        {
            fBits1 = 0;
            fBits2 = 0;
        }
         else
        {
            for (int index = fByteCount - 1; index >= 0; index--)
                fByteArray[index] = 0;
        }
    }
    int         fBitCount;
    int         fByteCount;
    int         fBits1;
    int         fBits2;
    byte[]      fByteArray;
    public boolean equals(Object o) {
	if (!(o instanceof CMStateSet)) return false;
	return isSameSet((CMStateSet)o);
    }
    public int hashCode() {
        if (fBitCount < 65)
        {
            return fBits1+ fBits2 * 31;
        }
         else
        {
            int hash = 0;
            for (int index = fByteCount - 1; index >= 0; index--)
                hash = fByteArray[index] + hash * 31;
            return hash;
        }
    }
};
