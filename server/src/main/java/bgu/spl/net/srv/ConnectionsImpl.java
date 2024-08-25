package bgu.spl.net.srv;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;


public class ConnectionsImpl<T> implements Connections<T> {

    ConcurrentHashMap<Integer,ConnectionHandler<T>> idConnected=new ConcurrentHashMap<>();
    public void connect(int connectionId, ConnectionHandler<T> handler)
    {
        
        idConnected.putIfAbsent(connectionId,handler);
        
    }

    public boolean send(int connectionId, T msg)
    {
        
        if(idConnected.containsKey((Integer)connectionId)){
            
            (idConnected.get((Integer)connectionId)).send(msg);
            return true;
        }
        return false;
    }

    public void disconnect(int connectionId){
         if(idConnected.containsKey(connectionId)){
            try{
                (idConnected.get((Integer)connectionId)).close();
            }catch(IOException ignored){}
            idConnected.remove((Integer)connectionId);
            System.out.println((idConnected.toString()));
            
        }
    }
    
}

