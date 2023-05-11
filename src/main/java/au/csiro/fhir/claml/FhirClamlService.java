package au.csiro.fhir.claml;


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.hl7.fhir.exceptions.FHIRException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import au.csiro.fhir.claml.model.claml.ClaML;
import au.csiro.fhir.claml.model.claml.Class;
import au.csiro.fhir.claml.model.claml.ClassKind;
import au.csiro.fhir.claml.model.claml.ExcludeModifier;
import au.csiro.fhir.claml.model.claml.Fragment;
import au.csiro.fhir.claml.model.claml.Identifier;
import au.csiro.fhir.claml.model.claml.Label;
import au.csiro.fhir.claml.model.claml.ListItem;
import au.csiro.fhir.claml.model.claml.Meta;
import au.csiro.fhir.claml.model.claml.ModifiedBy;
import au.csiro.fhir.claml.model.claml.Modifier;
import au.csiro.fhir.claml.model.claml.ModifierClass;
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
                      List<String> displayRubrics,
                      String definitionRubric,
                      List<String> designationRubrics,
                      List<String> excludeClassKind,
                      Boolean excludeKindlessClasses,
                      String hierarchyMeaning,
                      String id,
                      String url,
                      String valueSet,
                      String content,
                      boolean versionNeeded,
                      Boolean applyModifiers,
                      File output) throws DataFormatException, IOException, ParserConfigurationException, SAXException {

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

            CodeSystem cs = claml2FhirObject(claml, displayRubrics, definitionRubric, designationRubrics, excludeClassKind,
                    excludeKindlessClasses, hierarchyMeaning, id, url, valueSet, content, versionNeeded, applyModifiers);

            if (output.getParentFile() != null) {
            	output.getParentFile().mkdirs();
            }
            context.newJsonParser().encodeResourceToWriter(cs, new FileWriter(output));


        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        return "";

    }

    protected CodeSystem claml2FhirObject(ClaML claml, List<String> displayRubrics, String definitionRubric,
            List<String> designationRubrics, List<String> excludeClassKind, Boolean excludeKindlessClasses,
            String hierarchyMeaning, String id, String url, String valueSet, String content,
            boolean versionNeeded, Boolean applyModifiers) {
        
        // Default values
        if (displayRubrics == null || displayRubrics.isEmpty()) {
            displayRubrics = new ArrayList<String>();
            displayRubrics.add("preferred");
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
        cs.setVersionNeeded(versionNeeded);

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
                    !displayRubrics.contains(rk.getName()) &&
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

        Integer count = 0;
        
        Map<String,ConceptDefinitionComponent> concepts = new HashMap<>();
        Map<String,List<ModifiedBy>> modifiedBy = new HashMap<>();
        Map<String,Set<ExcludeModifier>> excludeModifiers = new HashMap<>();
        Map<String,Set<String>> descendents = new HashMap<>();

        for (Class c : claml.getClazz()) {
            if (c.getKind() != null && excludeClassKind.contains(getClassKindName(c.getKind()))) {
                log.info("Concept " + c.getCode() + " has excluded kind " + getClassKindName(c.getKind()) + ": skipping");
                continue;
            }
            if (concepts.containsKey(c.getCode()) ) {
                log.error("A concept already exists with code " + c);
            }
            ConceptDefinitionComponent concept = cs.addConcept().setCode(c.getCode());
            concepts.put(c.getCode(), concept);
            modifiedBy.put(c.getCode(),  new ArrayList<>());
            if (!descendents.containsKey(c.getCode()) ) {
                descendents.put(c.getCode(), new HashSet<>());
            }
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
                descendents.get(c.getCode()).add(sub.getCode());
                if (descendents.containsKey(sub.getCode()) && !descendents.get(sub.getCode()).isEmpty()) {
                    descendents.get(c.getCode()).addAll(descendents.get(sub.getCode()));
                }
            }
            for (SuperClass sup : c.getSuperClass()) {
                concept.addProperty().setCode("parent").setValue(new CodeType(sup.getCode()));
                if (!descendents.containsKey(sup.getCode())) {
                    descendents.put(sup.getCode(), new HashSet<>());
                }
                descendents.get(sup.getCode()).add(c.getCode());
                if (!descendents.get(c.getCode()).isEmpty()) {
                    descendents.get(sup.getCode()).addAll(descendents.get(c.getCode()));
                }
            }
            Map<String,List<Rubric>> displayRubricValues = new HashMap<>();
            for (Rubric rubric : c.getRubric()) {
                Object kind = rubric.getKind();
                if (kind instanceof RubricKind) {
                    RubricKind rkind = (RubricKind) kind;
                    if (displayRubrics.contains(rkind.getName())) {
                        final String value;
                        if (rubric.getLabel().size() > 1) {
                            log.warn("Found more than one label on display rubric " + rkind.getName() + " for code " + c.getCode());
                        }
                        value = getLabelValue(rubric.getLabel().get(0)).trim();
                        if (!displayRubricValues.containsKey(rkind.getName())) {
                        	displayRubricValues.put(rkind.getName(), new ArrayList<>());
                        }
                        displayRubricValues.get(rkind.getName()).add(rubric);
                    } else if (rkind.getName().equals(definitionRubric)) {
                            final String value;
                            if (rubric.getLabel().size() > 1) {
                                log.warn("Found more than one label on definition rubric for code " + c.getCode());
                            }
                            value = getLabelValue(rubric.getLabel().get(0)).trim();
                            concept.setDefinition(value);

                    } else if (designationRubrics.contains(rkind.getName())) {
                        addDesignationsForRubric(concept, rubric);
                    } else {
                        for (Label l : rubric.getLabel()) {
                            String v = getLabelValue(l).trim();
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
            
            for (String dr : displayRubrics) {
            	if (!displayRubricValues.containsKey(dr)) {
            		continue;
            	}
        		List<Rubric> values = displayRubricValues.get(dr);
            	if (!concept.hasDisplay()) {
            		if (values.size() > 1) {
            			log.warn("Found multiple display rubrics " + dr + " for code " + c.getCode());
            		}
            		Rubric rubric = values.get(0);
                    String value = getLabelValue(rubric.getLabel().get(0)).trim();

            		concept.setDisplay(value);
            		if (rubric.getLabel().size() > 1) {
            			if (log.isWarnEnabled()) {
                            log.warn("Found more than one label on display rubric " + dr + " for code " + c.getCode());
            			}
            			for (int i = 1; i < values.size(); i++) {
            				addDesignationForLabel(concept, rubric, (RubricKind) rubric.getKind(), rubric.getLabel().get(i));
            			}
            		}
            	} else {
            		// We've already got a display, dump everything else as a designation
            		for (Rubric r : values) {
            			addDesignationsForRubric(concept, r);
            		}
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
            
            if (!c.getModifiedBy().isEmpty()) {
                if (!modifiedBy.containsKey(c.getCode())) {
                    modifiedBy.put(c.getCode(), new ArrayList<>());
                }
                if (log.isDebugEnabled()) {
                    log.debug("Adding " + c.getModifiedBy().size() + " modifiers to class " + c.getCode());
                }
                modifiedBy.get(c.getCode()).addAll(c.getModifiedBy());
            }
            if (!c.getExcludeModifier().isEmpty()) {
                if (!excludeModifiers.containsKey(c.getCode())) {
                    excludeModifiers.put(c.getCode(), new HashSet<>());
                }
                if (log.isDebugEnabled()) {
                    log.debug("Adding " + c.getExcludeModifier().size() + " modifier exclusions to class " + c.getCode());
                }
                excludeModifiers.get(c.getCode()).addAll(c.getExcludeModifier());
            }
        }
        
        Map <String,Set<ModifierClass>> modifierClasses = new HashMap<>();
        for (ModifierClass modClass : claml.getModifierClass()) {
            if (!modifierClasses.containsKey(modClass.getModifier())) {
                modifierClasses.put(modClass.getModifier(), new HashSet<>());
            }
            modifierClasses.get(modClass.getModifier()).add(modClass);
        }
        
        if (applyModifiers) {
            for (String modifiedConcept : modifiedBy.keySet()) {
                //Don't add modifiers to non-leaf classes
                if (descendents.containsKey(modifiedConcept) && !modifiedBy.get(modifiedConcept).isEmpty() && !descendents.get(modifiedConcept).isEmpty()) {
                    if (log.isInfoEnabled()) {
                        log.info("Modifiers are only applied to leaf classes. Skipping " + modifiedConcept);
                    }
                    for (String desc : descendents.get(modifiedConcept)) {
                        log.info("Applying modifiers to descendent " + desc + " of code " + modifiedConcept);
                        applyModifiersToClass(desc, modifierClasses, modifiedBy.get(modifiedConcept), concepts, displayRubrics, excludeModifiers, cs, count);
                    }
                } else {
                    count = applyModifiersToClass(modifiedConcept, modifierClasses, modifiedBy.get(modifiedConcept), concepts, displayRubrics, excludeModifiers, cs, count);
                }
            }
        }
        
        cs.setCount(count);
        return cs;
    }

    private int applyModifiersToClass(String modifiedConcept, Map<String, Set<ModifierClass>> modifierClasses,
            List<ModifiedBy> modifiedBy, Map<String, ConceptDefinitionComponent> concepts,
            List<String> displayRubrics, Map<String, Set<ExcludeModifier>> excludeModifiers, CodeSystem cs, Integer count) {
        List<ConceptDefinitionComponent> candidates = new ArrayList<>();
        candidates.add(concepts.get(modifiedConcept));
        //Apply the modifiers in order to the modified concept
        List<ConceptDefinitionComponent> newCandidates = null;

        modifiers : for (ModifiedBy modBy : modifiedBy) {

            if (excludeModifiers.containsKey(modifiedConcept) && !excludeModifiers.get(modifiedConcept).isEmpty()) {
                for (ExcludeModifier excludeMod : excludeModifiers.get(modifiedConcept)) {
                    if (modBy.getCode().equals(excludeMod.getCode())) {
                        log.info("Modifier " + modBy.getCode() + " is excluded for class " + modifiedConcept + " : Skipping");
                        continue modifiers;
                    }
                }
            }
                
            // for each candidate
            newCandidates = new ArrayList<>();
            for (ConceptDefinitionComponent cand : candidates) {
                log.info("Applying modifier " + modBy.getCode() + " with " + modifierClasses.get(modBy.getCode()).size() + " modifierClasses to " + cand.getCode());

                // for each applicable ModifierClass
                modifierClasses : for (ModifierClass modClass : modifierClasses.get(modBy.getCode())) {
                    log.debug("Applying modifierClass " + modClass.getCode() + " to " + cand.getCode());

                    // If ModifiedBy.all == false and there is no ModifiedBy.ValidModifierClass for this modifier class, then skip it
                    if (!modBy.isAll() && !modBy.getValidModifierClass().stream().anyMatch(vmc -> vmc.getCode().equals(modClass.getCode()))) {
                        log.info("Skipping modifierClass " + modClass.getCode() + " due to missing ValidModifierClass on class " + cand.getCode());

                        continue modifierClasses;
                    }
                    for ( Meta excl : modClass.getMeta().stream().filter(met -> met.getName().equals("excludeOnPrecedingModifier") ).collect(Collectors.toList())) {
                        String[] substrings = excl.getValue().split(" ");
                        if (substrings.length == 2 && cand.getCode().endsWith(substrings[1])) {
                            log.info("Skipping modifierClass " + modClass.getCode() + " due to excludeOnPrecedingModifier on class " + cand.getCode());
                            continue modifierClasses;
                        }
                    }
                    String newCode = cand.getCode() + modClass.getCode();
                    if (concepts.containsKey(newCode)) {
                        log.warn("Code " + newCode + " already exists as a declared Class - skipping application of modifierClass " + modBy.getCode() + "::" + modClass.getCode() + " to code " + cand.getCode());
                        continue modifierClasses;
                    }
                    ConceptDefinitionComponent concept = cs.addConcept();
                    count++;
                    newCandidates.add(concept);
                    //Set code to append modifierClass code
                    concept.setCode(newCode);
                    log.debug("Creating code " + concept.getCode());
                    Map<String,List<Rubric>> displayRubricValues = new HashMap<>();
                    //Fix display to append modifierClass display
                    for (Rubric rubric : modClass.getRubric()) {
                        Object kind = rubric.getKind();
                        if (kind instanceof RubricKind) {
                            RubricKind rkind = (RubricKind) kind;
                            if (displayRubrics.contains(rkind.getName())) {
                                if (rubric.getLabel().size() > 1) {
                                    log.warn("Found more than one label on display rubric " + rkind.getName() + " for code " + modClass.getCode());
                                }
                                if (!displayRubricValues.containsKey(rkind.getName())) {
                                    displayRubricValues.put(rkind.getName(), new ArrayList<>());
                                }
                                displayRubricValues.get(rkind.getName()).add(rubric);
                            }
                        }
                    }
                    for (String dr : displayRubrics) {
                        if (!displayRubricValues.containsKey(dr)) {
                            continue;
                        }
                        List<Rubric> values = displayRubricValues.get(dr);
                        if (values.size() > 1) {
                            log.warn("Found multiple display rubrics " + dr + " for modifierClass " + modClass.getCode());
                        }
                        Rubric rubric = values.get(0);
                        String value = getLabelValue(rubric.getLabel().get(0)).trim();

                        concept.setDisplay(cand.getDisplay() + " : " + value);
                        if (rubric.getLabel().size() > 1) {
                            if (log.isWarnEnabled()) {
                                log.warn("Found more than one label on display rubric " + dr + " for code " + modClass.getCode());
                            }
                        }
                    }
                    // Remove old parent/child links
                    //                            concept.getProperty().removeIf(p -> p.getCode().equals("parent") || p.getCode().equals("child"));
                    concept.addProperty().setCode("parent").setValue(new CodeType(cand.getCode()));
                }
            }
            candidates = newCandidates;

        }
        return count;
    }

	private void addDesignationsForRubric(ConceptDefinitionComponent concept, Rubric rubric) {
		if (rubric.getKind() instanceof RubricKind) {
			RubricKind rkind = (RubricKind) rubric.getKind();
			for (Label l : rubric.getLabel()) {
				addDesignationForLabel(concept, rubric, rkind, l);
			}
		} else {
			log.warn("Unexpected rubric kind " + rubric.getKind());
		}
	}

	private void addDesignationForLabel(ConceptDefinitionComponent concept, Rubric rubric, RubricKind rkind, Label l) {
		String v = getLabelValue(l).trim();
		if (v != null && v.length() > 0) {
			ConceptDefinitionDesignationComponent desig = concept.addDesignation();
			desig.setUse(new Coding().setDisplay(rkind.getName()));
			desig.setValue(v);
			desig.setLanguage(l.getLang());
		} else {
			log.warn("Skipping empty label for rubric " + rubric.getId());
		}
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
                if (result.length() > 0) {
                    result += "\n";
                }
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
            Fragment frg = (Fragment) l;
            for (Object cont : frg.getContent()) {
                if (frg.getType() != null && frg.getType().equals("list")) {
                    result += " - ";
                }
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