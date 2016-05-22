package io.deepstream;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class MockTcpServer {

    ServerSocket serverSocket;
    ArrayList<Socket> connections;
    ArrayList<Thread> threads;
    String lastMessage;
    Socket lastSocket;
    DataInputStream in;
    DataOutputStream out;
    public boolean isOpen;

    public MockTcpServer( int port ) throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress( true );
        serverSocket.bind(new InetSocketAddress(port));
        connections = new ArrayList<>();
        threads = new ArrayList<>();
        isOpen = true;
        MockTcpServer self = this;
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Socket sock = serverSocket.accept();
                    self.lastSocket = sock;
                    self.handleConnection(sock);
                } catch ( SocketTimeoutException e) {
                } catch ( SocketException e ) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    private void handleConnection( Socket sock ) {
        this.connections.add( sock );
        MockTcpServer self = this;

        Thread connectionThread = new Thread() {
            @Override
            public void run() {
                while( self.isOpen ) {
                    try {
                        self.in = new DataInputStream(sock.getInputStream());
                        self.out = new DataOutputStream(sock.getOutputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        String message = in.readUTF();
                        self.lastMessage = message;
                    } catch ( SocketException e) {
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        this.threads.add( connectionThread );
        connectionThread.start();
    }

    public void send( String message ) throws IOException {
        this.out.writeUTF( message );
    }

    public void close() throws InterruptedException, IOException {
        this.isOpen = false;
        for ( Socket sock : this.connections ) {
            sock.close();
        }
        for ( Thread connectedThread : this.threads ) {
            connectedThread.join(1);
        }
        this.serverSocket.close();
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public int getNumberOfConnections() {
        return this.connections.size();
    }
}
