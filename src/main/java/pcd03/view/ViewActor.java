package pcd03.view;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import pcd03.application.MsgProtocol;
import pcd03.controller.ControllerActor;
import pcd03.model.SimulationState;


public class ViewActor extends AbstractBehavior<MsgProtocol> {

    private final ActorRef<MsgProtocol> controllerActor;
    private final SimulationGUI gui;
    private final StashBuffer<MsgProtocol> stashBuffer;



    public ViewActor(ActorContext<MsgProtocol> context, ActorRef<MsgProtocol> controllerActor, StashBuffer<MsgProtocol> stash) {
        super(context);
        this.controllerActor = controllerActor;
        this.gui = new SimulationGUI(getContext().getSelf());
        this.stashBuffer = stash;
    }

    public static Behavior<MsgProtocol> create(ActorRef<MsgProtocol> controllerActor) {
        return Behaviors.withStash(100,
                stash -> Behaviors.setup(context -> new ViewActor(context, controllerActor, stash)));
    }

    @Override
    public Receive<MsgProtocol> createReceive() {
        return newReceiveBuilder()
                .onMessage(DisplayViewMsg.class, this::onDisplayViewMsg)
                .build();
    }

    private Behavior<MsgProtocol> onDisplayViewMsg(DisplayViewMsg msg ) {
        this.gui.start();
        return Behaviors.setup(context -> new WaitingBehaviour(context, this.controllerActor, this.gui));
    }


    public static class UpdateViewMsg implements MsgProtocol {
        public final SimulationState state;
        public UpdateViewMsg(SimulationState state) {
            this.state = state;
        }
    }

    public static class DisplayViewMsg implements MsgProtocol {  }
    public static class StartButtonPressedMsg implements MsgProtocol { }
    public static class StopButtonPressedMsg implements MsgProtocol {  }

    class RunningBehaviour extends AbstractBehavior<MsgProtocol> {

        private final ActorRef<MsgProtocol> controllerActor;
        private final SimulationGUI gui;

        private RunningBehaviour(ActorContext<MsgProtocol> context, ActorRef<MsgProtocol> controllerActor, SimulationGUI gui) {
            super(context);
            this.controllerActor = controllerActor;
            this.gui = gui;
        }

        @Override
        public Receive<MsgProtocol> createReceive() {
            return newReceiveBuilder()
                    .onMessage(UpdateViewMsg.class, this::onUpdateViewMsg)
                    .onMessage(StopButtonPressedMsg.class, this::onStopButtonPressedMsg)
                    .build();
        }

        private Behavior<MsgProtocol> onUpdateViewMsg(UpdateViewMsg msg ) {
            this.gui.display(msg.state);
            return this;
        }

        private Behavior<MsgProtocol> onStopButtonPressedMsg(StopButtonPressedMsg msg ) {
            this.controllerActor.tell(new ControllerActor.StopMsg());
            return Behaviors.setup(context -> new WaitingBehaviour(context, this.controllerActor, this.gui));
        }
    }

    class WaitingBehaviour extends AbstractBehavior<MsgProtocol> {

        private final ActorRef<MsgProtocol> controllerActor;
        private final SimulationGUI gui;

        private WaitingBehaviour(ActorContext<MsgProtocol> context, ActorRef<MsgProtocol> controllerActor, SimulationGUI gui) {
            super(context);
            this.controllerActor = controllerActor;
            this.gui = gui;
        }

        @Override
        public Receive<MsgProtocol> createReceive() {
            return newReceiveBuilder()
                    .onMessage(StartButtonPressedMsg.class, this::onStartButtonPressedMsg)
                    .onMessage(MsgProtocol.class, this::onAnyMessage)
                    .build();
        }

        private Behavior<MsgProtocol> onAnyMessage(MsgProtocol msg) {
            stashBuffer.stash(msg);
            return this;
        }

        private Behavior<MsgProtocol> onStartButtonPressedMsg(StartButtonPressedMsg msg ) {
            this.controllerActor.tell(new ControllerActor.StartMsg(getContext().getSelf()));
            return stashBuffer.unstashAll(
                    Behaviors.setup(context -> new RunningBehaviour(context, controllerActor, gui)));
        }
    }

}
