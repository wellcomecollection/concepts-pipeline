package weco.concepts.ingestor

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class IngestorTest extends AnyFunSpec with Matchers {
  it("identifies the truth of a statement given the Peano axioms") {
    2 + 2 shouldBe 4
  }
}
