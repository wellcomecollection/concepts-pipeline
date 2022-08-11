package weco.concepts.aggregator
import ujson.Value.Value
import weco.concepts.common.model._
import weco.concepts.common.json.JsonOps._

object ConceptExtractor {

  def apply(jsonString: String): List[Concept] = {
    val json: Value = ujson.read(jsonString)
    val conceptNodes = allConcepts(json)
    println(conceptNodes)
    json.toString()
    Nil
  }

  def allConcepts(json: Value): List[Concept] = {
    json match {
      case arr: ujson.Arr => arr.arr.flatMap(allConcepts).toList
      case obj: ujson.Obj if isConcept(obj) => List(JSONConcept(obj))
      case obj: ujson.Obj => obj.obj.values.flatMap(allConcepts).toList
      case _ => Nil
    }
  }

  def isConcept(json: Value): Boolean =
    json.opt[String]("type").contains("Concept")
}

object JSONConcept  {
  def apply(json:ujson.Obj): Concept = {
    val sourceIdentifier: Value = json.optSeq("identifiers").get.head
    val authority = sourceIdentifier.opt[ujson.Value]("identifierType").get
    Concept(
      identifier = Identifier(
        value=sourceIdentifier.opt[String]("value").get,
        identifierType = identifierTypeFromAuthority(authority)
      ),
      label = json.opt[String]("label").get,
      canonicalId = json.opt[String]("id"),
      alternativeLabels = Nil
    )
  }

  private def identifierTypeFromAuthority(sourceType: ujson.Value): IdentifierType = {
    val sourceTyepId = sourceType.opt[String]("id").get
    IdentifierType.typeMap.getOrElse(sourceTyepId, throw BadIdentifierTypeException(sourceTyepId))
  }
}
