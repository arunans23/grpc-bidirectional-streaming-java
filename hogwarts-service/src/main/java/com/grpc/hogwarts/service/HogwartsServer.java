
package com.grpc.hogwarts.service;

import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class HogwartsServer  {
    private static List<StreamObserver<ServerData>> observers = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        // create a server and send data to client based on user input
        Server server = ServerBuilder.forPort(8080)
                .addService(new HogwartsServiceGrpc.HogwartsServiceImplBase() {
                    @Override
                    public StreamObserver<ClientData> connect(StreamObserver<ServerData> responseObserver) {
                        observers.add(responseObserver);
                        return new HogwartsServerObserver(responseObserver);
                    }
                })
                .build();
        server.start();
        System.out.println("Server started on port: " + server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Received shutdown request");
            try {
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Server has been shutdown");
        }));


        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter node ID");
            int nodeId = scanner.nextInt();
            if (observers.size() < nodeId) {
                System.out.println("Node ID doesn't exist");
                continue;
            }
            System.out.println("Enter 1 to send electronic list, 2 to send vehicle list, 3 to exit");
            int choice = scanner.nextInt();
            StreamObserver<ServerData> serverObserver = observers.get(nodeId - 1);
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
}

class HogwartsServerObserver implements StreamObserver<ClientData> {
    private StreamObserver<ServerData> serverObserver;
    private String clientId;

    public HogwartsServerObserver(StreamObserver<ServerData> serverObserver) {
        this.serverObserver = serverObserver;
    }
    @Override
    public void onNext(ClientData clientData) {
        if (clientData.hasNode()) {
            clientId = clientData.getNode().getId();
            System.out.println("Received registration request from client : " + clientData.getNode().getId());
        } else if (clientData.hasHeartBeat()) {
            System.out.println("Received heartbeat from client : " + clientId);
        } else if (clientData.hasVehicleList()) {
            System.out.println("Received vehicle list from client : " + clientData.getVehicleList().toString());
        } else if (clientData.hasElectronicList()) {
            System.out.println("Received electronic list from client : " + clientData.getElectronicList().toString());
        } else {
            System.out.println("Received from client: " + clientData);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        //on error
        Status status = Status.fromThrowable(throwable);
        System.out.println("Error: " + status);
    }

    @Override
    public void onCompleted() {
        //on completion
        System.out.println("Client has completed sending us something");
    }
}


