package com.example.osmz_cv1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;


import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;

public class SocketServer extends Thread {
	
	ServerSocket serverSocket;
	public final int port = 12345;
	boolean bRunning;
	private Handler handle;
	private Semaphore semaphoreAvailable;
	private int maxThreads = 1;
	private HttpServerActivity activity;


	public SocketServer(Handler h, int maxAvailable, HttpServerActivity a) {
		this.handle = h;
		this.maxThreads = maxAvailable;
		this.semaphoreAvailable = new Semaphore(this.maxThreads, true);
		this.activity = a;
	}
	
	public void close() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			Log.d("SERVER", "Error, probably interrupted in accept(), see log");
			e.printStackTrace();
		}
		bRunning = false;
	}
	
	public void run() {
        try {
        	Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            bRunning = true;
            while (bRunning) {
            	Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept(); 
                Log.d("SERVER", "Socket Accepted");
                try {
					// Vyžádání vlákna
					Log.d("SERVER", "Před vyžádáním, aktualně volné: " + this.semaphoreAvailable.availablePermits());
					this.semaphoreAvailable.acquire();
					Log.d("SERVER", "Po vyžádání, zbývající volné:" + this.semaphoreAvailable.availablePermits());

					ThreadSocketServer p = new ThreadSocketServer(s, this.handle, this.semaphoreAvailable, this.activity);
					p.start();

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
            }
        } 
        catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
            	Log.d("SERVER", "Normal exit");
            else {
            	Log.d("SERVER", "Error");
            	e.printStackTrace();
            }
        }
        finally {
        	serverSocket = null;
        	bRunning = false;
        }

    }

}

