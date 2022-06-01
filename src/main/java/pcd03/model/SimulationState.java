package pcd03.model;

import java.util.ArrayList;
import java.util.Random;

/**
 * Contains all the information about the simulation.
 */
public class SimulationState {

    /* bodies in the field */
    private final ArrayList<Body> bodies;

    /* boundary of the field */
    private final Boundary bounds;

    /* virtual time */
    private double vt;

    /* virtual time step */
    double dt;

    long currentStep;

    long stepToDo;

    public SimulationState(final int nBodies, final long stepToDo) {
        this.vt = 0;
        this.dt = 0.001;
        this.currentStep = 0;
        this.stepToDo = stepToDo;

        this.bounds = new Boundary(-6.0, -6.0, 6.0, 6.0);
        Random rand = new Random(System.currentTimeMillis());
        this.bodies = new ArrayList<>();
        for (int i = 0; i < nBodies; i++) {
            double x = bounds.getX0()*0.25 + rand.nextDouble() * (bounds.getX1() - bounds.getX0()) * 0.25;
            double y = bounds.getY0()*0.25 + rand.nextDouble() * (bounds.getY1() - bounds.getY0()) * 0.25;
            Body b = new Body(i, new P2d(x, y), new V2d(0, 0), 10);
            bodies.add(b);
        }
    }

    public ArrayList<Body> getBodies() {
        return bodies;
    }

    public Boundary getBounds() {
        return bounds;
    }

    public double getVt() {
        return vt;
    }

    public double getDt() {
        return dt;
    }

    public void setVt(final double vt) {
        this.vt = vt;
    }

    public long getSteps() {
        return currentStep;
    }

    public void incrementSteps() {
        this.currentStep++;
    }

    public long getStepToDo() {
        return stepToDo;
    }
}
