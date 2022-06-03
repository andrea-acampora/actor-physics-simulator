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

    @Override
    public Receive<MsgProtocol> createReceive() {
        return newReceiveBuilder()
                .onMessage(ComputeForcesMsg.class, this::onComputeForcesMsg)
                .onMessage(UpdatePositionMsg.class, this::onUpdatePositionMsg)
                .build();
    }

    private Behavior<MsgProtocol> onComputeForcesMsg(ComputeForcesMsg msg) {
        getContext().getLog().info("WorkerActor computing forces!");
        msg.sender.tell(new MasterActor.ComputeForcesTaskDoneMsg());
        return this;
    }


    private Behavior<MsgProtocol> onUpdatePositionMsg(UpdatePositionMsg msg) {
        getContext().getLog().info("WorkerActor updating positions!");
        msg.sender.tell(new MasterActor.UpdatePositionTaskDoneMsg());
        return this;
    }

    public static class ComputeForcesMsg implements MsgProtocol{
        public ActorRef<MsgProtocol> sender;
        public ComputeForcesMsg(ActorRef<MsgProtocol> sender) {
            this.sender = sender;
        }
    }
    public static class UpdatePositionMsg implements MsgProtocol{
        public ActorRef<MsgProtocol> sender;
        public UpdatePositionMsg(ActorRef<MsgProtocol> sender) {
            this.sender = sender;
        }
    }
}
