package org.apache.solr.handler.dataimport;
import org.junit.Assert;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
public class TestTemplateTransformer {
  @Test
  @SuppressWarnings("unchecked")
  public void testTransformRow() {
    List fields = new ArrayList();
    fields.add(AbstractDataImportHandlerTest.createMap("column", "firstName"));
    fields.add(AbstractDataImportHandlerTest.createMap("column", "lastName"));
    fields.add(AbstractDataImportHandlerTest.createMap("column", "middleName"));
    fields.add(AbstractDataImportHandlerTest.createMap("column", "name",
            TemplateTransformer.TEMPLATE,
            "${e.lastName}, ${e.firstName} ${e.middleName}"));
    fields.add(AbstractDataImportHandlerTest.createMap("column", "emails",
            TemplateTransformer.TEMPLATE,
            "${e.mail}"));
    fields.add(AbstractDataImportHandlerTest.createMap("column", "mrname",
            TemplateTransformer.TEMPLATE,"Mr ${e.name}"));
    List<String> mails = Arrays.asList(new String[]{"a@b.com", "c@d.com"});
    Map row = AbstractDataImportHandlerTest.createMap(
            "firstName", "Shalin",
            "middleName", "Shekhar", 
            "lastName", "Mangar",
            "mail", mails);
    VariableResolverImpl resolver = new VariableResolverImpl();
    resolver.addNamespace("e", row);
    Map<String, String> entityAttrs = AbstractDataImportHandlerTest.createMap(
            "name", "e");
    Context context = AbstractDataImportHandlerTest.getContext(null, resolver,
            null, Context.FULL_DUMP, fields, entityAttrs);
    new TemplateTransformer().transformRow(row, context);
    Assert.assertEquals("Mangar, Shalin Shekhar", row.get("name"));
    Assert.assertEquals("Mr Mangar, Shalin Shekhar", row.get("mrname"));
    Assert.assertEquals(mails,row.get("emails"));
  }
}
