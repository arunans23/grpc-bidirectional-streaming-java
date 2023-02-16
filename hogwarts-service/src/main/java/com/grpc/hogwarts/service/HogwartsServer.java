
package com.grpc.hogwarts.service;

import com.grpc.hogwarts.service.Data;
import com.grpc.hogwarts.service.HogwartsServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

public class HogwartsServer {

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(8080).addService(new HogwartsServiceImpl()).build();
        server.start();

        System.out.println("Server started");
        server.awaitTermination();
    }

    private static class HogwartsServiceImpl extends HogwartsServiceGrpc.HogwartsServiceImplBase {
        @Override
        public StreamObserver<Data> connect(StreamObserver<Data> responseObserver) {
            return new StreamObserver<Data>() {
                @Override
                public void onNext(Data request) {

                }

                @Override
                public void onError(Throwable throwable) {
                    throwable.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }

            };
        }
    }
}

