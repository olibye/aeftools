To run the "test" target in ant, fix up the "lib" folder first, then put the following jar files here:

appengine-api-stubs.jar (from latest SDK)
appengine-local-runtime.jar (from latest SDK)
cglib-nodep-2.2.jar (or later, whatever easymock needs)
easymock.jar
easymockclassextension.jar  
junit.jar (I was using Junit 3, but newer versions should be fine, too)

