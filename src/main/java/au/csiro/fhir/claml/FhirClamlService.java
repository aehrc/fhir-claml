package au.csiro.fhir.claml;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemHierarchyMeaning;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionDesignationComponent;
import org.hl7.fhir.r4.model.CodeSystem.ConceptPropertyComponent;
import org.hl7.fhir.r4.model.CodeSystem.PropertyComponent;
import org.hl7.fhir.r4.model.CodeSystem.PropertyType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.exceptions.FHIRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import au.csiro.fhir.claml.model.claml.ClaML;
import au.csiro.fhir.claml.model.claml.Class;
import au.csiro.fhir.claml.model.claml.ClassKind;
import au.csiro.fhir.claml.model.claml.Fragment;
import au.csiro.fhir.claml.model.claml.Identifier;
import au.csiro.fhir.claml.model.claml.Label;
import au.csiro.fhir.claml.model.claml.ListItem;
import au.csiro.fhir.claml.model.claml.Para;
import au.csiro.fhir.claml.model.claml.Reference;
import au.csiro.fhir.claml.model.claml.Rubric;
import au.csiro.fhir.claml.model.claml.RubricKind;
import au.csiro.fhir.claml.model.claml.SubClass;
import au.csiro.fhir.claml.model.claml.SuperClass;
import au.csiro.fhir.claml.model.claml.Term;
import au.csiro.fhir.claml.model.claml.Title;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;

@Service
public class FhirClamlService {

	  @Autowired
	  private FhirContext context;
	  

	
    private static final Logger log = LoggerFactory.getLogger(FhirClamlService.class);

    String claml2fhir(File clamlFile,
            String displayRubric,
            String fallBackDisplayRubric,
            String definitionRubric,
            List<String> designationRubrics,
            List<String> excludeClassKind,
            Boolean excludeKindlessClasses,
            String hierarchyMeaning,
            String id,
            String url,
            String valueSet,
            String content,
            File output) throws DataFormatException, IOException, ParserConfigurationException, SAXException {

    	// Default values
    	if (displayRubric == null) {
    		displayRubric = "preferred";
    	}
    	if (definitionRubric == null) {
    		definitionRubric = "definition";
    	}
    	if (hierarchyMeaning == null) {
    		hierarchyMeaning = "is-a";
    	}
    	if (content == null) {
    		content = "complete";
    	}
    	
    	
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ClaML.class);

            if (designationRubrics == null) {
                designationRubrics = Collections.emptyList();
            }
            if (excludeClassKind == null) {
                excludeClassKind = Collections.emptyList();
            }
            
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            spf.setFeature("http://xml.org/sax/features/validation", false);

            XMLReader xmlReader = spf.newSAXParser().getXMLReader();
            InputSource inputSource = new InputSource(
                    new FileReader(clamlFile));
            SAXSource source = new SAXSource(xmlReader, inputSource);
            
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            ClaML claml = (ClaML) jaxbUnmarshaller.unmarshal(source);

            CodeSystem cs = new CodeSystem();
            cs.setStatus(PublicationStatus.DRAFT);
            cs.setExperimental(true);
            try {
                cs.setContent(CodeSystemContentMode.fromCode(content));
                cs.setHierarchyMeaning(CodeSystemHierarchyMeaning.fromCode(hierarchyMeaning));
            } catch (FHIRException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }  //TODO

            if (id != null) {
                cs.setId(id);
            }

            if (url != null) {
                cs.setUrl(url);
            }

            if (valueSet != null) {
                cs.setValueSet(valueSet);
            }

            if (claml.getIdentifier().size() > 1) {
                log.warn("Multiple identifiers not currently supported by FHIR Code Systems");
            }
			for  (Identifier ident : claml.getIdentifier()) {
                cs.addIdentifier(new org.hl7.fhir.r4.model.Identifier().setSystem(ident.getAuthority()).setValue(ident.getUid()));
            }

            Title title = claml.getTitle();
            if (title != null) {
                if (title.getVersion() != null) {
                    cs.setVersion(title.getVersion());
                }
                if (title.getName() != null) {
                    cs.setName(title.getName());
                    cs.setDescription(title.getContent());
                } else {
                    cs.setName(title.getContent());
                    cs.setDescription(title.getContent());
                }
//                if (title.getDate() != null) {
//                    try {
//                        cs.setDate(DateFormat.getDateInstance().parse(title.getDate()));
//                    } catch (ParseException e) {
//                        e.printStackTrace();
//                    }
//                }
            }

            cs.addProperty().setCode("kind").setType(PropertyType.CODE);
            for (RubricKind rk : claml.getRubricKinds().getRubricKind()) {
                if (! definitionRubric.equals(rk.getName()) &&
                        !displayRubric.equals(rk.getName()) &&
                        !designationRubrics.contains(rk.getName())) {
                    PropertyComponent p = cs.addProperty();
                    p.setCode(rk.getName());
                    p.setType(PropertyType.STRING);
                    if (rk.getDisplay() != null && !rk.getDisplay().isEmpty()) {
                        if (rk.getDisplay().size() > 1) {
                            log.warn("Found more than one display for rubric kind " + rk.getName() + ": ignoring additional displays");
                        }
                        p.setDescription(rk.getDisplay().get(0).getContent());
                    }
                    //TODO filters?
                }
                if (rk.isInherited()) {
                    log.warn("Inherited rubric kinds are not fully supported: " + rk.getName());
                }
            }

            int count = 0;

            for (Class c : claml.getClazz()) {
                if (c.getKind() != null && excludeClassKind.contains(getClassKindName(c.getKind()))) {
                    log.info("Concept " + c.getCode() + " has excluded kind " + getClassKindName(c.getKind()) + ": skipping");
                    continue;
                }
                ConceptDefinitionComponent concept = cs.addConcept().setCode(c.getCode());
                count++;
                if (c.getKind() != null) {
                    if (getClassKindName(c.getKind()) != null) {
                        concept.addProperty().setCode("kind").setValue(new CodeType(getClassKindName(c.getKind())));
                   } else {
                        log.warn("Unrecognised class kind on class " + c.getCode() + ": " + c.getKind());
                    }
                } else if (excludeKindlessClasses) {
                    log.info("Concept " + c.getCode() + " has excluded kind " + getClassKindName(c.getKind()) + ": skipping");
                    continue;
                } else {
                    log.info("Concept " + c.getCode() + " has no kind.");
                }
                for (SubClass sub : c.getSubClass()) {
                    concept.addProperty().setCode("child").setValue(new CodeType(sub.getCode()));
                }
                for (SuperClass sup : c.getSuperClass()) {
                    concept.addProperty().setCode("parent").setValue(new CodeType(sup.getCode()));
                }
                for (Rubric rubric : c.getRubric()) {
                    Object kind = rubric.getKind();
                    if (kind instanceof RubricKind) {
                        RubricKind rkind = (RubricKind) kind;
                        if (rkind.getName().equals(displayRubric)) {
                            final String value;
                            if (rubric.getLabel().size() > 1) {
                                log.warn("Found more than one label on display rubric for code " + c.getCode());
                            }
                            value = getLabelValue(rubric.getLabel().get(0));
                            concept.setDisplay(value);

                        } else if (fallBackDisplayRubric != null && rkind.getName().equals(fallBackDisplayRubric)) {
                            final String value;
                            if (rubric.getLabel().size() > 1) {
                                log.warn("Found more than one label on display rubric for code " + c.getCode());
                            }
                            log.info("display: "+ displayRubric + "not found. Falling back to: " + fallBackDisplayRubric);
                            value = getLabelValue(rubric.getLabel().get(0));
                            concept.setDisplay(value);

                        } else if (rkind.getName().equals(definitionRubric)) {
                                final String value;
                                if (rubric.getLabel().size() > 1) {
                                    log.warn("Found more than one label on definition rubric for code " + c.getCode());
                                }
                                value = getLabelValue(rubric.getLabel().get(0));
                                concept.setDefinition(value);

                        } else if (designationRubrics.contains(rkind.getName())) {
                            for (Label l : rubric.getLabel()) {
                                String v = getLabelValue(l);
                                if (v != null && v.length() > 0) {
                                    ConceptDefinitionDesignationComponent desig = concept.addDesignation();
                                    desig.setUse(new Coding().setCode(rkind.getName()));
                                    desig.setValue(v);
                                    desig.setLanguage(l.getLang());
                                } else {
                                    log.warn("Skipping empty label for rubric " + rubric.getId());
                                }
                            }
                        } else {
                            for (Label l : rubric.getLabel()) {
                                String v = getLabelValue(l);
                                if (v != null && v.length() > 0) {
                                    ConceptPropertyComponent prop = concept.addProperty();
                                    prop.setCode(rkind.getName());
                                    prop.setValue(new StringType(v));
                                }
                            }
                        }
                    } else {
                        log.warn("Unexpected rubric kind " + kind);
                    }
                }

                if (!concept.hasCode()) {
                    log.warn("Concept " + concept + " has no code!");
                } else {
                    if (!concept.hasDisplay()) {
                        log.warn("Concept " + concept.getCode() + " has no display text. Using code as display text");
                        concept.setDisplay(concept.getCode());
                        if (!concept.hasDefinition()) {
                            concept.setDefinition(concept.getCode());
                        }
                    } else if (!concept.hasDefinition()) {
                        concept.setDefinition(concept.getDisplay());
                    }
                }
            }

            cs.setCount(count);

            context.newJsonParser().encodeResourceToWriter(cs, new FileWriter(output));


        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        return "";

    }

    private String getClassKindName(Object kind) {
        if (kind instanceof String) {
            return (String) kind;
        } else if (kind instanceof ClassKind) {
            return ((ClassKind) kind).getName();
        } else {
            log.warn("Unrecognized class kind:" + kind);
            return null;
        }
    }

    private String getLabelValue(Object l) {
        if (l instanceof Label) {
            String result = "";
            for (Object cont : ((Label) l).getContent()) {
                result += getLabelValue(cont);
            }
            return result;
        } else if (l instanceof String) { // This is a hack, check all contents?
            return (String) l;
        } else if (l instanceof Reference) {
            return "[" + ((Reference) l).getContent() + "]";
        } else if (l instanceof Para) {
            String result = "";
            for (Object cont : ((Para) l).getContent()) {
                result += getLabelValue(cont);
            }
            return result;
        } else if (l instanceof Fragment) {
            String result = "";
            for (Object cont : ((Fragment) l).getContent()) {
                result += getLabelValue(cont);
            }
            return result;
        } else if (l instanceof Term) {
            Term term = (Term) l;
            if (term.getClazz().equals("tab")) {
                return "\t";
            } else if (term.getClazz().equals("subscript")) {
                return "_" + term.getContent();
            } else if (term.getClazz().equals("italics")) {
                return term.getContent();
            } else if (term.getClazz().equals("bold")) {
                return term.getContent();
            } else {
                log.warn("Unrecognized Term class:" + term.getClazz());
                return term.getContent();
            }
        } else if (l instanceof au.csiro.fhir.claml.model.claml.List) {
            String result = "";
            for (ListItem item : ((au.csiro.fhir.claml.model.claml.List) l).getListItem()) {
                result += " - ";
                for (Object cont : item.getContent()) {
                    result += getLabelValue(cont);
                }
                result += "\n";
            }
            return result;
        } else {
            log.warn("Ignoring non-String label contents on Label (" + l.getClass().getSimpleName() + ")");
            return l.getClass().getSimpleName().toUpperCase();
        }
    }

}