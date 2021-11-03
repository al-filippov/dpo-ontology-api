package ontology;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.swrlapi.exceptions.SWRLBuiltInException;
import org.swrlapi.factory.SWRLAPIFactory;
import org.swrlapi.parser.SWRLParseException;
import org.swrlapi.sqwrl.SQWRLQueryEngine;
import org.swrlapi.sqwrl.exceptions.SQWRLException;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplDouble;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplString;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class MainApp {
    private static final String OWL_THING = "owl:Thing";
    private final IRI ontologyIRI = IRI.create("http://ontology/test");
    private final OWLOntologyManager manager;
    private final OWLOntology ontology;
    private final OWLDataFactory dataFactory;
    private final SQWRLQueryEngine queryEngine;

    MainApp() throws OWLOntologyCreationException {
        final OWLOntologyID ontologyID = new OWLOntologyID(
                com.google.common.base.Optional.of(ontologyIRI),
                com.google.common.base.Optional.absent());
        final IRI documentIRI = ontologyID.getDefaultDocumentIRI().get();

        manager = OWLManager.createOWLOntologyManager();
        manager.getIRIMappers().add(new SimpleIRIMapper(documentIRI, ontologyIRI));
        ontology = manager.createOntology(ontologyID);
        dataFactory = manager.getOWLDataFactory();
        manager.getOntologyFormat(ontology).asPrefixOWLOntologyFormat().setDefaultPrefix(ontologyIRI + "#");
        queryEngine = SWRLAPIFactory.createSQWRLQueryEngine(ontology, SWRLAPIFactory.createIRIResolver());
    }

    public void save() throws FileNotFoundException, OWLOntologyStorageException {
        ontology.saveOntology(
                new OWLXMLDocumentFormat(),
                new FileOutputStream("./ontology.owl"));
    }

    private String format(String iri) {
        return iri.trim().replace(" ", "_");
    }

    private IRI getEntityFullIri(String entityName) throws URISyntaxException {
        final String fragment = format(entityName);
        String base = format(ontologyIRI.toString());
        if (!base.endsWith("#") && !base.endsWith("/")) {
            base += "#";
        }
        return IRI.create(new URI(base + fragment));
    }

    public void createClass(String name) throws URISyntaxException {
        final IRI classIri = getEntityFullIri(name);
        final OWLClass newClass = dataFactory.getOWLClass(classIri);
        final OWLDeclarationAxiom declarationAxiom =
                dataFactory.getOWLDeclarationAxiom(newClass);
        manager.addAxiom(ontology, declarationAxiom);
    }

    public void createDataProperty(String name, String domainClass, OWL2Datatype dataType) throws URISyntaxException {
        final IRI propertyIri = getEntityFullIri(name);
        final OWLDataProperty newProperty = dataFactory.getOWLDataProperty(propertyIri);
        final OWLDeclarationAxiom declarationAxiom =
                dataFactory.getOWLDeclarationAxiom(newProperty);
        manager.addAxiom(ontology, declarationAxiom);

        if (domainClass.equalsIgnoreCase(OWL_THING)) {
            final OWLDataPropertyDomainAxiom domainAxiom =
                    dataFactory.getOWLDataPropertyDomainAxiom(newProperty, dataFactory.getOWLThing());
            manager.addAxiom(ontology, domainAxiom);
        } else {
            final IRI domainClassIri = getEntityFullIri(domainClass);
            final OWLClass innerDomainClass = dataFactory.getOWLClass(domainClassIri);
            final OWLDataPropertyDomainAxiom domainAxiom =
                    dataFactory.getOWLDataPropertyDomainAxiom(newProperty, innerDomainClass);
            manager.addAxiom(ontology, domainAxiom);
        }

        final OWLDatatype rangeDatatype = dataType.getDatatype(dataFactory);
        final OWLDataPropertyRangeAxiom rangeAxiom =
                dataFactory.getOWLDataPropertyRangeAxiom(newProperty, rangeDatatype);
        manager.addAxiom(ontology, rangeAxiom);
    }

    public void createObjectProperty(String name, String domainClass, String rangeClass) throws URISyntaxException {
        final IRI propertyIri = getEntityFullIri(name);
        final OWLObjectProperty newProperty = dataFactory.getOWLObjectProperty(propertyIri);
        final OWLDeclarationAxiom declarationAxiom =
                dataFactory.getOWLDeclarationAxiom(newProperty);
        manager.addAxiom(ontology, declarationAxiom);

        final IRI domainClassIri = getEntityFullIri(domainClass);
        final OWLClass innerDomainClass = dataFactory.getOWLClass(domainClassIri);
        final OWLObjectPropertyDomainAxiom domainAxiom =
                dataFactory.getOWLObjectPropertyDomainAxiom(newProperty, innerDomainClass);
        manager.addAxiom(ontology, domainAxiom);

        final IRI rangeClassIri = getEntityFullIri(rangeClass);
        final OWLClass innerRangeClass = dataFactory.getOWLClass(rangeClassIri);
        final OWLObjectPropertyRangeAxiom rangeAxiom =
                dataFactory.getOWLObjectPropertyRangeAxiom(newProperty, innerRangeClass);
        manager.addAxiom(ontology, rangeAxiom);
    }

    public void createIndividual(String name, String owlClass) throws URISyntaxException {
        final IRI individualIri = getEntityFullIri(name);
        final OWLNamedIndividual newIndividual = dataFactory.getOWLNamedIndividual(individualIri);
        final OWLDeclarationAxiom declarationAxiom =
                dataFactory.getOWLDeclarationAxiom(newIndividual);
        manager.addAxiom(ontology, declarationAxiom);

        final IRI classIri = getEntityFullIri(owlClass);
        final OWLClass innerClass = dataFactory.getOWLClass(classIri);
        final OWLClassAssertionAxiom classAssertionAxiom =
                dataFactory.getOWLClassAssertionAxiom(innerClass, newIndividual);
        manager.addAxiom(ontology, classAssertionAxiom);
    }

    public void createObjectPropertyAssertion(String domainIndividual, String objectProperty, String rangeIndividual)
            throws URISyntaxException {
        final IRI individualIri = getEntityFullIri(domainIndividual);
        final OWLNamedIndividual individual = dataFactory.getOWLNamedIndividual(individualIri);
        final IRI propertyIri = getEntityFullIri(objectProperty);
        final OWLObjectProperty property = dataFactory.getOWLObjectProperty(propertyIri);
        final IRI valueIndividualIri = getEntityFullIri(rangeIndividual);
        final OWLNamedIndividual valueIndividual = dataFactory.getOWLNamedIndividual(valueIndividualIri);
        final OWLObjectPropertyAssertionAxiom axiom =
                dataFactory.getOWLObjectPropertyAssertionAxiom(property, individual, valueIndividual);
        manager.addAxiom(ontology, axiom);
    }

    private OWLLiteral getLiteralFromObject(Object value) {
        if (value instanceof Double) {
            return new OWLLiteralImplDouble((Double) value,
                    OWL2Datatype.XSD_DOUBLE.getDatatype(dataFactory));
        }
        if (value instanceof String) {
            return new OWLLiteralImplString(value.toString());
        }
        return null;
    }

    public void createDataPropertyAssertion(String domainIndividual, String dataProperty, Object rangeIndividual)
            throws URISyntaxException {
        final IRI individualIri = getEntityFullIri(domainIndividual);
        final OWLNamedIndividual individual = dataFactory.getOWLNamedIndividual(individualIri);
        final IRI propertyIri = getEntityFullIri(dataProperty);
        final OWLDataProperty property = dataFactory.getOWLDataProperty(propertyIri);
        final OWLLiteral valueLiteral = getLiteralFromObject(rangeIndividual);
        final OWLDataPropertyAssertionAxiom axiom =
                dataFactory.getOWLDataPropertyAssertionAxiom(property, individual, valueLiteral);
        manager.addAxiom(ontology, axiom);
    }

    public void createSwrlRule(String name, String rule) throws SWRLParseException, SWRLBuiltInException {
        queryEngine.createSWRLRule(name, rule);
    }

    public void createSqwrlQuery(String name, String query) throws SWRLParseException, SQWRLException {
        queryEngine.createSQWRLQuery(name, query);
    }

    public String executeSqwrlQuery(String name) throws SQWRLException {
        return queryEngine.runSQWRLQuery(name).toString();
    }

    public static void main(String[] args) throws Exception {
        final MainApp mainApp = new MainApp();
        final String indicator = "Indicator";
        final String label = "Label";
        mainApp.createClass(indicator);
        mainApp.createClass(label);
        final String hasName = "hasName";
        mainApp.createDataProperty(hasName, OWL_THING, OWL2Datatype.XSD_STRING);
        final String hasValue = "hasValue";
        mainApp.createDataProperty(hasValue, indicator, OWL2Datatype.XSD_DOUBLE);
        final String hasDescription = "hasDescription";
        mainApp.createDataProperty(hasDescription, label, OWL2Datatype.XSD_STRING);
        final String hasLabel = "hasLabel";
        mainApp.createObjectProperty(hasLabel, indicator, label);
        final String indicator1 = "Indicator1";
        mainApp.createIndividual(indicator1, indicator);
        final String label1 = "Label1";
        mainApp.createIndividual(label1, label);
        mainApp.createDataPropertyAssertion(indicator1, hasName, "Индикатор1");
        mainApp.createDataPropertyAssertion(indicator1, hasValue, 10.0d);
        mainApp.createDataPropertyAssertion(label1, hasName, "Метка1");
        mainApp.createDataPropertyAssertion(label1, hasDescription, "Метка для значения показателя");
        mainApp.createSwrlRule("S1",
                "Indicator(?i) ^ hasValue(?i, ?v) ^ swrlb:greaterThan(?v, 9) ^ Label(?l) -> hasLabel(?i, ?l)");
        final String query = "S2";
        mainApp.createSqwrlQuery(query,
                "Indicator(?i) ^ hasLabel(?i, ?l) -> sqwrl:select(?i, ?l)");
        System.out.println(mainApp.executeSqwrlQuery(query));
        mainApp.save();
    }
}
