package bgu.spl.net.impl.tftp;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TftpClient {
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"localhost", "hello"};
        }

        if (args.length < 2) {
            System.out.println("you must supply two arguments: host, message");
            System.exit(1);
        }

        
        try (Socket sock = new Socket(args[0], Integer.parseInt(args[1]));
                BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
                BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream());) {
                
                TftpProtocol protocol=new TftpProtocol();
                TftpEncoderDecoder encdec=new TftpEncoderDecoder();
                BlockingQueue<Boolean> msgs=new LinkedBlockingQueue<>();
                List<byte[]> list=new LinkedList<byte[]>();
                
                
                keyboardThread key = new keyboardThread();
                key.start(out, in, protocol, encdec, msgs, list);
                Thread threadKey = new Thread(key);
                threadKey.start();

                listeningThread listening = new listeningThread();
                listening.start(out, in, protocol, encdec, msgs, list);
                Thread threadListening = new Thread(listening);
                threadListening.start();

                threadKey.join(); 
                threadListening.join(); 
                            
            
            } catch (Exception ignored) {}
                
                
        
        
    }
}


