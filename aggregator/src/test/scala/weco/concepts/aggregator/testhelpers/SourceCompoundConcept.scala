package weco.concepts.aggregator.testhelpers

object SourceCompoundConcept extends ValueGenerators {
  def apply(
             authority: String = anAuthority,
             identifier: String = anExternalId,
             label: String = aLabel,
             canonicalId: String = aCanonicalId,
             ontologyType: String = aType,
             concepts: List[SourceConcept]
           ): SourceCompoundConcept = new SourceCompoundConcept(
    authority,
    identifier,
    label,
    canonicalId,
    ontologyType,
    concepts
  )

}

/*
 * Utility object to generate concepts in the form in which they are
 * presented in the Works API.  This is the form in which they arrive
 * into the ConceptExtractor
 */
case class SourceCompoundConcept(
                          authority: String,
                          identifier: String,
                          label: String,
                          canonicalId: String,
                          ontologyType: String,
                          concepts: List[SourceConcept]
                        ) {
  override def toString: String =
    s"""
       |{
       |  "id": "$canonicalId",
       |  "identifiers": [
       |    {
       |      "identifierType": {
       |        "id": "$authority",
       |        "label": "This field is ignored",
       |        "type": "IdentifierType"
       |      },
       |      "value": "$identifier",
       |      "type": "Identifier"
       |    }
       |  ],
       |  "label": "$label",
       |  "type": "$ontologyType"
       |  "concepts": [${concepts.mkString(", ")}]
       |}
       |""".stripMargin
}
