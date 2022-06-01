package pcd03.application;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import pcd03.controller.ControllerActor;
import pcd03.model.ModelActor;
import pcd03.view.ViewActor;

public class MainActor extends AbstractBehavior<MsgProtocol> {

    private final ActorRef<MsgProtocol> modelActor;
    private final ActorRef<MsgProtocol> controllerActor;
    private final ActorRef<MsgProtocol> viewActor;

    private MainActor(ActorContext<MsgProtocol> context) {
        super(context);
        modelActor = context.spawn(ModelActor.create(), "modelActor");
        viewActor = context.spawn(ViewActor.create(), "viewActor");
        controllerActor = context.spawn(ControllerActor.create(modelActor, viewActor), "controllerActor");
        controllerActor.tell(new ControllerActor.StartMsg());

    }

    public static Behavior<MsgProtocol> create() {
        return Behaviors.setup(MainActor::new);
    }

    @Override
    public Receive<MsgProtocol> createReceive() {
        return null;
    }
}
