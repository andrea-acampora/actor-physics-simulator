package pcd03.controller;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import pcd03.application.MsgProtocol;

public class WorkerActor extends AbstractBehavior<MsgProtocol> {

    public WorkerActor(ActorContext<MsgProtocol> context) {
        super(context);
    }

    public static Behavior<MsgProtocol> create() {
        return Behaviors.setup(WorkerActor::new);
    }

//    private Behavior<MsgProtocol> onStartMsg(MsgProtocol msg ){
//        return this;
//    }

    @Override
    public Receive<MsgProtocol> createReceive() {
        return newReceiveBuilder()
                .onMessage(ComputeForcesMsg.class, this::onComputeForcesMsg)
                .onMessage(UpdatePositionMsg.class, this::onUpdatePositionMsg)
                .build();
    }

    private Behavior<MsgProtocol> onUpdatePositionMsg(M msg) {
        return this;
    }

    private Behavior<MsgProtocol> onComputeForcesMsg(ComputeForcesMsg msg) {
        return this;
    }


    public static class ComputeForcesMsg implements MsgProtocol{}
    public static class UpdatePositionMsg implements MsgProtocol{}
}
