import javax.inject.{Inject, Provider, Singleton}
import actors._
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.Materializer
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import service.StockDataIngestService

import scala.concurrent.ExecutionContext

class Module extends AbstractModule with AkkaGuiceSupport {
  val werr = new StockDataIngestService()

  override def configure(): Unit = {
    bindTypedActor(StocksActor(i = 12, j = 122, stockDataIngestService = werr), "stocksActor")
    bindTypedActor(UserParentActor, "userParentActor")
    bind(classOf[UserActor.Factory]).toProvider(classOf[UserActorFactoryProvider])
  }
}

@Singleton
class UserActorFactoryProvider @Inject()(
    stocksActor: ActorRef[StocksActor.GetStocks],
    mat: Materializer,
    ec: ExecutionContext,
) extends Provider[UserActor.Factory] {
  def get() = UserActor(_, stocksActor)(mat, ec)
}
