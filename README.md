# rd-commons-lib
rd-commons-lib is reusable common library for excel file upload functionality does following particular jobs

1) Post Upload Excel File activities like file extension validation, header validation, row/record validation
2) Auditing the file upload activity 
3) Exception logging in table

This library has been published in bin tray with git push actions on new release (https://bintray.com/hmcts/hmcts-maven/rd-commons-lib).

# To build the project in local execute the following command
./gradlew build 

# How to use library
Common library properties like excel file headers configured in library and customized properties should be configured with specific 
microservices eg. rd-location-ref-api (https://github.com/hmcts/rd-location-ref-api)
It can be used in respective projects as gradle dependency like below
compile group: 'uk.gov.hmcts.reform', name: 'rd-commons-lib', version: '0.0.1'





