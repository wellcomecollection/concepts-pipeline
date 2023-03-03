package weco.concepts.recorder

import org.scalatest.Inspectors
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.LoneElement.convertToCollectionLoneElementWrapper

import weco.concepts.common.model.{
  AuthoritativeConcept,
  CatalogueConcept,
  Concept,
  Identifier,
  IdentifierType
}

class MergeConceptsTest
    extends AnyFunSpec
    with Matchers
    with TableDrivenPropertyChecks
    with Inspectors {

  private val specificTypeTable = Table(
    ("typeList", "bestType"),
    (Seq("Agent", "Person"), "Person"),
    (Seq("Person", "Agent"), "Person"),
    (Seq("Concept", "Agent"), "Agent"),
    (Seq("Concept", "Place"), "Place"),
    (Seq("Organisation", "Meeting", "Place"), "Organisation")
  )

  private val equalSpecificityTable = Table(
    ("typeList", "bestType"),
    (Seq("Organisation", "Meeting", "Place"), "Organisation"),
    (Seq("Agent", "Meeting", "Organisation", "Place"), "Meeting")
  )

  describe("merging authoritative and catalogue concepts") {
    it("merges an AuthoritativeConcept and a CatalogueConcept") {
      info(
        """
          The AuthoritativeConcept is the sole source for alternative labels, 
          and the true source for the main label.
          The CatalogueConcept is the sole source of canonicalIds and ontologyTypes
        """
      )
      val authoritativeConcept = AuthoritativeConcept(
        identifier = Identifier(
          value = "sh95000541",
          identifierType = IdentifierType.LCSubjects
        ),
        label = "World Wide Web",
        alternativeLabels = Seq(
          "W3 (World Wide Web)",
          "WWW (World Wide Web)",
          "Web (World Wide Web)",
          "World Wide Web (Information retrieval system)"
        )
      )
      val catalogueConcept = CatalogueConcept(
        identifier = Identifier(
          value = "sh95000541",
          identifierType = IdentifierType.LCSubjects
        ),
        label = "The Internet",
        canonicalId = "123abcde",
        ontologyType = "Concept"
      )
      val result =
        MergeConcepts(Some(authoritativeConcept), Some(catalogueConcept)).head

      result.identifiers shouldBe Seq(authoritativeConcept.identifier)
      result.label shouldBe authoritativeConcept.label
      result.alternativeLabels shouldBe authoritativeConcept.alternativeLabels
      result.canonicalId shouldBe "123abcde"
      result.ontologyType shouldBe "Concept"
    }

    it(
      "uses the catalogue concept's best ontologyType when merging an AuthoritativeConcept and a CatalogueConcept"
    ) {
      forAll(specificTypeTable) { (typeList: Seq[String], bestType: String) =>
        val authoritativeConcept = AuthoritativeConcept(
          identifier = Identifier(
            value = "n78095637",
            identifierType = IdentifierType.LCNames
          ),
          label = "Darwin, Charles, 1809-1882",
          alternativeLabels = Seq(
            "Chuck D."
          )
        )
        val catalogueConcept = CatalogueConcept(
          identifier = Identifier(
            value = "n78095637",
            identifierType = IdentifierType.LCNames
          ),
          label = "Charles Darwin",
          canonicalId = Seq("123abcde", "deny75m5"),
          ontologyType = typeList
        )
        val results =
          MergeConcepts(Some(authoritativeConcept), Some(catalogueConcept))

        results.length shouldBe 2
        results.head.canonicalId shouldBe "123abcde"
        results(1).canonicalId shouldBe "deny75m5"

        forAll(results) { result: Concept =>
          result.identifiers shouldBe Seq(authoritativeConcept.identifier)
          result.label shouldBe authoritativeConcept.label
          result.alternativeLabels shouldBe authoritativeConcept.alternativeLabels
          result.ontologyType shouldBe bestType
        }
      }
    }

  }

  describe("catalogue concept only") {
    info("""
        When a concept is only present in catalogue-concepts and not in authoritative concepts,
        MergeConcepts should still generate results from it.
        This allows the concepts api to serve results for label derived concepts, and any other
        scheme where this pipeline does not (yet) ingest content from The Authority.
      """)
    it(
      "creates a Concept from a CatalogueConcept without an AuthoritativeConcept"
    ) {
      forAll(Table("ontologyType", "Concept", "Person", "Place")) {
        ontologyType =>
          val catalogueConcept = CatalogueConcept(
            identifier = Identifier(
              value = "things",
              identifierType = IdentifierType.LabelDerived
            ),
            label = "Things",
            canonicalId = "123abcde",
            ontologyType = ontologyType
          )
          val result = MergeConcepts(None, Some(catalogueConcept)).head

          result.canonicalId shouldBe catalogueConcept.canonicalId.head
          result.identifiers shouldBe Seq(catalogueConcept.identifier)
          result.label shouldBe catalogueConcept.label
          result.alternativeLabels shouldBe Nil
          result.ontologyType shouldBe ontologyType
      }
    }

    it("creates a Concept for each identifier in the canonicalId list") {

      val catalogueConcept = CatalogueConcept(
        identifier = Identifier(
          value = "things",
          identifierType = IdentifierType.LabelDerived
        ),
        label = "Things",
        canonicalId = Seq("123abcde", "deny75m5"),
        ontologyType = Seq("Concept")
      )
      val results: Seq[Concept] = MergeConcepts(None, Some(catalogueConcept))

      results.length shouldBe 2
      results.head.canonicalId shouldBe "123abcde"
      results(1).canonicalId shouldBe "deny75m5"
      results.head.sameAs.loneElement shouldBe "deny75m5"
      results(1).sameAs.loneElement shouldBe "123abcde"

      forAll(results) { result: Concept =>
        result.identifiers shouldBe Seq(catalogueConcept.identifier)
        result.label shouldBe catalogueConcept.label
        result.alternativeLabels shouldBe Nil
        result.ontologyType shouldBe "Concept"
      }
    }

    it("always uses the most specific ontologyType from the list") {
      forAll(specificTypeTable) { (typeList: Seq[String], bestType: String) =>
        val catalogueConcept = CatalogueConcept(
          identifier = Identifier(
            value = "things",
            identifierType = IdentifierType.LabelDerived
          ),
          label = "Things",
          canonicalId = Seq("123abcde", "deny75m5"),
          ontologyType = typeList
        )
        val results: Seq[Concept] = MergeConcepts(None, Some(catalogueConcept))
        results.length shouldBe 2
        forAll(results) { result: Concept =>
          result.ontologyType shouldBe bestType
        }
      }
    }
    it("chooses the first type from a list of equal specificity") {
      forAll(
        equalSpecificityTable
      ) { (typeList: Seq[String], bestType: String) =>
        val catalogueConcept = CatalogueConcept(
          identifier = Identifier(
            value = "things",
            identifierType = IdentifierType.LabelDerived
          ),
          label = "Things",
          canonicalId = Seq("123abcde", "deny75m5"),
          ontologyType = typeList
        )
        val results: Seq[Concept] = MergeConcepts(None, Some(catalogueConcept))
        results.length shouldBe 2
        forAll(results) { result: Concept =>
          result.ontologyType shouldBe bestType
        }
      }
    }
  }

  describe("failure conditions") {
    it("errors if the concepts do not have matching identifiers") {
      val authoritativeConcept = AuthoritativeConcept(
        identifier = Identifier(
          value = "sh95000541",
          identifierType = IdentifierType.LCSubjects
        ),
        label = "World Wide Web",
        alternativeLabels = Seq(
          "W3 (World Wide Web)",
          "WWW (World Wide Web)",
          "Web (World Wide Web)",
          "World Wide Web (Information retrieval system)"
        )
      )
      val catalogueConcept = CatalogueConcept(
        identifier = Identifier(
          value = "sh92002816",
          identifierType = IdentifierType.LCSubjects
        ),
        label = "The Internet",
        canonicalId = "123abcde",
        ontologyType = "Concept"
      )

      the[IllegalArgumentException] thrownBy MergeConcepts(
        Some(authoritativeConcept),
        Some(catalogueConcept)
      ) should have message s"requirement failed: Cannot merge concepts with different identifiers (${authoritativeConcept.identifier} and ${catalogueConcept.identifier}): if you are seeing this error then assumptions about ordering in the recorder have been broken."
    }

    it("errors if the CatalogueConcept is None") {
      val authoritativeConcept = AuthoritativeConcept(
        identifier = Identifier(
          value = "sh95000541",
          identifierType = IdentifierType.LCSubjects
        ),
        label = "World Wide Web",
        alternativeLabels = Seq(
          "W3 (World Wide Web)",
          "WWW (World Wide Web)",
          "Web (World Wide Web)",
          "World Wide Web (Information retrieval system)"
        )
      )

      the[IllegalArgumentException] thrownBy MergeConcepts(
        Some(authoritativeConcept),
        None
      ) should have message s"This error should never occur: we've been asked to merge a concept (${authoritativeConcept.identifier}) which isn't used in the catalogue"
    }

    it("errors if given 2 Nones") {
      the[IllegalArgumentException] thrownBy MergeConcepts(
        None,
        None
      ) should have message "This error should never occur: we've been asked to merge a concept which exists in neither the authoritative nor the catalogue concepts indices. Has something gone wrong in the aggregator?"
    }
  }
}
