
package com.grpc.hogwarts.service;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class HogwartsServer {

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(8080).addService(new HogwartsServiceImpl()).build();
        server.start();
        System.out.println("Server started");
        Scanner scanner = new Scanner(System.in);
        TimeUnit.SECONDS.sleep(10);
        HogwartsServiceImpl.broadcast("Test");
        server.awaitTermination();

    }

}

