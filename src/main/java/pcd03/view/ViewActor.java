package pcd03.view;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class ViewActor extends AbstractBehavior<Void> {

    public ViewActor(ActorContext<Void> context) {
        super(context);
    }

    public static Behavior<Void> create() {
        return Behaviors.setup(ViewActor::new);
    }

    @Override
    public Receive<Void> createReceive() {
        return null;
    }
}
