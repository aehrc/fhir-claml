package au.csiro.fhir.claml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.hl7.fhir.r4.model.CodeSystem;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import au.csiro.fhir.claml.model.claml.ClaML;

public class ModifierTest {

    private static FhirClamlService controller;
    private static JAXBContext jaxbContext;

    @BeforeClass
    public static void init() throws JAXBException {
        controller = new FhirClamlService();
        jaxbContext = JAXBContext.newInstance(ClaML.class);
    }

    @Test
    public void testTransform() throws ParserConfigurationException, SAXException, JAXBException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        spf.setFeature("http://xml.org/sax/features/validation", false);

        XMLReader xmlReader = spf.newSAXParser().getXMLReader();
        InputSource inputSource = new InputSource(
                new FileReader(new ClassPathResource("modifiers-test.xml").getFile()));
        SAXSource source = new SAXSource(xmlReader, inputSource);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        ClaML claml = (ClaML) jaxbUnmarshaller.unmarshal(source);

        CodeSystem cs = controller.claml2FhirObject(claml, Collections.emptyList(), null, Collections.emptyList(), Collections.emptyList(), false, null, null, null, null, null, false, true, true, "draft");
        Set<String> codes = cs.getConcept().stream().map(c -> c.getCode()).collect(Collectors.toSet());

        String[] expectedCodes = {"A", "A.1", "A.2", "A.3", "B", "C", "D", "BM1", "BM2", "CM1", "DM1", "DM1N1", "DM2", "DM2N1", "DM2N2" };
        
        List<String> expectedCodesList = Arrays.asList(expectedCodes);
        List<String> missingCodes = new ArrayList<>(expectedCodesList);
        missingCodes.removeAll(codes);
        assertTrue("Missing codes " + missingCodes.stream().collect(Collectors.joining(",")), codes.containsAll(Arrays.asList(expectedCodes)));
        assertEquals(expectedCodes.length, codes.size());
        
        assertEquals("chapter D : Modification 2 : Nodification 1", cs.getConcept().stream().filter(cdc -> cdc.getCode().equals("DM2N1")).findFirst().get().getDisplay());
        assertEquals(1, cs.getConcept().stream().filter(c -> c.getCode().equals("DM2N1")).findAny().get().getProperty().stream().filter(p->p.getCode().equals("parent")).count());
        assertEquals("DM2", cs.getConcept().stream().filter(c -> c.getCode().equals("DM2N1")).findAny().get().getProperty().stream().filter(p->p.getCode().equals("parent")).findAny().get().getValueCodeType().getCode());
        assertEquals("D", cs.getConcept().stream().filter(c -> c.getCode().equals("DM2")).findAny().get().getProperty().stream().filter(p->p.getCode().equals("parent")).findAny().get().getValueCodeType().getCode());

    }

}
