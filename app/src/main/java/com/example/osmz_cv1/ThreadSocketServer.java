package com.example.osmz_cv1;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.MimeTypeMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ThreadSocketServer extends Thread {
    private Socket s;
    private Handler h;
    private Semaphore semaphore;
    private HttpServerActivity activity;
    private ByteArrayOutputStream imageBuffer;
    private static DataOutputStream stream;
    private boolean canClose = true;

    ThreadSocketServer(Socket s, Handler handle, Semaphore semaphoreAvailable, HttpServerActivity a) {
        this.s = s;
        this.h = handle;
        this.semaphore = semaphoreAvailable;
        this.activity = a;
        this.imageBuffer = new ByteArrayOutputStream();
    }

    public void run() {
        try {
            OutputStream o = s.getOutputStream();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            List<String> requestList = new ArrayList<>();
            String line;

            try {
                if ((line = in.readLine()) != null) {
                    while(!line.isEmpty()){
                        requestList.add(line);
                        Log.d("SERVER", line);
                        line = in.readLine();
                    }
                }
            }catch(IOException e){
                s.close();
            }

            String path = Environment.getExternalStorageDirectory().getPath() + "/OSMZ";
            String fileName = "";
            try {
                fileName = requestList.get(0).split(" ")[1];
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.d("SERVER-Path", path);

            if (!(fileName.endsWith("/"))) {
                fileName = fileName + "/";
            }

            if (fileName.equals("/")) {
                fileName = "/index.html";
            }


            Log.d("SERVER-Filename", fileName);
            switch (fileName) {
                case "/snapshot/": {
                    fileName = "/snapchot.jpg/";
                    File file = new File(path + fileName);
                    FileInputStream fs = new FileInputStream(path + fileName);
                    out.write("HTTP/1.1 200 OK\r\n");
                    out.write("Content-Type: " + "text/html" + "\r\n");
                    out.write("Content-Length: " + file.length() + "\r\n");
                    out.write("\r\n");
                    out.write("<html><head><title>Snapchot</title><meta http-equiv='refresh' content='5'></head><body><img style='transform: rotate(90deg);max-width: auto; height: 400px;' src='http://127.0.0.1:12345/snapchot.jpg'></body></html>\r\n");
                    out.flush();

                    returnHandle("Snapchot: ", fileName, file.length());

                    break;
                }
                case "/camera/snapshot/":
                    byte[] picture = activity.takePicture();
                    out.write("HTTP/1.0 200 OK\r\n");
                    out.write("Content-Length: " + picture.length + "\r\n");
                    out.write("Content-Type: image/jpeg\r\n");
                    out.write("\r\n");
                    out.flush();

                    o.write(activity.takePicture());
                    o.flush();

                    returnHandle("Camera snapchot: ", fileName, picture.length);
                    break;
                case "/camera/stream/":
                    stream = new DataOutputStream(s.getOutputStream());
                    try {
                        Log.d("onPreviewFrame", "stream");
                        stream.write(("HTTP/1.0 200 OK\r\n" +
                                "Content-Type: multipart/x-mixed-replace; boundary=\"OSMZ_boundary\"\r\n").getBytes());
                        stream.flush();
                        canClose = false;

                        h.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                streaming();
                            }
                        }, 500);

                        o.flush();
                    } catch (IOException e) {
                        Log.d("ERROR:", e.getLocalizedMessage());
                    }

                    break;
                default: {
                    File file = new File(path + fileName);
                    if (file.exists() && file.isFile()) {
                        FileInputStream fs = new FileInputStream(path + fileName);
                        Log.d("SERVER", "File Exists");
                        out.write("HTTP/1.1 200 OK\r\n");
                        out.write("Content-Type: " + getMimeType(fileName) + "\r\n");
                        out.write("Content-Length: " + file.length() + "\r\n");
                        out.write("\r\n");
                        out.flush();

                        returnHandle("File: ", fileName, file.length());

                        int len = 0;
                        byte[] buffer = new byte[2048];
                        while ((len = fs.read(buffer)) > 0) {
                            o.write(buffer, 0, len);
                        }
                    } else if (fileName.contains("/cgi-bin/")) {
                        fileName = fileName.substring(0, fileName.length() - 1);
                        String commands[] = fileName.split("/");

                        try
                        {
                            String urlArray[] = fileName.split("/cgi-bin/");
                            String commandWithArgs = urlArray[1];
                            commandWithArgs = commandWithArgs.replace("%20", " ");
                            String commandsArray[] = commandWithArgs.split(" ");
                            String contentHTML = "<html><head><title>cgi-bin: " + commandsArray[0] + "</title></head><body><h1>Command: " + commandsArray[0] + "</h1>";
                            ProcessBuilder p = new ProcessBuilder(commandsArray);
                            Process process = p.start();
                            String resultLine;
                            BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            while ((resultLine = stdOut.readLine()) != null) {
                                contentHTML += resultLine + "<br>";
                            }
                            BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                            while ((resultLine = stdErr.readLine()) != null) {
                                contentHTML += resultLine + "<br>";
                            }

                            contentHTML +="</body></html>";

                            String bodyHTML = "";
                            bodyHTML += "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/html\r\n" +
                                    "Content-Length: " + contentHTML.length() + "\r\n" +
                                    "\r\n" +
                                    contentHTML;

                            out.write(bodyHTML);
                            out.flush();

                            returnHandle("cgi-bin: ", fileName, Long.valueOf(bodyHTML.length()));
                        }
                        catch (Exception e)
                        {
                            Log.d("ProcessOutput", "just failed: " + e.getMessage());

                        }

                    } else if (file.exists() && file.isDirectory()) {
                        String[] pathnames;
                        pathnames = file.list();
                        out.write("HTTP/1.1 200 OK\r\n");
                        out.write("Content-Type: " + "text/html" + "\r\n");
                        out.write("Content-Length: " + file.length() + "\r\n");
                        out.write("\r\n");
                        out.write("<html><head><title>List File</title></head><body><h1>Index of: ~" + fileName + "</h1>");
                        out.write("<table><tbody>");
                        out.write("<tr><th style='width:150px; text-align: left;'>Name</th><th style='width:80px;'>Type</th><th style='width:100px;'>Last Modified</th><th style='width:80px; text-align: right;'>Size</th></tr>");
                        out.write("<tr><th colspan='4'><hr></th></tr>");
                        out.write("<tr><td><a href='" + fileName + "..'>Parent Folder</a></td></tr>");
                        for (String pathname : pathnames) {
                            File file2 = new File(path + fileName + pathname);
                            float fileSize;
                            String sizeExtension;
                            Date date = new Date(file2.lastModified());
                            SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yyyy");
                            String lastModify = df2.format(date);
                            if (file2.length() > 999) {
                                fileSize = (file2.length() / 1024);
                                sizeExtension = "KB";
                            } else if (file2.length() / 1024 > 999) {
                                fileSize = (file2.length() / 1024) / 1024;
                                sizeExtension = "MB";
                            } else {
                                fileSize = file2.length();
                                sizeExtension = "B";
                            }
                            String type = getMimeType(fileName + pathname);
                            out.write("<tr><td><a href=' " + fileName + pathname + "'>" + pathname + "</a></td><td>" + ((type == null) ? "folder" : type) + "</td><td style='text-align: right;'>" + lastModify + "</td><td style='text-align: right;'>" + fileSize + sizeExtension + "</td></tr>");
                        }
                        out.write("<tr><th colspan='4'><hr></th></tr>");
                        out.write("</tbody></table>");
                        out.write("</body></html");
                        out.flush();

                        returnHandle("Directory: ", fileName, 0);

                    } else {
                        Log.d("SERVER", "File not found");
                        out.write("HTTP/1.1 404 Not Found\r\n");
                        out.write("\r\n");
                        out.write("<html><head><title>404 Not Found</title></head><body><h1>File not found - 404</h1></body></html>\r\n");
                        out.flush();

                        returnHandle("404 Not Found: ", fileName, 0);

                    }
                    break;
                }
            }
            out.flush();
            if (canClose) {
                s.close();
                Log.d("SERVER", "Socket Closed");

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
            Log.d("SERVER", "Uvolnění, aktuální počet: " + semaphore.availablePermits());
        }
    }

    private void returnHandle(String request, String name, float size) {
        Message msg = h.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("REQUEST", request);
        bundle.putString("NAME", name);
        bundle.putFloat("SIZE", size);
        msg.setData(bundle);
        h.sendMessage(msg);
    }

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    private void streaming() {
        if (stream != null) {
            try {
                byte[] imagebuf = activity.takePicture();

                imageBuffer.reset();
                imageBuffer.write(imagebuf);
                imageBuffer.flush();
                stream.write(("\n--OSMZ_boundary\n" +
                        "Content-type: image/jpeg\n" +
                        "Content-Length: " + imageBuffer.size() + "\n\n").getBytes());
                stream.write(imageBuffer.toByteArray());
                stream.write(("\n").getBytes());
                stream.flush();

                returnHandle("Streaming: ", "Live", (long) imageBuffer.size());

            } catch (IOException e) {
                Log.d("ERROR:", "Streaming error: " + e.getLocalizedMessage());
            }
        }
    }
}