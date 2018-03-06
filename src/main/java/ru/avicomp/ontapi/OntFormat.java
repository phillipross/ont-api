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

package ru.avicomp.ontapi;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParserRegistry;
import org.apache.jena.riot.RDFWriterRegistry;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import ru.avicomp.ontapi.OWLLangRegistry.LangKey;
import ru.avicomp.ontapi.OWLLangRegistry.OWLLang;

import javax.annotation.Nullable;
import java.io.Writer;
import java.util.*;
import java.util.stream.Stream;

/**
 * The map between jena languages ({@link Lang}) and OWL-API syntax formats ({@link OWLLang}).
 * There are 22 ONT formats (22(19 actual, i.e. without intersection) OWL document formats + 15(11 actual) jena languages),
 * but only 12 of them can be used without any hesitation (see {@link #isSupported()} for more details).
 * For working with the OWL-API interfaces the {@link #createOwlFormat()} method can be used.
 * OWL-API formats are located inside <a href='https://github.com/owlcs/owlapi/tree/version5/api'>owlapi-api</a>,
 * <a href='https://github.com/owlcs/owlapi/tree/version5/rio'>owlapi-rio</a>,
 * <a href='https://github.com/owlcs/owlapi/tree/version5/parsers'>owlapi-parsers</a> and
 * <a href='https://github.com/owlcs/owlapi/tree/version5/oboformat'>owlapi-obiformat</a> modules,
 * which may absent in dependencies.
 * Jena formats are located inside <a href='https://github.com/apache/jena/tree/master/jena-arq'>jena-arq</a> and
 * <a href='https://github.com/apache/jena/tree/master/jena-csv'>jena-csv</a>.
 * Note: on loading Apache Jena has more priority than OWL-API, in other cases the enum order are used.
 * <p>
 * Created by @szuev on 27.09.2016.
 */
public enum OntFormat {
    TURTLE("Turtle", "ttl", Arrays.asList(Lang.TURTLE, Lang.TTL, Lang.N3), // n3 is treated as turtle
            Arrays.asList(LangKey.TURTLE, LangKey.RIOTURTLE, LangKey.N3)),
    RDF_XML("RDF/XML", "rdf", Collections.singletonList(Lang.RDFXML), Arrays.asList(LangKey.RDFXML, LangKey.RIORDFXML)),
    // json has more priority since json-ld can't be read as json, but json can be as json-ld
    RDF_JSON("RDF/JSON", "rj", Lang.RDFJSON, LangKey.RDFJSON),
    JSON_LD("JSON-LD", "jsonld", Lang.JSONLD, LangKey.RDFJSONLD),

    NTRIPLES("N-Triples", "nt", Arrays.asList(Lang.NTRIPLES, Lang.NT), Collections.singletonList(LangKey.NTRIPLES)),
    NQUADS("N-Quads", "nq", Arrays.asList(Lang.NQUADS, Lang.NQ), Collections.singletonList(LangKey.NQUADS)),
    TRIG("TriG", "trig", Lang.TRIG, LangKey.TRIG),
    TRIX("TriX", "trix", Lang.TRIX, LangKey.TRIX),
    // jena only:
    RDF_THRIFT("RDF-THRIFT", "trdf", Lang.RDFTHRIFT, null),
    CSV("CSV", "csv", Lang.CSV, null),
    TSV("TSV", "tsv", Lang.TSV, null),
    // owl-api formats only
    OWL_XML("OWL/XML", "owl", null, LangKey.OWLXML),
    MANCHESTER_SYNTAX("ManchesterSyntax", "omn", null, LangKey.MANCHESTERSYNTAX),
    FUNCTIONAL_SYNTAX("FunctionalSyntax", "fss", null, LangKey.FUNCTIONALSYNTAX),
    BINARY_RDF("BinaryRDF", "brf", null, LangKey.BINARYRDF),
    RDFA("RDFA", "xhtml", null, LangKey.RDFA),
    OBO("OBO", "obo", null, LangKey.OBO),
    KRSS("KRSS", "krss", null, LangKey.KRSS),
    KRSS2("KRSS2", "krss2", null, LangKey.KRSS2),
    DL("DL", "dl", null, LangKey.DLSYNTAX),
    DL_HTML("DL/HTML", "html", null, LangKey.DLSYNTAXHTML),
    LATEX("LATEX", "tex", null, LangKey.LATEX),;

    private final String id;
    private String ext;
    private final List<Lang> jenaLangs;
    private final List<LangKey> owlTypes;

    /**
     * The main constructor.
     *
     * @param id   String, unique id or "short name".
     * @param ext  String, primary extension.
     * @param jena List of jena language objects (i.e {@link Lang})
     * @param owl  List of owl language keys {@link LangKey}
     */
    OntFormat(String id, String ext, List<Lang> jena, List<LangKey> owl) {
        this.id = OntApiException.notNull(id, "Id is required.");
        this.ext = ext;
        this.jenaLangs = Collections.unmodifiableList(jena);
        this.owlTypes = Collections.unmodifiableList(owl);
    }

    OntFormat(String id, String ext, Lang jena, LangKey owl) {
        this(id, ext,
                jena == null ? Collections.emptyList() : Collections.singletonList(jena),
                owl == null ? Collections.emptyList() : Collections.singletonList(owl));
    }

    /**
     * Returns the format identifier, which can be considered as syntax short name.
     * Could be used in Jena model as language tip to read and write
     * (see e.g. {@link org.apache.jena.rdf.model.Model#read(String, String)}, {@link org.apache.jena.rdf.model.Model#write(Writer, String)})
     *
     * @return String
     */
    public String getID() {
        return id;
    }

    /**
     * The primary file extension.
     * There is no direct usage in the API, so it's for convenience only.
     *
     * @return String.
     */
    public String getExt() {
        return ext;
    }

    /**
     * Returns the primary jena language (first alias from the list)
     *
     * @return {@link Lang} or null.
     * @see OntFormat#getOWLLang()
     */
    @Nullable
    public Lang getLang() {
        return jenaLangs.size() == 0 ? null : jenaLangs.get(0);
    }

    /**
     * Gets all jena languages associated with this type in the form of Stream.
     * Technically it is the stream of aliases, i.e. it does no matter what to use.
     *
     * @return Stream of {@link Lang}, could be empty.
     */
    public Stream<Lang> jenaLangs() {
        return jenaLangs.stream();
    }

    /**
     * Chooses the most suitable owl language details container:
     * it should have associated storer and parser factories or at least one of them.
     *
     * @return {@link OWLLangRegistry.OWLLang} or null.
     * @see OntFormat#getLang()
     */
    public OWLLangRegistry.OWLLang getOWLLang() {
        return owlLangs().min(Comparator.comparingInt(OntFormat::computeOrder)).orElse(null);
    }

    private static int computeOrder(OWLLang d) {
        int res = 1;
        if (d.isWritable()) res -= 1;
        if (d.isReadable()) res -= 1;
        return res;
    }

    /**
     * Gets all OWL-API language keys associated with this enum-type instance in the form of Stream.
     *
     * @return Stream of {@link LangKey}s, could be empty.
     */
    public Stream<LangKey> owlKeys() {
        return owlTypes.stream();
    }

    /**
     * Gets all registered OWL-langs in form of LangDetails Stream.
     *
     * @return Stream of {@link OWLLang}
     */
    public Stream<OWLLang> owlLangs() {
        return owlKeys().map(k -> OWLLangRegistry.getLang(k.getKey()))
                .filter(Optional::isPresent).map(Optional::get);
    }


    /**
     * Creates new instance of {@link OWLDocumentFormat} by type setting.
     * If there is no any ready to use owl-document format then the 'fake' format ({@link SimpleDocumentFormat})
     * with reference to this enum-instance will be returned.
     *
     * @return {@link OWLDocumentFormat}, no null.
     * @throws OntApiException if something wrong.
     */
    public OWLDocumentFormat createOwlFormat() {
        OWLLang res = getOWLLang();
        if (res == null) return new SimpleDocumentFormat();
        return res.getFormatFactory().get();
    }

    /**
     * Returns {@code true} if format is good for using by ONT-API without any restrictions both to read and write.
     * <b>Note: this method has an advisory character and reflects the current state of ONT-API.</b>
     * Even if the format is considered 'no good', and it is supported by OWL-API, usually it is still possible use it by the native mechanisms.
     * The list of reasons why these formats require special attention:
     * <ul>
     * <li>CSV (Jena lang: {@link Lang#CSV}) is not a valid Jena RDF serialization format, it is only for SPARQL results.
     * But it is possible to use it for reading csv files. Need to add jena-csv to class-path.
     * For more details see <a href='http://jena.apache.org/documentation/csv/'>jena-csv documentation</a>.
     * <b>Be warned: it is very tolerant format: almost any text file could be treated as csv.</b></li>
     * <li>TSV (Jena lang:{@link Lang#TSV}) is used by Jena for result sets, not RDF syntax. It was added here for completeness only.</li>
     * <li>RDFA (OWL-API lang: {@link LangKey#RDFA}) is available only for reading (it has no {@link org.semanticweb.owlapi.model.OWLStorerFactory storer factory} in OWL-API 5.1.4 supply).</li>
     * <li>LATEX (OWL-API lang: {@link LangKey#LATEX}) is available only for writing (it has no {@link org.semanticweb.owlapi.io.OWLParserFactory parser factory} in OWL-API 5.1.4 supply).</li>
     * <li>DL_HTML (OWL-API lang: {@link LangKey#DLSYNTAXHTML}) is available only for writing (it has no {@link org.semanticweb.owlapi.io.OWLParserFactory parser factory} in OWL-API 5.1.4 supply).</li>
     * <li>KRSS (OWL-API lang: {@link LangKey#KRSS}) is excluded in standard OWL-API (5.1.4) supply</li>
     * <li>KRSS2 (OWL-API lang: {@link LangKey#KRSS2}) does not pass reload test, i.e. the list of significant axioms do not match after sequential saving and loading.</li>
     * <li>DL (OWL-API lang: {@link LangKey#DLSYNTAX}) does not pass reload test.</li>
     * <li>OBO (OWL-API lang: {@link LangKey#OBO}) does not pass reload test.</li>
     * </ul>
     * The test ontology for last three formats:
     * <pre>
     * &#064;prefix test:  &lt;http://test.org/&gt; .
     * &#064;prefix owl:   &lt;http://www.w3.org/2002/07/owl#&gt; .
     * &#064;prefix rdfs:  &lt;http://www.w3.org/2000/01/rdf-schema#&gt; .
     * test:simple  a  owl:Ontology .
     * test:class1  a  owl:Class .
     * test:class2  a           owl:Class ;
     *         rdfs:subClassOf  test:class1 .
     * test:individual  a  test:class1 , owl:NamedIndividual .
     * </pre>
     *
     * @return false if format is considered dangerous and requires special attention.
     * @see OWLLangRegistry.LangKey
     * @see #isReadSupported()
     * @see #isWriteSupported()
     */
    public boolean isSupported() {
        return isWriteSupported() && isReadSupported() && isNoneOf(OBO, DL, KRSS2);
    }

    /**
     * Answers iff this format supports read operation.
     * @return true if reading is possible through OWL-API or Jena.
     * @see #isSupported()
     */
    public boolean isReadSupported() {
        return jenaLangs().anyMatch(RDFParserRegistry::isRegistered) ||
                owlLangs().anyMatch(OWLLang::isReadable);
    }

    /**
     * Answers iff this format supports write operation.
     * @return true if writing is possible through OWL-API or Jena.
     * @see #isSupported()
     */
    public boolean isWriteSupported() {
        return jenaLangs().anyMatch(RDFWriterRegistry::contains) ||
                owlLangs().anyMatch(OWLLangRegistry.OWLLang::isWritable);
    }

    /**
     * Answers iff this format is provided by jena.
     *
     * @return boolean
     */
    public boolean isJena() {
        return !jenaLangs.isEmpty();
    }

    /**
     * Answers iff this format is provided by owl-api.
     * @return boolean
     */
    public boolean isOWL() {
        return !owlTypes.isEmpty();
    }

    /**
     * Answers iff this format is provided by jena only, i.e. no owl-api support.
     * @return boolean
     */
    public boolean isJenaOnly() {
        return isJena() && !isOWL();
    }

    /**
     * Answers iff this format is provided by owl-api, i.e. no jena-support.
     * @return boolean
     */
    public boolean isOWLOnly() {
        return isOWL() && !isJena();
    }

    /**
     * Answers iff it is XML.
     * Now there are only two xml formats: RDF/XML and OWL/XML
     *
     * @return true if one of them.
     */
    public boolean isXML() {
        return isOneOf(RDF_XML, OWL_XML);
    }

    /**
     * Answers iff it is JSON.
     * Now there are only two json formats: RDF/JSON and JSONLD
     *
     * @return true if one of them.
     */
    public boolean isJSON() {
        return isOneOf(RDF_JSON, JSON_LD);
    }

    private boolean isOneOf(OntFormat... formats) {
        return Stream.of(formats).anyMatch(this::equals);
    }

    private boolean isNoneOf(OntFormat... formats) {
        return Stream.of(formats).noneMatch(this::equals);
    }

    /**
     * Returns all formats as stream.
     * @return Stream of formats.
     */
    public static Stream<OntFormat> formats() {
        return Stream.of(values());
    }

    /**
     * Finds OntFormat by instance of OWLDocumentFormat.
     *
     * @param owlFormat {@link OWLDocumentFormat} instance
     * @return OntFormat or null
     * @throws NullPointerException in case of wrong argument
     */
    public static OntFormat get(OWLDocumentFormat owlFormat) {
        Class<? extends OWLDocumentFormat> type = OntApiException.notNull(owlFormat, "Null owl-document-format specified.").getClass();
        return SimpleDocumentFormat.class.equals(type) ? get(owlFormat.getKey()) : get(type);
    }

    /**
     * Finds OntFormat by class of OWLDocumentFormat.
     *
     * @param type {@link OWLDocumentFormat} class type
     * @return OntFormat or null
     * @throws NullPointerException in case of wrong argument
     */
    public static OntFormat get(Class<? extends OWLDocumentFormat> type) {
        String key = OntApiException.notNull(type, "Null owl-document-format class specified.").getName();
        for (OntFormat f : values()) {
            if (f.owlKeys().map(LangKey::getKey).anyMatch(key::equals)) return f;
        }
        return null;
    }

    /**
     * Finds OntFormat by jena-lang.
     *
     * @param lang {@link Lang}
     * @return OntFormat or null
     * @throws NullPointerException in case of wrong argument
     */
    public static OntFormat get(Lang lang) {
        OntApiException.notNull(lang, "Null jena-language specified.");
        for (OntFormat r : values()) {
            if (r.jenaLangs().anyMatch(lang::equals)) return r;
        }
        return null;
    }

    /**
     * Finds OntFormat by identifier.
     *
     * @param id String, the id of format
     * @return OntFormat or null
     * @throws NullPointerException in case of wrong argument
     * @see OntFormat#getID()
     */
    public static OntFormat get(String id) {
        OntApiException.notNull(id, "Null ont-format id specified.");
        for (OntFormat r : values()) {
            if (Objects.equals(r.id, id)) return r;
        }
        return null;
    }

    /**
     * Implementation of {@link OWLDocumentFormat} attached to this enum-type.
     */
    public class SimpleDocumentFormat extends org.semanticweb.owlapi.formats.AbstractRDFPrefixDocumentFormat {
        @Override
        public String getKey() {
            return id;
        }
    }
}
