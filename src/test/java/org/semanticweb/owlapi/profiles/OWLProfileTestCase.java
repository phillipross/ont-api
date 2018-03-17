/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 */
package org.semanticweb.owlapi.profiles;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.profiles.violations.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;
import ru.avicomp.owlapi.OWLManager;

import java.lang.Class;
import java.util.*;
import java.util.stream.Stream;

import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.*;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Boolean;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Double;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Integer;

@ru.avicomp.ontapi.utils.ModifiedForONTApi
@SuppressWarnings({"javadoc", "rawtypes"})
public class OWLProfileTestCase extends TestBase {

    private static final String START = OWLThing().getIRI().getNamespace();
    private static final OWLClass CL = Class(IRI("urn:test#", "fakeclass"));
    private static final OWLDataProperty DATAP = DataProperty(IRI("urn:datatype#", "fakedatatypeproperty"));
    private static final OWLDataPropertyRangeAxiom DATA_PROPERTY_RANGE2 = DataPropertyRange(DATAP, DatatypeRestriction(Integer(), FacetRestriction(OWLFacet.LANG_RANGE, Literal(1))));
    private static final OWLDataPropertyRangeAxiom DATA_PROPERTY_RANGE = DataPropertyRange(DATAP, DatatypeRestriction(Integer(), FacetRestriction(OWLFacet.MAX_EXCLUSIVE, Literal(1))));
    private static final OWLObjectProperty OP = ObjectProperty(IRI("urn:datatype#", "fakeobjectproperty"));
    private static final OWLDatatype UNKNOWNFAKEDATATYPE = Datatype(IRI(START, "unknownfakedatatype"));
    private static final OWLDatatype FAKEUNDECLAREDDATATYPE = Datatype(IRI("urn:datatype#", "fakeundeclareddatatype"));
    private static final OWLDatatype FAKEDATATYPE = Datatype(IRI("urn:datatype#", "fakedatatype"));
    private static final IRI onto = IRI.create("urn:test#", "ontology");
    private static final OWLDataFactory DF = OWLManager.getOWLDataFactory();
    private static final OWLObjectProperty P = ObjectProperty(IRI("urn:test#", "objectproperty"));
    private OWLOntology o;

    @Before
    @Override
    public void setupManagersClean() {
        super.setupManagersClean();
        if (!OWLManager.DEBUG_USE_OWL) {
            // allow illegal punnings
            ru.avicomp.ontapi.config.OntLoaderConfiguration conf = ((ru.avicomp.ontapi.OntologyManager) m).getOntologyLoaderConfiguration()
                    .setPersonality(ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig.ONT_PERSONALITY_LAX);
            m.setOntologyLoaderConfiguration(conf);
        }
        try {
            o = getOWLOntology(onto);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError("Exception", e);
        }
    }

    private void declare(OWLOntology ont, OWLEntity... entities) {
        Stream.of(entities).map(OWLFunctionalSyntaxFactory::Declaration).forEach(ont::add);
    }

    private static final Comparator<Class> comp = Comparator.comparing(Class::getSimpleName);

    private void checkInCollection(List<OWLProfileViolation> violations, Class[] inputList) {
        List<Class> list = new ArrayList<>(Arrays.asList(inputList));
        List<Class> list1 = new ArrayList<>();
        for (OWLProfileViolation v : violations) {
            list1.add(v.getClass());
        }
        list.sort(comp);
        list1.sort(comp);
        Assert.assertEquals(list1.toString(), list, list1);
    }

    private void runAssert(OWLOntology ontology, OWLProfile profile, int expected, Class[] expectedViolations) {
        ru.avicomp.ontapi.utils.ReadWriteUtils.print(ontology);
        List<OWLProfileViolation> violations = profile.checkOntology(ontology).getViolations();
        violations.forEach(v -> LOGGER.debug("Violation: [{}]", v));
        Assert.assertEquals(violations.toString(), expected, violations.size());
        checkInCollection(violations, expectedViolations);
        for (OWLProfileViolation violation : violations) {
            ontology.getOWLOntologyManager().applyChanges(violation.repair());
            violation.accept(new OWLProfileViolationVisitor() {
            });
            violation.accept(new OWLProfileViolationVisitorEx<String>() {

                @Override
                public Optional<String> doDefault(OWLProfileViolation object) {
                    return Optional.of(object.toString());
                }
            });
        }
        violations = profile.checkOntology(ontology).getViolations();
        Assert.assertEquals(0, violations.size());
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatype datatype)")
    public void shouldCreateViolationForOWLDatatypeInOWL2DLProfile() {
        declare(o, UNKNOWNFAKEDATATYPE, FAKEDATATYPE, Class(FAKEDATATYPE.getIRI()), DATAP);
        o.addAxiom(DataPropertyRange(DATAP, FAKEUNDECLAREDDATATYPE));
        int expected = 1;
        Class[] expectedViolations = {UseOfUndeclaredDatatype.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeDefinitionAxiom axiom)")
    public void shouldCreateViolationForOWLDatatypeDefinitionAxiomInOWL2DLProfile() {
        declare(o, Integer(), Boolean(), FAKEDATATYPE);
        o.add(DatatypeDefinition(Boolean(), Integer()), DatatypeDefinition(FAKEDATATYPE, Integer()), DatatypeDefinition(
                Integer(), FAKEDATATYPE));
        int expected = 4;
        Class[] expectedViolations = {CycleInDatatypeDefinition.class, CycleInDatatypeDefinition.class,
                UseOfBuiltInDatatypeInDatatypeDefinition.class, UseOfBuiltInDatatypeInDatatypeDefinition.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeDefinitionAxiom axiom)")
    public void shouldCreateViolationForOWLDatatypeDefinitionAxiomInOWL2DLProfileCycles() {
        OWLDatatype d = Datatype(IRI(START, "test"));
        declare(o, d, Integer(), Boolean(), FAKEDATATYPE);
        o.add(DatatypeDefinition(d, Boolean()), DatatypeDefinition(Boolean(), d), DatatypeDefinition(FAKEDATATYPE,
                Integer()), DatatypeDefinition(Integer(), FAKEDATATYPE));
        int expected = 9;
        Class[] expectedViolations = {CycleInDatatypeDefinition.class, CycleInDatatypeDefinition.class,
                CycleInDatatypeDefinition.class, CycleInDatatypeDefinition.class,
                UseOfBuiltInDatatypeInDatatypeDefinition.class, UseOfBuiltInDatatypeInDatatypeDefinition.class,
                UseOfBuiltInDatatypeInDatatypeDefinition.class, UseOfUnknownDatatype.class, UseOfUnknownDatatype.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectProperty property)")
    public void shouldCreateViolationForOWLObjectPropertyInOWL2DLProfile() {
        IRI iri = IRI(START, "test");
        declare(o, ObjectProperty(iri), DataProperty(iri), AnnotationProperty(iri));
        o.addAxiom(SubObjectPropertyOf(OP, ObjectProperty(iri)));
        int expected = 4;
        Class[] expectedViolations = {UseOfReservedVocabularyForObjectPropertyIRI.class,
                UseOfUndeclaredObjectProperty.class, IllegalPunning.class, IllegalPunning.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataProperty property)")
    public void shouldCreateViolationForOWLDataPropertyInOWL2DLProfile1() {
        declare(o, DataProperty(IRI(START, "fail")));
        int expected = 0;
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataProperty property)")
    public void shouldCreateViolationForOWLDataPropertyInOWL2DLProfile2() {
        o.addAxiom(FunctionalDataProperty(DATAP));
        int expected = 1;
        Class[] expectedViolations = {UseOfUndeclaredDataProperty.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataProperty property)")
    public void shouldCreateViolationForOWLDataPropertyInOWL2DLProfile3() {
        declare(o, DATAP, AnnotationProperty(DATAP.getIRI()));
        int expected = 0;
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataProperty property)")
    public void shouldCreateViolationForOWLDataPropertyInOWL2DLProfile4() {
        declare(o, DATAP, ObjectProperty(DATAP.getIRI()));
        int expected = 0;
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLAnnotationProperty property)")
    public void shouldCreateViolationForOWLAnnotationPropertyInOWL2DLProfile() {
        IRI iri = IRI(START, "test");
        declare(o, ObjectProperty(iri), DataProperty(iri), AnnotationProperty(iri));
        o.add(SubAnnotationPropertyOf(AnnotationProperty(IRI("urn:test#", "t")), AnnotationProperty(iri)));
        int expected = 4;
        Class[] expectedViolations = {UseOfReservedVocabularyForAnnotationPropertyIRI.class,
                UseOfUndeclaredAnnotationProperty.class, IllegalPunning.class, IllegalPunning.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLOntology ontology)")
    public void shouldCreateViolationForOWLOntologyInOWL2DLProfile() throws OWLOntologyCreationException {
        o = m.createOntology(new OWLOntologyID(Optional.of(IRI(START, "test")), Optional.of(IRI(START, "test1"))));
        int expected = 2;
        Class[] expectedViolations = {UseOfReservedVocabularyForOntologyIRI.class,
                UseOfReservedVocabularyForVersionIRI.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLClass desc)")
    public void shouldCreateViolationForOWLClassInOWL2DLProfile() {
        declare(o, Class(IRI(START, "test")), FAKEDATATYPE);
        o.add(ClassAssertion(Class(FAKEDATATYPE.getIRI()), AnonymousIndividual()));
        int expected = 2;
        Class[] expectedViolations = {UseOfUndeclaredClass.class, DatatypeIRIAlsoUsedAsClassIRI.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataOneOf node)")
    public void shouldCreateViolationForOWLDataOneOfInOWL2DLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataOneOf()));
        int expected = 1;
        Class[] expectedViolations = {EmptyOneOfAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataUnionOf node)")
    public void shouldCreateViolationForOWLDataUnionOfInOWL2DLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataUnionOf()));
        int expected = 1;
        Class[] expectedViolations = {InsufficientOperands.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataIntersectionOf node)")
    public void shouldCreateViolationForOWLDataIntersectionOfInOWL2DLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataIntersectionOf()));
        int expected = 1;
        Class[] expectedViolations = {InsufficientOperands.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectIntersectionOf node)")
    public void shouldCreateViolationForOWLObjectIntersectionOfInOWL2DLProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectIntersectionOf()));
        int expected = 1;
        Class[] expectedViolations = {InsufficientOperands.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectOneOf node)")
    public void shouldCreateViolationForOWLObjectOneOfInOWL2DLProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectOneOf()));
        int expected = 1;
        Class[] expectedViolations = {EmptyOneOfAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectUnionOf node)")
    public void shouldCreateViolationForOWLObjectUnionOfInOWL2DLProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectUnionOf()));
        int expected = 1;
        Class[] expectedViolations = {InsufficientOperands.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLEquivalentClassesAxiom node)")
    public void shouldCreateViolationForOWLEquivalentClassesAxiomInOWL2DLProfile() {
        declare(o, OP);
        o.add(EquivalentClasses());
        if (OWLManager.DEBUG_USE_OWL) {
            runAssert(o, Profiles.OWL2_DL, 1, Stream.of(InsufficientOperands.class).toArray(Class[]::new));
        } else {
            // ONT-API: no possibility to add axiom which has no any triples.
            Assert.assertTrue("Ontology is not valid...", Profiles.OWL2_DL.checkOntology(o).isInProfile());
        }
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointClassesAxiom node)")
    public void shouldCreateViolationForOWLDisjointClassesAxiomInOWL2DLProfile() {
        declare(o, OP);
        o.add(DisjointClasses());
        if (OWLManager.DEBUG_USE_OWL) {
            int expected = 1;
            Class[] expectedViolations = {InsufficientOperands.class};
            runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
        } else {
            // In ONT-API there is no possibility to add or read empty owl:disjointWith axiom.
            // Although it is possible to create section "_:x rdf:type owl:AllDisjointClasses. _:x owl:members rdf:nil.",
            // but it doesn't make sense and disabled in API. So nothing to check
            Assert.assertTrue("Ontology is not valid...", Profiles.OWL2_DL.checkOntology(o).isInProfile());
        }
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointUnionAxiom node)")
    public void shouldCreateViolationForOWLDisjointUnionAxiomInOWL2DLProfile() {
        declare(o, OP);
        OWLClass otherfakeclass = Class(IRI("urn:test#", "otherfakeclass"));
        declare(o, CL);
        declare(o, otherfakeclass);
        o.add(DisjointUnion(CL, otherfakeclass));
        int expected = 1;
        Class[] expectedViolations = {InsufficientOperands.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLEquivalentObjectPropertiesAxiom node)")
    public void shouldCreateViolationForOWLEquivalentObjectPropertiesAxiomInOWL2DLProfile() {
        o.add(EquivalentObjectProperties());
        if (OWLManager.DEBUG_USE_OWL) {
            int expected = 1;
            Class[] expectedViolations = {InsufficientPropertyExpressions.class};
            runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
        } else {
            // in ONT-API there is no possibility to add or read empty owl:equivalentProperty axiom
            Assert.assertTrue("Ontology is not valid...", Profiles.OWL2_DL.checkOntology(o).isInProfile());
        }
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointDataPropertiesAxiom node)")
    public void shouldCreateViolationForOWLDisjointDataPropertiesAxiomInOWL2DLProfile() {
        o.add(DisjointDataProperties());
        if (OWLManager.DEBUG_USE_OWL) {
            int expected = 1;
            Class[] expectedViolations = {InsufficientPropertyExpressions.class};
            runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
        } else {
            // In ONT-API there is no possibility to add or read empty owl:propertyDisjointWith axiom.
            // Although it is possible to create section "_:x rdf:type owl:AllDisjointProperties. _:x owl:members rdf:nil.",
            // but it doesn't make sense and disabled in API. So nothing to check
            Assert.assertTrue("Ontology is not valid...", Profiles.OWL2_DL.checkOntology(o).isInProfile());
        }
    }

    @Test
    @Tests(method = "public Object visit(OWLEquivalentDataPropertiesAxiom node)")
    public void shouldCreateViolationForOWLEquivalentDataPropertiesAxiomInOWL2DLProfile() {
        o.add(EquivalentDataProperties());
        if (OWLManager.DEBUG_USE_OWL) {
            runAssert(o, Profiles.OWL2_DL, 1, Stream.of(InsufficientPropertyExpressions.class).toArray(Class[]::new));
        } else {
            // in ONT-API there is no possibility to add or read empty owl:equivalentProperty axiom
            Assert.assertTrue("Ontology is not valid...", Profiles.OWL2_DL.checkOntology(o).isInProfile());
        }
    }

    @Test
    @Tests(method = "public Object visit(OWLHasKeyAxiom node)")
    public void shouldCreateViolationForOWLHasKeyAxiomInOWL2DLProfile() {
        declare(o, CL);
        o.add(HasKey(CL));
        int expected = 1;
        Class[] expectedViolations = {InsufficientPropertyExpressions.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSameIndividualAxiom node)")
    public void shouldCreateViolationForOWLSameIndividualAxiomInOWL2DLProfile() {
        o.add(SameIndividual());
        if (OWLManager.DEBUG_USE_OWL) {
            int expected = 1;
            Class[] expectedViolations = {InsufficientIndividuals.class};
            runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
        } else {
            // In ONT-API there is no possibility to add(or read) empty owl:sameAs axiom.
            Assert.assertTrue("Ontology is not valid...", Profiles.OWL2_DL.checkOntology(o).isInProfile());
        }
    }

    @Test
    @Tests(method = "public Object visit(OWLDifferentIndividualsAxiom node)")
    public void shouldCreateViolationForOWLDifferentIndividualsAxiomInOWL2DLProfile() {
        o.add(DifferentIndividuals());
        if (OWLManager.DEBUG_USE_OWL) {
            int expected = 1;
            Class[] expectedViolations = {InsufficientIndividuals.class};
            runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
        } else {
            // In ONT-API there is no possibility to add or read empty owl:differentFrom axiom.
            // Although it is possible to create section "_:x rdf:type owl:AllDifferent. _:x owl:members rdf:nil.",
            // it doesn't make sense and disabled in API. So nothing to check
            Assert.assertTrue("Ontology is not valid...", Profiles.OWL2_DL.checkOntology(o).isInProfile());
        }
    }

    @Test
    @Tests(method = "public Object visit(OWLNamedIndividual individual)")
    public void shouldCreateViolationForOWLNamedIndividualInOWL2DLProfile() {
        o.add(ClassAssertion(OWLThing(), NamedIndividual(IRI(START, "i"))));
        int expected = 1;
        Class[] expectedViolations = {UseOfReservedVocabularyForIndividualIRI.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSubDataPropertyOfAxiom axiom)")
    public void shouldCreateViolationForOWLSubDataPropertyOfAxiomInOWL2DLProfile() {
        o.add(SubDataPropertyOf(DF.getOWLTopDataProperty(), DF.getOWLTopDataProperty()));
        int expected = 1;
        Class[] expectedViolations = {UseOfTopDataPropertyAsSubPropertyInSubPropertyAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectMinCardinality desc)")
    public void shouldCreateViolationForOWLObjectMinCardinalityInOWL2DLProfile() {
        declare(o, OP, CL);
        o.add(TransitiveObjectProperty(OP), SubClassOf(CL, ObjectMinCardinality(1, OP, OWLThing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSimplePropertyInCardinalityRestriction.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectMaxCardinality desc)")
    public void shouldCreateViolationForOWLObjectMaxCardinalityInOWL2DLProfile() {
        declare(o, OP, CL);
        o.add(TransitiveObjectProperty(OP), SubClassOf(CL, ObjectMaxCardinality(1, OP, OWLThing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSimplePropertyInCardinalityRestriction.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectExactCardinality desc)")
    public void shouldCreateViolationForOWLObjectExactCardinalityInOWL2DLProfile() {
        declare(o, OP, CL);
        o.add(TransitiveObjectProperty(OP), SubClassOf(CL, ObjectExactCardinality(1, OP, OWLThing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSimplePropertyInCardinalityRestriction.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectHasSelf desc)")
    public void shouldCreateViolationForOWLObjectHasSelfInOWL2DLProfile() {
        declare(o, OP);
        o.add(TransitiveObjectProperty(OP), ObjectPropertyRange(OP, ObjectHasSelf(OP)));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSimplePropertyInObjectHasSelf.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLFunctionalObjectPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLFunctionalObjectPropertyAxiomInOWL2DLProfile() {
        declare(o, OP);
        o.add(TransitiveObjectProperty(OP), FunctionalObjectProperty(OP));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSimplePropertyInFunctionalPropertyAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLInverseFunctionalObjectPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLInverseFunctionalObjectPropertyAxiomInOWL2DLProfile() {
        declare(o, OP);
        o.add(TransitiveObjectProperty(OP), InverseFunctionalObjectProperty(OP));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSimplePropertyInInverseFunctionalObjectPropertyAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLIrreflexiveObjectPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLIrreflexiveObjectPropertyAxiomInOWL2DLProfile() {
        declare(o, OP);
        o.add(TransitiveObjectProperty(OP), IrreflexiveObjectProperty(OP));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSimplePropertyInIrreflexivePropertyAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLAsymmetricObjectPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLAsymmetricObjectPropertyAxiomInOWL2DLProfile() {
        declare(o, OP);
        o.add(TransitiveObjectProperty(OP), AsymmetricObjectProperty(OP));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSimplePropertyInAsymmetricObjectPropertyAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointObjectPropertiesAxiom axiom)")
    public void shouldCreateViolationForOWLDisjointObjectPropertiesAxiomInOWL2DLProfile() {
        declare(o, OP);
        o.add(TransitiveObjectProperty(OP), DisjointObjectProperties(OP));
        int expected = 2;
        Class[] expectedViolations = {InsufficientPropertyExpressions.class,
                UseOfNonSimplePropertyInDisjointPropertiesAxiom.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSubPropertyChainOfAxiom axiom)")
    public void shouldCreateViolationForOWLSubPropertyChainOfAxiomInOWL2DLProfile() {
        OWLObjectProperty op1 = ObjectProperty(IRI("urn:test#", "op"));
        declare(o, OP, op1);
        o.add(SubPropertyChainOf(Collections.singletonList(op1), OP), SubPropertyChainOf(Arrays.asList(OP, op1, OP), OP),
                SubPropertyChainOf(Arrays.asList(OP, op1), OP), SubPropertyChainOf(Arrays.asList(op1, OP, op1, OP), OP));
        int expected = 4;
        Class[] expectedViolations = {InsufficientPropertyExpressions.class, UseOfPropertyInChainCausesCycle.class,
                UseOfPropertyInChainCausesCycle.class, UseOfPropertyInChainCausesCycle.class};
        runAssert(o, Profiles.OWL2_DL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLOntology ont)")
    public void shouldCreateViolationForOWLOntologyInOWL2Profile() throws OWLOntologyCreationException {
        o = m.createOntology(new OWLOntologyID(Optional.of(IRI("test", "")), Optional.of(IRI("test1", ""))));
        int expected = 2;
        Class[] expectedViolations = {OntologyIRINotAbsolute.class, OntologyVersionIRINotAbsolute.class};
        runAssert(o, Profiles.OWL2_FULL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(IRI iri)")
    public void shouldCreateViolationForIRIInOWL2Profile() {
        declare(o, Class(IRI("test", "")));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonAbsoluteIRI.class};
        runAssert(o, Profiles.OWL2_FULL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLLiteral node)")
    public void shouldCreateViolationForOWLLiteralInOWL2Profile() {
        declare(o, DATAP);
        o.add(DataPropertyAssertion(DATAP, AnonymousIndividual(), Literal("wrong", OWL2Datatype.XSD_INTEGER)));
        int expected = 1;
        Class[] expectedViolations = {LexicalNotInLexicalSpace.class};
        runAssert(o, Profiles.OWL2_FULL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeRestriction node)")
    public void shouldCreateViolationForOWLDatatypeRestrictionInOWL2Profile() {
        declare(o, DATAP);
        o.add(DatatypeDefinition(Integer(), Boolean()), DATA_PROPERTY_RANGE2);
        int expected = 3;
        Class[] expectedViolations = {UseOfDefinedDatatypeInDatatypeRestriction.class,
                UseOfIllegalFacetRestriction.class, UseOfUndeclaredDatatype.class};
        runAssert(o, Profiles.OWL2_FULL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeDefinitionAxiom axiom)")
    public void shouldCreateViolationForOWLDatatypeDefinitionAxiomInOWL2Profile() {
        o.add(DatatypeDefinition(FAKEDATATYPE, Boolean()));
        int expected = 1;
        Class[] expectedViolations = {UseOfUndeclaredDatatype.class};
        runAssert(o, Profiles.OWL2_FULL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatype node)")
    public void shouldCreateViolationForOWLDatatypeInOWL2ELProfile() {
        declare(o, Boolean());
        int expected = 0;
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLAnonymousIndividual individual)")
    public void shouldCreateViolationForOWLAnonymousIndividualInOWL2ELProfile() {
        o.add(ClassAssertion(OWLThing(), DF.getOWLAnonymousIndividual()));
        int expected = 1;
        Class[] expectedViolations = {UseOfAnonymousIndividual.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectInverseOf property)")
    public void shouldCreateViolationForOWLObjectInverseOfInOWL2ELProfile() {
        declare(o, OP);
        o.add(SubObjectPropertyOf(OP, ObjectInverseOf(OP)));
        int expected = 1;
        Class[] expectedViolations = {UseOfObjectPropertyInverse.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataAllValuesFrom desc)")
    public void shouldCreateViolationForOWLDataAllValuesFromInOWL2ELProfile() {
        declare(o, DATAP, CL);
        o.add(SubClassOf(CL, DataAllValuesFrom(DATAP, Integer())));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataExactCardinality desc)")
    public void shouldCreateViolationForOWLDataExactCardinalityInOWL2ELProfile() {
        declare(o, DATAP, CL, Integer());
        o.add(SubClassOf(CL, DataExactCardinality(1, DATAP, Integer())));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataMaxCardinality desc)")
    public void shouldCreateViolationForOWLDataMaxCardinalityInOWL2ELProfile() {
        declare(o, DATAP, CL, Integer());
        o.add(SubClassOf(CL, DataMaxCardinality(1, DATAP, Integer())));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataMinCardinality desc)")
    public void shouldCreateViolationForOWLDataMinCardinalityInOWL2ELProfile() {
        declare(o, DATAP, CL, Integer());
        o.add(SubClassOf(CL, DataMinCardinality(1, DATAP, Integer())));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectAllValuesFrom desc)")
    public void shouldCreateViolationForOWLObjectAllValuesFromInOWL2ELProfile() {
        declare(o, OP, CL);
        o.add(SubClassOf(CL, ObjectAllValuesFrom(OP, OWLThing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectComplementOf desc)")
    public void shouldCreateViolationForOWLObjectComplementOfInOWL2ELProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectComplementOf(OWLNothing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectExactCardinality desc)")
    public void shouldCreateViolationForOWLObjectExactCardinalityInOWL2ELProfile() {
        declare(o, OP, CL);
        o.add(SubClassOf(CL, ObjectExactCardinality(1, OP, OWLThing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectMaxCardinality desc)")
    public void shouldCreateViolationForOWLObjectMaxCardinalityInOWL2ELProfile() {
        declare(o, OP, CL);
        o.add(SubClassOf(CL, ObjectMaxCardinality(1, OP, OWLThing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectMinCardinality desc)")
    public void shouldCreateViolationForOWLObjectMinCardinalityInOWL2ELProfile() {
        declare(o, OP, CL);
        o.add(SubClassOf(CL, ObjectMinCardinality(1, OP, OWLThing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectOneOf desc)")
    public void shouldCreateViolationForOWLObjectOneOfInOWL2ELProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectOneOf(NamedIndividual(IRI("urn:test#", "i1")), NamedIndividual(IRI(
                "urn:test#", "i2")))));
        int expected = 1;
        Class[] expectedViolations = {UseOfObjectOneOfWithMultipleIndividuals.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectUnionOf desc)")
    public void shouldCreateViolationForOWLObjectUnionOfInOWL2ELProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectUnionOf(OWLThing(), OWLNothing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalClassExpression.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataComplementOf node)")
    public void shouldCreateViolationForOWLDataComplementOfInOWL2ELProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataComplementOf(Double())));
        int expected = 2;
        Class[] expectedViolations = {UseOfIllegalDataRange.class, UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataOneOf node)")
    public void shouldCreateViolationForOWLDataOneOfInOWL2ELProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataOneOf(Literal(1), Literal(2))));
        int expected = 1;
        Class[] expectedViolations = {UseOfDataOneOfWithMultipleLiterals.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeRestriction node)")
    public void shouldCreateViolationForOWLDatatypeRestrictionInOWL2ELProfile() {
        declare(o, DATAP);
        o.add(DATA_PROPERTY_RANGE);
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataUnionOf node)")
    public void shouldCreateViolationForOWLDataUnionOfInOWL2ELProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataUnionOf(Double(), Integer())));
        int expected = 2;
        Class[] expectedViolations = {UseOfIllegalDataRange.class, UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLAsymmetricObjectPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLAsymmetricObjectPropertyAxiomInOWL2ELProfile() {
        declare(o, OP);
        o.add(AsymmetricObjectProperty(OP));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointDataPropertiesAxiom axiom)")
    public void shouldCreateViolationForOWLDisjointDataPropertiesAxiomInOWL2ELProfile() {
        OWLDataProperty dp = DataProperty(IRI("urn:test#", "other"));
        declare(o, DATAP, dp);
        o.add(DisjointDataProperties(DATAP, dp));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointObjectPropertiesAxiom axiom)")
    public void shouldCreateViolationForOWLDisjointObjectPropertiesAxiomInOWL2ELProfile() {
        OWLObjectProperty op1 = ObjectProperty(IRI("urn:test#", "test"));
        declare(o, OP, op1);
        o.add(DisjointObjectProperties(op1, OP));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointUnionAxiom axiom)")
    public void shouldCreateViolationForOWLDisjointUnionAxiomInOWL2ELProfile() {
        declare(o, CL);
        o.add(DisjointUnion(CL, OWLThing(), OWLNothing()));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLFunctionalObjectPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLFunctionalObjectPropertyAxiomInOWL2ELProfile() {
        declare(o, OP);
        o.add(FunctionalObjectProperty(OP));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLHasKeyAxiom axiom)")
    public void shouldCreateViolationForOWLHasKeyAxiomInOWL2ELProfile() {
        declare(o, CL, OP);
        o.add(HasKey(CL, OP));
        int expected = 0;
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLInverseFunctionalObjectPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLInverseFunctionalObjectPropertyAxiomInOWL2ELProfile() {
        declare(o, P);
        o.add(InverseFunctionalObjectProperty(P));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLInverseObjectPropertiesAxiom axiom)")
    public void shouldCreateViolationForOWLInverseObjectPropertiesAxiomInOWL2ELProfile() {
        declare(o, P);
        OWLObjectProperty p1 = ObjectProperty(IRI("urn:test#", "objectproperty"));
        declare(o, p1);
        o.add(InverseObjectProperties(P, p1));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLIrreflexiveObjectPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLIrreflexiveObjectPropertyAxiomInOWL2ELProfile() {
        declare(o, P);
        o.add(IrreflexiveObjectProperty(P));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSymmetricObjectPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLSymmetricObjectPropertyAxiomInOWL2ELProfile() {
        declare(o, P);
        o.add(SymmetricObjectProperty(P));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(SWRLRule rule)")
    public void shouldCreateViolationForSWRLRuleInOWL2ELProfile() {
        o.add(DF.getSWRLRule(new HashSet<>(), new HashSet<>()));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSubPropertyChainOfAxiom axiom)")
    public void shouldCreateViolationForOWLSubPropertyChainOfAxiomInOWL2ELProfile() {
        OWLObjectProperty op1 = ObjectProperty(IRI("urn:test#", "op1"));
        OWLObjectProperty op2 = ObjectProperty(IRI("urn:test#", "op"));
        declare(o, op1, OP, op2, CL);
        o.add(ObjectPropertyRange(OP, CL));
        List<OWLObjectProperty> asList = Arrays.asList(op2, op1);
        o.add(SubPropertyChainOf(asList, OP));
        int expected = 1;
        Class[] expectedViolations = {LastPropertyInChainNotInImposedRange.class};
        runAssert(o, Profiles.OWL2_EL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatype node)")
    public void shouldCreateViolationForOWLDatatypeInOWL2QLProfile() {
        declare(o, FAKEDATATYPE);
        int expected = 0;
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLAnonymousIndividual individual)")
    public void shouldCreateViolationForOWLAnonymousIndividualInOWL2QLProfile() {
        o.add(ClassAssertion(OWLThing(), DF.getOWLAnonymousIndividual()));
        int expected = 1;
        Class[] expectedViolations = {UseOfAnonymousIndividual.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLHasKeyAxiom axiom)")
    public void shouldCreateViolationForOWLHasKeyAxiomInOWL2QLProfile() {
        declare(o, CL, OP);
        o.add(HasKey(CL, OP));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSubClassOfAxiom axiom)")
    public void shouldCreateViolationForOWLSubClassOfAxiomInOWL2QLProfile() {
        declare(o, OP);
        o.add(SubClassOf(ObjectComplementOf(OWLNothing()), ObjectUnionOf(OWLThing(), OWLNothing())));
        int expected = 2;
        Class[] expectedViolations = {UseOfNonSubClassExpression.class, UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLEquivalentClassesAxiom axiom)")
    public void shouldCreateViolationForOWLEquivalentClassesAxiomInOWL2QLProfile() {
        o.add(EquivalentClasses(ObjectUnionOf(OWLNothing(), OWLThing()), OWLNothing()));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSubClassExpression.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointClassesAxiom axiom)")
    public void shouldCreateViolationForOWLDisjointClassesAxiomInOWL2QLProfile() {
        o.add(DisjointClasses(ObjectComplementOf(OWLThing()), OWLThing()));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSubClassExpression.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectPropertyDomainAxiom axiom)")
    public void shouldCreateViolationForOWLObjectPropertyDomainAxiomInOWL2QLProfile() {
        declare(o, OP);
        o.add(ObjectPropertyDomain(OP, ObjectUnionOf(OWLNothing(), OWLThing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectPropertyRangeAxiom axiom)")
    public void shouldCreateViolationForOWLObjectPropertyRangeAxiomInOWL2QLProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectUnionOf(OWLNothing(), OWLThing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSubPropertyChainOfAxiom axiom)")
    public void shouldCreateViolationForOWLSubPropertyChainOfAxiomInOWL2QLProfile() {
        OWLObjectProperty op1 = ObjectProperty(IRI("urn:test#", "op"));
        declare(o, OP, op1);
        o.add(SubPropertyChainOf(Arrays.asList(OP, op1), OP));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLFunctionalObjectPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLFunctionalObjectPropertyAxiomInOWL2QLProfile() {
        declare(o, OP);
        o.add(FunctionalObjectProperty(OP));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLInverseFunctionalObjectPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLInverseFunctionalObjectPropertyAxiomInOWL2QLProfile() {
        declare(o, OP);
        o.add(InverseFunctionalObjectProperty(OP));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLTransitiveObjectPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLTransitiveObjectPropertyAxiomInOWL2QLProfile() {
        declare(o, OP);
        o.add(TransitiveObjectProperty(OP));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLFunctionalDataPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLFunctionalDataPropertyAxiomInOWL2QLProfile() {
        declare(o, DATAP);
        o.add(FunctionalDataProperty(DATAP));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataPropertyDomainAxiom axiom)")
    public void shouldCreateViolationForOWLDataPropertyDomainAxiomInOWL2QLProfile() {
        declare(o, DATAP, OP);
        o.add(DataPropertyDomain(DATAP, ObjectMaxCardinality(1, OP, OWLNothing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLClassAssertionAxiom axiom)")
    public void shouldCreateViolationForOWLClassAssertionAxiomInOWL2QLProfile() {
        OWLNamedIndividual i = NamedIndividual(IRI("urn:test#", "i"));
        declare(o, OP, i);
        o.add(ClassAssertion(ObjectSomeValuesFrom(OP, OWLThing()), i));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonAtomicClassExpression.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSameIndividualAxiom axiom)")
    public void shouldCreateViolationForOWLSameIndividualAxiomInOWL2QLProfile() {
        o.add(SameIndividual(NamedIndividual(IRI("urn:test#", "individual1")), NamedIndividual(IRI("urn:test#",
                "individual2"))));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLNegativeObjectPropertyAssertionAxiom axiom)")
    public void shouldCreateViolationForOWLNegativeObjectPropertyAssertionAxiomInOWL2QLProfile() {
        declare(o, OP);
        OWLNamedIndividual i = NamedIndividual(IRI("urn:test#", "i"));
        OWLNamedIndividual i1 = NamedIndividual(IRI("urn:test#", "i"));
        declare(o, i, i1);
        o.add(NegativeObjectPropertyAssertion(OP, i, i1));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLNegativeDataPropertyAssertionAxiom axiom)")
    public void shouldCreateViolationForOWLNegativeDataPropertyAssertionAxiomInOWL2QLProfile() {
        declare(o, DATAP);
        OWLNamedIndividual i = NamedIndividual(IRI("urn:test#", "i"));
        declare(o, i);
        o.add(NegativeDataPropertyAssertion(DATAP, i, Literal(1)));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointUnionAxiom axiom)")
    public void shouldCreateViolationForOWLDisjointUnionAxiomInOWL2QLProfile() {
        declare(o, CL);
        o.add(DisjointUnion(CL, OWLThing(), OWLNothing()));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLIrreflexiveObjectPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLIrreflexiveObjectPropertyAxiomInOWL2QLProfile() {
        declare(o, OP);
        o.add(IrreflexiveObjectProperty(OP));
        int expected = 0;
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(SWRLRule rule)")
    public void shouldCreateViolationForSWRLRuleInOWL2QLProfile() {
        o.add(DF.getSWRLRule(new HashSet<>(), new HashSet<>()));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataComplementOf node)")
    public void shouldCreateViolationForOWLDataComplementOfInOWL2QLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataComplementOf(Integer())));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataOneOf node)")
    public void shouldCreateViolationForOWLDataOneOfInOWL2QLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataOneOf(Literal(1), Literal(2))));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeRestriction node)")
    public void shouldCreateViolationForOWLDatatypeRestrictionInOWL2QLProfile() {
        declare(o, DATAP);
        o.add(DATA_PROPERTY_RANGE);
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataUnionOf node)")
    public void shouldCreateViolationForOWLDataUnionOfInOWL2QLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataUnionOf(Integer(), Boolean())));
        int expected = 2;
        Class[] expectedViolations = {UseOfIllegalDataRange.class, UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_QL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLClassAssertionAxiom axiom)")
    public void shouldCreateViolationForOWLClassAssertionAxiomInOWL2RLProfile() {
        declare(o, OP);
        o.add(ClassAssertion(ObjectMinCardinality(1, OP, OWLThing()), NamedIndividual(IRI("urn:test#", "i"))));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataPropertyDomainAxiom axiom)")
    public void shouldCreateViolationForOWLDataPropertyDomainAxiomInOWL2RLProfile() {
        declare(o, DATAP, OP);
        o.add(DataPropertyDomain(DATAP, ObjectMinCardinality(1, OP, OWLThing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointClassesAxiom axiom)")
    public void shouldCreateViolationForOWLDisjointClassesAxiomInOWL2RLProfile() {
        o.add(DisjointClasses(ObjectComplementOf(OWLThing()), OWLThing()));
        int expected = 2;
        Class[] expectedViolations = {UseOfNonSubClassExpression.class, UseOfNonSubClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointDataPropertiesAxiom axiom)")
    public void shouldCreateViolationForOWLDisjointDataPropertiesAxiomInOWL2RLProfile() {
        OWLDataProperty dp = DataProperty(IRI("urn:test#", "dproperty"));
        declare(o, DATAP, dp);
        o.add(DisjointDataProperties(DATAP, dp));
        int expected = 0;
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDisjointUnionAxiom axiom)")
    public void shouldCreateViolationForOWLDisjointUnionAxiomInOWL2RLProfile() {
        declare(o, CL);
        o.add(DisjointUnion(CL, OWLThing(), OWLNothing()));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLEquivalentClassesAxiom axiom)")
    public void shouldCreateViolationForOWLEquivalentClassesAxiomInOWL2RLProfile() {
        o.add(EquivalentClasses(ObjectComplementOf(OWLThing()), OWLNothing()));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonEquivalentClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLEquivalentDataPropertiesAxiom axiom)")
    public void shouldCreateViolationForOWLEquivalentDataPropertiesAxiomInOWL2RLProfile() {
        OWLDataProperty dp = DataProperty(IRI("urn:test#", "test"));
        declare(o, DATAP, dp);
        o.add(EquivalentDataProperties(DATAP, dp));
        int expected = 0;
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLFunctionalDataPropertyAxiom axiom)")
    public void shouldCreateViolationForOWLFunctionalDataPropertyAxiomInOWL2RLProfile() {
        declare(o, DATAP);
        o.add(FunctionalDataProperty(DATAP));
        int expected = 0;
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLHasKeyAxiom axiom)")
    public void shouldCreateViolationForOWLHasKeyAxiomInOWL2RLProfile() {
        declare(o, CL, OP);
        o.add(HasKey(ObjectComplementOf(CL), OP));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSubClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectPropertyDomainAxiom axiom)")
    public void shouldCreateViolationForOWLObjectPropertyDomainAxiomInOWL2RLProfile() {
        declare(o, OP, OP);
        o.add(ObjectPropertyDomain(OP, ObjectMinCardinality(1, OP, OWLThing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLObjectPropertyRangeAxiom axiom)")
    public void shouldCreateViolationForOWLObjectPropertyRangeAxiomInOWL2RLProfile() {
        declare(o, OP);
        o.add(ObjectPropertyRange(OP, ObjectMinCardinality(1, OP, OWLThing())));
        int expected = 1;
        Class[] expectedViolations = {UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLSubClassOfAxiom axiom)")
    public void shouldCreateViolationForOWLSubClassOfAxiomInOWL2RLProfile() {
        o.add(SubClassOf(ObjectComplementOf(OWLThing()), ObjectOneOf(NamedIndividual(IRI("urn:test#", "test")))));
        int expected = 2;
        Class[] expectedViolations = {UseOfNonSubClassExpression.class, UseOfNonSuperClassExpression.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(SWRLRule rule)")
    public void shouldCreateViolationForSWRLRuleInOWL2RLProfile() {
        o.add(DF.getSWRLRule(new HashSet<>(), new HashSet<>()));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalAxiom.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataComplementOf node)")
    public void shouldCreateViolationForOWLDataComplementOfInOWL2RLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataComplementOf(Integer())));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataIntersectionOf node)")
    public void shouldCreateViolationForOWLDataIntersectionOfInOWL2RLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataIntersectionOf(Integer(), Boolean())));
        int expected = 0;
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataOneOf node)")
    public void shouldCreateViolationForOWLDataOneOfInOWL2RLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataOneOf(Literal(1), Literal(2))));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatype node)")
    public void shouldCreateViolationForOWLDatatypeInOWL2RLProfile() {
        declare(o, Datatype(IRI("urn:test#", "test")));
        int expected = 0;
        Class[] expectedViolations = {};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeRestriction node)")
    public void shouldCreateViolationForOWLDatatypeRestrictionInOWL2RLProfile() {
        declare(o, DATAP);
        o.add(DATA_PROPERTY_RANGE);
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDataUnionOf node)")
    public void shouldCreateViolationForOWLDataUnionOfInOWL2RLProfile() {
        declare(o, DATAP);
        o.add(DataPropertyRange(DATAP, DataUnionOf(Double(), Integer())));
        int expected = 1;
        Class[] expectedViolations = {UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }

    @Test
    @Tests(method = "public Object visit(OWLDatatypeDefinitionAxiom axiom)")
    public void shouldCreateViolationForOWLDatatypeDefinitionAxiomInOWL2RLProfile() {
        OWLDatatype datatype = Datatype(IRI("urn:test#", "datatype"));
        declare(o, datatype);
        o.add(DatatypeDefinition(datatype, Boolean()));
        int expected = 2;
        Class[] expectedViolations = {UseOfIllegalAxiom.class, UseOfIllegalDataRange.class};
        runAssert(o, Profiles.OWL2_RL, expected, expectedViolations);
    }
}
