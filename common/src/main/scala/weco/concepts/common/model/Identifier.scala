// TODO: These will probably end up living in a common project/library in this repo
package weco.concepts.common.model

case class Identifier(
  value: String,
  identifierType: IdentifierType,
  ontologyType: "Identifier" = "Identifier"
)

sealed trait IdentifierType {
  val id: String;
  override def toString: String = id
}
object IdentifierType {
  case object LCSubjects extends IdentifierType {
    val id = "lc-subjects"
  }
  case object LCNames extends IdentifierType {
    val id = "lc-names"
  }
}
