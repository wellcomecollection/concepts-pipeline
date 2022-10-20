package weco.concepts.aggregator

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.aggregator.testhelpers.UsedConcept
import weco.concepts.common.fixtures.TestElasticHttpClient

import scala.concurrent.ExecutionContext.Implicits.global

class UsedConceptBulkUpdateFlowTest extends AnyFunSpec with Matchers {
  it("formats UsedConcepts correctly") {
    val concept =
      UsedConcept(
        authority = "lc-names",
        identifier = "n84165387",
        label = "Pujol, Joseph, 1857-1945",
        canonicalId = "baadbeef"
      )
    val updateFlow = new UsedConceptBulkUpdateFlow(
      elasticHttpClient = new TestElasticHttpClient({ case _ =>
        throw new RuntimeException("No request expected!")
      }),
      maxBulkRecords = 1,
      indexName = "test"
    )

    val formatted = updateFlow.format(concept).get
    val lines = formatted.linesIterator.toList
    lines.length shouldBe 2
    val action = ujson.read(lines.head)
    val document = ujson.read(lines(1))

    action
      .obj("update")
      .obj("_id")
      .value shouldBe "lc-names:n84165387"

    val expectedDocument = ujson.read("""{
      "authority": "lc-names",
      "identifier": "n84165387",
      "label": "Pujol, Joseph, 1857-1945",
      "canonicalId":"baadbeef"
    }""")
    document("doc_as_upsert").value shouldBe true
    document("doc") shouldBe expectedDocument
  }

}
