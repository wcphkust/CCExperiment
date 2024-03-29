package org.apache.lucene.analysis.th;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
public class TestThaiAnalyzer extends BaseTokenStreamTestCase {
	public void testOffsets() throws Exception {
		assertAnalyzesTo(new ThaiAnalyzer(TEST_VERSION_CURRENT), "เดอะนิวยอร์กไทมส์", 
				new String[] { "เด", "อะนิว", "ยอ", "ร์ก", "ไทมส์"},
				new int[] { 0, 2, 7, 9, 12 },
				new int[] { 2, 7, 9, 12, 17});
	}
	public void testBuggyTokenType() throws Exception {
		assertAnalyzesTo(new ThaiAnalyzer(TEST_VERSION_CURRENT), "เดอะนิวยอร์กไทมส์ ๑๒๓", 
				new String[] { "เด", "อะนิว", "ยอ", "ร์ก", "ไทมส์", "๑๒๓" },
				new String[] { "<ALPHANUM>", "<ALPHANUM>", "<ALPHANUM>", "<ALPHANUM>", "<ALPHANUM>", "<ALPHANUM>" });
	}
	public void testAnalyzer() throws Exception {
		ThaiAnalyzer analyzer = new ThaiAnalyzer(TEST_VERSION_CURRENT);
		assertAnalyzesTo(analyzer, "", new String[] {});
		assertAnalyzesTo(
			analyzer,
			"การที่ได้ต้องแสดงว่างานดี",
			new String[] { "การ", "ที่", "ได้", "ต้อง", "แสดง", "ว่า", "งาน", "ดี"});
		assertAnalyzesTo(
			analyzer,
			"บริษัทชื่อ XY&Z - คุยกับ xyz@demo.com",
			new String[] { "บริษัท", "ชื่อ", "xy&z", "คุย", "กับ", "xyz@demo.com" });
		assertAnalyzesTo(
			analyzer,
			"ประโยคว่า The quick brown fox jumped over the lazy dogs",
			new String[] { "ประโยค", "ว่า", "quick", "brown", "fox", "jumped", "over", "lazy", "dogs" });
	}
	public void testPositionIncrements() throws Exception {
	  ThaiAnalyzer analyzer = new ThaiAnalyzer(TEST_VERSION_CURRENT);
	  assertAnalyzesTo(analyzer, "ประโยคว่า the ประโยคว่า",
	          new String[] { "ประโยค", "ว่า", "ประโยค", "ว่า" },
	          new int[] { 0, 6, 14, 20 },
	          new int[] { 6, 9, 20, 23 },
	          new int[] { 1, 1, 2, 1 });
	  assertAnalyzesTo(analyzer, "ประโยคว่าtheประโยคว่า",
	      new String[] { "ประโยค", "ว่า", "ประโยค", "ว่า" },
	      new int[] { 0, 6, 12, 18 },
	      new int[] { 6, 9, 18, 21 },
	      new int[] { 1, 1, 2, 1 });
	}
	public void testReusableTokenStream() throws Exception {
	  ThaiAnalyzer analyzer = new ThaiAnalyzer(TEST_VERSION_CURRENT);
	  assertAnalyzesToReuse(analyzer, "", new String[] {});
      assertAnalyzesToReuse(
          analyzer,
          "การที่ได้ต้องแสดงว่างานดี",
          new String[] { "การ", "ที่", "ได้", "ต้อง", "แสดง", "ว่า", "งาน", "ดี"});
      assertAnalyzesToReuse(
          analyzer,
          "บริษัทชื่อ XY&Z - คุยกับ xyz@demo.com",
          new String[] { "บริษัท", "ชื่อ", "xy&z", "คุย", "กับ", "xyz@demo.com" });
	}
}
