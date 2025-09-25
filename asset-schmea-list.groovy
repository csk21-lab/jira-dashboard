def objectSchemaFacade = ComponentAccessor.getOSGiComponentInstanceOfType(
    com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectSchemaFacade
)
def schemas = objectSchemaFacade.findAll()
if (schemas == null) {
    println "schemas is null"
} else if (schemas.size() == 0) {
    println "schemas is empty"
} else {
    println "schemas count: " + schemas.size()
    println "First schema object class: " + schemas[0].getClass().name
    println "First schema properties: " + schemas[0].properties
}
