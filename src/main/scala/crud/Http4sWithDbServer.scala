package crud

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import crud.dao.{AuthorsDaoImpl, BooksDaoImpl, DaoInit}
import crud.routes.{AuthorsRoutes, BooksRoutes}
import doobie.util.transactor.Transactor
import fs2.Stream
import io.circe.config.parser
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import configs.{DbConfig, ServerConfig, Config}
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

object Http4sWithDbServer extends IOApp {

  def makeRoutes(xa: Transactor[IO]): HttpRoutes[IO] =
    BooksRoutes.routes(new BooksDaoImpl(xa)) <+> AuthorsRoutes.routes(new AuthorsDaoImpl(xa))

  def serveStream(transactor: Transactor[IO], serverConfig: ServerConfig): Stream[IO, ExitCode] = {
    BlazeServerBuilder[IO]
      .bindHttp(serverConfig.port, serverConfig.host)
      .withHttpApp(makeRoutes(transactor).orNotFound)
      .serve
  }

  def createTransactor(dbConfig: DbConfig): Transactor[IO] = {
    val transactor = DaoInit.transactor(dbConfig)
    DaoInit.initTables(transactor).unsafeRunSync()
    transactor
  }

  def loadConfig(): IO[Config] = for {
    serverConfig <- parser.decodePathF[IO, ServerConfig]("server")
    dbConfig <- parser.decodePathF[IO, DbConfig]("db")
  } yield Config(serverConfig, dbConfig)

  override def run(args: List[String]): IO[ExitCode] = {

    val stream: Stream[IO, ExitCode] = for {
      config <- Stream.eval(loadConfig())
      transactor = createTransactor(config.dbConfig)
      server <- serveStream(transactor, config.serverConfig)
    } yield server

    stream.compile.drain.as(ExitCode.Success)
  }
}
