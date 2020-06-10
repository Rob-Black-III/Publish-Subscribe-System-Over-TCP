/**
 * Filename:        Server.java
 * Date:            9/22/2019
 * Author:          Rob Black
 * CS Account:      rdb5063@rit.edu
 */

package edu.rit.cs.pubsub;

import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * TCP Server implementation that can send and receive objects.
 * Fully compartmentalized, has nothing to do with WordCount.
 */
public class Server {
    private ServerSocket listenSocket;
    private ExecutorService executorService;
    private List<Handler> handlers;

    /**
     * Server constructor
     * @param port - Make a new server accepting connections on this port.
     */
    public Server(int port) {

        //Try to bind to port until successful (only halts on success)
        while (listenSocket == null) {
            try {
                this.listenSocket = new ServerSocket(port);
            } catch (BindException b) {
                //Wait for port to free up. Thus, never quits if existing connection never closes.
                continue;
            } catch (IOException e) {
                e.printStackTrace();
            }
            //System.out.println("[SERVER] Created");
        }

        handlers = new ArrayList<>();
    }

    public boolean isClientOnline(String ip){
        for(Handler h : this.handlers){
            if(h == null || h.getClientIP() == null){
                return false;
            }
            else if(h.getClientIP().equals(ip)){
                if(!(h.getClientSocket().isClosed())){
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    /**
     * The main functionality of the server.
     * Listens and dispatches each client to a thread
     *
     * Fully multithreaded using a Handler Callable class.
     * @return List<Object> received over TCP from Clients
     */
    public void listen(EventManager eventManager) {

        //Create an executor that spawns threads as needed
        this.executorService = Executors.newCachedThreadPool();

        //Place to hold all the eventual return Maps
        //List<Future> listOfClientData = new ArrayList<>();

        //Handle all clients
        while (true) {
            //Accept Client
            Socket clientSocket = null;
            try {
                clientSocket = listenSocket.accept();
                //System.out.println("[SERVER] Client Connected " + clientSocket.getInetAddress() + " over port " + clientSocket.getLocalPort());

                //System.out.println("[SERVER] Dispatching client to Handler Callable through Executor");
                Handler newHandler = new Handler(clientSocket,eventManager);
                this.handlers.add(newHandler);
                executorService.submit(newHandler);


            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("[Server] ERROR: Incoming Connection Problem");
                System.exit(1);
            }
        }

        //The Blocking part. Forces the threads to finish to get objects back
        //List<Object> returnList = getObjectsFromFutures(listOfClientData);

        //Stop the server
        //shutdownServer();

        //return returnList;
    }

    /**
     * Writes to clients given a list of ips
     * @param message
     * @param ips
     */
    public void writeToClientGivenIPs(Object message, List<String> ips){
        for(String ip : ips){
            writeToClientGivenIP(message, ip);
        }
    }

    /**
     * Writes to clients given an ip
     * @param message
     * @param ip
     */
    public void writeToClientGivenIP(Object message, String ip){
        for(Handler h : this.handlers){
            if(h.getClientIP().equals(ip)){
                if(!(h.getClientSocket().isClosed())){
                    System.out.println("[HANDLER] writing (" + (String) message + ") to " + ip);
                    h.writeToClient(message);
                }
            }
        }
    }

    /**
     * Writes to all connected clients
     * @param message
     */
    public void writeToAllClients(Object message){
        for(Handler h : this.handlers){
            if(!(h.getClientSocket().isClosed())){
                h.writeToClient(message);
            }
        }
    }

    /**
     * Converts Futures into their objects
     * "Merges" the threaded computations
     * @param myFutureList - list of futures that may or may not be complete
     * @return List<Object> list of objects of any type
     */
    private static List<Object> getObjectsFromFutures(List<Future> myFutureList) {
        //Create the list of objects to return;
        List<Object> listOfObjects = new ArrayList<>();

        //Loop until all threads finish computation
        while (!myFutureList.isEmpty()) {

            for (int i = 0; i < myFutureList.size(); i++) {

                //Get a thread result (Future)
                Future temp = myFutureList.get(i);

                //Check if it is done (has an answer)
                if (temp.isDone()) {
                    try {
                        listOfObjects.add((Object) temp.get());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    myFutureList.remove(i);
                    break;
                }
            }
        }
        return listOfObjects;
    }

    /**
     * Code ripped right from the java doc
     * on how to stop the executor service thread pool properly.
     * https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html
     *
     * Could just use pool.shutdownNow() because I verify completion
     * of the threads in implementation, but doesn't matter.
     * @param pool - Thread pool
     */
    public static void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
    /**
     * Shutdown the server and close the socket.
     * Close the executor service.
     * @return true if shutdown, false if not
     */
    public boolean shutdownServer() {
        shutdownAndAwaitTermination(executorService);
        try {
            System.out.println("[SERVER] Killing Server...");
            this.listenSocket.close();
            System.out.println("[SERVER] Killed");
            return true;
        } catch (IOException e) {
            System.out.println("[SERVER] Connection Close Failure:" + e.getMessage());
            return false;
        }
    }
}

/**
 * Threaded Class that implements Callable.
 * Handles all connections to the server. Self-contained
 */
class Handler implements Callable<Object> {
    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private EventManager currentEventManager;
    private String clientIP;

    /**
     * Handler constructor. Takes a socket in as params.
     * @param clientSocket Socket for the client connected to the server
     * @throws IOException
     */
    public Handler(Socket clientSocket, EventManager eventManager) throws IOException {
        this.clientSocket = clientSocket;
        this.clientIP = this.clientSocket.getInetAddress().toString().replace("/","");
        System.out.println("[HANDLER] ip = " + this.clientIP);

        this.currentEventManager = eventManager;

        //Assign Output Stream first - Blocking operations
        //https://stackoverflow.com/questions/8186135/java-sockets-program-stops-at-socket-getinputstream-w-o-error
        //System.out.println("[HANDLER] Assigning OUT Stream...");
        this.out = new ObjectOutputStream(clientSocket.getOutputStream());
        //System.out.println("[HANDLER] Assigning IN Stream...");
        this.in = new ObjectInputStream(clientSocket.getInputStream());
        //System.out.println("[HANDLER] Handler created for " + clientSocket.getRemoteSocketAddress().toString() + " on port " + clientSocket.getLocalPort());
    }

    public String getClientIP(){
        return this.clientIP;
    }

    public Socket getClientSocket(){
        return this.clientSocket;
    }

    public Object readFromClient(){
        try {
            return this.in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void writeToClient(Object o){
        try {
            this.out.writeObject(o);
            this.out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Override call. Reads and writes objects via EventmManager
     * @return Object returned from client
     * @throws Exception
     */
    @Override
    public Object call() throws Exception {

        //Do Everything for the Implementation

        //Read the Data From the Client
        while(true){

            //Read input command
            Object myObject = readFromClient();

            //Pass input command to the event manager and get response
            //Space separated.
            //Message is of format: <IP> <Message>
            String response = this.currentEventManager.passInput(this.clientSocket.getInetAddress().toString().replace("/","") + " " + (String)myObject);

            //Write response to client
            writeToClient(response);

            //If response is kill, kill
            if(response.equals("exit good")){
                kill();
                return "exit good";
            }
        }
    }

    /**
     * Closes the server/client connection socket
     * @return true for kill success, false for fail
     */
    public boolean kill() {
        try {
            this.clientSocket.close();
            return true;
        } catch (IOException e) {
            System.out.println("Connection Close Failure:" + e.getMessage());
            return false;
        }
    }
}