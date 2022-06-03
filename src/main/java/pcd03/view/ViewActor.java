package pcd03.view;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import pcd03.application.MsgProtocol;
import pcd03.controller.ControllerActor;
import pcd03.model.SimulationState;


public class ViewActor extends AbstractBehavior<MsgProtocol> {

    private final ActorRef<MsgProtocol> controllerActor;
    private final SimulationGUI gui;

    public ViewActor(ActorContext<MsgProtocol> context, ActorRef<MsgProtocol> controllerActor) {
        super(context);
        this.controllerActor = controllerActor;
        this.gui = new SimulationGUI(getContext().getSelf());
    }

    public static Behavior<MsgProtocol> create(ActorRef<MsgProtocol> controllerActor) {
        return Behaviors.setup(context -> new ViewActor(context, controllerActor));
    }

    @Override
    public Receive<MsgProtocol> createReceive() {
        return newReceiveBuilder()
                .onMessage(DisplayViewMsg.class, this::onDisplayViewMsg)
                .build();
    }

    private Behavior<MsgProtocol> onDisplayViewMsg(DisplayViewMsg msg ) {
        this.getContext().getLog().info("Created view");
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
            this.getContext().getLog().info("Updating view");
            this.gui.display(msg.state);
            return this;
        }

        private Behavior<MsgProtocol> onStopButtonPressedMsg(StopButtonPressedMsg msg ) {
            this.getContext().getLog().info("Stop button pressed");
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
                    .build();
        }

        private Behavior<MsgProtocol> onStartButtonPressedMsg(StartButtonPressedMsg msg ) {
            this.getContext().getLog().info("Start button pressed");
            this.controllerActor.tell(new ControllerActor.StartMsg(getContext().getSelf()));
            return Behaviors.setup(context -> new RunningBehaviour(context, controllerActor, gui));
        }
    }

}
