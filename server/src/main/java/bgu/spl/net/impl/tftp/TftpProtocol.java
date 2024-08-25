package bgu.spl.net.impl.tftp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {
    private Connections<byte[]> connections;
    private int connectionId;
    private boolean shouldTerminate;
    private boolean logged =false;
    private byte[] data=null;
    private byte [] lastPacketSent=null; 
    private short currentOpCode=(short)0;
    private String filePath="";
    static ConcurrentHashMap<Integer,String> idLoggedIn=new ConcurrentHashMap<>();
    private boolean isLast=false;
    private BlockingQueue<byte[]> fileToWrite = new LinkedBlockingQueue<>();

    


    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connections=connections;
        this.connectionId=connectionId;
        shouldTerminate=false;
    
    }
    @Override
    public void process(byte[] message) {
        byte[] opCodeArray={message[0],message[1]};
        short opCode=byteToShort(opCodeArray);
        if  (opCode==(short)3){ 
                if (currentOpCode==(short)2){
                     try (FileOutputStream fos = new FileOutputStream(filePath,true)) {
                    byte[] b={message[4],message[5]};
                         fos.write(Arrays.copyOfRange(message,6,message.length));
                    
                    ACKPacket(byteToShort(b));
                    }catch(IOException e){
                        errorPacket((short)0,"");
                    }
                    if (message.length==6||message.length<518){
                        try {
                            File finalFile = new File (filePath);
                            boolean exists=finalFile.createNewFile();
                            
                                try (FileOutputStream fos = new FileOutputStream(filePath,true)) {
                                    while(!fileToWrite.isEmpty()){
                                        fos.write(fileToWrite.remove());
                                    }
                                    }catch(IOException e){
                                        errorPacket((short)0,"");
                                }
                            
                        }
                        catch(IOException e){
                            errorPacket((short)0,"couldnt save the file");
                        }
                        broadCast(filePath.substring(2).getBytes(), (short)1);
                        filePath=null;
                        currentOpCode=(short)0;
                    }

                }
            }
        else if (opCode==(short)1){
            byte[] fileNameArray=Arrays.copyOfRange(message,2,message.length-1);
            String filename = new String(fileNameArray);
            String path="./Flies/"+filename;
            filePath=path;
            File file = new File(path);
            if(file.exists()&&logged){
                try {
                    FileInputStream inputStream = new FileInputStream(file);
                    byte[] fileBytes = new byte[(int) file.length()];
                    int numOfBytes = inputStream.read(fileBytes);
                    data=fileBytes;
                    short size=(short)512;
                    if(data.length<512){
                        size=(short)data.length;
                        isLast=true;
                    }
                    dataPacket(size, (short)1, data);
                    currentOpCode=(short)1;
                            
                } catch (IOException e) {
                    errorPacket((short)2,"Access violation");
                }
            }
            else if(!file.exists())
                errorPacket((short)1,"File not found.");
            else if (!logged)
                errorPacket((short)6,"user not logged in");
        }
        else if(opCode==(short)8){
                byte[] fileNameArray=Arrays.copyOfRange(message,2,message.length-1);
                String filename = new String(fileNameArray);
                String path="./Flies/"+filename;
                File file = new File(path);
                if(file.exists()&&logged){
                    boolean test = file.delete();
                    ACKPacket((short)0);
                    broadCast(fileNameArray,(short)0);
                }
                else if(!file.exists()){ 
                    errorPacket((short)1,"File not found");
                }
                else if (!logged)
                errorPacket((short)6,"user not logged in");
            }
        else if(opCode>10||opCode<1)
            errorPacket((short)4,"Unknown Opcode.");
        
        else if(opCode==(short)2){
            byte[] fileNameArray=Arrays.copyOfRange(message,2,message.length-1);
            String filename = new String(fileNameArray);

            String path="./Flies/"+filename;
            filePath=path;
            File f = new File(path);
            boolean opened=false;
            if(!f.exists()&&logged){
                ACKPacket((short)0);
                currentOpCode=2;
                filePath=path; 
            }
            else if(f.exists()){ 
                errorPacket((short)5, "File already exists");
            }
            else if (!logged)
                errorPacket((short)6,"user not logged in");
            
        }
        else if(opCode==(short)7){
            String userName=Arrays.toString(Arrays.copyOfRange(message,2,message.length-1));
            if(!idLoggedIn.contains(userName)&&!idLoggedIn.containsKey(connectionId)){
                logged=true;
                idLoggedIn.put(connectionId,Arrays.toString(Arrays.copyOfRange(message,2,message.length-1)));
                ACKPacket((short)0);
            }
            else if(idLoggedIn.contains(userName)){
                errorPacket((short)7,"This user name already taken");
            }
            else if(idLoggedIn.containsKey(connectionId)){
                errorPacket((short)7,"User allready logged in");
            }
        }

    
        else if(opCode==(short)4){
            byte[] b={message[2],message[3]};
            Short blockNumber=byteToShort(b);
            
            int blockNumberInt= blockNumber.intValue()+1;
            blockNumber=(short)(blockNumberInt); 
            Short size=(short)512;
            if(isLast){
                isLast=false;
                lastPacketSent=null;
                filePath=null;
            }
            else{

                if(((data.length)-((blockNumberInt-1)*512))<512){
                    
                    size=(short)((data.length)-((blockNumberInt-1)*512));
                    isLast=true;
                     
                }
                
                
                dataPacket(size, (short)blockNumber, data); 
            }
            
        
            
        }
        else if (opCode==(short)5){
            connections.send(connectionId,lastPacketSent);
            
        }
        else if(opCode==(short)6){
            
            if(logged){
                String dirq="";
                File directoryPath = new File("./Flies");
                File[] files= directoryPath.listFiles();
                
                List<Byte> byteList = new ArrayList<>();
                currentOpCode=(short)6;

                if (files != null) {
                    for (File file : files) {
                        String fileName = file.getName();
                        
                        byte[] fileNameBytes = fileName.getBytes();
                        for (byte b : fileNameBytes) {
                            byteList.add(b);
                        }
                        byteList.add((byte) 0);
                    }
                }
                byte[] byteArray = new byte[byteList.size()];
                for (int i = 0; i < byteList.size(); i++) {
                    byteArray[i] = byteList.get(i);
                }
                data=byteArray;
                short size=512;
                if(data.length<512){
                    size=(short)data.length;
                    isLast=true;
                }
                
                dataPacket(size, (short)1, data);
            }
            else
                errorPacket((short)6,"user not logged in");
        }
        else if(opCode==(short)10){
           
            if(logged){
               
                shouldTerminate=true;
                logged=false;
                idLoggedIn.remove(connectionId);
                ACKPacket((short)0);
                connections.disconnect(connectionId);
            }
            else{
               
                errorPacket((short)6,"user not logged in");
            }
        }
    }




    @Override
    public boolean shouldTerminate() {
        
        return shouldTerminate;
    } 


    public void broadCast (byte[] fileBytes,short flag){
        byte[] B=shortToByte((short) 9 );
        byte [] C= shortToByte(flag);
        Set<Integer> keys = idLoggedIn.keySet();
        byte [] msg = new byte[fileBytes.length+4];
        msg [0]=B[0];
        msg [1]=B[1];
        msg [2]=C[1];
        for(int i=0;i<fileBytes.length;i++){
            msg[i+3]=fileBytes[i];
        }
        msg[msg.length-1]=0;
        for (Integer key : keys) {
            connections.send(key, msg);
        }
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
    public void errorPacket (short errorCode,String errorMsg)
    {
        byte[] b=shortToByte((short)5);
        byte[] c=shortToByte(errorCode);
        
        byte[] response1 = errorMsg.getBytes();
        byte[] response=new byte[4+response1.length+1];
        response[0]=b[0];
        response[1]=b[1];
        response[2]=c[0];
        response[3]=c[1];
        response[response.length-1]=(byte)0;
        for (int i=0; i<response1.length; i++){
            response[i+4]=response1[i];
        }
        connections.send(connectionId,response);
    }
    public void ACKPacket (short blockNumber){
        byte[] b=shortToByte((short)4);
        byte[] c=shortToByte(blockNumber);
        byte[] response= {b[0],b[1],c[0],c[1]};
        
        connections.send(connectionId,response);
    }
    public void dataPacket(Short size,short block,byte[] data){
        byte[] b=shortToByte((short)3);
        byte[] c=shortToByte(size);
        byte[] d=shortToByte(block);
        int sizeInt=size.intValue();
        byte[] response=new byte[6+sizeInt]; 
        response[0]=b[0];
        response[1]=b[1];
        response[2]=c[0];
        response[3]=c[1];
        response[4]=d[0];
        response[5]=d[1];
        if(data!=null){
            for (int i=((int)block-1)*512 ; i<((int)block-1)*512+size&&i<data.length;i++){
                response[(i-(((int)block-1)*512))+6]=data[i];
            }
        }
        connections.send(connectionId,response);
    }
}
