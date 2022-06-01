package pcd03.application;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import pcd03.controller.ControllerActor;
import pcd03.model.ModelActor;
import pcd03.view.ViewActor;

public class MainActor extends AbstractBehavior<Void> {

    private final ActorRef<Void> modelActor;
    private final ActorRef<Void> controllerActor;
    private final ActorRef<Void> viewActor;

    private MainActor(ActorContext<Void> context) {
        super(context);
        modelActor = context.spawn(ModelActor.create(), "modelActor");
        viewActor = context.spawn(ViewActor.create(), "viewActor");
        controllerActor = context.spawn(ControllerActor.create(modelActor, viewActor), "controllerActor");

    }

    public static Behavior<Void> create() {
        return Behaviors.setup(MainActor::new);
    }

    @Override
    public Receive<Void> createReceive() {
        return null;
    }
}
