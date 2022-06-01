package pcd03.controller;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import pcd03.application.MainActor;

public class ControllerActor extends AbstractBehavior<Void> {

    private ActorRef<Void> modelActor;
    private ActorRef<Void> viewActor;

    public ControllerActor(ActorContext<Void> context, ActorRef<Void> modelActor, ActorRef<Void> viewActor) {
        super(context);
        this.modelActor = modelActor;
        this.viewActor = viewActor;
    }

    public static Behavior<Void> create(ActorRef<Void> modelActor, ActorRef<Void> viewActor) {
        return Behaviors.setup(context -> new ControllerActor(context, modelActor, viewActor));
    }

    @Override
    public Receive<Void> createReceive() {
        return null;
    }
}
