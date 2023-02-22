package com.grpc.hogwarts.client;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.grpc.hogwarts.service.Data;
import io.grpc.*;
import com.grpc.hogwarts.service.HogwartsServiceGrpc;
import io.grpc.stub.StreamObserver;



public class HogwartsClient {

    private HogwartsServiceGrpc.HogwartsServiceStub stub;

    public HogwartsClient(Channel channel){
        stub = HogwartsServiceGrpc.newStub(channel);
    }
    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext().build();
        HogwartsClient client = new HogwartsClient(channel);

        client.connect();
    }


    public void connect() {
        StreamObserver<Data> responseObserver = new StreamObserver<Data>() {
            @Override
            public void onNext(Data data) {
                Any any = data.getData();
                if (any.is(StringValue.class)) {
                    try {
                        String receivedStr = any.unpack(StringValue.class).getValue();
                        System.out.println("Client: "+receivedStr);
                    } catch (InvalidProtocolBufferException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }


            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Client completed!");
            }

        };

        StreamObserver<Data> requestObserver = stub.connect(responseObserver);
        try {

            Data data = Data.newBuilder().setData(Any.pack(StringValue.of("63vds73gs6"))).build() ;
            requestObserver.onNext(data);
            requestObserver.onCompleted();
        }
        catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }
    }
}
