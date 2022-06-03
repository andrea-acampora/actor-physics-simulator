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
    private ActorRef<MsgProtocol> modelActor;
    private ActorRef<MsgProtocol> viewActor;
    private final int nWorkers;
    private List<ActorRef<MsgProtocol>> workerActors;
    Chrono time = new Chrono();
    private SimulationState simulationState;
    private long nComputeForcesTaskDone;
    private long nUpdatePositionTaskDone;


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
                .onMessage(ResumeSimulationMsg.class, this::onResumeSimulationMsg)
                .onMessage(StopSimulationMsg.class, this::onStopSimulationMsg)
                .onMessage(ModelActor.SimulationStateValueMsg.class, this::onSimulationStateValueMsg)
                .onMessage(DoSimulationStepMsg.class, this::onDoSimulationStepMsg)
                .onMessage(SimulationStepDoneMsg.class, this::onSimulationStepDoneMsg)
                .onMessage(DoComputeForcesTaskMsg.class, this::onDoComputeForcesTaskMsg)
                .onMessage(ComputeForcesTaskDoneMsg.class, this::onComputeForcesTaskDoneMsg)
                .onMessage(DoUpdatePositionTaskMsg.class, this::onDoUpdatePositionTaskMsg)
                .onMessage(UpdatePositionTaskDoneMsg.class, this::onUpdatePositionTaskDoneMsg)
                .build();
    }


    private Behavior<MsgProtocol> onStartSimulationMsg(StartSimulationMsg msg) {
        this.time.start();
        for(int i = 0; i < this.nWorkers; i++){
            this.workerActors.add(getContext().spawn(WorkerActor.create(), "workerActor" + i));
        }
        this.modelActor.tell(new ModelActor.GetSimulationStateMsg(this.getContext().getSelf()));
        return this;
    }

    //creare handler per task fatto da ciascun worker che incrementa un contatore, in modo che il master si accorge quando tutti i worker hanno terminato
    //a questo punto può fare il secondo task
    //quando il secondo task finisce si manda un messaggio a se stesso per dire di fare il prossimo step e manda il messaggio alla view per aggiornare
    //mando messaggio al worker passandogli tutti i body e un range che deve computare
    private Behavior<MsgProtocol> onSimulationStateValueMsg(ModelActor.SimulationStateValueMsg msg) {
        this.simulationState = msg.state;
        getContext().getSelf().tell(new DoSimulationStepMsg());
        return this;
    }

    private Behavior<MsgProtocol> onDoSimulationStepMsg(DoSimulationStepMsg msg) {
        getContext().getLog().info("MasterActor computing a simulation step!");
        if (this.simulationState.getCurrentStep() < this.simulationState.getStepToDo()) {
        //CONTROLLARE SE È STOPPATA
            getContext().getSelf().tell(new DoComputeForcesTaskMsg());

        } else {
            time.stop();
            getContext().getLog().info("Time elapsed: " + time.getTime() + " ms.");
            //STOPPARE ACTOR SYSTEM
        }
        return this;
    }

    private Behavior<MsgProtocol> onDoComputeForcesTaskMsg(DoComputeForcesTaskMsg msg) {
        this.workerActors.forEach(workerActor -> workerActor.tell(new WorkerActor.ComputeForcesMsg(this.getContext().getSelf())));
        return this;
    }

    private Behavior<MsgProtocol> onComputeForcesTaskDoneMsg(ComputeForcesTaskDoneMsg msg) {
        this.nComputeForcesTaskDone++;
        if (this.nComputeForcesTaskDone == this.workerActors.size()){
            this.nComputeForcesTaskDone = 0;
            this.getContext().getSelf().tell(new DoUpdatePositionTaskMsg());
        }
        return this;
    }

    private Behavior<MsgProtocol> onDoUpdatePositionTaskMsg(DoUpdatePositionTaskMsg msg) {
        this.workerActors.forEach(workerActor -> workerActor.tell(new WorkerActor.UpdatePositionMsg(this.getContext().getSelf())));
        return this;
    }

    private Behavior<MsgProtocol> onUpdatePositionTaskDoneMsg(UpdatePositionTaskDoneMsg msg) {
        this.nUpdatePositionTaskDone++;
        if (this.nUpdatePositionTaskDone == this.workerActors.size()){
            this.nUpdatePositionTaskDone = 0;
            this.getContext().getSelf().tell(new SimulationStepDoneMsg());
        }
        return this;
    }

    private Behavior<MsgProtocol> onSimulationStepDoneMsg(SimulationStepDoneMsg msg) {
        this.simulationState.setVt(simulationState.getVt() + simulationState.getDt());
        this.simulationState.incrementSteps();
        this.viewActor.tell(new ViewActor.UpdateViewMsg(this.simulationState));
        this.getContext().getSelf().tell(new DoSimulationStepMsg());
        return this;
    }

    private Behavior<MsgProtocol> onStopSimulationMsg(StopSimulationMsg msg) {
        getContext().getLog().info("MasterActor stopped");
        return this;
    }

    private Behavior<MsgProtocol> onResumeSimulationMsg(ResumeSimulationMsg msg) {
        getContext().getLog().info("MasterActor resumed");
        return this;
    }

    public static class StartSimulationMsg implements MsgProtocol{}
    public static class StopSimulationMsg implements MsgProtocol{}
    public static class ResumeSimulationMsg implements MsgProtocol{}
    public static class DoSimulationStepMsg implements MsgProtocol{}
    public static class SimulationStepDoneMsg implements MsgProtocol{}

    public static class DoComputeForcesTaskMsg implements MsgProtocol { }

    public static class DoUpdatePositionTaskMsg implements MsgProtocol {}

    public static class ComputeForcesTaskDoneMsg implements MsgProtocol{}
    public static class UpdatePositionTaskDoneMsg implements MsgProtocol{}




}
