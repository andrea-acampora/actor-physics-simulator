package pcd03.model;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class ModelActor extends AbstractBehavior<Void> {

    private final SimulationState simulationState;

    public ModelActor(ActorContext<Void> context) {
        super(context);
        this.simulationState = new SimulationState(1000);

    }

    public static Behavior<Void> create() {
        return Behaviors.setup(ModelActor::new);
    }

    @Override
    public Receive<Void> createReceive() {
        return null;
    }
}
