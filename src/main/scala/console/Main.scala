package console

import java.util.concurrent.Executors

import scala.io.StdIn
import cats.effect.{CancelToken, ContextShift, Fiber, IO, Timer}
import cats.syntax.apply._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

object Main {

  implicit val timer = IO.timer(ExecutionContext.global)
  private var workers = Map[String, CancelToken[IO]]()
  private var workerMessages = Map[String, Int]()

  private def createContextShift(): ContextShift[IO] = {
    val context = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
    IO.contextShift(context)
  }

  def printMessage(workerName: String, message: String): Unit = {
    workerMessages.get(workerName) match {
      case Some(value) => workerMessages += (workerName -> (value + 1))
      case None => workerMessages += (workerName -> 1)
    }
    println(message)
  }

  def startWorker(workerName: String, message: String): Unit = {
    implicit val c: ContextShift[IO] = createContextShift()
    def repeat: IO[Unit] = IO(printMessage(workerName, message)).flatMap(_ => IO.sleep(5.seconds) *> repeat)
    val worker: CancelToken[IO] = repeat.unsafeRunCancelable(r => println(s"Done: $r"))
    workers += (workerName -> worker)
  }

  def stopWorker(workerName: String): Unit = {
    workers.get(workerName) match {
      case Some(value) => {
        print(s"$workerName sent ${workerMessages(workerName)} messages")
        value.unsafeRunSync()
      }
      case None => println(s"$workerName doesn't exist")
    }
  }

  def executeCommand(command: String)(): Unit = command.split(" ") match {
    case values if values(0) == "start" && values.length == 3 => startWorker(values(1), values(2))
    case values if values(0) == "stop" && values.length == 2 => stopWorker(values(1))
    case _ => ()
  }

  def startInputWorker(): IO[Fiber[IO, Unit]] = {
    def repeat: IO[Unit] = IO( executeCommand(StdIn.readLine()) ).flatMap(_ => repeat)
    repeat.start(createContextShift())
  }

  def main(args: Array[String]): Unit = {
    val worker = startInputWorker()
    worker.unsafeRunSync()
  }
}


