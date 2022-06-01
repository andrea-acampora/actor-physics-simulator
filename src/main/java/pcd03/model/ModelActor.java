package pcd03.model;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import pcd03.application.MsgProtocol;

public class ModelActor extends AbstractBehavior<MsgProtocol> {

    private final SimulationState simulationState;

    public ModelActor(ActorContext<MsgProtocol> context) {
        super(context);
        this.simulationState = new SimulationState(1000);

    }

    public static Behavior<MsgProtocol> create() {
        return Behaviors.setup(ModelActor::new);
    }

    @Override
    public Receive<MsgProtocol> createReceive() {
        return null;
    }
}
