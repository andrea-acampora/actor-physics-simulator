package pcd03.controller;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import pcd03.application.MsgProtocol;
import pcd03.model.ModelActor;

import java.util.LinkedList;
import java.util.List;

public class MasterActor extends AbstractBehavior<MsgProtocol> {
    private ActorRef<MsgProtocol> modelActor;
    private ActorRef<MsgProtocol> viewActor;
    private final int nWorkers;
    private List<ActorRef<MsgProtocol>> workerActors;

    private long stepToDo;

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
                .onMessage(ModelActor.StepToDoValueMsg.class, this::onStepsTodoValueMsg)
                .onMessage(ModelActor.BodiesValueMsg.class, this::onBodyValueMsg)
                .build();
    }

    //creare handler per task fatto da ciascun worker che incrementa un contatore, in modo che il master si accorge quando tutti i worker hanno terminato
    //a questo punto può fare il secondo task
    //quando il secondo task finisce si manda un messaggio a se stesso per dire di fare il prossimo step e manda il messaggio alla view per aggiornare
    private Behavior<MsgProtocol> onBodyValueMsg(ModelActor.BodiesValueMsg msg) {
        //mando messaggio al worker passandogli tutti i body e un range che deve computare
        return this;
    }

    private Behavior<MsgProtocol> onStepsTodoValueMsg(ModelActor.StepToDoValueMsg msg) {
        this.stepToDo = msg.value;
        getContext().getLog().info("step to do " + stepToDo);
        modelActor.tell(new ModelActor.GetBodiesMsg(getContext().getSelf()));
        return this;
    }

    private Behavior<MsgProtocol> onStartSimulationMsg(StartSimulationMsg msg) {
        for(int i = 0; i < this.nWorkers; i++){
            this.workerActors.add(getContext().spawn(WorkerActor.create(), "workerActor[" + i + "]"));
        }
        this.modelActor.tell(new ModelActor.GetStepToDoMsg(this.getContext().getSelf()));
        return this;
    }

    public static class StartSimulationMsg implements MsgProtocol{}
}
