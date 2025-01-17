
package com.grpc.hogwarts.service;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class HogwartsServer  {
    private static final Map<String, StreamObserver<ServerData>> observers = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        // create a server and send data to client based on user input
        Server server = ServerBuilder.forPort(8080)
                .addService(new HogwartsServiceGrpc.HogwartsServiceImplBase() {
                    @Override
                    public StreamObserver<ClientData> connect(StreamObserver<ServerData> responseObserver) {
                        return new HogwartsServerObserver(responseObserver, observers);
                    }
                }).intercept(getAuthorizationInterceptor())
                .build();
        server.start();
        System.out.println("Server started on port: " + server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Received shutdown request");
            try {
                server.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                for (StreamObserver<ServerData> observer : observers.values()) {
                    observer.onCompleted();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Server has been shutdown");
        }));


        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter node ID");
            String nodeId = scanner.next();
            if (!observers.containsKey(nodeId)) {
                System.out.println("Node ID doesn't exist");
                continue;
            }
            System.out.println("Enter 1 to send electronic list, 2 to send vehicle list, 3 to exit");
            int choice = scanner.nextInt();
            StreamObserver<ServerData> serverObserver = observers.get(nodeId);
            if (choice == 1) {
                serverObserver.onNext(ServerData.newBuilder()
                        .setItem(ServerData.ITEM.ELECTRONIC)
                        .build());
            } else if (choice == 2) {
                serverObserver.onNext(ServerData.newBuilder()
                        .setItem(ServerData.ITEM.VEHICLE)
                        .build());
            } else if (choice == 3) {
                server.shutdown();
                break;
            }
        }
    }

    private static ServerInterceptor getAuthorizationInterceptor() {
        return new AuthorizationInterceptor();
    }
}

class HogwartsServerObserver implements StreamObserver<ClientData> {
    private final Map<String, StreamObserver<ServerData>> serverObservers;
    private final StreamObserver<ServerData> serverObserver;
    private String clientId;

    public HogwartsServerObserver(StreamObserver<ServerData> serverObserver,
                                  Map<String, StreamObserver<ServerData>> serverObservers) {
        this.serverObservers = serverObservers;
        this.serverObserver = serverObserver;
    }
    @Override
    public void onNext(ClientData clientData) {
        if (clientData.hasNode()) {
            clientId = clientData.getNode().getId();
            System.out.println("Received registration request from client : " + clientData.getNode().getId());
            serverObservers.put(clientId, serverObserver);
        } else if (clientData.hasHeartBeat()) {
            System.out.println("Received heartbeat from client : " + clientId);
        } else if (clientData.hasVehicleList()) {
            System.out.println("Received vehicle list from client : " + clientData.getVehicleList());
        } else if (clientData.hasElectronicList()) {
            System.out.println("Received electronic list from client : " + clientData.getElectronicList());
        } else {
            System.out.println("Received from client: " + clientData);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        //on error
        Status status = Status.fromThrowable(throwable);
        System.out.println("Error: " + status);
        System.out.println("Removing client " + clientId + " from server");
        serverObservers.remove(clientId);
    }

    @Override
    public void onCompleted() {
        //on completion
        System.out.println("Client " + clientId + "has completed sending us something");
        serverObservers.remove(clientId);
    }
}

class AuthorizationInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
                                                                 Metadata metadata,
                                                                 ServerCallHandler<ReqT, RespT> serverCallHandler) {

        // add authorization header to metadata before sending to client
        return serverCallHandler.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(serverCall) {
            @Override
            public void sendHeaders(Metadata responseHeaders) {
                responseHeaders.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "hogwarts123");
                super.sendHeaders(responseHeaders);
            }
        }, metadata);
    }
}


