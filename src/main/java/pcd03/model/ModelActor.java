package pcd03.model;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import pcd03.application.MsgProtocol;

import java.util.ArrayList;
import java.util.List;

public class ModelActor extends AbstractBehavior<MsgProtocol> {

    private final SimulationState simulationState;

    public ModelActor(ActorContext<MsgProtocol> context) {
        super(context);
        this.simulationState = new SimulationState(1000, 10000);
    }

    public static Behavior<MsgProtocol> create() {
        return Behaviors.setup(ModelActor::new);
    }

    @Override
    public Receive<MsgProtocol> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetSimulationStateMsg.class, this::onGetSimulationStateMsg)
                .onMessage(SimulationStepDoneMsg.class, this::onSimulationStepDoneMsg)
                .onMessage(BodiesSubListUpdatedMsg.class, this::onBodiesListUpdatedMsg)
                .build();
    }

    private Behavior<MsgProtocol> onBodiesListUpdatedMsg(BodiesSubListUpdatedMsg msg) {
        int k = 0;
        for(int i = msg.start; i < msg.end; i++) {
            simulationState.getBodies().set(i, msg.subList.get(k++));
        }
        return this;
    }

    private Behavior<MsgProtocol> onSimulationStepDoneMsg(SimulationStepDoneMsg msg) {
        this.simulationState.setVt(simulationState.getVt() + simulationState.getDt());
        this.simulationState.incrementSteps();
        msg.replyTo.tell(new SimulationStateValueMsg(this.simulationState));
        return this;
    }

    private Behavior<MsgProtocol> onGetSimulationStateMsg(GetSimulationStateMsg msg) {
        ArrayList<Body> defCopy = new ArrayList<>();
        this.simulationState.getBodies().forEach(b -> defCopy.add(new Body(b.getId(), new P2d(b.getPos().getX(), b.getPos().getY()), new V2d(b.getVel().x, b.getVel().y), b.getMass())));
        SimulationState defState = new SimulationState(defCopy,this.simulationState.getBounds(),this.simulationState.getVt(), this.simulationState.getDt(), this.simulationState.getCurrentStep(), this.simulationState.getStepToDo());
        msg.replyTo.tell(new SimulationStateValueMsg(defState));
        return this;
    }

    public static class GetSimulationStateMsg implements MsgProtocol{
        public final ActorRef<MsgProtocol> replyTo;
        public GetSimulationStateMsg(ActorRef<MsgProtocol> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static class SimulationStateValueMsg implements MsgProtocol {
        public final SimulationState state;
        public SimulationStateValueMsg(SimulationState state) {
            this.state = state;
        }
    }

    public static class SimulationStepDoneMsg implements MsgProtocol{
        public final ActorRef<MsgProtocol> replyTo;
        public SimulationStepDoneMsg(ActorRef<MsgProtocol> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static class BodiesSubListUpdatedMsg implements MsgProtocol{
        public final List<Body> subList;
        public final int start;
        public final int end;

        public BodiesSubListUpdatedMsg(List<Body> subList, int start, int end) {
            this.subList = subList;
            this.start = start;
            this.end = end;
        }
    }
}
