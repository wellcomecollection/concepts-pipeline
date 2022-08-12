package weco.concepts.aggregator
import grizzled.slf4j.Logging
import ujson.Obj
import ujson.Value.Value
import weco.concepts.common.model._
import weco.concepts.common.json.JsonOps._

object ConceptExtractor {
<<<<<<< HEAD
  val conceptTypes =
    Seq("Concept", "Person", "Organisation", "Meeting", "Period")
  def apply(jsonString: String): List[UsedConcept] =
    allConcepts(ujson.read(jsonString)).distinctBy(_.identifier)

  private def allConcepts(json: Value): List[UsedConcept] =
=======
  val conceptTypes = Seq("Concept", "Person", "Organisation", "Meeting", "Period")
  def apply(jsonString: String): Seq[UsedConcept] =
    allConcepts(ujson.read(jsonString)).distinctBy(_.identifier)


  private def allConcepts(json: Value): Seq[UsedConcept] =
>>>>>>> 5eb421d (extract from document is feature-complete)
    json match {
      case arr: ujson.Arr => arr.arr.flatMap(allConcepts).toList
      case obj: ujson.Obj if isConcept(obj) => UsedConcepts(obj)
      case obj: ujson.Obj => obj.obj.values.flatMap(allConcepts).toList
      case _              => Nil
    }

  private def isConcept(json: Value): Boolean = {
    json.opt[String]("type") match {
      case None              => false
      case Some(conceptType) => conceptTypes.contains(conceptType)
    }
  }
}

object UsedConcepts extends Logging{
  def apply(conceptJson: ujson.Obj): Seq[UsedConcept] = {
    conceptJson.optSeq("identifiers") match {
      case Some(sourceIdentifiers) => sourceIdentifiers flatMap {
        conceptWithSource(conceptJson, _)
      }
      case None =>
        warn(s"Malformed Concept encountered, identifiers property missing ${conceptJson.render(indent=2)}")
        Nil
    }
  }

  private def conceptWithSource(conceptJson: Obj, sourceIdentifier: Value): Option[UsedConcept] = {
    try {
      val authority = sourceIdentifier.opt[Value]("identifierType").get
      Some(UsedConcept(
        identifier = Identifier(
          value = sourceIdentifier.opt[String]("value").get,
          identifierType = identifierTypeFromAuthority(authority)
        ),
        label = conceptJson.opt[String]("label").get,
        canonicalId = conceptJson.opt[String]("id").get,
      ))
    } catch {
      case exception: Exception =>
        warn(s"Malformed Concept encountered: $exception ${conceptJson.render(indent=2)}")
        None

    }
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
