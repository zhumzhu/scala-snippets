package examples.part05

import akka.actor._
import akka.event.LoggingReceive

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random


class BasicLifecycleLoggingActor extends Actor with ActorLogging {

  log.info("Inside BasicLifecycleLoggingActor Constructor")
  log.info(context.self.toString())
  if (Random.nextBoolean()) {
    log.info("Stopping self due to randomness")
    context.stop(self)
  }

  override def preStart() = {
    log.info("Inside the preStart method of BasicLifecycleLoggingActor")
  }

  def receive = LoggingReceive {
    case "hello" => log.info("hello")
    case "stop" => {
      log.info("stopping self due to stop message")
      context.stop(self)
    }
  }

  override def postStop() = {
    log.info("Inside postStop method of BasicLifecycleLoggingActor")
  }
}

class MyCustomDeadLetterListener extends Actor {
  def receive = {
    case deadLetter: DeadLetter => println(s"FROM CUSTOM LISTENER $deadLetter")
  }
}

object LifecycleExampleDriver {
  def main(args: Array[String]) {
    val actorSystem = ActorSystem("LifecycleActorSystem")
    val logger = actorSystem.log
    val lifecycleActor = actorSystem.actorOf(Props[BasicLifecycleLoggingActor], "lifecycleActor")
    val deadLetterRef = actorSystem.actorOf(Props[MyCustomDeadLetterListener])
    actorSystem.eventStream.subscribe(deadLetterRef, classOf[DeadLetter])

    lifecycleActor ! "hello"

    //wait for a couple of seconds before shutdown
    logger.info("Before sleep")
    Thread.sleep(2000)
    logger.info("After sleep")

    val r = Random.nextInt(3)
    r match {
      case 0 =>
        logger.info("Sending message: PoisonPill")
        lifecycleActor ! PoisonPill
      case 1 =>
        logger.info("Sending message: stop")
        lifecycleActor ! "stop"
      case 2 =>
        logger.info("Sending message: Kill")
        lifecycleActor ! Kill
    }

    lifecycleActor ! "hello"

    val termF = actorSystem.terminate()
    logger.info("Terminate invoked")
    Await.result(termF, 10 seconds)
  }
}
