# Make sure you have the configured repository in your properties file running before building.
# Rename your properties file to 'runner.properties' so it gets picked up by the JCRRunner application.

# build & run
> mvn clean compile exec:java

# rerun (faster)
> mvn -o -q compile exec:java

# create app
- configure your latest runner plugin in runner.properties
- Advise: do not use admin user in a server environment. Use jcr-runner user
- update the version number in pom.xml (versioning is separate from common-trunk)  
 
> mvn clean package appassembler:assemble

- create target/jcr-runner/config and copy log4j.xml to it
- copy runner.properties to target/jcr-runner/bin
- if desired, rename target/jcr-runner-${version} appropriately
- zip (possibly renamed) target/jcr-runner-${version} and distribute
- write short release note

# run app
sh jcr-runner/bin/jcr-runner

# More info: https://onehippo-forge.github.io/jcr-runner
