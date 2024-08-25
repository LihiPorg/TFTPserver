package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessagingProtocol;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;

public class TftpProtocol implements MessagingProtocol<byte[]> {

    private boolean shouldTerminate = false;

    @Override
    public byte[] process(byte[] msg) {
        
        byte [] opCode= new byte [2];
        opCode [0]= msg[0];
        opCode [1]= msg[1];
        Short opCodeShort = byteToShort(opCode);

        if (opCodeShort==(short)4){
            byte [] b = {msg[2],msg[3]};
            System.out.println(">ACK"+" "+(short)byteToShort(b));
        }
        else if (opCodeShort==(short)9){
            
            
            String s =new String (Arrays.copyOfRange(msg, 3, msg.length-1), StandardCharsets.UTF_8);
           
            String add="del ";
            if (msg[2]==(byte)1){
                add="add ";
                s=s.substring(6);
            }
            
            
            
            System.out.println(">BCAST"+" " +add+s);
            
        }
        else if (opCodeShort==(short)5){
            byte [] code = {msg[2],msg[3]}; 
            String s=new String(Arrays.copyOfRange(msg,4,msg.length-1), StandardCharsets.UTF_8);

            System.out.println(">ERROR"+" "+(short)byteToShort(code)+" "+s);
        }
        else if (opCodeShort==(short)10){
            System.out.println(">ACK"+" "+(short)(0));
            shouldTerminate=true;
        }
        return null;
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
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
