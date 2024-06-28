package nl.roboteamtwente.autoref;

import com.google.protobuf.InvalidProtocolBufferException;
import nl.roboteamtwente.proto.StateOuterClass;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

public class WorldConnection implements Runnable {
    private final String ip;
    private final int port;
    private ZMQ.Socket worldSocket;
    private final SSLAutoRef ref;

    /**
     * Establish connection with World
     */
    public void connect() {
        try (ZContext context = new ZContext()) {
            //Create connection
            worldSocket = context.createSocket(SocketType.SUB);
            worldSocket.subscribe("");
            worldSocket.connect("tcp://" + ip + ":" + port);
            listener();
        } catch (ZMQException e) {
            //4 is the error code when we close the connection by hand, which can be ignored
            if (e.getErrorCode() != 4) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Receive and process messages
     */
    public void listener() {
        while (!Thread.currentThread().isInterrupted() && worldSocket != null) {
            try {
                byte[] buffer = worldSocket.recv();
                StateOuterClass.State packet = StateOuterClass.State.parseFrom(buffer);
                ref.checkViolations(packet);
                
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            } catch (ZMQException e) {
                //4 is the error code when we close the connection by hand, which can be ignored
                if (e.getErrorCode() != 4) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Close connection to world
     */
    public void close() {
        try {
            worldSocket.close();
        } catch (ZMQException e) {
            //4 is the error code when we close the connection by hand, which can be ignored
            if (e.getErrorCode() != 4) {
                e.printStackTrace();
            }
        } finally {
            worldSocket = null;
        }
    }

    @Override
    public void run() {
        connect();
    }


    public WorldConnection(String ip, int port, SSLAutoRef ref) {
        this.ip = ip;
        this.port = port;
        this.ref = ref;
        this.worldSocket = null;
    }
}
