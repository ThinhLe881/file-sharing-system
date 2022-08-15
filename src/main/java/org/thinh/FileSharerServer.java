package org.thinh;

import java.io.*;
import java.net.*;

public class FileSharerServer {
    protected Socket clientSocket = null;
    protected ServerSocket serverSocket = null;
    protected FileSharerThread[] threads = null;
    protected File serverRoot = null;
    protected int numClients = 0;

    public static int MAX_CLIENTS = 25;
    public static int SERVER_PORT = 6868;
    public static String SERVER_ADDRESS = null;

    public FileSharerServer(String path) {
        try {
            serverRoot = new File(path);
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("---------------------------------------------");
            System.out.println("File Sharer Application Server is running");
            System.out.println("---------------------------------------------");
            InetAddress localhost = InetAddress.getLocalHost();
            SERVER_ADDRESS = localhost.getHostAddress();
            System.out.println("Server address: " + SERVER_ADDRESS);
            System.out.println("Listening to port: " + SERVER_PORT);
            System.out.println("---------------------------------------------");
            threads = new FileSharerThread[MAX_CLIENTS];
            while (true) {
                clientSocket = serverSocket.accept();
                threads[numClients] = new FileSharerThread(clientSocket, serverRoot);
                threads[numClients].start();
                numClients++;
                if (numClients == MAX_CLIENTS) {
                    numClients = 0;
                }
            }
        } catch (IOException e) {
            System.err.println("IOException while creating server connection");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        FileSharerServer server = new FileSharerServer(args[0]);
    }
}
