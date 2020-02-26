package org.acme;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.reactivestreams.Publisher;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.vertx.reactivex.core.Vertx;

@Path("/")
public class GreetingResource {

    Flowable<Beverage> queue = createFlowableBeverage();

    private Jsonb json = JsonbBuilder.create();

    @Path("hello")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String methodname() {
        return "hello";
    }

    @Path("queue")
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Publisher<String> getQueue() {
        System.out.println("I got called ?! ...");
        return Flowable.merge(
                queue.map(b -> {
                    System.out.println(b);
                    return json.toJson(b);
                }),
                // Trick OpenShift router, resetting idle connections
                Flowable.interval(10, TimeUnit.SECONDS).map(x -> "{}"));
    }


    public Flowable<Beverage> createFlowableBeverage() {
        Flowable<Beverage> source = Flowable.create(new FlowableOnSubscribe<Beverage>() {
            @Override
            public void subscribe(FlowableEmitter<Beverage> emitter) throws Exception {
       
                // signal an item
                emitter.onNext(new Beverage(1, "Hello"));
       
                // could be some blocking operation
                Thread.sleep(1000);
       
                // the consumer might have cancelled the flow
                if (emitter.isCancelled()) {
                    return;
                }
       
                emitter.onNext(new Beverage(2, "World"));
       
                Thread.sleep(1000);
       
                // the end-of-sequence has to be signaled, otherwise the
                // consumers may never finish
                emitter.onComplete();
            }
        }, BackpressureStrategy.BUFFER);

        source.subscribe();

        return source;
    }



    @Inject
    Vertx vertx;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{name}")
    public CompletionStage<String> greeting(@PathParam String name) {
        CompletableFuture<String> future = new CompletableFuture<>();

        long start = System.nanoTime();

        vertx.setTimer(10, l -> {
            long duration = TimeUnit.MILLISECONDS.convert(
                System.nanoTime() - start, TimeUnit.NANOSECONDS);

            String message = String.format("Hello %s! (%d ms)%n", name, duration);

            future.complete(message);
        });

        return future;
    }


    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path("vertx/streaming")
    public Publisher<String> greeting() {
        return vertx.periodicStream(2000).toFlowable()
            .map(l -> String.format("Your barista is %s. (%s)%n", pickName(), new Date()));
    }

    private static final String[] names = {
        "hello", "world", "Brasil", "China"
    };

    private static int baristaIdx = 0;

    public static String pickName() {
        if (baristaIdx == names.length)
            baristaIdx = 0;

        return names[baristaIdx++];
    }
}