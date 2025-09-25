def objectSchemaFacade = ComponentAccessor.getOSGiComponentInstanceOfType(
    com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectSchemaFacade
)
def schemas = objectSchemaFacade.findAll()
println "schemas class: " + schemas.getClass().name
if (schemas != null && schemas.size() > 0) {
    println "First schema object class: " + schemas[0].getClass().name
    println "First schema properties: " + schemas[0].properties
}
