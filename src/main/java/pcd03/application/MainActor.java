package pcd03.application;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class MainActor extends AbstractBehavior<Void> {

    private final ActorRef<Void> modelActor;
    private final ActorRef<Void> controllerActor;
    private final ActorRef<Void> viewActor;

    private MainActor(ActorContext<Void> context) {
        super(context);
        modelActor = context.spawn(ModelActor.create(), "modelActor");
        controllerActor = context.spawn(ControllerActor.create(), "controllerActor");
        viewActor = context.spawn(ViewActor.create(), "viewActor");
    }

    public static Behavior<Void> create() {
        return Behaviors.setup(MainActor::new);
    }

    @Override
    public Receive<Void> createReceive() {
        return null;
    }
}
