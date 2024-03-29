package org.apache.batik.test.svg;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.StringReader;
import java.net.URL;
import org.apache.batik.test.AbstractTest;
import org.apache.batik.test.DefaultTestSuite;
import org.apache.batik.test.Test;
import org.apache.batik.test.TestReport;
import org.apache.batik.test.TestReportValidator;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
public class SVGRenderingAccuracyTestValidator extends DefaultTestSuite {
    private static final String validSVG 
        = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"450\" height=\"500\" viewBox=\"0 0 450 500\"> \n" +
        "    <rect x=\"25\" y=\"25\" width=\"400\" height=\"450\" fill=\"blue\" /> \n" +
        "</svg>\n";
    private static final String validSVGVariation
        = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"450\" height=\"500\" viewBox=\"0 0 450 500\"> \n" +
        "    <rect x=\"25\" y=\"25\" width=\"400\" height=\"450\" fill=\"#0000cc\" /> \n" +
        "</svg>\n";
    private static final String validSmallSVG 
        = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"45\" height=\"50\" viewBox=\"0 0 45 50\"> \n" +
        "    <rect x=\"2.5\" y=\"2.5\" width=\"40\" height=\"45\" fill=\"blue\" /> \n" +
        "</svg>\n";
    private static final String validRedSVG 
        = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"450\" height=\"500\" viewBox=\"0 0 450 500\"> \n" +
        "    <rect x=\"25\" y=\"25\" width=\"400\" height=\"450\" fill=\"red\" /> \n" +
        "</svg>\n";
    private static final String invalidSVG 
        = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"450 height=\"500\" viewBox=\"0 0 450 500\"> \n" +
        "    <rect x=\"25\" y=\"25\" width=\"400\" height=\"450\" fill=\"blue\" /> \n" +
        "</svg>\n";
    public SVGRenderingAccuracyTestValidator(){
        addTest(new InvalidSVGURL());
        addTest(new InvalidSVGContent());
        addTest(new InvalidReferenceImageURL());
        addTest(new InexistingReferenceImage());
        addTest(new DifferentSizes());
        addTest(new SameSizeDifferentContent());
        addTest(new AccurateRendering());
        addTest(new AccurateRenderingWithVariation());
        addTest(new DefaultConfigTest());
    }
    public static URL createSVGSourceURL(String svgContent) throws Exception{
        File tmpFile = File.createTempFile(SVGRenderingAccuracyTest.TEMP_FILE_PREFIX,
                                           null);
        FileWriter writer = new FileWriter(tmpFile);
        writer.write(svgContent);
        writer.close();
        return tmpFile.toURL();
    }
    public static URL createValidReferenceImage(String svgContent) throws Exception{
        TranscoderInput validSrc = new TranscoderInput(new StringReader(svgContent));
        File tmpFile = File.createTempFile(SVGRenderingAccuracyTest.TEMP_FILE_PREFIX,
                                           SVGRenderingAccuracyTest.TEMP_FILE_SUFFIX);
        TranscoderOutput validDst 
            = new TranscoderOutput(new FileOutputStream(tmpFile));
        ImageTranscoder transcoder 
            = new PNGTranscoder();
        transcoder.transcode(validSrc, validDst);
        tmpFile.deleteOnExit();
        return tmpFile.toURL();
    }
    static class DefaultConfigTest extends AbstractTest {
        String svgURL = "samples/anne.svg";
        String expectedRefImgURL = "test-references/samples/anne.png";
        String expectedVariationURL = "test-references/samples/accepted-variation/anne.png";
        String expectedCandidateURL = "test-references/samples/candidate-variation/anne.png";
        String ERROR_EXCEPTION_WHILE_BUILDING_TEST
            = "error.exception.while.building.test";
        String ERROR_UNEXPECTED_REFERENCE_IMAGE_URL
            = "error.unexpected.reference.image.url";
        String ERROR_UNEXPECTED_VARIATION_URL
            = "error.unexpected.variation.url";
        String ERROR_UNEXPECTED_CANDIDATE_URL
            = "error.unexpected.candidate.url";
        String ENTRY_KEY_EXPECTED_VALUE 
            = "entry.key.expected.value";
        String ENTRY_KEY_FOUND_VALUE
            = "entry.key.found.value";
        public DefaultConfigTest(){
            super();
            setId("defaultTest");
        }
        public TestReport runImpl() throws Exception {
            SVGRenderingAccuracyTest t 
                = new SamplesRenderingTest();
            t.setId(svgURL);
            if(!t.refImgURL.toString().endsWith(expectedRefImgURL)){
                TestReport r = reportError(ERROR_UNEXPECTED_REFERENCE_IMAGE_URL);
                r.addDescriptionEntry(ENTRY_KEY_EXPECTED_VALUE, expectedRefImgURL);
                r.addDescriptionEntry(ENTRY_KEY_FOUND_VALUE, "" + t.refImgURL);
                return r;
            }
            if (t.variationURLs == null
                    || t.variationURLs.size() != 1
                    || !t.variationURLs.get(0).toString()
                            .endsWith(expectedVariationURL)) {
                TestReport r = reportError(ERROR_UNEXPECTED_VARIATION_URL);
                r.addDescriptionEntry(ENTRY_KEY_EXPECTED_VALUE, expectedVariationURL);
                String found;
                if (t.variationURLs == null) {
                    found = "null";
                } else if (t.variationURLs.size() != 1) {
                    found = "<list of " + t.variationURLs.size() + " URLs>";
                } else {
                    found = t.variationURLs.get(0).toString();
                }
                r.addDescriptionEntry(ENTRY_KEY_FOUND_VALUE, found);
                return r;
            }
            if(!t.saveVariation.toURL().toString().endsWith(expectedCandidateURL)){
                TestReport r = reportError(ERROR_UNEXPECTED_CANDIDATE_URL);
                r.addDescriptionEntry(ENTRY_KEY_EXPECTED_VALUE, expectedCandidateURL);
                r.addDescriptionEntry(ENTRY_KEY_FOUND_VALUE, "" + t.saveVariation.toURL().toString());
                return r;
            }
            return reportSuccess();
        }
    }
    static class InvalidSVGURL extends TestReportValidator {
        public TestReport runImpl() throws Exception {
            URL invalidSVGURL = new URL("http",
                                        "dummyHost",
                                        "dummyFile.svg");
            URL refImgURL = createValidReferenceImage(validSVG);
            Test t = new SVGRenderingAccuracyTest(invalidSVGURL.toString(),
                                                  refImgURL.toString());
            setConfig(t,
                      false,
                      SVGRenderingAccuracyTest.ERROR_CANNOT_TRANSCODE_SVG);
            return super.runImpl();
        }
    }
    static class InvalidSVGContent extends TestReportValidator {
        public TestReport runImpl() throws Exception {
            URL validSVGURL = createSVGSourceURL(invalidSVG);
            URL refImgURL = createValidReferenceImage(validSVG);
            Test t = new SVGRenderingAccuracyTest(validSVGURL.toString(),
                                                  refImgURL.toString());
            setConfig(t,
                      false,
                      SVGRenderingAccuracyTest.ERROR_CANNOT_TRANSCODE_SVG);
            return super.runImpl();
        }
    }
    static class InvalidReferenceImageURL extends TestReportValidator {
        public TestReport runImpl() throws Exception {
            URL validSVGURL = createSVGSourceURL(validSVG);
            URL invalidReferenceImageURL = null;
            invalidReferenceImageURL = new URL("http",
                                               "dummyHost",
                                               "dummyFile.png");
            Test t = new SVGRenderingAccuracyTest(validSVGURL.toString(),
                                                  invalidReferenceImageURL.toString());
            setConfig(t,
                      false,
                      SVGRenderingAccuracyTest.ERROR_CANNOT_OPEN_REFERENCE_IMAGE);
            return super.runImpl();
        }
    }
    static class InexistingReferenceImage extends TestReportValidator {
        public TestReport runImpl() throws Exception {
            URL validSVGURL = createSVGSourceURL(validSVG);
            File tmpFile = File.createTempFile(SVGRenderingAccuracyTest.TEMP_FILE_PREFIX,
                                               null);
            URL refImgURL = tmpFile.toURL();
            tmpFile.delete();
            Test t = new SVGRenderingAccuracyTest(validSVGURL.toString(),
                                                  refImgURL.toString());
            setConfig(t,
                      false,
                      SVGRenderingAccuracyTest.ERROR_CANNOT_OPEN_REFERENCE_IMAGE);
            return super.runImpl();
        }
    }
    static class DifferentSizes extends TestReportValidator {
        public TestReport runImpl() throws Exception {
            URL validSVGURL = createSVGSourceURL(validSVG);
            URL validRefImageURL = createValidReferenceImage(validSmallSVG);
            Test t = new SVGRenderingAccuracyTest(validSVGURL.toString(),
                                                  validRefImageURL.toString());
            setConfig(t,
                      false,
                      SVGRenderingAccuracyTest.ERROR_SVG_RENDERING_NOT_ACCURATE);                      
            return super.runImpl();
        }
    }
    static class SameSizeDifferentContent extends TestReportValidator {
        public TestReport runImpl() throws Exception {
            URL validSVGURL = createSVGSourceURL(validSVG);
            URL validRefImageURL = createValidReferenceImage(validRedSVG);
            Test t = new SVGRenderingAccuracyTest(validSVGURL.toString(),
                                                  validRefImageURL.toString());
            setConfig(t,
                      false, 
                      SVGRenderingAccuracyTest.ERROR_SVG_RENDERING_NOT_ACCURATE);
            return super.runImpl();
        }
    }
    static class AccurateRendering extends TestReportValidator {
        public TestReport runImpl() throws Exception {
            URL validSVGURL = createSVGSourceURL(validSVG);
            URL validRefImageURL = createValidReferenceImage(validSVG);
            setConfig(new SVGRenderingAccuracyTest(validSVGURL.toString(),
                                                   validRefImageURL.toString()),
                      true,
                      null);
            return super.runImpl();
        }
    }
    static class AccurateRenderingWithVariation extends TestReportValidator {
        public TestReport runImpl() throws Exception {
            URL validSVGURL = createSVGSourceURL(validSVG);
            URL validRefImageURL = createValidReferenceImage(validSVGVariation);
            SVGRenderingAccuracyTest t 
                = new SVGRenderingAccuracyTest(validSVGURL.toString(),
                                               validRefImageURL.toString());
            File tmpVariationFile = File.createTempFile(SVGRenderingAccuracyTest.TEMP_FILE_PREFIX, null);
            t.setSaveVariation(tmpVariationFile);
            setConfig(t,
                      false,
                      SVGRenderingAccuracyTest.ERROR_SVG_RENDERING_NOT_ACCURATE);
            super.runImpl();            
            t.addVariationURL(tmpVariationFile.toURL().toString());
            t.setSaveVariation(null);
            setConfig(t, true, null);
            return super.runImpl();
        }
    }
}
