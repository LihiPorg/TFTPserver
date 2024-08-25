package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class listeningThread implements Runnable{
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private TftpProtocol protocol;
    private TftpEncoderDecoder encdec;
    private BlockingQueue<Boolean> msgs;
    private List<byte[]> list;
    private Short blockNumber=(short)0;
    private byte[][] FileToWrite=null;
    private ArrayList<Byte> DIRQ=new ArrayList<Byte>();
    private int counter=0;
    private Boolean IsWaitingForBCast= false; 

    
    
    @Override
    public void run(){
        while (!protocol.shouldTerminate()) {
            int read=0;
            byte[] bytes=null;
            try {
                if((read=in.read())>=0){
				    bytes=encdec.decodeNextByte((byte)read);}
			} catch (IOException ignored) {}
            
            if(bytes!=null){
                byte[] opCodeArray=(Arrays.copyOfRange(bytes,0,2));
                short opCode=byteToShort(opCodeArray);
                if(opCode!=(short)9){
                    byte[] originalOpCodeArray=list.get(0);
                    short originalOpCode=byteToShort(originalOpCodeArray);
                    
                    
                    if(opCode==(short)4)
                    {
                        if(originalOpCode!=(short)2){
                            if(originalOpCode==(short)8){
                                IsWaitingForBCast=true;
                                protocol.process(bytes);
                            }
                            else{
                                if(originalOpCode==(short)10){
                                    bytes=shortToByte(originalOpCode);
                                }
                                
                                protocol.process(bytes);
                                try {
                                    msgs.put(Boolean.TRUE);
                                } catch (InterruptedException ignored) {}
                            }
                        }
                        else{
                            protocol.process(bytes);
                            if(blockNumber==(short)0){
                                String path=new String(list.get(1));
                                File file = new File(path);
                                try (FileInputStream inputStream = new FileInputStream(file)) {
                                    byte[] fileBytes = new byte[(int) file.length()];
                                    int numOfBytes = inputStream.read(fileBytes);
                                    int numOfPackets=(int)Math.ceil(numOfBytes/512.0);
                                    if(numOfPackets==numOfBytes/512){
                                        numOfPackets=numOfPackets+1;
                                    }
                                    byte [] dataSend=null;
                                    FileToWrite=new byte[numOfPackets][];    
                                    byte[] opCodeData=shortToByte((short)3);     
                                    for(int i=0;i<FileToWrite.length;i++)
                                    {
                                        int sizePacket=0;
                                        for (int j=i*512;j<fileBytes.length&&j<(i*512)+512;j++)
                                            sizePacket++;
                                        dataSend = new byte [6+sizePacket];
                                        for (int j=i*512;j<fileBytes.length&&j<(i*512)+512;j++)
                                            dataSend[j-(i*512)+6]=fileBytes[j];
                                        byte []blockNum= shortToByte((short)(i+1));
                                        dataSend[0]=opCodeData[0];
                                        dataSend[1]=opCodeData[1];
                                        dataSend[4]=blockNum[0];
                                        dataSend[5]=blockNum[1];
                                        byte[] size=shortToByte((short)sizePacket);
                                        dataSend[2]=size[0];
                                        dataSend[3]=size[1];
                                        FileToWrite[i]=dataSend;
                                    }
                                    
                                }
                                catch (IOException ignored) {}
                            } 

                           
                            if(((Short)blockNumber).intValue()!=FileToWrite.length){
                                blockNumber++;
                                try {
                                    out.write(FileToWrite[(int)(blockNumber-(short)1)]);
                                    out.flush();
                                } catch (IOException ignored) {}
                            }
                            else{
                                IsWaitingForBCast=true;
                                String path=new String(list.get(1));
                                System.out.println("> WRQ "+path.substring(2)+" complete");
                                list.clear();
                                blockNumber=(short)0;
                                FileToWrite=null;

                            }
                            
                        }
                    }

                    if(opCode==(short)5){
                        protocol.process(bytes);
                        if(originalOpCode==(short)2){
                            list.clear();
                            blockNumber=(short)0;
                            FileToWrite=null;
                            try {
                                msgs.put(Boolean.TRUE);
                            } catch (InterruptedException ignored) {}
                        }
                        else if(originalOpCode==(short)1){
                            String path=new String(list.get(1));
                            File file = new File(path);
                            file.delete();
                            list.clear();
                            blockNumber=(short)0;
                            FileToWrite=null;
                            try {
                                msgs.put(Boolean.TRUE);
                            } catch (InterruptedException ignored) {}
                        }
                        else{ 
                            list.clear();
                            blockNumber=(short)0;
                            FileToWrite=null;
                            try {
                                msgs.put(Boolean.TRUE);
                            } catch (InterruptedException ignored) {}
                        }
                    
                        

                    }
                    if(opCode==(short)3){
                        if(originalOpCode==(short)6){
                            for(int i=6;i<bytes.length;i++){
                                DIRQ.add(bytes[i]);
                            }
                            if(bytes.length==6||bytes.length<518){
                                String s="";
                                ArrayList<Byte> fileName=new ArrayList<Byte>();
                                for(Byte b:DIRQ){
                                    if(b!=(byte)0){
                                        fileName.add(b);
                                    }
                                    else{
                                        byte[] fileNameArray=new byte[fileName.size()];
                                        for (int i = 0; i < fileName.size(); i++) {
                                            fileNameArray[i] = fileName.get(i);
                                        }
                                        String fileNameStr=new String(fileNameArray,StandardCharsets.UTF_8);
                                        s=s+">"+fileNameStr+"\n";
                                        fileName.clear();
                                    }
                                }
                                System.out.print(s);
                                list.clear();
                                DIRQ.clear();
                                blockNumber=(short)0;
                                FileToWrite=null;
                                try {
                                    byte[] opCodeAck=shortToByte((short)4);
                                    byte[] ACK= {opCodeAck[0],opCodeAck[1],bytes [4],bytes[5]};
                                    try {
                                        out.write(ACK);
                                        out.flush();
                                    } catch (IOException ignored) {}
                                    msgs.put(Boolean.TRUE);
                                } catch (InterruptedException ignored) {}
                            }
                            else{
                                
                                    byte[] opCodeAck=shortToByte((short)4);
                                    byte[] ACK= {opCodeAck[0],opCodeAck[1],bytes [4],bytes[5]};
                                    try {
                                        out.write(ACK);
                                        out.flush();
                                    } catch (IOException ignored) {}
                            
                            }

                        }
                        if(originalOpCode==(short)1){
                            String path=new String(list.get(1));
                            try (FileOutputStream fos = new FileOutputStream(path,true)) {
                                fos.write(Arrays.copyOfRange(bytes,6,bytes.length));
                            }catch (IOException ignored) {}
                            if(bytes.length==6||bytes.length<518){ //check!!!!!!!!!!!!
                                try {
                                    byte[] opCodeAck=shortToByte((short)4);
                                    byte[] ACK= {opCodeAck[0],opCodeAck[1],bytes [4],bytes[5]};
                                    path=new String(list.get(1));
                                    System.out.println("> RRQ "+path.substring(2)+" complete");
                                    try {
                                        out.write(ACK);
                                        out.flush();
                                    } catch (IOException ignored) {}
                                    list.clear();
                                    msgs.put(Boolean.TRUE);
                                } catch (InterruptedException ignored) {}
                                
                            }
                            else{
                                
                                byte[] opCodeAck=shortToByte((short)4);
                                byte[] ACK= {opCodeAck[0],opCodeAck[1],bytes [4],bytes[5]};
                                try {
                                    out.write(ACK);
                                    out.flush();
                                } catch (IOException ignored) {}
                        
                            }
                        } 
                    } 
                }
                else{
                    if (IsWaitingForBCast){
                        protocol.process(bytes);
                        try {
                            IsWaitingForBCast=false;
                            msgs.put(Boolean.TRUE);
                        }
                        catch (InterruptedException ignored) {}
                    }
                    else{
                        System.out.print("\n");
                        protocol.process(bytes);
                        System.out.print("<");
                    }
                }
            }
            

        }
    }

	void start(BufferedOutputStream out,BufferedInputStream in,TftpProtocol protocol,TftpEncoderDecoder encdec,BlockingQueue<Boolean> msgs,List<byte[]> list){
        this.out=out;
        this.in=in;
        this.protocol=protocol;
        this.encdec=encdec;
        this.msgs=msgs;
        this.list=list;

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