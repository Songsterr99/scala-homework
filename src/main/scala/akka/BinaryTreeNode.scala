package akka

import akka.actor.{Actor, ActorRef, Props}
import akka.BinaryTreeSet.Operation

object BinaryTreeNode {

  sealed trait Position

  case object Left extends Position

  case object Right extends Position

  def props(elem: Int, initiallyRemoved: Boolean): Props = Props(classOf[BinaryTreeNode], elem, initiallyRemoved)
}

class BinaryTreeNode(val elem: Int, initiallyRemoved: Boolean) extends Actor {

  import BinaryTreeNode._
  import BinaryTreeSet.Operation._
  import BinaryTreeSet.OperationReply._

  var subtrees = Map[Position, ActorRef]()
  var removed = initiallyRemoved

  def insertRightOrLeft(m: Insert): Unit = {
    if (m.elem == elem) {
      removed = false
      m.requester ! OperationFinished(m.id)
    } else {
      subtrees.get(getPosition(m.elem)) match {
        case Some(node) => node ! m
        case None =>
          subtrees += (getPosition(m.elem) -> context.actorOf(BinaryTreeNode.props(m.elem, initiallyRemoved = false)))
          m.requester ! OperationFinished(m.id)
      }
    }
  }

  def isExistedElement(m: Contains): Unit = {
    var result = true
    if (m.elem == elem) {
      result = !removed
      m.requester ! ContainsResult(m.id, result)
    } else {
      subtrees.get(getPosition(m.elem)) match {
        case Some(node) => node ! m
        case None => m.requester ! ContainsResult(m.id, result = false)
      }
    }
  }

  def removeElement(m: Remove): Unit = {
    if (m.elem == elem) {
      removed = true
      m.requester ! OperationFinished(m.id)
    } else {
      subtrees.get(getPosition(m.elem)) match {
        case Some(node) => node ! m
        case None => m.requester ! OperationFinished(m.id)
      }
    }
  }

  def getPosition(baseElem: Int): Position = if (baseElem < elem) Left else Right

  def receive: Receive = {
    case m: Insert => insertRightOrLeft(m)
    case m: Contains => isExistedElement(m)
    case m: Remove => removeElement(m)
    case _ => println("Wrong message.")
  }
}
