package weco.concepts.common.model

case class AuthoritativeConcept(
  identifier: Identifier,
  label: String,
  alternativeLabels: Seq[String]
)
