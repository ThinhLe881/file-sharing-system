package org.thinh;

import java.io.*;
import java.net.*;

public class FileSharerThread extends Thread{
    Socket socket;
    private DataOutputStream dataOutputStream = null;
    private DataInputStream dataInputStream = null;

    File serverRoot;

    public FileSharerThread(Socket socket, File serverRoot) {
        super();
        this.socket = socket;
        this.serverRoot = new File(serverRoot.getPath());
        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.err.println("IOException while opening a read/write connection");
        }
    }

    public void run() {
        try {
            boolean endOfSession = false;
            while (!endOfSession) {
                endOfSession = processCommand();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
            System.out.println("Connection destroyed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean processCommand() throws IOException {
        String command;
        try {
            command = dataInputStream.readUTF();
        } catch (IOException e) {
            System.err.println("Error reading command from socket");
            return true;
        }

        if (command.equalsIgnoreCase("DIR")) {
            File[] content = serverRoot.listFiles();
            for (File file : content) {
                dataOutputStream.writeUTF(file.getName());
            }
            dataOutputStream.writeUTF("exit()");
            return true;
        }

        if (command.equalsIgnoreCase("UPLOAD")) {
            String fileName = dataInputStream.readUTF();
            synchronized (this) {
                File file = new File(serverRoot.getPath() + "\\" + fileName);
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
                    dataOutputStream.writeUTF("Upload successfully");
                    return true;
                } else {
                    dataOutputStream.writeUTF("File name already exists");
                }
            }
        }

        if (command.equalsIgnoreCase("DOWNLOAD")) {
            String fileName = dataInputStream.readUTF();
            File[] content = serverRoot.listFiles();
            for (File file : content) {
                if (file.getName().equals(fileName)) {
                    int bytes = 0;
                    FileInputStream fileInputStream = new FileInputStream(file);
                    dataOutputStream.writeLong(file.length());
                    byte[] buffer = new byte[8*1024];
                    while ((bytes = fileInputStream.read(buffer)) != -1){
                        dataOutputStream.write(buffer,0, bytes);
                        dataOutputStream.flush();
                    }
                    fileInputStream.close();
                    return true;
                }
            }
        }

        return true;
    }
}
