package weco.concepts.aggregator
import ujson.Value.Value
import weco.concepts.common.model._
import weco.concepts.common.json.JsonOps._

object ConceptExtractor {
  val conceptTypes = Seq("Concept", "Person", "Organisation", "Meeting", "Period")
  def apply(jsonString: String): List[UsedConcept] =
    allConcepts(ujson.read(jsonString)).distinctBy(_.identifier)


  private def allConcepts(json: Value): List[UsedConcept] =
    json match {
      case arr: ujson.Arr => arr.arr.flatMap(allConcepts).toList
      case obj: ujson.Obj if isConcept(obj) => List(JSONConcept(obj))
      case obj: ujson.Obj => obj.obj.values.flatMap(allConcepts).toList
      case _              => Nil
    }

  private def isConcept(json: Value): Boolean = {
    json.opt[String]("type") match{
      case None => false
      case Some(conceptType) => conceptTypes.contains(conceptType)
    }
  }
}

object JSONConcept {
  def apply(json: ujson.Obj): UsedConcept = {
    val sourceIdentifier: Value = json.optSeq("identifiers").get.head
    val authority = sourceIdentifier.opt[ujson.Value]("identifierType").get
    UsedConcept(
      identifier = Identifier(
        value = sourceIdentifier.opt[String]("value").get,
        identifierType = identifierTypeFromAuthority(authority)
      ),
      label = json.opt[String]("label").get,
      canonicalId = json.opt[String]("id").get,
    )
  }

  private def identifierTypeFromAuthority(
    sourceType: ujson.Value
  ): IdentifierType = {
    val sourceTyepId = sourceType.opt[String]("id").get
    IdentifierType.typeMap.getOrElse(
      sourceTyepId,
      throw BadIdentifierTypeException(sourceTyepId)
    )
  }
}
