package com.grpc.hogwarts.client;

import com.grpc.hogwarts.service.*;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Timer;
import java.util.TimerTask;

public class HogwartsClient {

    private final HogwartsServiceGrpc.HogwartsServiceStub stub;
    private StreamObserver<ClientData> requestObserver;
    private final String nodeId = getRandomNodeId();

    private final ElectronicList electronicList = populateElectronicList();
    private final VehicleList vehicleList = populateVehicleList();

    public HogwartsClient(Channel channel){
        stub = HogwartsServiceGrpc.newStub(channel);
    }
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext().build();
        HogwartsClient client = new HogwartsClient(channel);

        client.connect();

        // send regular heartbeat to server
        TimerTask heartBeatService = new HeartBeatService(client.requestObserver);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(heartBeatService, 0, 10000);
    }


    public void connect() {
        //connect to server and send data based on the response received from the server
        requestObserver = stub.connect(new StreamObserver<>() {
            @Override
            public void onNext(ServerData serverData) {
                if (ServerData.ITEM.ELECTRONIC.equals(serverData.getItem())) {
                    sendElectronicList();
                } else if (ServerData.ITEM.VEHICLE.equals(serverData.getItem())) {
                    sendVehicleList();
                } else {
                    System.out.println("Received from server: " + serverData);
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
                System.out.println("Server has completed sending us something");
            }
        });

        //sending Node Id
        requestObserver.onNext(ClientData.newBuilder()
                .setNode(Node.newBuilder()
                        .setId(nodeId)
                        .build())
                .build());
    }

    private String getRandomNodeId() {
        //get random number between 1 and 100
        return String.valueOf((int) (Math.random() * 100));
    }

    private void sendElectronicList() {
        requestObserver.onNext(ClientData.newBuilder()
                .setElectronicList(electronicList)
                .build());
    }

    private void sendVehicleList() {
        requestObserver.onNext(ClientData.newBuilder()
                .setVehicleList(vehicleList)
                .build());
    }

    private ElectronicList populateElectronicList() {
        Electronic electronic1 = Electronic.newBuilder()
                .setName("Iphone 12")
                .setType("PHONE")
                .build();

        Electronic electronic2 = Electronic.newBuilder()
                .setName("Iphone 11")
                .setType("PHONE")
                .build();

        Electronic electronic3 = Electronic.newBuilder()
                .setName("Iphone 10")
                .setType("PHONE")
                .build();

        Electronic electronic4 = Electronic.newBuilder()
                .setName("Galaxy S10")
                .setType("PHONE")
                .build();

        Electronic electronic5 = Electronic.newBuilder()
                .setName("Galaxy S9")
                .setType("PHONE")
                .build();

        return ElectronicList.newBuilder()
                .addElectronics(electronic1)
                .addElectronics(electronic2)
                .addElectronics(electronic3)
                .addElectronics(electronic4)
                .addElectronics(electronic5)
                .build();
    }

    private VehicleList populateVehicleList() {
        Vehicle vehicle1 = Vehicle.newBuilder()
                .setName("Audi")
                .setType("CAR")
                .build();

        Vehicle vehicle2 = Vehicle.newBuilder()
                .setName("BMW")
                .setType("CAR")
                .build();

        Vehicle vehicle3 = Vehicle.newBuilder()
                .setName("Mercedes")
                .setType("CAR")
                .build();

        Vehicle vehicle4 = Vehicle.newBuilder()
                .setName("Honda")
                .setType("CAR")
                .build();

        Vehicle vehicle5 = Vehicle.newBuilder()
                .setName("Toyota")
                .setType("CAR")
                .build();

        return VehicleList.newBuilder()
                .addVehicles(vehicle1)
                .addVehicles(vehicle2)
                .addVehicles(vehicle3)
                .addVehicles(vehicle4)
                .addVehicles(vehicle5)
                .build();
    }
}

class HeartBeatService extends TimerTask {
    private final StreamObserver<ClientData> requestObserver;
    HeartBeatService(StreamObserver<ClientData> requestObserver) {
        this.requestObserver = requestObserver;
    }
    public void run() {
        requestObserver.onNext(ClientData.newBuilder()
                .setHeartBeat(HeartBeat.newBuilder().build())
                .build());
    }
}
