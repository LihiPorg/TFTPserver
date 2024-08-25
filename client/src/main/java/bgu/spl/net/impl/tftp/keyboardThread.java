package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;


public class keyboardThread implements Runnable{
    private BufferedOutputStream out;
    private BufferedInputStream in;
    private TftpProtocol protocol;
    private TftpEncoderDecoder encdec;
    private BlockingQueue<Boolean> msgs;
    private Short opCode=0;
    private List<byte[]> list;
    public BlockingQueue<Boolean> broadCast;
    
    @Override
    public void run(){
        Scanner scan=new Scanner(System.in);
        while (!protocol.shouldTerminate()){
            System.out.print("<");
            String line=scan.nextLine();
            
            msgs.clear();
            byte[] bytes=encdec.encode(line.getBytes());  
            if (bytes!=null){    
                byte [] opCodeArray=Arrays.copyOfRange(bytes,0,2);
                opCode=byteToShort(opCodeArray);
                
                
                
                if(opCode==(short)2){
                    String filename = line.substring(4);
                    String path="./"+filename;
                    File file = new File(path);
                    if(file.exists()){
                        list.add(opCodeArray);
                        list.add(path.getBytes());
                        try {
                            out.write(bytes);
                            out.flush();
                            msgs.take();
                        } catch (IOException ignored) {
                        } catch (InterruptedException ignored) {}
                        
                    }
                    else
                        System.out.println("<File dosent exist in the client side");
                }
                else if (opCode==(short)1){
                    String filename = line.substring(4);
                    String path="./"+filename;
                    File file = new File(path);
                    if(!file.exists()){
                        try {
                            file.createNewFile();
                        } catch (IOException ignored) {}
                        list.add(opCodeArray);
                        list.add(path.getBytes());
                        try {
                            out.write(bytes);
                            out.flush();
                            msgs.take();
                        } catch (IOException ignored) {
                        } catch (InterruptedException ignored) {}
                    }
                    else
                        System.out.println("<File allready exist in the client side");
                }
                else {
                    try {
                        list.add(opCodeArray);
                        out.write(bytes);
                        out.flush();
                        msgs.take();
                    } catch (IOException ignored) {
                    } catch (InterruptedException ignored) {}
                }
                list.clear();
            }
            else{ 
                System.out.println("<Invalid commend");
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