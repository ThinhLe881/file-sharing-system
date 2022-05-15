# File Sharing System - Thinh Le

## Project Information:

This project is a file sharing system that can download and upload files from multiple clients.
The file sharing clients will connect to a central server, which will respond to a single client command, and then disconnect. These are the following commands:

-   DIR:
    -   Returns a listing of the contents of the shared directory in the server's machine
    -   The server will disconnect immediately after sending the list of files to the client.
-   UPLOAD filename:
    -   Send the chosen file from the client to the server's shared directory
    -   The server will connect and transfer the bytes, and save it as a new file "filename" in the shared directory
    -   The server will disconnect immediately after saving the file
-   DOWNLOAD filename:
    -   The server will transfer the chosen file "filename" to the client, and then immediately disconnect

**Server:**

-   The server doesn't have any UI, but it is multithreaded, each incoming client connection is handled with a separate thread. This thread and its corresponding socket, will remain open only until the command has been handled.
-   The server can handle same filename file, able to transfer various types of file (tested: .txt, .pdf, .docx, .mp3, .mp4,...), able to navigate to subdirectories in local folder.

**Client:**

-   The client will have a user interface. When the client is started, the client will be asked for the local folder. The client will then show a split screen showing two directories (local and shared). On the left will be the list of all files in the local folder of the local client. On the right will be the list of files in the shared folder of the server.
-   The user interface: icon for the application and for each file and folders in the directory, local host name, "Refresh" button for user to refresh the state of both directories at will. Otherwise, the UI will automatically refresh after "Upload" or "Download", or every 10 seconds.

![ui](ui.PNG)

## How To Run:

1. Install Java, requires Java JDK version 8 or higher, [instruction](https://www.oracle.com/java/technologies/javase-downloads.html)
2. Install Gradle, recommend v6.8.3, [instruction](https://gradle.org/install/)
3. Clone this repository into your local machine, [instruction](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/cloning-a-repository)
4. Go into this repository in your local machine, run the program with following command in command line:
    - Run server:
      When the server is running, it will display the server IP address, and the server local directory (make sure the directory's name has **no space**)
    - Run client:
      Then it will display the directory chooser for the user to choose their local directory (make sure the directory's name has **no space**)

**Run server**

```
gradle start --args="<your server directory's path>"
```

**Run client**

```
gradle run --args="<the server's IP address>"
```

## Other Resources:
