// Get the object by key
def objectKey = "ASSET-123" // Replace with your asset key
def assetObject = objectFacade.loadObjectBeanByKey(objectKey)
def attributeName = "Serial Number"
def attribute = assetObject.getAttribute(attributeName)
def value = attribute?.getValue()
return value
