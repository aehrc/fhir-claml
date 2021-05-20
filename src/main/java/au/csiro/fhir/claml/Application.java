package au.csiro.fhir.claml;

import static java.lang.System.exit;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import ca.uhn.fhir.context.FhirContext;

@SpringBootApplication
public class Application implements CommandLineRunner {
    @Autowired
    private FhirClamlService fhirClamlController;

    /**
     * Created here as a bean because it is expensive to create and we only need one instance that can
     * be shared.
     *
     * @return
     */
    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    /**
     * Main method.
     * 
     * @param args Arguments.
     */
    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(Application.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);

    }

    @Override
    public void run(String... args) throws Exception {
        Options options = new Options();

        options.addOption(new Option("help", "Print this message."));

        options.addOption("content", true, "The extent of the content in this resource. Valid values "
                + "are not-present, example, fragment, complete and supplement. Defaults to complete. The "
                + "actual value does not affect the output of the transformation.");
        
        options.addOption("applyModifiers", true, "Apply Modifiers (default: false)");

        options.addOption(
                Option.builder("d")
                .hasArgs()
                .valueSeparator(',')
                .longOpt("display")
                .desc("A comma-separated list of ClaML rubric/s that might contain the "
                        + "concepts' displays. The first populated rubric in the list that is present on a class will be "
                        + "used as the concept's display text. Subsequent rubrics in the list will be used as designations. "
                        + "Default is 'preferred'.")
                .build());

        options.addOption("definition", true, "Indicates which ClaML rubric contains the "
                + "concepts' definitions. Default is 'definition'");

        options.addOption(Option.builder()
                .longOpt("designations")
                .hasArgs()
                .valueSeparator(',')
                .desc("Comma-separated list of ClaML rubrics that contain the concepts' synonyms.")
                .build());

        options.addOption(Option.builder()
                .longOpt("excludeClassKinds")
                .hasArgs()
                .valueSeparator(',')
                .desc("Comma-separated list of class kinds to exclude.")
                .build());

        options.addOption("excludeKindlessClasses", true, "Exclude ClaML classes that do not have kinds (default: false)");

        options.addOption("hierarchyMeaning", true, "The hierarchyMeaning of the code system. Allowable values are 'is-a', 'part-of', 'grouped-by', and 'classified-with'. Default is 'is-a'.");

        options.addOption(
                Option.builder("i")
                .required(true)
                .hasArg(true)
                .longOpt("input")
                .desc("The input ClaML file.")
                .build()
                );

        options.addOption("id", true, "The technical id of the code system. Required if using PUT to "
                + "upload the resource to a FHIR server.");

        options.addOption(
                Option.builder("o")
                .required(true)
                .hasArg(true)
                .longOpt("output")
                .desc("The output FHIR JSON file.")
                .build()
                );

        options.addOption("url", true, "Canonical identifier of the code system.");

        options.addOption("valueSet", true, "The value set that represents the entire code system.");

        options.addOption("versionNeeded", false, "Flag to indicate if the code system commits "
                + "to concept permanence across versions.");

        // The following options are not yet supported
        /*


	    	    options.addOption("contact", true, "Comma-separated list of contact details for the "
	        + "publisher. Each contact detail has the format [name|system|value], where system has "
	        + "the following possible values: phone, fax, email, pager, url, sms or other.");

	    options.addOption("copyright", true, "A copyright statement about the code system.");

	    options.addOption("date", true, "The published date. Valid formats are: YYYY, YYYY-MM, "
	        + "YYYY-MM-DD and YYYY-MM-DDThh:mm:ss+zz:zz.");

	    options.addOption("description", true, "The description of the code system. If this parameter is "
	    		+ "not specified, the description will be taken from the content of the ClaML title element.");

	    options.addOption("experimental", false, "Indicates if the code system is for testing "
	        + "purposes or real usage.");

	    options.addOption("identifier", true, "Comma-separated list of additional business "
	        + "identifiers. Each business identifer has the format [system]|[value].");

	    options.addOption("language", true, "The language of the generated code system. This is a code from the "
	        + "FHIR Common Languages value set.");

	    options.addOption("n", "name", true, "Used to specify the computer-friendly name of the code "
	        + "system. If not specified, the name will be taken from the ClaML Title.name attribute element");

	    options.addOption("publisher", true, "The publisher of the code system.");

	    options.addOption("purpose", true, "Explanation of why this code system is needed.");

	    options.addOption("status", true, "Code system status. Valid values are draft, active, "
	        + "retired and unknown");

	    options.addOption("t", "title", true, "A human-friendly name for the code system.");

	    options.addOption("v", "version", true, "Business version. If this option is not specified "
	        + "then the ClaML title.version attribute value will be used.");

         */


        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            try {

                String[] designationOptions = line.getOptionValues("designations");
                List<String> designations = designationOptions != null ? Arrays.asList(designationOptions) : Collections.emptyList();
                String[] excludeClassKindsOptions = line.getOptionValues("excludeClassKind");
                List<String> excludeClassKinds = excludeClassKindsOptions != null ? Arrays.asList(excludeClassKindsOptions) : Collections.emptyList();
                String[] displayRubricOptions = line.getOptionValues("d");
                List<String> displayRubrics = displayRubricOptions != null ? Arrays.asList(displayRubricOptions) : Collections.emptyList();
                fhirClamlController.claml2fhir(new File(line.getOptionValue("input")),
                        displayRubrics,
                        line.getOptionValue("definition"),
                        designations,
                        excludeClassKinds,
                        Boolean.parseBoolean(line.getOptionValue("excludeKindlessClasses")),
                        line.getOptionValue("hierarchyMeaning"),
                        line.getOptionValue("id"),
                        line.getOptionValue("url"),
                        line.getOptionValue("valueSet"),
                        line.getOptionValue("content"),
                        line.hasOption("versionNeeded"),
                        Boolean.parseBoolean(line.getOptionValue("applyModifiers")),
                        new File(line.getOptionValue("output")));
            } catch (Throwable t) {
                System.out.println("There was a problem transforming the ClaML file into FHIR: " 
                        + t.getLocalizedMessage());
                t.printStackTrace();
            }

        } catch (ParseException exp) {
            // oops, something went wrong
            System.out.println(exp.getMessage());
            printUsage(options);
        }

        exit(0);
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        final PrintWriter writer = new PrintWriter(System.out);
        formatter.printUsage(writer, 80, "FHIR ClaML", options);
        writer.flush();
    }


}
