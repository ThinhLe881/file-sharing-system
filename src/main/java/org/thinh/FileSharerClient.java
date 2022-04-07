package org.thinh;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class FileSharerClient {
    private Socket socket = null;
    private DataOutputStream dataOutputStream = null;
    private DataInputStream dataInputStream = null;

    public static String SERVER_ADDRESS = null;
    public static int SERVER_PORT = 6868;

    public FileSharerClient(String serverAddress) {
        try {
            SERVER_ADDRESS = serverAddress;
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connection established");
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + SERVER_ADDRESS);
        } catch (IOException e) {
            System.err.println("IOException while connecting to server: " + SERVER_ADDRESS);
        }
        if (socket == null) {
            System.err.println("Socket is null");
        }
        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.err.println("IOException while opening a read/write connection");
        }
    }

    public String dir() throws IOException {
        dataOutputStream.writeUTF("DIR");

        String dirContent = "";
        String message;
        while (true) {
            if (dataInputStream.available() > 0) {
                message = dataInputStream.readUTF();
                if (message.equalsIgnoreCase("exit()")) {
                    break;
                }
                dirContent += message + "\n";
            }
        }
        socket.close();
        System.out.println("Connection destroyed");
        return dirContent;
    }

    public void upload(File file) throws IOException {
        dataOutputStream.writeUTF("UPLOAD");
        dataOutputStream.writeUTF(file.getName());
        int bytes = 0;
        FileInputStream fileInputStream = new FileInputStream(file);
        dataOutputStream.writeLong(file.length());
        byte[] buffer = new byte[8*1024];
        while ((bytes = fileInputStream.read(buffer)) != -1){
            dataOutputStream.write(buffer,0, bytes);
            dataOutputStream.flush();
        }
        fileInputStream.close();
        String errorMessage = dataInputStream.readUTF();
        System.err.println(errorMessage);
        socket.close();
        System.out.println("Connection destroyed");
    }

    public void download(String fileName, String path) throws IOException {
        dataOutputStream.writeUTF("DOWNLOAD");
        dataOutputStream.writeUTF(fileName);
        File file = new File(path + "\\" + fileName);
        if (!file.exists()) {
            int bytes = 0;
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            long size = dataInputStream.readLong();
            byte[] buffer = new byte[8*1024];
            while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
                fileOutputStream.write(buffer,0,bytes);
                size -= bytes;
            }
            fileOutputStream.close();
        } else {
            System.err.println("File name already exists");
        }
        socket.close();
        System.out.println("Connection destroyed");
    }
}