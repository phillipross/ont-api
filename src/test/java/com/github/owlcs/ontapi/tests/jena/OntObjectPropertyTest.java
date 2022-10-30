/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests.jena;

import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntObjectProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class OntObjectPropertyTest {

    @Test
    public void testObjectPropertyDomainsAndRanges() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass.Named c = m.createOntClass("C");
        OntObjectProperty.Named p = m.createObjectProperty("O");
        Assertions.assertNotNull(p.addRangeStatement(m.getOWLThing()));
        Assertions.assertNotNull(p.addDomainStatement(m.getOWLNothing()));
        Assertions.assertSame(p, p.addDomain(m.getOWLThing()).addRange(m.getOWLNothing()).addDomain(c));
        Assertions.assertEquals(2, p.ranges().count());
        Assertions.assertEquals(3, p.domains().count());

        Assertions.assertSame(p, p.removeDomain(m.getOWLThing()).removeRange(m.getOWLNothing()));
        Assertions.assertEquals(1, p.ranges().count());
        Assertions.assertEquals(2, p.domains().count());

        p.removeRange(null).removeDomain(null);
        Assertions.assertEquals(2, m.size());
    }

    @Test
    public void testObjectSuperProperties() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty.Named p1 = m.createObjectProperty("O");
        OntObjectProperty.Named p2 = m.createObjectProperty("P");
        Assertions.assertNotNull(p1.addSubPropertyOfStatement(m.getOWLBottomObjectProperty()));
        Assertions.assertSame(p1, p1.addSuperProperty(m.getOWLTopObjectProperty())
                .addSuperProperty(p2));
        Assertions.assertEquals(3, p1.superProperties().count());

        Assertions.assertSame(p1, p1.removeSuperProperty(m.getOWLThing()).removeSuperProperty(m.getOWLTopObjectProperty()));
        Assertions.assertEquals(2, p1.superProperties().count());
        p1.removeSuperProperty(null);
        Assertions.assertEquals(0, p1.superProperties().count());
    }

    @Test
    public void testObjectPropertyAdditionalDeclarations() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty.Named p = m.createObjectProperty("P");
        Assertions.assertNotNull(p.addFunctionalDeclaration().getSubject(OntObjectProperty.class)
                .addInverseFunctionalDeclaration().getSubject(OntObjectProperty.class)
                .addAsymmetricDeclaration().getSubject(OntObjectProperty.class)
                .addSymmetricDeclaration().getSubject(OntObjectProperty.class)
                .addReflexiveDeclaration().getSubject(OntObjectProperty.class)
                .addIrreflexiveDeclaration().getSubject(OntObjectProperty.class)
                .addTransitiveDeclaration().getSubject(OntObjectProperty.class));

        Assertions.assertTrue(p.isFunctional());
        Assertions.assertTrue(p.isInverseFunctional());
        Assertions.assertTrue(p.isSymmetric());
        Assertions.assertTrue(p.isAsymmetric());
        Assertions.assertTrue(p.isReflexive());
        Assertions.assertTrue(p.isIrreflexive());
        Assertions.assertTrue(p.isTransitive());

        Assertions.assertSame(p, p.setFunctional(false)
                .setInverseFunctional(false)
                .setAsymmetric(false)
                .setSymmetric(false)
                .setIrreflexive(false)
                .setReflexive(false)
                .setTransitive(false));
        Assertions.assertEquals(1, m.size());

        Assertions.assertSame(p, p.setFunctional(true)
                .setInverseFunctional(true)
                .setAsymmetric(true)
                .setSymmetric(true)
                .setIrreflexive(true)
                .setReflexive(true)
                .setTransitive(true));
        Assertions.assertEquals(8, m.size());
    }

    @Test
    public void testPropertyChains() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty.Named p = m.createObjectProperty("P");
        OntObjectProperty.Named p1 = m.createObjectProperty("P1");
        OntObjectProperty.Named p2 = m.createObjectProperty("P2");
        Assertions.assertNotNull(p.addPropertyChainAxiomStatement());
        Assertions.assertSame(p, p.addPropertyChain());
        Assertions.assertEquals(0, p.fromPropertyChain().count());
        Assertions.assertSame(p, p.addPropertyChain(Arrays.asList(p1, p1)).addPropertyChain(Arrays.asList(p2, p2)));
        Assertions.assertEquals(2, p.fromPropertyChain().count());
    }

    @Test
    public void testObjectPropertyInverseOf() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty.Named a = m.createObjectProperty("A");
        OntObjectProperty.Named b = m.createObjectProperty("B");
        OntObjectProperty.Named c = m.createObjectProperty("C");
        Assertions.assertNotNull(a.addInverseOfStatement(b));
        Assertions.assertEquals(b, a.findInverseProperty().orElseThrow(AssertionError::new));
        Assertions.assertEquals(1, a.inverseProperties().count());
        Assertions.assertSame(c, c.addInverseProperty(b).addInverseProperty(a));
        Assertions.assertEquals(2, c.inverseProperties().count());
        Assertions.assertSame(c, c.removeInverseProperty(c).removeInverseProperty(b));
        Assertions.assertEquals(1, c.inverseProperties().count());
        Assertions.assertSame(a, a.removeInverseProperty(null));
        Assertions.assertEquals(4, m.size());
    }

    @Test
    public void testObjectPropertyEquivalentProperties() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty.Named a = m.createObjectProperty("A");
        OntObjectProperty.Named b = m.createObjectProperty("B");
        OntObjectProperty.Named c = m.createObjectProperty("C");
        Assertions.assertNotNull(a.addEquivalentPropertyStatement(b));
        Assertions.assertSame(a, a.addEquivalentProperty(c).addEquivalentProperty(m.getOWLTopObjectProperty()));
        Assertions.assertEquals(3, a.equivalentProperties().count());
        Assertions.assertSame(a, a.removeEquivalentProperty(b).removeEquivalentProperty(m.getOWLThing()));
        Assertions.assertEquals(2, a.equivalentProperties().count());
        Assertions.assertSame(a, a.removeEquivalentProperty(null));
        Assertions.assertEquals(3, m.size());
    }

    @Test
    public void testObjectPropertyDisjointProperties() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty.Named a = m.createObjectProperty("A");
        OntObjectProperty.Named b = m.createObjectProperty("B");
        OntObjectProperty.Named c = m.createObjectProperty("C");
        Assertions.assertNotNull(a.addPropertyDisjointWithStatement(b));
        Assertions.assertSame(a, a.addDisjointProperty(c).addDisjointProperty(m.getOWLTopObjectProperty()));
        Assertions.assertEquals(3, a.disjointProperties().count());
        Assertions.assertSame(a, a.removeDisjointProperty(b).removeDisjointProperty(m.getOWLThing()));
        Assertions.assertEquals(2, a.disjointProperties().count());
        Assertions.assertSame(a, a.removeDisjointProperty(null));
        Assertions.assertEquals(3, m.size());
    }

    @Test
    public void testListDisjoints() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty p1 = m.createObjectProperty("op1");
        OntObjectProperty p2 = m.createObjectProperty("op2").createInverse();
        OntObjectProperty p3 = m.createObjectProperty("op3");
        OntObjectProperty p4 = m.createObjectProperty("op4");
        m.createDisjointObjectProperties(p1, p2);
        m.createDisjointObjectProperties(p1, p3);

        Assertions.assertEquals(0, p4.disjoints().count());
        Assertions.assertEquals(2, p1.disjoints().count());
        Assertions.assertEquals(1, p2.disjoints().count());
        Assertions.assertEquals(1, p3.disjoints().count());
    }

}