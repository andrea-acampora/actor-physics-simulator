package pcd03.controller;

import akka.actor.Actor;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.event.slf4j.SLF4JLogging;
import pcd03.application.MsgProtocol;

public class ControllerActor extends AbstractBehavior<MsgProtocol> {

    private ActorRef<MsgProtocol> modelActor;
    private ActorRef<MsgProtocol> viewActor;
    private ActorRef<MsgProtocol> masterActor;


    public ControllerActor(ActorContext<MsgProtocol> context, ActorRef<MsgProtocol> modelActor) {
        super(context);
        this.modelActor = modelActor;
    }

    public static Behavior<MsgProtocol> create(ActorRef<MsgProtocol> modelActor) {
        return Behaviors.setup(context -> new ControllerActor(context, modelActor));
    }

    private Behavior<MsgProtocol> onStartMsg(StartMsg msg ){
        this.getContext().getLog().info("ControllerActor start");
        this.viewActor = msg.sender;
        this.masterActor = getContext().spawn(MasterActor.create(modelActor, viewActor), "masterActor");
        this.masterActor.tell(new MasterActor.StartSimulationMsg());
        return Behaviors.setup(context -> new RunningBehaviour(context, modelActor, viewActor, masterActor));
    }

    @Override
    public Receive<MsgProtocol> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartMsg.class, this::onStartMsg)
                .build();
    }

    public static class StartMsg implements MsgProtocol {
        public ActorRef<MsgProtocol> sender;
        public StartMsg(ActorRef<MsgProtocol> sender) {
            this.sender = sender;
        }
    }

    public static class StopMsg implements MsgProtocol {}

    class RunningBehaviour extends AbstractBehavior<MsgProtocol> {

        private final ActorRef<MsgProtocol> modelActor;
        private ActorRef<MsgProtocol> viewActor;
        private ActorRef<MsgProtocol> masterActor;


        public RunningBehaviour(ActorContext<MsgProtocol> context, ActorRef<MsgProtocol> modelActor, ActorRef<MsgProtocol> viewActor, ActorRef<MsgProtocol> masterActor) {
            super(context);
            this.modelActor = modelActor;
            this.viewActor = viewActor;
            this.masterActor = masterActor;
        }

        @Override
        public Receive<MsgProtocol> createReceive() {
            return newReceiveBuilder()
                    .onMessage(StartMsg.class, this::onStartMsg)
                    .onMessage(StopMsg.class, this::onStopMsg)
                    .build();
        }

        private Behavior<MsgProtocol> onStartMsg(StartMsg msg ){
            this.getContext().getLog().info("ControllerActor resumed");
            this.masterActor.tell(new MasterActor.ResumeSimulationMsg());
            return this;
        }

        private Behavior<MsgProtocol> onStopMsg(StopMsg msg ){
            this.getContext().getLog().info("Controller stopped");
            this.masterActor.tell(new MasterActor.StopSimulationMsg());
            return this;
        }
    }
}
