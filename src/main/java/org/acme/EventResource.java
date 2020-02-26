package org.acme;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.vertx.axle.core.eventbus.EventBus;
import io.vertx.axle.core.eventbus.Message;

@Path("/async")
public class EventResource {

    @Inject
    EventBus bus;

    @GET
    @Path("/{name}")
    public CompletionStage<String> hello(@PathParam String name) {
        return bus.<String>request("greeting", name)
            .thenApply(Message::body);
    }
}