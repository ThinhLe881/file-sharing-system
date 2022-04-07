package org.thinh;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientUI extends Application {

    AtomicBoolean shutdownRequested = new AtomicBoolean();

    private final Image iconFolder = new Image("/images/icon-folder.png");
    private final Image iconFile = new Image("/images/icon-file.png");

    private void display(TreeItem<File> root, File folder) {
        File[] content = folder.listFiles();
        TreeItem<File> item;
        for (File file : content) {
            if (file.isFile()) {
                item = new TreeItem<>(file);
            } else {
                item = new TreeItem<>(file);
                display(item, file);
            }
            root.getChildren().add(item);
        }
    }

    public void refreshLocal(TreeItem<File> root, File folder) {
        root.getChildren().clear();
        display(root, folder);
    }

    public void refreshServer(TreeItem<String> root, String dir) {
        root.getChildren().clear();
        if (!dir.isBlank()) {
            String[] folder = dir.split("\n");
            for (String file : folder) {
                TreeItem<String> item = new TreeItem<>(file, new ImageView(iconFile));
                root.getChildren().add(item);
            }
        }
    }

    @Override
    public void stop() {
        shutdownRequested.set(true);
    }

    @Override
    public void start(Stage primaryStage) {
        //Text hostname = new Text(getParameters().getRaw().get(0));
        //String path = getParameters().getRaw().get(1);
        String serverAddress = getParameters().getRaw().get(0);
        Label hostname = new Label();
        try {
            InetAddress address;
            address = InetAddress.getLocalHost();
            hostname.setText(address.getHostName());
            hostname.setGraphic(new ImageView(new Image("/images/icon-user.png")));
        } catch(UnknownHostException e) {
            System.err.println("Hostname can not be resolved");
        }
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("."));
        File rootFolder = directoryChooser.showDialog(primaryStage);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.TOP_LEFT);
        grid.setPadding(new Insets(5,5,5,5));
        grid.setVgap(5);
        grid.setHgap(5);

        Button downloadBtn = new Button("Download");
        downloadBtn.setGraphic(new ImageView(new Image("/images/icon-download.png")));
        Button uploadBtn = new Button("Upload");
        uploadBtn.setGraphic(new ImageView(new Image("/images/icon-upload.png")));
        Button refreshBtn = new Button("Refresh");
        refreshBtn.setGraphic(new ImageView(new Image("/images/icon-refresh.png")));
        HBox leftHBox = new HBox();
        leftHBox.setSpacing(5);
        leftHBox.getChildren().addAll(downloadBtn, uploadBtn, refreshBtn);
        HBox rightHBox = new HBox(hostname);
        rightHBox.setAlignment(Pos.CENTER_RIGHT);

        TreeView<File> clientView = new TreeView<>();
        clientView.setMinHeight(460);
        TreeItem<File> root = new TreeItem<>(rootFolder);
        root.setExpanded(true);
        refreshLocal(root, rootFolder);
        clientView.setRoot(root);
        clientView.setCellFactory(new Callback<>() {
            @Override
            public TreeCell<File> call(TreeView<File> tv) {
                return new TreeCell<>() {
                    @Override
                    protected void updateItem(File item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText("");
                            setGraphic(null);
                            return;
                        }
                        if (item.isDirectory()) {
                            setText(item.getName());
                            setGraphic(new ImageView(iconFolder));
                            return;
                        }
                        if (item.isFile()) {
                            setText(item.getName());
                            setGraphic(new ImageView(iconFile));
                        }
                    }
                };
            }
        });

        TreeView<String> serverView = new TreeView<>();
        serverView.setMinHeight(460);
        TreeItem<String> serverRoot = new TreeItem<>("Server", new ImageView("/images/icon-server.png"));
        serverRoot.setExpanded(true);
        try {
            FileSharerClient client = new FileSharerClient(serverAddress);
            refreshServer(serverRoot, client.dir());
        } catch (IOException e) {
            System.err.println("Can not update server folder");
            e.printStackTrace();
        }
        serverView.setRoot(serverRoot);

        grid.add(leftHBox,0,0,3,1);
        grid.add(rightHBox,2,0,2,1);
        grid.add(clientView,0,1,2,1);
        grid.add(serverView,2,1,2,1);

        primaryStage.getIcons().add(new Image("/images/icon.png"));
        primaryStage.setScene(new Scene(grid,500,500));
        primaryStage.setTitle("File Sharer");

        Thread update = new Thread(new Runnable() {
            @Override
            public void run() {
                Runnable updater = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (this) {
                            try {
                                FileSharerClient clientTemp = new FileSharerClient(serverAddress);
                                refreshLocal(root, rootFolder);
                                refreshServer(serverRoot, clientTemp.dir());
                            } catch (IOException e) {
                                System.err.println("Can not update server folder");
                                e.printStackTrace();
                            }
                        }
                    }
                };
                while (!shutdownRequested.get()) {
                    Platform.runLater(updater);
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {}
                }
            }
        });
        update.start();

        primaryStage.show();

        final File[] uploadFile = new File[1];
        clientView.getSelectionModel().selectedItemProperty()
                .addListener((ChangeListener<? super TreeItem<File>>) (observable, oldValue, newValue)
                        -> uploadFile[0] = newValue.getValue());

        final String[] downloadFile = new String[1];
        serverView.getSelectionModel().selectedItemProperty()
                .addListener((ChangeListener<? super TreeItem<String>>) (observable, oldValue, newValue)
                        -> downloadFile[0] = newValue.getValue());

        uploadBtn.setOnAction(event -> {
            // Pause the background thread to prevent JavaFX elements being changed simultaneously
            try {
                update.sleep(500);
            } catch (InterruptedException ex) {}
            try {
                FileSharerClient clientTemp;
                if (uploadFile[0].isDirectory()) {
                    System.err.println("Can not upload folder");
                } else {
                    clientTemp = new FileSharerClient(serverAddress);
                    clientTemp.upload(uploadFile[0]);
                }
                refreshLocal(root, rootFolder);
                clientTemp = new FileSharerClient(serverAddress);
                refreshServer(serverRoot, clientTemp.dir());
                update.interrupt();
            } catch (IOException e) {
                System.err.println("Error uploading file");
                e.printStackTrace();
            }
        });

        downloadBtn.setOnAction(event -> {
            // Pause the background thread to prevent JavaFX elements being changed simultaneously
            try {
                update.sleep(500);
            } catch (InterruptedException ex) {}
            try {
                FileSharerClient clientTemp = new FileSharerClient(serverAddress);
                clientTemp.download(downloadFile[0], rootFolder.getPath());
                refreshLocal(root, rootFolder);
                clientTemp = new FileSharerClient(serverAddress);
                refreshServer(serverRoot, clientTemp.dir());
                update.interrupt();
            } catch (IOException e) {
                System.err.println("Error downloading file");
                e.printStackTrace();
            }
        });

        refreshBtn.setOnAction(event -> {
            // Pause the background thread to prevent JavaFX elements being changed simultaneously
            try {
                update.sleep(500);
            } catch (InterruptedException ex) {}
            try {
                FileSharerClient clientTemp = new FileSharerClient(serverAddress);
                refreshLocal(root, rootFolder);
                refreshServer(serverRoot, clientTemp.dir());
                update.interrupt();
            } catch (IOException e) {
                System.err.println("Can not update server folder");
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) { launch(args); }
}
