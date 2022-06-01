package pcd03.view;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import pcd03.application.MsgProtocol;

public class ViewActor extends AbstractBehavior<MsgProtocol> {

    public ViewActor(ActorContext<MsgProtocol> context) {
        super(context);
    }

    public static Behavior<MsgProtocol> create() {
        return Behaviors.setup(ViewActor::new);
    }

    @Override
    public Receive<MsgProtocol> createReceive() {
        return null;
    }
}
