# ClaML to FHIR Transformer

This Spring Boot CLI application transforms a ClaML classification into a FHIR Code System.

## Building from source

You will need Maven installed in your computer. You can build the jar file using Maven.

```
mvn package
```

## Running

You need a JVM to run the application. The only mandatory options are -i and -o.

```
java -jar fhir-claml-0.0.1-SNAPSHOT.jar -i [input ClaML file] -o [output FHIR JSON file]
```

The following options are available:

| Parameter                | Type        | Description   |
| :----------------------- | :---------- |:------------- |
| -content                 | string      | The extent of the content in this resource. Valid values are not-present, example, fragment, complete and supplement. Defaults to complete. The actual value does not affect the output of the transformation. |
| -d                       | string      | Indicates which ClaML rubric contains the concepts' displays. Default is 'preferred'. |
| -definition              | string      | Indicates which ClaML rubric contains the concepts' definitions. Default is 'definition'. |
| -designations            | string      | Comma-separated list of ClaML rubrics that contain the concepts' synonyms. |
| -excludeClassKinds       | string      | Comma-separated list of class kinds to exclude. |
| -excludeKindlessClasses  | boolean     | Exclude ClaML classes that do not have kinds (default: false). |
| -help                    | none        | Print the help message. |
| -i                       | string      | The input ClaML file. |
| -id                      | string      | The technical id of the code system. Required if using PUT to upload the resource to a FHIR server. |
| -o                       | string      | The output FHIR JSON file. |
| -url                     | string      | Canonical identifier of the code system. |
| -valueset                | string      | The value set that represents the entire code system. |

### Examples

The ICD-10-GM classification was transformed using the following command:

```
java -jar fhir-claml-0.0.1-SNAPSHOT.jar -i icd10gm2020syst_claml_20190920.xml -designations preferredLong -o codesystem-icd10gm-2020.json -id icd10gm2020 -url http://hl7.org/fhir/sid/icd-10-gm -valueset http://hl7.org/fhir/sid/icd-10-gm/vs
```

### Known Issues

* Modifiers are not currently supported

* In some Linux environments (including docker deployments), you may need to install some packages to properly support UTF-8 encodings. Andreas Streichardt provided these commands for inclusion in a Dockerfile to fix this (these can be adapted to Linux commands if you need):
```RUN apt-get update && apt-get install -y python3 git openjdk-11-jdk maven jq unzip locales
# Set the locale
RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
    locale-gen
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8```
