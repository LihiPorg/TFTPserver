package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

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
        }
        else if(opCode==(short)9&&len>3&& nextByte==(byte)0)
        {
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
        else if (opCode==(short)3&&len==((dataSize).intValue()+6)){
            
            int lenTemp=len;
            len=0;
            opCode=(short)0;
            dataSize=(short)0;
            return Arrays.copyOfRange(bytes,0,lenTemp);
        }

        return null; 
    }

    @Override
    public byte[] encode(byte[] message) {
        String s=new String(message, StandardCharsets.UTF_8);
        String[] split=s.split(" ");
        if(split[0].equals("DIRQ")){
            byte[] msg= shortToByte ((short) 6);
            return msg;
        }
        if(split[0].equals("DISC")){
            byte[] msg= shortToByte ((short) 10);
            return msg;
        }
        
        if(split[0].equals("RRQ")||split[0].equals("WRQ")||split[0].equals("DELRQ")||split[0].equals("LOGRQ"))
        {
            String fileName="";
            byte[] opCode=new byte[2];
            if(split[0].equals("RRQ"))
            {
                fileName=s.substring(4,s.length());
                opCode=shortToByte((short) 1);
            }
            if (split[0].equals("WRQ")){
                fileName=s.substring(4,s.length());
                opCode= shortToByte ((short) 2);
            }
            if (split[0].equals("DELRQ")){
                fileName=s.substring(6,s.length());
                opCode=shortToByte ((short) 8);
            }
            if(split[0].equals("LOGRQ")){
                
                fileName=s.substring(6,s.length());
                opCode=shortToByte ((short) 7);
            }
            byte [] name= fileName.getBytes();
            byte[] msg=new byte[2+name.length+1];  
            msg[0]=opCode[0];
            msg[1]=opCode[1];
            for (int i=0;i<name.length;i++){
                msg[i+2]=name[i];
            }
            msg[msg.length-1]=((byte)0);
            return msg; 
        }
        return null; 
    }
                
    

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
    }


    public byte[] shortToByte(short number) 
    {
        // converting short to byte array
      
        byte [] number_bytes = new byte []{(byte)(number>>8),(byte)(number&0xff) };
        return number_bytes;

    }
    
    public short byteToShort(byte[] b) 
    {
        // converting 2 byte array to a short
        //byte [] b = bytes;
        short b_short = ( short ) ((( short ) b [0]) << 8 | ( short ) ( b [1]) & 0x00ff);
        return b_short;
    }
    
}
