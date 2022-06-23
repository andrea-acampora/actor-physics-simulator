package pcd03.controller;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import pcd03.application.MsgProtocol;
import pcd03.model.Body;
import pcd03.model.ModelActor;
import pcd03.model.SimulationState;
import pcd03.model.V2d;

import java.util.LinkedList;
import java.util.List;

public class WorkerActor extends AbstractBehavior<MsgProtocol> {

    private final ActorRef<MsgProtocol> modelActor;

    public WorkerActor(ActorContext<MsgProtocol> context, ActorRef<MsgProtocol> modelActor) {
        super(context);
        this.modelActor = modelActor;
    }

    public static Behavior<MsgProtocol> create(ActorRef<MsgProtocol> modelActor) {
        return Behaviors.setup(context -> new WorkerActor(context, modelActor));
    }

    @Override
    public Receive<MsgProtocol> createReceive() {
        return newReceiveBuilder()
                .onMessage(ComputeForcesMsg.class, this::onComputeForcesMsg)
                .onMessage(UpdatePositionMsg.class, this::onUpdatePositionMsg)
                .build();
    }

    private Behavior<MsgProtocol> onComputeForcesMsg(ComputeForcesMsg msg) {
        List<Body> bodies = msg.simulationState.getBodies();
        List<Body> updatedBodies = new LinkedList<>();
        for(int i = msg.start; i < msg.end; i++) {
            Body b = bodies.get(i);
            V2d totalForce = new V2d(0, 0);
            /* compute total repulsive force */
            for (Body otherBody : bodies) {
                if (!b.equals(otherBody)) {
                    try {
                        V2d forceByOtherBody = b.computeRepulsiveForceBy(otherBody);
                        totalForce.sum(forceByOtherBody);
                    } catch (Exception ignored) {
                    }
                }
            }
            /* add friction force */
            totalForce.sum(b.getCurrentFrictionForce());
            /* compute instant acceleration */
            V2d acc = new V2d(totalForce).scalarMul(1.0 / b.getMass());
            Body body = new Body(b.getId(), b.getPos(), b.getVel(), b.getMass());
            /* update velocity */
            body.updateVelocity(acc, msg.simulationState.getDt());
            updatedBodies.add(body);
        }
        modelActor.tell(new ModelActor.BodiesSubListUpdatedMsg(updatedBodies, msg.start, msg.end));
        msg.sender.tell(new MasterActor.ComputeForcesTaskDoneMsg());
        return this;
    }

    private Behavior<MsgProtocol> onUpdatePositionMsg(UpdatePositionMsg msg) {
        List<Body> bodies = msg.simulationState.getBodies();
        List<Body> updatedBodies = new LinkedList<>();
        for(int i = msg.start; i < msg.end; i++) {
            Body b = bodies.get(i);
            Body body = new Body(b.getId(), b.getPos(), b.getVel(), b.getMass());
            body.updatePos(msg.simulationState.getDt());
            body.checkAndSolveBoundaryCollision(msg.simulationState.getBounds());
            updatedBodies.add(body);
        }
        msg.sender.tell(new MasterActor.UpdatePositionTaskDoneMsg());
        modelActor.tell(new ModelActor.BodiesSubListUpdatedMsg(updatedBodies, msg.start, msg.end));
        return this;
    }

    public static class ComputeForcesMsg implements MsgProtocol{
        public final int start;
        public final int end;
        public final ActorRef<MsgProtocol> sender;
        public final SimulationState simulationState;
        public ComputeForcesMsg(ActorRef<MsgProtocol> sender, SimulationState simulationState, int start, int end) {
            this.sender = sender;
            this.simulationState = simulationState;
            this.start = start;
            this.end = end;
        }
    }
    public static class UpdatePositionMsg implements MsgProtocol{
        public final ActorRef<MsgProtocol> sender;
        public final int start;
        public final int end;
        public final SimulationState simulationState;
        public UpdatePositionMsg(ActorRef<MsgProtocol> sender, SimulationState simulationState, int start, int end) {
            this.sender = sender;
            this.simulationState = simulationState;
            this.start = start;
            this.end = end;
        }
    }
}
