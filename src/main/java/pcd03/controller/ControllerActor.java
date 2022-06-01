package pcd03.controller;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import pcd03.application.MsgProtocol;

public class ControllerActor extends AbstractBehavior<MsgProtocol> {

    private ActorRef<MsgProtocol> modelActor;
    private ActorRef<MsgProtocol> viewActor;

    public ControllerActor(ActorContext<MsgProtocol> context, ActorRef<MsgProtocol> modelActor, ActorRef<MsgProtocol> viewActor) {
        super(context);
        this.modelActor = modelActor;
        this.viewActor = viewActor;
    }

    public static Behavior<MsgProtocol> create(ActorRef<MsgProtocol> modelActor, ActorRef<MsgProtocol> viewActor) {
        return Behaviors.setup(context -> new ControllerActor(context, modelActor, viewActor));
    }

    private Behavior<MsgProtocol> onStartMsg(){
        this.getContext().getLog().info("start");
        return this;
    }

    @Override
    public Receive<MsgProtocol> createReceive() {
        return newReceiveBuilder()
                .onMessage(MsgProtocol.Start.class, this::onStartMsg)
                .build();
    }
}
