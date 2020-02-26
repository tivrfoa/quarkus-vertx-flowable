package org.acme;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.ConsumeEvent;

/**
 * https://quarkus.io/guides/reactive-messaging
 */
@ApplicationScoped
public class GreetingService {

    @ConsumeEvent("greeting")
    public String consume(String name) {
        System.out.println("consume got: " + name);
        return name.toUpperCase();
    }

    @ConsumeEvent("greeting")
    public CompletionStage<String> consumeAsync(String name) {
        System.out.println("consumeAsync got: " + name);
        return CompletableFuture.supplyAsync(name::toUpperCase);
    }
}