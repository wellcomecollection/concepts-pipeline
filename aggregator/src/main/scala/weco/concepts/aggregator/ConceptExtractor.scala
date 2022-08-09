package weco.concepts.aggregator
import weco.concepts.aggregator.model._

object ConceptExtractor {

  def apply(jsonString: String): List[Concept] = {
    val json = ujson.read(jsonString)

    println(json)
    Nil
  }

}
