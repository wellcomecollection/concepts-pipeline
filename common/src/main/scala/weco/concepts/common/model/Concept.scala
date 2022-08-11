package weco.concepts.common.model

case class Concept(
  identifier: Identifier,
  label: String,
  alternativeLabels: Seq[String],
  canonicalId: Option[String] = None
)
