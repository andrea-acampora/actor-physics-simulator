package pcd03.application;


import akka.actor.typed.ActorSystem;

public class Main {

    public static void main(String[] args) {
        final ActorSystem<MsgProtocol> system = ActorSystem.create(MainActor.create(), "mainActor");
    }
}
