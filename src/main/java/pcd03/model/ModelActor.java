package pcd03.model;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import pcd03.application.MsgProtocol;

import java.util.Collections;
import java.util.List;

public class ModelActor extends AbstractBehavior<MsgProtocol> {

    private final SimulationState simulationState;

    public ModelActor(ActorContext<MsgProtocol> context) {
        super(context);
        this.simulationState = new SimulationState(1000, 1000);
    }

    public static Behavior<MsgProtocol> create() {
        return Behaviors.setup(ModelActor::new);
    }

    @Override
    public Receive<MsgProtocol> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetStepToDoMsg.class, this::onGetStepToDo)
                .onMessage(GetBodiesMsg.class, this::onGetBodies)
                .build();
    }

    private Behavior<MsgProtocol> onGetBodies(GetBodiesMsg msg) {
        msg.replyTo.tell(new BodiesValueMsg(this.simulationState.getBodies()));
        return this;
    }

    private Behavior<MsgProtocol> onGetStepToDo(GetStepToDoMsg msg) {
        msg.replyTo.tell(new StepToDoValueMsg(this.simulationState.getStepToDo()));
        return this;
    }

    public static class GetStepToDoMsg implements MsgProtocol{
            public final ActorRef<MsgProtocol> replyTo;
            public GetStepToDoMsg(ActorRef<MsgProtocol> replyTo) {
                this.replyTo = replyTo;
            }
    }
    public static class GetBodiesMsg implements MsgProtocol{
            public final ActorRef<MsgProtocol> replyTo;
            public GetBodiesMsg(ActorRef<MsgProtocol> replyTo) {
                this.replyTo = replyTo;
            }
    }

    public static class StepToDoValueMsg implements MsgProtocol{
        public final long value;
        public StepToDoValueMsg(long value) {
            this.value = value;
        }
    }
    public static class BodiesValueMsg implements MsgProtocol{
        public final List<Body> bodyList;
        public BodiesValueMsg(List<Body> bodyList) {
            this.bodyList = Collections.unmodifiableList(bodyList);
            //se si vuole passre uno snapshot occorre ricreare tutta la lista. Da capire se serve
        }
    }


}
