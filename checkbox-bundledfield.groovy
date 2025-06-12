import com.deviniti.plugins.bundledfields.api.issue.BundledField
import com.deviniti.plugins.bundledfields.api.issue.Subfield
import com.deviniti.plugins.bundledfields.api.issue.CheckboxField
import com.deviniti.plugins.bundledfields.api.issue.SelectField
import com.deviniti.plugins.bundledfields.api.issue.Option
import com.intenso.jira.plugin.customfield.bundledfields.api.issue.BundledFieldDFService
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import org.apache.log4j.Logger
import org.apache.log4j.Level

log.setLevel(Level.DEBUG)

String ISSUE_OR_KEY = "DFT-67";
String DF_BF_CUSTOM_FIELD_ID = "customfield_15500";
int FIRST_GROUP = 0;

@WithPlugin("com.intenso.jira.plugin.dynamic-forms")
@PluginModule
BundledFieldDFService bundledFieldDFService

BundledField df_bf = bundledFieldDFService.getBundledField(ISSUE_OR_KEY, DF_BF_CUSTOM_FIELD_ID);

String selectFieldName = "Select Field 1";
def selectField1 = df_bf.getSubfield(selectFieldName, FIRST_GROUP)
if (selectField1 instanceof SelectField) {
    Option selectedOption = selectField1.getSelectedOption()
    log.debug("Value of \""+selectFieldName+"\" in group " + FIRST_GROUP + ":")
    log.debug("Id: " + selectedOption.getId())
  	log.debug("Name: " + selectedOption.getName())
}

log.debug("---------")

String checkboxFieldName = "Checkbox Field 1";
def checkboxField1 = df_bf.getSubfield(checkboxFieldName, FIRST_GROUP)
if (checkboxField1 instanceof CheckboxField) {
    List<Option> selectedOptions = checkboxField1.getSelectedOptions();
    log.debug("Values of \""+checkboxFieldName+"\" in group " + FIRST_GROUP + ":")
    for(selectedOption in selectedOptions){
        log.debug("Id: " + selectedOption.getId())
        log.debug("Name: " + selectedOption.getName())
    }
}
