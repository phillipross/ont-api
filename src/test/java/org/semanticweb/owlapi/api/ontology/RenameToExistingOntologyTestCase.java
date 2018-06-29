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
 */
package org.semanticweb.owlapi.api.ontology;

import org.junit.Test;
import org.semanticweb.owlapi.api.baseclasses.TestBase;
import org.semanticweb.owlapi.model.*;

import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.IRI;
import static org.semanticweb.owlapi.util.OWLAPIPreconditions.emptyOptional;
import static org.semanticweb.owlapi.util.OWLAPIPreconditions.optional;

/**
 * @author Matthew Horridge, The University of Manchester, Information
 *         Management Group
 * @since 3.0.0
 */
@SuppressWarnings("javadoc")
public class RenameToExistingOntologyTestCase extends TestBase {

    @Test(expected = OWLOntologyRenameException.class)
    public void testRenameToExistingOntology() throws OWLOntologyCreationException {
        IRI ontologyAIRI = IRI("http://www.semanticweb.org/ontologies/", "ontologyA");
        OWLOntology onto = getOWLOntology(ontologyAIRI);
        onto.add(df.getOWLDeclarationAxiom(Class(IRI("urn:test:", "testclass"))));
        IRI ontologyBIRI = IRI("http://www.semanticweb.org/ontologies/", "ontologyB");
        OWLOntology ontologyB = getOWLOntology(ontologyBIRI);
        ontologyB.applyChange(new SetOntologyID(ontologyB, new OWLOntologyID(optional(ontologyAIRI), emptyOptional(
                IRI.class))));
    }
}