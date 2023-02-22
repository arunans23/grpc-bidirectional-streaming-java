package com.grpc.hogwarts.service;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HogwartsServiceImpl extends HogwartsServiceGrpc.HogwartsServiceImplBase {
    private static final ConcurrentMap<StreamObserver<Data>, Boolean> clients = new ConcurrentHashMap<>();

    public static void broadcast(String message) {
        for (StreamObserver<Data> client : clients.keySet()) {
            Any response = Any.pack(StringValue.of(message));
            client.onNext(Data.newBuilder().setData(response).build());
        }
    }
    @Override
    public StreamObserver<Data> connect(StreamObserver<Data> responseObserver) {
        clients.put(responseObserver, true);
        return new StreamObserver<Data>() {
            @Override
            public void onNext(Data request) {
                Any data =request.getData();
                if (data.is(StringValue.class)) {
                    try {
                        String receivedString = data.unpack(StringValue.class).getValue();
                        System.out.println("Server: "+receivedString);
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                    Any response = Any.pack(StringValue.of("Connection Established"));
                    responseObserver.onNext(Data.newBuilder().setData(response).build());
                }

            }


            @Override
            public void onError(Throwable throwable) {
                clients.remove(responseObserver);
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                clients.remove(responseObserver);
                responseObserver.onCompleted();
            }

        };
    }
}
