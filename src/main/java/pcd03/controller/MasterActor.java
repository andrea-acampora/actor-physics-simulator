package pcd03.controller;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import pcd03.application.MsgProtocol;
import pcd03.model.ModelActor;
import pcd03.model.SimulationState;
import pcd03.utils.Chrono;
import pcd03.view.ViewActor;

import java.util.LinkedList;
import java.util.List;

public class MasterActor extends AbstractBehavior<MsgProtocol> {

    private final ActorRef<MsgProtocol> modelActor;
    private final ActorRef<MsgProtocol> viewActor;
    private final int nWorkers;
    private final List<ActorRef<MsgProtocol>> workerActors;
    Chrono time = new Chrono();
    private long nComputeForcesTaskDone;
    private long nUpdatePositionTaskDone;
    private int bodiesListSize;
    private int offset;
    private boolean stopFlag;

    public MasterActor(ActorContext<MsgProtocol> context, ActorRef<MsgProtocol> modelActor, ActorRef<MsgProtocol> viewActor) {
        super(context);
        this.modelActor = modelActor;
        this.viewActor = viewActor;
        this.nWorkers = Runtime.getRuntime().availableProcessors() + 1;
        this.workerActors = new LinkedList<>();
    }

    public static Behavior<MsgProtocol> create(ActorRef<MsgProtocol> modelActor, ActorRef<MsgProtocol> viewActor) {
        return Behaviors.setup(context -> new MasterActor(context, modelActor, viewActor));
    }

    @Override
    public Receive<MsgProtocol> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartSimulationMsg.class, this::onStartSimulationMsg)
                .onMessage(ModelActor.SimulationStateValueMsg.class, this::onSimulationStateValueMsg)
                .build();
    }

    private Behavior<MsgProtocol> onStartSimulationMsg(StartSimulationMsg msg) {
        this.time.start();
        for(int i = 0; i < this.nWorkers; i++){
            this.workerActors.add(getContext().spawn(WorkerActor.create(modelActor), "workerActor" + i));
        }
        this.modelActor.tell(new ModelActor.GetSimulationStateMsg(this.getContext().getSelf()));
        return this;
    }

    private Behavior<MsgProtocol> onSimulationStateValueMsg(ModelActor.SimulationStateValueMsg msg) {
            this.bodiesListSize = msg.state.getBodies().size();
            this.offset = bodiesListSize / nWorkers;
            getContext().getSelf().tell(new DoSimulationStepMsg(msg.state));
            return Behaviors.setup(RunningBehaviour::new);
    }

    public static class StartSimulationMsg implements MsgProtocol{}
    public static class StopSimulationMsg implements MsgProtocol{}
    public static class ResumeSimulationMsg implements MsgProtocol{}
    public static class DoSimulationStepMsg implements MsgProtocol{
        SimulationState simulationState;
        public DoSimulationStepMsg(SimulationState simulationState) {
            this.simulationState = simulationState;
        }
    }
    public static class SimulationStepDoneMsg implements MsgProtocol{}
    public static class DoComputeForcesTaskMsg implements MsgProtocol {
        SimulationState simulationState;
        public DoComputeForcesTaskMsg(SimulationState simulationState) {
            this.simulationState = simulationState;
        }
    }

    public static class DoUpdatePositionTaskMsg implements MsgProtocol {
        SimulationState simulationState;
        public DoUpdatePositionTaskMsg(SimulationState simulationState) {
            this.simulationState = simulationState;
        }
    }

    public static class ComputeForcesTaskDoneMsg implements MsgProtocol{}
    public static class UpdatePositionTaskDoneMsg implements MsgProtocol{}

    class RunningBehaviour extends AbstractBehavior<MsgProtocol> {

        private RunningBehaviour(ActorContext<MsgProtocol> context) {
            super(context);
        }

        @Override
        public Receive<MsgProtocol> createReceive() {
            return newReceiveBuilder()
                    .onMessage(DoSimulationStepMsg.class, this::onDoSimulationStepMsg)
                    .onMessage(DoComputeForcesTaskMsg.class, this::onDoComputeForcesTaskMsg)
                    .onMessage(StopSimulationMsg.class, this::onStopSimulationMsg)
                    .onMessage(SimulationStepDoneMsg.class, this::onSimulationStepDoneMsg)
                    .onMessage(ModelActor.SimulationStateValueMsg.class, this::onSimulationStateValueMsg)
                    .onMessage(ComputeForcesTaskDoneMsg.class, this::onComputeForcesTaskDoneMsg)
                    .onMessage(DoUpdatePositionTaskMsg.class, this::onDoUpdatePositionTaskMsg)
                    .onMessage(UpdatePositionTaskDoneMsg.class, this::onUpdatePositionTaskDoneMsg)
                    .onMessage(ResumeSimulationMsg.class, this::onResumeSimulationMsg)
                    .build();
        }


        private Behavior<MsgProtocol> onDoSimulationStepMsg(DoSimulationStepMsg msg) {
            if (msg.simulationState.getCurrentStep() < msg.simulationState.getStepToDo()) {
                if (!stopFlag) {
                    getContext().getSelf().tell(new DoComputeForcesTaskMsg(msg.simulationState));
                }
            } else {
                time.stop();
                getContext().getLog().info("Time elapsed: " + time.getTime() + " ms.");
            }
            return this;
        }

        private Behavior<MsgProtocol> onDoComputeForcesTaskMsg(DoComputeForcesTaskMsg msg) {
            int start = 0;
            for (int i = 0; i < nWorkers - 1; i++) {
                workerActors.get(i).tell(new WorkerActor.ComputeForcesMsg(this.getContext().getSelf(), msg.simulationState, start, start + offset));
                start = start + offset;
            }
            workerActors.get(workerActors.size() - 1).tell(new WorkerActor.ComputeForcesMsg(this.getContext().getSelf(), msg.simulationState, start, bodiesListSize));
            return this;
        }

        private Behavior<MsgProtocol> onComputeForcesTaskDoneMsg(ComputeForcesTaskDoneMsg msg) {
            nComputeForcesTaskDone++;
            if (nComputeForcesTaskDone == workerActors.size()) {
                nComputeForcesTaskDone = 0;
                modelActor.tell(new ModelActor.GetSimulationStateMsg(this.getContext().getSelf()));
            }
            return this;
        }

        private Behavior<MsgProtocol> onDoUpdatePositionTaskMsg(DoUpdatePositionTaskMsg msg) {
            int start = 0;
            for (int i = 0; i < nWorkers - 1; i++) {
                workerActors.get(i).tell(new WorkerActor.UpdatePositionMsg(this.getContext().getSelf(), msg.simulationState, start, start + offset));
                start = start + offset;
            }
            workerActors.get(workerActors.size() - 1).tell(new WorkerActor.UpdatePositionMsg(this.getContext().getSelf(), msg.simulationState, start, bodiesListSize));
            return this;
        }

        private Behavior<MsgProtocol> onUpdatePositionTaskDoneMsg(UpdatePositionTaskDoneMsg msg) {
            nUpdatePositionTaskDone++;
            if (nUpdatePositionTaskDone == workerActors.size()) {
                nUpdatePositionTaskDone = 0;
                this.getContext().getSelf().tell(new SimulationStepDoneMsg());
            }
            return this;
        }

        private Behavior<MsgProtocol> onSimulationStepDoneMsg(SimulationStepDoneMsg msg) {
            modelActor.tell(new ModelActor.SimulationStepDoneMsg(getContext().getSelf()));
            return Behaviors.setup(UpdatingViewBehaviour::new);
        }

        private Behavior<MsgProtocol> onStopSimulationMsg(StopSimulationMsg msg) {
            stopFlag = true;
            return this;
        }

        private Behavior<MsgProtocol> onSimulationStateValueMsg(ModelActor.SimulationStateValueMsg msg) {
            this.getContext().getSelf().tell(new DoUpdatePositionTaskMsg(msg.state));
            return this;
        }

        private Behavior<MsgProtocol> onResumeSimulationMsg(ResumeSimulationMsg msg) {
            stopFlag = false;
            modelActor.tell(new ModelActor.GetSimulationStateMsg(this.getContext().getSelf()));
            return this;
        }
    }

    class UpdatingViewBehaviour extends AbstractBehavior<MsgProtocol> {

        private UpdatingViewBehaviour(ActorContext<MsgProtocol> context) {
            super(context);
        }

        @Override
        public Receive<MsgProtocol> createReceive() {
            return newReceiveBuilder()
                    .onMessage(ModelActor.SimulationStateValueMsg.class, this::onSimulationStateValueMsg)
                    .onMessage(StopSimulationMsg.class, this::onStopSimulationMsg)
                    .build();
        }

        private Behavior<MsgProtocol> onSimulationStateValueMsg(ModelActor.SimulationStateValueMsg msg) {
                //viewActor.tell(new ViewActor.UpdateViewMsg(msg.state));
                this.getContext().getSelf().tell(new DoSimulationStepMsg(msg.state));
                return Behaviors.setup(RunningBehaviour::new);
        }

        private Behavior<MsgProtocol> onStopSimulationMsg(StopSimulationMsg msg) {
            stopFlag = true;
            return this;
        }
    }
}
