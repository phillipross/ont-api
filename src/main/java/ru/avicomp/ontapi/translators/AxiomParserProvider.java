package ru.avicomp.ontapi.translators;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;

import com.google.common.reflect.ClassPath;
import ru.avicomp.ontapi.OntApiException;

/**
 * Axiom Graph Translator loader.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public abstract class AxiomParserProvider {
    private static final Logger LOGGER = Logger.getLogger(AxiomParserProvider.class);

    public static Map<AxiomType, AxiomTranslator> getParsers() {
        return ParserHolder.PARSERS;
    }

    public static <T extends OWLAxiom> AxiomTranslator<T> get(T axiom) {
        return get(OntApiException.notNull(axiom, "Null axiom.").getAxiomType());
    }

    @SuppressWarnings("unchecked")
    public static <T extends OWLAxiom> AxiomTranslator<T> get(AxiomType<?> type) {
        return OntApiException.notNull(getParsers().get(OntApiException.notNull(type, "Null axiom type")), "Cam't find parser for axiom " + type.getActualClass());
    }

    private static class ParserHolder {
        private static final Map<AxiomType, AxiomTranslator> PARSERS = init();

        static {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("There are following axiom-parsers (" + PARSERS.size() + "): ");
                PARSERS.forEach((type, parser) -> LOGGER.debug(type + " ::: " + parser.getClass()));
            }
        }

        private static Map<AxiomType, AxiomTranslator> init() {
            Map<AxiomType, AxiomTranslator> res = new HashMap<>();
            Set<Class<? extends AxiomTranslator>> parserClasses = collectParserClasses();
            AxiomType.AXIOM_TYPES.forEach(type -> {
                Class<? extends AxiomTranslator> parserClass = findParserClass(parserClasses, type);
                try {
                    res.put(type, parserClass.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new OntApiException("Can't instance parser for type: " + type, e);
                }
            });
            return res;
        }

        private static Class<? extends AxiomTranslator> findParserClass(Set<Class<? extends AxiomTranslator>> classes, AxiomType<? extends OWLAxiom> type) {
            return classes.stream()
                    .filter(p -> isRelatedToAxiom(p, type.getActualClass()))
                    .findFirst()
                    .orElseThrow(() -> new OntApiException("Can't find parser class for type " + type));
        }

        private static boolean isRelatedToAxiom(Class<? extends AxiomTranslator> parserClass, Class<? extends OWLAxiom> actualClass) {
            ParameterizedType type = ((ParameterizedType) parserClass.getGenericSuperclass());
            if (type == null) return false;
            for (Type t : type.getActualTypeArguments()) {
                if (actualClass.getName().equals(t.getTypeName())) return true;
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        private static Set<Class<? extends AxiomTranslator>> collectParserClasses() {
            try {
                Set<ClassPath.ClassInfo> classes = ClassPath.from(Thread.currentThread().getContextClassLoader()).getTopLevelClasses(AxiomTranslator.class.getPackage().getName());
                Stream<Class> res = classes.stream().map(ParserHolder::parserClass).filter(c -> !Modifier.isAbstract(c.getModifiers())).filter(AxiomTranslator.class::isAssignableFrom);
                return res.map((Function<Class, Class<? extends AxiomTranslator>>) c -> c).collect(Collectors.toSet());
            } catch (IOException e) {
                throw new OntApiException("Can't collect parsers classes", e);
            }
        }

        private static Class parserClass(ClassPath.ClassInfo info) {
            try {
                return Class.forName(info.getName());
            } catch (ClassNotFoundException e) {
                throw new OntApiException("Can't find class " + info, e);
            }
        }
    }

}