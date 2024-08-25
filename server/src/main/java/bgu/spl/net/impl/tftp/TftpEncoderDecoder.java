package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;
import java.util.List;
import java.util.zip.ZipError;
import java.util.Arrays;
import java.util.LinkedList;


public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;
    private short opCode=(short)0;
    private Short dataSize=(short)0;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        //notice that the top 128 ascii characters have the same representation as their utf-8 counterparts
        //this allow us to do the following comparison
        pushByte(nextByte);
        if(len==2){
            opCode=byteToShort(Arrays.copyOfRange(bytes,0,len));
            if(opCode==(short)6||opCode==(short)10){
                int lenTemp=len;
                len=0;
                opCode=(short)0;
                return Arrays.copyOfRange(bytes,0,lenTemp);
            }
        }
        else if (opCode==(short)3&&len==((dataSize).intValue()+6)){
            int lenTemp=len;
            len=0;
            opCode=(short)0;
            dataSize=(short)0;
            return Arrays.copyOfRange(bytes,0,lenTemp);
        }
        else if(opCode!=(short)3&&opCode!=(short)4&&opCode!=(short)0&&opCode!=(short)5&& nextByte==(byte)0){
            int lenTemp=len;
            len=0;
            opCode=(short)0;
            return Arrays.copyOfRange(bytes,0,lenTemp);
        }
        else if(opCode==(short)5&&len>4&& nextByte==(byte)0)
        {
            int lenTemp=len;
            len=0;
            opCode=(short)0;
            return Arrays.copyOfRange(bytes,0,lenTemp);
        }
        else if(opCode==(short)3&&len==4){
            dataSize=byteToShort(Arrays.copyOfRange(bytes,2,len));
            
        }
        else if (opCode==(short)4&&len==4) {
            int lenTemp=len;
            len=0;
            opCode=(short)0;
            return Arrays.copyOfRange(bytes,0,lenTemp);
        }
        

        return null; //not a line yet
    }
    
    @Override
    public byte[] encode(byte[] message) { //makes bytes
        //TODO: implement this
        return message;
    }

    public byte[] shortToByte(short number) 
    {
        // converting short to byte array
      
        byte [] number_bytes = new byte []{(byte)(number>>8),(byte)(number&0xff) };
        return number_bytes;

    }
    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
    }
    public short byteToShort(byte[] b) 
    {
        // converting 2 byte array to a short
        //byte [] b = bytes;
        short b_short = ( short ) ((( short ) b [0]) << 8 | ( short ) ( b [1]) & 0x00ff);
        return b_short;

    }
    
  

}