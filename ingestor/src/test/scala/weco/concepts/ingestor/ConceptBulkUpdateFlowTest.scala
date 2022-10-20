package weco.concepts.ingestor

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.elasticsearch.BulkUpdateResult
import weco.concepts.common.fixtures.TestElasticHttpClient
import weco.concepts.common.model.{Concept, Identifier, IdentifierType}

import scala.concurrent.ExecutionContext.Implicits.global

class ConceptBulkUpdateFlowTest extends AnyFunSpec with Matchers {
  implicit val actorSystem: ActorSystem = ActorSystem("test")

  it("does not return documents for geographic subdivision records") {
    val invalidConcept = Concept(
      identifier = Identifier(
        value = "sh2014000619-781",
        identifierType = IdentifierType.LCSubjects
      ),
      label = "Gabon--Parc national de Loango",
      alternativeLabels = Nil
    )
    val updateFlow = new ConceptBulkUpdateFlow(
      elasticHttpClient = new TestElasticHttpClient({ case _ =>
        throw new RuntimeException("No request was expected!")
      }),
      maxBulkRecords = 1,
      indexName = "test"
    )

    Source
      .single(invalidConcept)
      .via(updateFlow.flow)
      .runWith(TestSink.probe[BulkUpdateResult])
      .request(1)
      .expectComplete()
  }
}