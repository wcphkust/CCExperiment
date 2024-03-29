package schema.annotations;
import org.apache.xerces.xs.XSAnnotation;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSObjectList;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
public class XSAttributeUseAnnotationsTest extends TestCase {
    private XSLoader fSchemaLoader;
    private DOMConfiguration fConfig;
    protected void setUp() {
        try {
            System.setProperty(DOMImplementationRegistry.PROPERTY,
                    "org.apache.xerces.dom.DOMXSImplementationSourceImpl");
            DOMImplementationRegistry registry = DOMImplementationRegistry
                    .newInstance();
            XSImplementation impl = (XSImplementation) registry
                    .getDOMImplementation("XS-Loader");
            fSchemaLoader = impl.createXSLoader(null);
            fConfig = fSchemaLoader.getConfig();
            fConfig.setParameter("validate", Boolean.TRUE);
        } catch (Exception e) {
            fail("Expecting a NullPointerException");
            System.err.println("SETUP FAILED: XSAttributeUseAnnotationsTest");
        }
    }
    protected void tearDown() {
        fConfig
                .setParameter(
                        "http://apache.org/xml/features/generate-synthetic-annotations",
                        Boolean.FALSE);
    }
    public void testAttrUseNoAnnotations() {
        XSModel model = fSchemaLoader
                .loadURI(getResourceURL("XSAttributeUseAnnotationsTest01.xsd"));
        XSComplexTypeDefinition ct = (XSComplexTypeDefinition) model
                .getTypeDefinition("CT", "XSAttributeUseAnnotationsTest");
        XSObjectList attrUses = ct.getAttributeUses();
        XSAttributeUse attr = (XSAttributeUse) attrUses.item(0);
        XSObjectList annotations = attr.getAnnotations();
        assertEquals("REF", 0, annotations.getLength());
        attr = (XSAttributeUse) attrUses.item(1);
        annotations = attr.getAnnotations();
        assertEquals("LOCAL", 0, annotations.getLength());
        attr = (XSAttributeUse) attrUses.item(2);
        annotations = attr.getAnnotations();
        assertEquals("GROUP", 0, annotations.getLength());
    }
    public void testAttrUseNoSynthAnnotations() {
        fConfig
                .setParameter(
                        "http://apache.org/xml/features/generate-synthetic-annotations",
                        Boolean.FALSE);
        XSModel model = fSchemaLoader
                .loadURI(getResourceURL("XSAttributeUseAnnotationsTest01.xsd"));
        XSComplexTypeDefinition ct = (XSComplexTypeDefinition) model
                .getTypeDefinition("CT", "XSAttributeUseAnnotationsTest");
        XSObjectList attrUses = ct.getAttributeUses();
        XSAttributeUse attr = (XSAttributeUse) attrUses.item(0);
        XSObjectList annotations = attr.getAnnotations();
        assertEquals("REF", 0, annotations.getLength());
        attr = (XSAttributeUse) attrUses.item(1);
        annotations = attr.getAnnotations();
        assertEquals("LOCAL", 0, annotations.getLength());
        attr = (XSAttributeUse) attrUses.item(2);
        annotations = attr.getAnnotations();
        assertEquals("GROUP", 0, annotations.getLength());
    }
    public void testAttrUseSynthAnnotations() {
        fConfig
                .setParameter(
                        "http://apache.org/xml/features/generate-synthetic-annotations",
                        Boolean.TRUE);
        XSModel model = fSchemaLoader
                .loadURI(getResourceURL("XSAttributeUseAnnotationsTest01.xsd"));
        XSComplexTypeDefinition ct = (XSComplexTypeDefinition) model
                .getTypeDefinition("CT", "XSAttributeUseAnnotationsTest");
        XSObjectList attrUses = ct.getAttributeUses();
        XSAttributeUse attr = (XSAttributeUse) attrUses.item(0);
        XSObjectList annotations = attr.getAnnotations();
        assertEquals("REF", 1, annotations.getLength());
        attr = (XSAttributeUse) attrUses.item(1);
        annotations = attr.getAnnotations();
        assertEquals("LOCAL", 1, annotations.getLength());
        attr = (XSAttributeUse) attrUses.item(2);
        annotations = attr.getAnnotations();
        assertEquals("GROUP", 0, annotations.getLength());
    }
    public void testAttrUseAnnotations() {
        fConfig
                .setParameter(
                        "http://apache.org/xml/features/generate-synthetic-annotations",
                        Boolean.FALSE);
        XSModel model = fSchemaLoader
                .loadURI(getResourceURL("XSAttributeUseAnnotationsTest02.xsd"));
        XSComplexTypeDefinition ct = (XSComplexTypeDefinition) model
                .getTypeDefinition("CT", "XSAttributeUseAnnotationsTest");
        XSObjectList attrUses = ct.getAttributeUses();
        XSAttributeUse attr = (XSAttributeUse) attrUses.item(0);
        XSObjectList annotations = attr.getAnnotations();
        assertEquals("REF", 1, annotations.getLength());
        attr = (XSAttributeUse) attrUses.item(1);
        annotations = attr.getAnnotations();
        assertEquals("LOCAL", 1, annotations.getLength());
        attr = (XSAttributeUse) attrUses.item(2);
        annotations = attr.getAnnotations();
        assertEquals("GROUP", 1, annotations.getLength());
    }
    public void testAttrUseAnnotationsSynthetic() {
        fConfig
                .setParameter(
                        "http://apache.org/xml/features/generate-synthetic-annotations",
                        Boolean.TRUE);
        XSModel model = fSchemaLoader
                .loadURI(getResourceURL("XSAttributeUseAnnotationsTest02.xsd"));
        XSComplexTypeDefinition ct = (XSComplexTypeDefinition) model
                .getTypeDefinition("CT", "XSAttributeUseAnnotationsTest");
        XSObjectList attrUses = ct.getAttributeUses();
        XSAttributeUse attr = (XSAttributeUse) attrUses.item(0);
        XSObjectList annotations = attr.getAnnotations();
        assertEquals("REF", 1, annotations.getLength());
        attr = (XSAttributeUse) attrUses.item(1);
        annotations = attr.getAnnotations();
        assertEquals("LOCAL", 1, annotations.getLength());
        attr = (XSAttributeUse) attrUses.item(2);
        annotations = attr.getAnnotations();
        assertEquals("GROUP", 1, annotations.getLength());
    }
    public void testWildAttrUseAnnotationsSynthetic() {
        fConfig
                .setParameter(
                        "http://apache.org/xml/features/generate-synthetic-annotations",
                        Boolean.TRUE);
        XSModel model = fSchemaLoader
                .loadURI(getResourceURL("XSAttributeUseAnnotationsTest03.xsd"));
        XSComplexTypeDefinition ct = (XSComplexTypeDefinition) model
                .getTypeDefinition("CT", "XSAttributeUseAnnotationsTest");
        XSObjectList attrUses = ct.getAttributeUses();
        XSAttributeUse attr = (XSAttributeUse) attrUses.item(0);
        XSObjectList annotations = attr.getAnnotations();
        assertEquals("REF", 1, annotations.getLength());
        XSAnnotation annotation = attr.getAttrDeclaration().getAnnotation();
        String expected = "<annotation sn:att=\"ATT1\"  id=\"ANNOT1\" xmlns=\"http://www.w3.org/2001/XMLSchema\" xmlns:sv=\"XSAttributeUseAnnotationsTest\" xmlns:sn=\"SyntheticAnnotationNS\" > "
                + "<appinfo>APPINFO1</appinfo>" + "</annotation>";
        assertEquals("REF_STRING", trim(expected), trim(annotation
                .getAnnotationString()));
        annotations = attr.getAttrDeclaration().getAnnotations();
        assertEquals(
                "REF_STRING_ANNOTATIONS",
                trim(expected),
                trim(((XSAnnotation) annotations.item(0)).getAnnotationString()));
        attr = (XSAttributeUse) attrUses.item(1);
        annotations = attr.getAnnotations();
        assertEquals("LOCAL", 1, annotations.getLength());
        annotation = attr.getAttrDeclaration().getAnnotation();
        expected = "<annotation sn:att=\"ATT11\"  id=\"ANNOT6\" xmlns=\"http://www.w3.org/2001/XMLSchema\" xmlns:sv=\"XSAttributeUseAnnotationsTest\" xmlns:sn=\"SyntheticAnnotationNS\" > "
                + "<appinfo>APPINFO6</appinfo>" + "</annotation>";
        assertEquals("LOCAL_STRING", trim(expected), trim(annotation
                .getAnnotationString()));
        annotations = attr.getAttrDeclaration().getAnnotations();
        assertEquals(
                "LOCAL_STRING_ANNOTATIONS",
                trim(expected),
                trim(((XSAnnotation) annotations.item(0)).getAnnotationString()));
        attr = (XSAttributeUse) attrUses.item(2);
        annotations = attr.getAnnotations();
        assertEquals("GROUP", 1, annotations.getLength());
        annotation = attr.getAttrDeclaration().getAnnotation();
        expected = "<annotation id=\"ANNOT3\" xmlns=\"http://www.w3.org/2001/XMLSchema\" xmlns:sv=\"XSAttributeUseAnnotationsTest\" xmlns:sn=\"SyntheticAnnotationNS\" > "
                + "<appinfo>APPINFO3</appinfo>" + "</annotation>";
        assertEquals("GROUP_STRING", trim(expected), trim(annotation
                .getAnnotationString()));
        annotations = attr.getAttrDeclaration().getAnnotations();
        assertEquals(
                "GROUP_STRING_ANNOTATIONS",
                trim(expected),
                trim(((XSAnnotation) annotations.item(0)).getAnnotationString()));
    }
    public static void main(String args[]) {
        junit.textui.TestRunner.run(XSAttributeUseAnnotationsTest.class);
    }
}
