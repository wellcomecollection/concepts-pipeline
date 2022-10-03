package weco.concepts.ingestor

import grizzled.slf4j.Logging

object Main extends App with IngestorMain with Logging {
  ingestStream.run
    .recover(err => error(err.getMessage))
    .onComplete(_ => actorSystem.terminate())
}
