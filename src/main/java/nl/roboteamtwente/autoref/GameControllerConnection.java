package nl.roboteamtwente.autoref;

import com.google.protobuf.ByteString;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcRcon;
import org.robocup.ssl.proto.SslGcRconAutoref;

import java.io.IOException;
import java.net.Socket;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

public class GameControllerConnection implements Runnable {
    private Socket socket;
    private Signature signature;
    private String token;
    private String ip;
    private int port;
    private List<SslGcGameEvent.GameEvent> queue = new ArrayList<>();
    private boolean autoConnect;
    //time between attempts to reconnect (in ms)
    private final int reconnectSleep = 1000;


    /**
     * Connect AutoRef to GameControl by:
     * First establish TCP connection
     * GameControl sends a token to us
     * AutoRef identifies itself by sending AutoRefRegistration
     * GameControl verifies
     * GameControl sends reply (OK|REJECT)
     *
     * @throws InterruptedException
     */
    private void connect() throws InterruptedException {
        try {
            this.socket = new Socket(ip, port);

            //Generate keyPair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(512, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();

            //Generate signature
            this.signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(keyPair.getPrivate());

            //receive token from GameController
            SslGcRcon.ControllerReply reply = receivePacket();
            if (reply == null) {
                return;
            }
            if (!reply.hasNextToken()) {
                throw new IllegalStateException("Missing next token");
            }

            //send registration
            SslGcRconAutoref.AutoRefRegistration registration = SslGcRconAutoref.AutoRefRegistration.newBuilder()
                    .setIdentifier("RoboTeam Twente")
                    .setSignature(getSignature())
                    .build();

            registration.writeDelimitedTo(socket.getOutputStream());
            socket.getOutputStream().flush();

            //receive reply
            reply = receivePacket();
            if (reply != null && reply.getStatusCode() != SslGcRcon.ControllerReply.StatusCode.OK) {
                System.out.println("Failed to connect to GameController: " + reply.getReason());
                reconnect();
            }
        } catch (IOException e) {
            //prevent spamming of trying to reconnect
            Thread.sleep(reconnectSleep);
            reconnect();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            //empty
        }

    }

    /**
     * Check if something is in the queue, if there is, send the first item in the queue to GC.
     * queue follows FIFO principle
     *
     * @throws InterruptedException
     */
    private void processQueue() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            if (isConnected()) {
                if (!queue.isEmpty()) {
                    //get first gameEvent from queue
                    SslGcGameEvent.GameEvent gameEvent = queue.remove(0);
                    try {
                        sendGameEvent(gameEvent);
                    } catch (IOException e) {
                        reconnect();
                    } catch (RuntimeException e) {
                        System.out.println(e.getMessage());
                    }
                }
            } else if (autoConnect) {
                reconnect();
            }
            //small delay to not always check queue but check in intervals
            Thread.sleep(10); //1 second / 100Hz = 10ms
        }
    }

    /**
     * close connection
     */
    public void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                //empty
            } finally {
                socket = null;
            }
        }
    }


    /**
     * Reconnect
     */
    private synchronized void reconnect() {
        System.out.println("Reconnecting to port " + port);
        queue.clear();
        try {
            if (this.socket != null) {
                this.socket.close();
            }

            this.socket = null;
            this.signature = null;
            if (autoConnect) {
                this.connect();
            }
        } catch (InterruptedException | IOException e) {
            //empty
        }
    }

    /**
     * Send Game Event to GameController
     *
     * @param gameEvent game event with details about the violation
     * @throws IOException      something is wrong with the connection
     * @throws RuntimeException GameEvent got rejected. This should be catched by method that calls sendGameEvent
     */
    public void sendGameEvent(SslGcGameEvent.GameEvent gameEvent) throws IOException {
        try {
            //build packet
            SslGcRconAutoref.AutoRefToController.newBuilder()
                    .setSignature(getSignature())
                    .setGameEvent(gameEvent)
                    .build()
                    .writeDelimitedTo(socket.getOutputStream());
            socket.getOutputStream().flush();

            SslGcRcon.ControllerReply reply = receivePacket();
            if (reply != null && reply.getStatusCode() != SslGcRcon.ControllerReply.StatusCode.OK) {
                throw new RuntimeException("Game event rejected: " + reply.getReason());
            }
        } catch (SignatureException e) {
            //empty
        }
    }

    /**
     * @return signature
     */
    private SslGcRcon.Signature getSignature() throws SignatureException {
        return SslGcRcon.Signature.newBuilder()
                .setToken(this.token)
                .setPkcs1V15(ByteString.copyFrom(signature.sign())).build();
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    /**
     * Receive controller reply
     * If reply has a nextToken, set this.token to nextToken and update signature
     *
     * @return reply
     */
    private SslGcRcon.ControllerReply receivePacket() {
        try {
            SslGcRcon.ControllerReply reply = SslGcRconAutoref.ControllerToAutoRef.parseDelimitedFrom(this.socket.getInputStream()).getControllerReply();
            if (reply.hasNextToken()) {
                this.token = reply.getNextToken();
                signature.update(reply.getNextTokenBytes().toByteArray());
            }
            return reply;
        } catch (IOException e) {
            this.reconnect();
        } catch (SignatureException e) {
            //empty
        }
        return null;
    }

    /**
     * @return if there is a connection
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void addToQueue(SslGcGameEvent.GameEvent gameEvent) {
        this.queue.add(gameEvent);
    }

    public int getReconnectSleep() {
        return reconnectSleep;
    }

    @Override
    public void run() {
        try {
            if (autoConnect) {
                this.connect();
            }
            this.processQueue();
        } catch (InterruptedException e) {
            //empty
        }
    }
}
