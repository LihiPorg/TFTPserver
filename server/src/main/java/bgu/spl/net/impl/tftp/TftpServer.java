package bgu.spl.net.impl.tftp;
import bgu.spl.net.srv.Server;

public class TftpServer {
    public static void main(String[] args) {
        if(args.length!=1){
            System.out.println("Invalid port");
        }
        else{
            // you can use any server... 
            Server.threadPerClient(
                    Integer.parseInt(args[0]), //port
                    () -> new TftpProtocol(), //protocol factory
                    TftpEncoderDecoder::new //message encoder decoder factory
            ).serve();
        }
    }
    
    
}
