package akka

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import scala.util.Random

object Main extends App {
  class MainActor extends Actor {
    val treeSetActorRef: ActorRef = actorSystem.actorOf(Props[BinaryTreeSet], "treeSet")
    val rnd = Random

    for( i <- 0 to 10) {
      treeSetActorRef ! BinaryTreeSet.Operation.Insert(treeSetActorRef, i, rnd.nextInt(20))
    }

    for( i <- 11 to 15) {
      treeSetActorRef ! BinaryTreeSet.Operation.Contains(treeSetActorRef, i, rnd.nextInt(20))
    }

    for( i <- 16 to 20) {
      treeSetActorRef ! BinaryTreeSet.Operation.Remove(treeSetActorRef, i, rnd.nextInt(20))
    }

    treeSetActorRef ! BinaryTreeSet.Operation.Insert(treeSetActorRef, 21, 12)
    treeSetActorRef ! BinaryTreeSet.Operation.Contains(treeSetActorRef, 22, 12)


    override def receive: Receive = {
      case _ => println("It's working")
    }
  }

  val actorSystem: ActorSystem = ActorSystem("actor-system")
  val mainActorRef: ActorRef = actorSystem.actorOf(Props[MainActor], "main")
}