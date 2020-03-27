package com.example.osmz_cv1;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
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
    private byte[] mPreviewFrameBuffer;
    private HttpServerActivity activity;
    private ByteArrayOutputStream imageBuffer;

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
//                while(!(line = in.readLine()).isEmpty()){
//                    requestList.add(line);
//                    Log.d("SERVER", line);
//                }
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
            Log.d("SERVER-Filename", fileName);

            if (!(fileName.endsWith("/"))) {
                fileName = fileName + "/";
            }

            if (fileName.equals("/")) {
                fileName = "/index.html";
            }

            Log.d("TEST", fileName);
            if(fileName.equals("/snapchot/")) {
                fileName = "/snapchot.jpg/";
                File file = new File(path + fileName);
                FileInputStream fs = new FileInputStream(path + fileName);
                out.write("HTTP/1.1 200 OK\r\n");
                out.write("Content-Type: " + "text/html" + "\r\n");
                out.write("Content-Length: " + file.length() + "\r\n");
                out.write("\r\n");
                out.write("<html><head><title>Snapchot</title><meta http-equiv='refresh' content='5'></head><body><h1>Snapchot</h1><img style='transform: rotate(90deg);' src='http://127.0.0.1:12345/snapchot.jpg'></body></html>\r\n");
                out.flush();

                returnHandle("File: ", fileName, file.length());

            }
            /*else if(fileName.equals("/camera/snapshot/")) {
                mCamera.takePicture(null, null, webPicture);
                //File file = new File(path + fileName);
                out.write("HTTP/1.1 200 OK\r\n");
                out.write("Content-Type: " + "image/jpeg" + "\r\n");
                //out.write("Content-Length: " + file.length() + "\r\n");

                out.write("\r\n");
                out.flush();


                //returnHandle("File: ", fileName, file.length());

                /*o.write(this.mPreviewFrameBuffer);*/

           /* }*/
            else {
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
                }
                else if (file.exists() && file.isDirectory()) {
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
                        if(file2.length() > 999) {
                            fileSize = (file2.length()/1024);
                            sizeExtension = "KB";
                        }
                        else if (file2.length()/1024 > 999){
                            fileSize = (file2.length()/1024)/1024;
                            sizeExtension = "MB";
                        }
                        else {
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
            }

            s.close();
            Log.d("SERVER", "Socket Closed");

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
}