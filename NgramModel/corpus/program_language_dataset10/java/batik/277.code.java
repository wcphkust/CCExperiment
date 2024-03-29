package org.apache.batik.css.engine;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.batik.css.engine.sac.CSSConditionFactory;
import org.apache.batik.css.engine.sac.CSSSelectorFactory;
import org.apache.batik.css.engine.sac.ExtendedSelector;
import org.apache.batik.css.engine.value.ComputedValue;
import org.apache.batik.css.engine.value.InheritValue;
import org.apache.batik.css.engine.value.ShorthandManager;
import org.apache.batik.css.engine.value.Value;
import org.apache.batik.css.engine.value.ValueManager;
import org.apache.batik.css.parser.ExtendedParser;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.util.ParsedURL;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.DocumentHandler;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.LexicalUnit;
import org.w3c.css.sac.SACMediaList;
import org.w3c.css.sac.SelectorList;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MutationEvent;
public abstract class CSSEngine {
    public static Node getCSSParentNode(Node n) {
        if (n instanceof CSSNavigableNode) {
            return ((CSSNavigableNode) n).getCSSParentNode();
        }
        return n.getParentNode();
    }
    protected static Node getCSSFirstChild(Node n) {
        if (n instanceof CSSNavigableNode) {
            return ((CSSNavigableNode) n).getCSSFirstChild();
        }
        return n.getFirstChild();
    }
    protected static Node getCSSNextSibling(Node n) {
        if (n instanceof CSSNavigableNode) {
            return ((CSSNavigableNode) n).getCSSNextSibling();
        }
        return n.getNextSibling();
    }
    protected static Node getCSSPreviousSibling(Node n) {
        if (n instanceof CSSNavigableNode) {
            return ((CSSNavigableNode) n).getCSSPreviousSibling();
        }
        return n.getPreviousSibling();
    }
    public static CSSStylableElement getParentCSSStylableElement(Element elt) {
        Node n = getCSSParentNode(elt);
        while (n != null) {
            if (n instanceof CSSStylableElement) {
                return (CSSStylableElement) n;
            }
            n = getCSSParentNode(n);
        }
        return null;
    }
    protected CSSEngineUserAgent userAgent;
    protected CSSContext cssContext;
    protected Document document;
    protected ParsedURL documentURI;
    protected boolean isCSSNavigableDocument;
    protected StringIntMap indexes;
    protected StringIntMap shorthandIndexes;
    protected ValueManager[] valueManagers;
    protected ShorthandManager[] shorthandManagers;
    protected ExtendedParser parser;
    protected String[] pseudoElementNames;
    protected int fontSizeIndex = -1;
    protected int lineHeightIndex = -1;
    protected int colorIndex = -1;
    protected StyleSheet userAgentStyleSheet;
    protected StyleSheet userStyleSheet;
    protected SACMediaList media;
    protected List styleSheetNodes;
    protected List fontFaces = new LinkedList();
    protected String styleNamespaceURI;
    protected String styleLocalName;
    protected String classNamespaceURI;
    protected String classLocalName;
    protected Set nonCSSPresentationalHints;
    protected String nonCSSPresentationalHintsNamespaceURI;
    protected StyleDeclarationDocumentHandler styleDeclarationDocumentHandler =
        new StyleDeclarationDocumentHandler();
    protected StyleDeclarationUpdateHandler styleDeclarationUpdateHandler;
    protected StyleSheetDocumentHandler styleSheetDocumentHandler =
        new StyleSheetDocumentHandler();
    protected StyleDeclarationBuilder styleDeclarationBuilder =
        new StyleDeclarationBuilder();
    protected CSSStylableElement element;
    protected ParsedURL cssBaseURI;
    protected String alternateStyleSheet;
    protected CSSNavigableDocumentHandler cssNavigableDocumentListener;
    protected EventListener domAttrModifiedListener;
    protected EventListener domNodeInsertedListener;
    protected EventListener domNodeRemovedListener;
    protected EventListener domSubtreeModifiedListener;
    protected EventListener domCharacterDataModifiedListener;
    protected boolean styleSheetRemoved;
    protected Node removedStylableElementSibling;
    protected List listeners = Collections.synchronizedList(new LinkedList());
    protected Set selectorAttributes;
    protected final int[] ALL_PROPERTIES;
    protected CSSConditionFactory cssConditionFactory;
    protected CSSEngine(Document doc,
                        ParsedURL uri,
                        ExtendedParser p,
                        ValueManager[] vm,
                        ShorthandManager[] sm,
                        String[] pe,
                        String sns,
                        String sln,
                        String cns,
                        String cln,
                        boolean hints,
                        String hintsNS,
                        CSSContext ctx) {
        document = doc;
        documentURI = uri;
        parser = p;
        pseudoElementNames = pe;
        styleNamespaceURI = sns;
        styleLocalName = sln;
        classNamespaceURI = cns;
        classLocalName = cln;
        cssContext = ctx;
        isCSSNavigableDocument = doc instanceof CSSNavigableDocument;
        cssConditionFactory = new CSSConditionFactory(cns, cln, null, "id");
        int len = vm.length;
        indexes = new StringIntMap(len);
        valueManagers = vm;
        for (int i = len - 1; i >= 0; --i) {
            String pn = vm[i].getPropertyName();
            indexes.put(pn, i);
            if (fontSizeIndex == -1 &&
                pn.equals(CSSConstants.CSS_FONT_SIZE_PROPERTY)) {
                fontSizeIndex = i;
            }
            if (lineHeightIndex == -1 &&
                pn.equals(CSSConstants.CSS_LINE_HEIGHT_PROPERTY)) {
                lineHeightIndex = i;
            }
            if (colorIndex == -1 &&
                pn.equals(CSSConstants.CSS_COLOR_PROPERTY)) {
                colorIndex = i;
            }
        }
        len = sm.length;
        shorthandIndexes = new StringIntMap(len);
        shorthandManagers = sm;
        for (int i = len - 1; i >= 0; --i) {
            shorthandIndexes.put(sm[i].getPropertyName(), i);
        }
        if (hints) {
            nonCSSPresentationalHints = new HashSet(vm.length+sm.length);
            nonCSSPresentationalHintsNamespaceURI = hintsNS;
            len = vm.length;
            for (int i = 0; i < len; i++) {
                String pn = vm[i].getPropertyName();
                nonCSSPresentationalHints.add(pn);
            }
            len = sm.length;
            for (int i = 0; i < len; i++) {
                String pn = sm[i].getPropertyName();
                nonCSSPresentationalHints.add(pn);
            }
        }
        if (cssContext.isDynamic() && document instanceof EventTarget) {
            addEventListeners((EventTarget) document);
            styleDeclarationUpdateHandler =
                new StyleDeclarationUpdateHandler();
        }
        ALL_PROPERTIES = new int[getNumberOfProperties()];
        for (int i = getNumberOfProperties() - 1; i >= 0; --i) {
            ALL_PROPERTIES[i] = i;
        }
    }
    protected void addEventListeners(EventTarget doc) {
        if (isCSSNavigableDocument) {
            cssNavigableDocumentListener = new CSSNavigableDocumentHandler();
            CSSNavigableDocument cnd = (CSSNavigableDocument) doc;
            cnd.addCSSNavigableDocumentListener(cssNavigableDocumentListener);
        } else {
            domAttrModifiedListener = new DOMAttrModifiedListener();
            doc.addEventListener("DOMAttrModified",
                                 domAttrModifiedListener,
                                 false);
            domNodeInsertedListener = new DOMNodeInsertedListener();
            doc.addEventListener("DOMNodeInserted",
                                 domNodeInsertedListener,
                                 false);
            domNodeRemovedListener = new DOMNodeRemovedListener();
            doc.addEventListener("DOMNodeRemoved",
                                 domNodeRemovedListener,
                                 false);
            domSubtreeModifiedListener = new DOMSubtreeModifiedListener();
            doc.addEventListener("DOMSubtreeModified",
                                 domSubtreeModifiedListener,
                                 false);
            domCharacterDataModifiedListener =
                new DOMCharacterDataModifiedListener();
            doc.addEventListener("DOMCharacterDataModified",
                                 domCharacterDataModifiedListener,
                                 false);
        }
    }
    protected void removeEventListeners(EventTarget doc) {
        if (isCSSNavigableDocument) {
            CSSNavigableDocument cnd = (CSSNavigableDocument) doc;
            cnd.removeCSSNavigableDocumentListener
                (cssNavigableDocumentListener);
        } else {
            doc.removeEventListener("DOMAttrModified",
                                    domAttrModifiedListener,
                                    false);
            doc.removeEventListener("DOMNodeInserted",
                                    domNodeInsertedListener,
                                    false);
            doc.removeEventListener("DOMNodeRemoved",
                                    domNodeRemovedListener,
                                    false);
            doc.removeEventListener("DOMSubtreeModified",
                                    domSubtreeModifiedListener,
                                    false);
            doc.removeEventListener("DOMCharacterDataModified",
                                    domCharacterDataModifiedListener,
                                    false);
        }
    }
    public void dispose() {
        setCSSEngineUserAgent(null);
        disposeStyleMaps(document.getDocumentElement());
        if (document instanceof EventTarget) {
            removeEventListeners((EventTarget) document);
        }
    }
    protected void disposeStyleMaps(Node node) {
        if (node instanceof CSSStylableElement) {
            ((CSSStylableElement)node).setComputedStyleMap(null, null);
        }
        for (Node n = getCSSFirstChild(node);
             n != null;
             n = getCSSNextSibling(n)) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                disposeStyleMaps(n);
            }
        }
    }
    public CSSContext getCSSContext() {
        return cssContext;
    }
    public Document getDocument() {
        return document;
    }
    public int getFontSizeIndex() {
        return fontSizeIndex;
    }
    public int getLineHeightIndex() {
        return lineHeightIndex;
    }
    public int getColorIndex() {
        return colorIndex;
    }
    public int getNumberOfProperties() {
        return valueManagers.length;
    }
    public int getPropertyIndex(String name) {
        return indexes.get(name);
    }
    public int getShorthandIndex(String name) {
        return shorthandIndexes.get(name);
    }
    public String getPropertyName(int idx) {
        return valueManagers[idx].getPropertyName();
    }
    public void setCSSEngineUserAgent(CSSEngineUserAgent userAgent) {
        this.userAgent = userAgent;
    }
    public CSSEngineUserAgent getCSSEngineUserAgent() {
        return userAgent;
    }
    public void setUserAgentStyleSheet(StyleSheet ss) {
        userAgentStyleSheet = ss;
    }
    public void setUserStyleSheet(StyleSheet ss) {
        userStyleSheet = ss;
    }
    public ValueManager[] getValueManagers() {
        return valueManagers;
    }
    public ShorthandManager[] getShorthandManagers() {
        return shorthandManagers;
    }
    public List getFontFaces() {
        return fontFaces;
    }
    public void setMedia(String str) {
        try {
            media = parser.parseMedia(str);
        } catch (Exception e) {
            String m = e.getMessage();
            if (m == null) m = "";
            String s =Messages.formatMessage
                ("media.error", new Object[] { str, m });
            throw new DOMException(DOMException.SYNTAX_ERR, s);
        }
    }
    public void setAlternateStyleSheet(String str) {
        alternateStyleSheet = str;
    }
    public void importCascadedStyleMaps(Element src,
                                        CSSEngine srceng,
                                        Element dest) {
        if (src instanceof CSSStylableElement) {
            CSSStylableElement csrc  = (CSSStylableElement)src;
            CSSStylableElement cdest = (CSSStylableElement)dest;
            StyleMap sm = srceng.getCascadedStyleMap(csrc, null);
            sm.setFixedCascadedStyle(true);
            cdest.setComputedStyleMap(null, sm);
            if (pseudoElementNames != null) {
                int len = pseudoElementNames.length;
                for (int i = 0; i < len; i++) {
                    String pe = pseudoElementNames[i];
                    sm = srceng.getCascadedStyleMap(csrc, pe);
                    cdest.setComputedStyleMap(pe, sm);
                }
            }
        }
        for (Node dn = getCSSFirstChild(dest), sn = getCSSFirstChild(src);
             dn != null;
             dn = getCSSNextSibling(dn), sn = getCSSNextSibling(sn)) {
            if (sn.getNodeType() == Node.ELEMENT_NODE) {
                importCascadedStyleMaps((Element)sn, srceng, (Element)dn);
            }
        }
    }
    public ParsedURL getCSSBaseURI() {
        if (cssBaseURI == null) {
            cssBaseURI = element.getCSSBase();
        }
        return cssBaseURI;
    }
    public StyleMap getCascadedStyleMap(CSSStylableElement elt,
                                        String pseudo) {
        int props = getNumberOfProperties();
        final StyleMap result = new StyleMap(props);
        if (userAgentStyleSheet != null) {
            ArrayList rules = new ArrayList();
            addMatchingRules(rules, userAgentStyleSheet, elt, pseudo);
            addRules(elt, pseudo, result, rules, StyleMap.USER_AGENT_ORIGIN);
        }
        if (userStyleSheet != null) {
            ArrayList rules = new ArrayList();
            addMatchingRules(rules, userStyleSheet, elt, pseudo);
            addRules(elt, pseudo, result, rules, StyleMap.USER_ORIGIN);
        }
        element = elt;
        try {
            if (nonCSSPresentationalHints != null) {
                ShorthandManager.PropertyHandler ph =
                    new ShorthandManager.PropertyHandler() {
                        public void property(String pname, LexicalUnit lu,
                                             boolean important) {
                            int idx = getPropertyIndex(pname);
                            if (idx != -1) {
                                ValueManager vm = valueManagers[idx];
                                Value v = vm.createValue(lu, CSSEngine.this);
                                putAuthorProperty(result, idx, v, important,
                                                  StyleMap.NON_CSS_ORIGIN);
                                return;
                            }
                            idx = getShorthandIndex(pname);
                            if (idx == -1)
                                return; 
                            shorthandManagers[idx].setValues
                                (CSSEngine.this, this, lu, important);
                        }
                    };
                NamedNodeMap attrs = elt.getAttributes();
                int len = attrs.getLength();
                for (int i = 0; i < len; i++) {
                    Node attr = attrs.item(i);
                    String an = attr.getNodeName();
                    if (nonCSSPresentationalHints.contains(an)) {
                        try {
                            LexicalUnit lu;
                            lu = parser.parsePropertyValue(attr.getNodeValue());
                            ph.property(an, lu, false);
                        } catch (Exception e) {
                            String m = e.getMessage();
                            if (m == null) m = "";
                            String u = ((documentURI == null)?"<unknown>":
                                        documentURI.toString());
                            String s = Messages.formatMessage
                                ("property.syntax.error.at",
                                 new Object[] { u, an, attr.getNodeValue(), m});
                            DOMException de = new DOMException(DOMException.SYNTAX_ERR, s);
                            if (userAgent == null) throw de;
                            userAgent.displayError(de);
                        }
                    }
                }
            }
            CSSEngine eng = cssContext.getCSSEngineForElement(elt);
            List snodes = eng.getStyleSheetNodes();
            int slen = snodes.size();
            if (slen > 0) {
                ArrayList rules = new ArrayList();
                for (int i = 0; i < slen; i++) {
                    CSSStyleSheetNode ssn = (CSSStyleSheetNode)snodes.get(i);
                    StyleSheet ss = ssn.getCSSStyleSheet();
                    if (ss != null &&
                        (!ss.isAlternate() ||
                         ss.getTitle() == null ||
                         ss.getTitle().equals(alternateStyleSheet)) &&
                        mediaMatch(ss.getMedia())) {
                        addMatchingRules(rules, ss, elt, pseudo);
                    }
                }
                addRules(elt, pseudo, result, rules, StyleMap.AUTHOR_ORIGIN);
            }
            if (styleLocalName != null) {
                String style = elt.getAttributeNS(styleNamespaceURI,
                                                  styleLocalName);
                if (style.length() > 0) {
                    try {
                        parser.setSelectorFactory(CSSSelectorFactory.INSTANCE);
                        parser.setConditionFactory(cssConditionFactory);
                        styleDeclarationDocumentHandler.styleMap = result;
                        parser.setDocumentHandler
                            (styleDeclarationDocumentHandler);
                        parser.parseStyleDeclaration(style);
                        styleDeclarationDocumentHandler.styleMap = null;
                    } catch (Exception e) {
                        String m = e.getMessage();
                        if (m == null) m = e.getClass().getName();
                        String u = ((documentURI == null)?"<unknown>":
                                    documentURI.toString());
                        String s = Messages.formatMessage
                            ("style.syntax.error.at",
                             new Object[] { u, styleLocalName, style, m });
                        DOMException de = new DOMException(DOMException.SYNTAX_ERR, s);
                        if (userAgent == null) throw de;
                        userAgent.displayError(de);
                    }
                }
            }
            StyleDeclarationProvider p =
                elt.getOverrideStyleDeclarationProvider();
            if (p != null) {
                StyleDeclaration over = p.getStyleDeclaration();
                if (over != null) {
                    int ol = over.size();
                    for (int i = 0; i < ol; i++) {
                        int idx = over.getIndex(i);
                        Value value = over.getValue(i);
                        boolean important = over.getPriority(i);
                        if (!result.isImportant(idx) || important) {
                            result.putValue(idx, value);
                            result.putImportant(idx, important);
                            result.putOrigin(idx, StyleMap.OVERRIDE_ORIGIN);
                        }
                    }
                }
            }
        } finally {
            element = null;
            cssBaseURI = null;
        }
        return result;
    }
    public Value getComputedStyle(CSSStylableElement elt,
                                  String pseudo,
                                  int propidx) {
        StyleMap sm = elt.getComputedStyleMap(pseudo);
        if (sm == null) {
            sm = getCascadedStyleMap(elt, pseudo);
            elt.setComputedStyleMap(pseudo, sm);
        }
        Value value = sm.getValue(propidx);
        if (sm.isComputed(propidx))
            return value;
        Value result = value;
        ValueManager vm = valueManagers[propidx];
        CSSStylableElement p = getParentCSSStylableElement(elt);
        if (value == null) {
            if ((p == null) || !vm.isInheritedProperty())
                result = vm.getDefaultValue();
        } else if ((p != null) && (value == InheritValue.INSTANCE)) {
            result = null;
        }
        if (result == null) {
            result = getComputedStyle(p, null, propidx);
            sm.putParentRelative(propidx, true);
            sm.putInherited     (propidx, true);
        } else {
            result = vm.computeValue(elt, pseudo, this, propidx,
                                     sm, result);
        }
        if (value == null) {
            sm.putValue(propidx, result);
            sm.putNullCascaded(propidx, true);
        } else if (result != value) {
            ComputedValue cv = new ComputedValue(value);
            cv.setComputedValue(result);
            sm.putValue(propidx, cv);
            result = cv;
        }
        sm.putComputed(propidx, true);
        return result;
    }
    public List getStyleSheetNodes() {
        if (styleSheetNodes == null) {
            styleSheetNodes = new ArrayList();
            selectorAttributes = new HashSet();
            findStyleSheetNodes(document);
            int len = styleSheetNodes.size();
            for (int i = 0; i < len; i++) {
                CSSStyleSheetNode ssn;
                ssn = (CSSStyleSheetNode)styleSheetNodes.get(i);
                StyleSheet ss = ssn.getCSSStyleSheet();
                if (ss != null) {
                    findSelectorAttributes(selectorAttributes, ss);
                }
            }
        }
        return styleSheetNodes;
    }
    protected void findStyleSheetNodes(Node n) {
        if (n instanceof CSSStyleSheetNode) {
            styleSheetNodes.add(n);
        }
        for (Node nd = getCSSFirstChild(n);
             nd != null;
             nd = getCSSNextSibling(nd)) {
            findStyleSheetNodes(nd);
        }
    }
    protected void findSelectorAttributes(Set attrs, StyleSheet ss) {
        int len = ss.getSize();
        for (int i = 0; i < len; i++) {
            Rule r = ss.getRule(i);
            switch (r.getType()) {
            case StyleRule.TYPE:
                StyleRule style = (StyleRule)r;
                SelectorList sl = style.getSelectorList();
                int slen = sl.getLength();
                for (int j = 0; j < slen; j++) {
                    ExtendedSelector s = (ExtendedSelector)sl.item(j);
                    s.fillAttributeSet(attrs);
                }
                break;
            case MediaRule.TYPE:
            case ImportRule.TYPE:
                MediaRule mr = (MediaRule)r;
                if (mediaMatch(mr.getMediaList())) {
                    findSelectorAttributes(attrs, mr);
                }
                break;
            }
        }
    }
    public interface MainPropertyReceiver {
        void setMainProperty(String name, Value v, boolean important);
    }
    public void setMainProperties
        (CSSStylableElement elt, final MainPropertyReceiver dst,
         String pname, String value, boolean important){
        try {
            element = elt;
            LexicalUnit lu = parser.parsePropertyValue(value);
            ShorthandManager.PropertyHandler ph =
                new ShorthandManager.PropertyHandler() {
                    public void property(String pname, LexicalUnit lu,
                                         boolean important) {
                        int idx = getPropertyIndex(pname);
                        if (idx != -1) {
                            ValueManager vm = valueManagers[idx];
                            Value v = vm.createValue(lu, CSSEngine.this);
                            dst.setMainProperty(pname, v, important);
                            return;
                        }
                        idx = getShorthandIndex(pname);
                        if (idx == -1)
                            return; 
                        shorthandManagers[idx].setValues
                            (CSSEngine.this, this, lu, important);
                    }
                };
            ph.property(pname, lu, important);
        } catch (Exception e) {
            String m = e.getMessage();
            if (m == null) m = "";                  
            String u = ((documentURI == null)?"<unknown>":
                        documentURI.toString());
            String s = Messages.formatMessage
                ("property.syntax.error.at",
                 new Object[] { u, pname, value, m});
            DOMException de = new DOMException(DOMException.SYNTAX_ERR, s);
            if (userAgent == null) throw de;
            userAgent.displayError(de);
        } finally {
            element = null;
            cssBaseURI = null;
        }
    }
    public Value parsePropertyValue(CSSStylableElement elt,
                                    String prop, String value) {
        int idx = getPropertyIndex(prop);
        if (idx == -1) return null;
        ValueManager vm = valueManagers[idx];
        try {
            element = elt;
            LexicalUnit lu;
            lu = parser.parsePropertyValue(value);
            return vm.createValue(lu, this);
        } catch (Exception e) {
            String m = e.getMessage();
            if (m == null) m = "";
            String u = ((documentURI == null)?"<unknown>":
                        documentURI.toString());
            String s = Messages.formatMessage
                ("property.syntax.error.at",
                 new Object[] { u, prop, value, m });
            DOMException de = new DOMException(DOMException.SYNTAX_ERR, s);
            if (userAgent == null) throw de;
            userAgent.displayError(de);
        } finally {
            element = null;
            cssBaseURI = null;
        }
        return vm.getDefaultValue();
    }
    public StyleDeclaration parseStyleDeclaration(CSSStylableElement elt,
                                                  String value) {
        styleDeclarationBuilder.styleDeclaration = new StyleDeclaration();
        try {
            element = elt;
            parser.setSelectorFactory(CSSSelectorFactory.INSTANCE);
            parser.setConditionFactory(cssConditionFactory);
            parser.setDocumentHandler(styleDeclarationBuilder);
            parser.parseStyleDeclaration(value);
        } catch (Exception e) {
            String m = e.getMessage();
            if (m == null) m = "";
            String u = ((documentURI == null)?"<unknown>":
                        documentURI.toString());
            String s = Messages.formatMessage
                ("syntax.error.at", new Object[] { u, m });
            DOMException de = new DOMException(DOMException.SYNTAX_ERR, s);
            if (userAgent == null) throw de;
            userAgent.displayError(de);
        } finally {
            element = null;
            cssBaseURI = null;
        }
        return styleDeclarationBuilder.styleDeclaration;
    }
    public StyleSheet parseStyleSheet(ParsedURL uri, String media)
        throws DOMException {
        StyleSheet ss = new StyleSheet();
        try {
            ss.setMedia(parser.parseMedia(media));
        } catch (Exception e) {
            String m = e.getMessage();
            if (m == null) m = "";
            String u = ((documentURI == null)?"<unknown>":
                        documentURI.toString());
            String s = Messages.formatMessage
                ("syntax.error.at", new Object[] { u, m });
            DOMException de = new DOMException(DOMException.SYNTAX_ERR, s);
            if (userAgent == null) throw de;
            userAgent.displayError(de);
            return ss;
        }
        parseStyleSheet(ss, uri);
        return ss;
    }
    public StyleSheet parseStyleSheet(InputSource is, ParsedURL uri,
                                      String media)
        throws DOMException {
        StyleSheet ss = new StyleSheet();
        try {
            ss.setMedia(parser.parseMedia(media));
            parseStyleSheet(ss, is, uri);
        } catch (Exception e) {
            String m = e.getMessage();
            if (m == null) m = "";
            String u = ((documentURI == null)?"<unknown>":
                        documentURI.toString());
            String s = Messages.formatMessage
                ("syntax.error.at", new Object[] { u, m });
            DOMException de = new DOMException(DOMException.SYNTAX_ERR, s);
            if (userAgent == null) throw de;
            userAgent.displayError(de);
        }
        return ss;
    }
    public void parseStyleSheet(StyleSheet ss, ParsedURL uri)
            throws DOMException {
        if (uri == null) {
            String s = Messages.formatMessage
                ("syntax.error.at",
                 new Object[] { "Null Document reference", "" });
            DOMException de = new DOMException(DOMException.SYNTAX_ERR, s);
            if (userAgent == null) throw de;
            userAgent.displayError(de);
            return;
        }
        try {
            cssContext.checkLoadExternalResource(uri, documentURI);
            parseStyleSheet(ss, new InputSource(uri.toString()), uri);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            String m = e.getMessage();
            if (m == null) m = e.getClass().getName();
            String s = Messages.formatMessage
                ("syntax.error.at", new Object[] { uri.toString(), m });
            DOMException de = new DOMException(DOMException.SYNTAX_ERR, s);
            if (userAgent == null) throw de;
            userAgent.displayError(de);
        }
    }
    public StyleSheet parseStyleSheet(String rules, ParsedURL uri, String media)
            throws DOMException {
        StyleSheet ss = new StyleSheet();
        try {
            ss.setMedia(parser.parseMedia(media));
        } catch (Exception e) {
            String m = e.getMessage();
            if (m == null) m = "";
            String u = ((documentURI == null)?"<unknown>":
                        documentURI.toString());
            String s = Messages.formatMessage
                ("syntax.error.at", new Object[] { u, m });
            DOMException de = new DOMException(DOMException.SYNTAX_ERR, s);
            if (userAgent == null) throw de;
            userAgent.displayError(de);
            return ss;
        }
        parseStyleSheet(ss, rules, uri);
        return ss;
    }
    public void parseStyleSheet(StyleSheet ss,
                                String rules,
                                ParsedURL uri) throws DOMException {
        try {
            parseStyleSheet(ss, new InputSource(new StringReader(rules)), uri);
        } catch (Exception e) {
            String m = e.getMessage();
            if (m == null) m = "";
            String s = Messages.formatMessage
                ("stylesheet.syntax.error",
                 new Object[] { uri.toString(), rules, m });
            DOMException de = new DOMException(DOMException.SYNTAX_ERR, s);
            if (userAgent == null) throw de;
            userAgent.displayError(de);
        }
    }
    protected void parseStyleSheet(StyleSheet ss, InputSource is, ParsedURL uri)
        throws IOException {
        parser.setSelectorFactory(CSSSelectorFactory.INSTANCE);
        parser.setConditionFactory(cssConditionFactory);
        try {
            cssBaseURI = uri;
            styleSheetDocumentHandler.styleSheet = ss;
            parser.setDocumentHandler(styleSheetDocumentHandler);
            parser.parseStyleSheet(is);
            int len = ss.getSize();
            for (int i = 0; i < len; i++) {
                Rule r = ss.getRule(i);
                if (r.getType() != ImportRule.TYPE) {
                    break;
                }
                ImportRule ir = (ImportRule)r;
                parseStyleSheet(ir, ir.getURI());
            }
        } finally {
            cssBaseURI = null;
        }
    }
    protected void putAuthorProperty(StyleMap dest,
                                     int idx,
                                     Value sval,
                                     boolean imp,
                                     short origin) {
        Value   dval = dest.getValue(idx);
        short   dorg = dest.getOrigin(idx);
        boolean dimp = dest.isImportant(idx);
        boolean cond = dval == null;
        if (!cond) {
            switch (dorg) {
            case StyleMap.USER_ORIGIN:
                cond = !dimp;
                break;
            case StyleMap.AUTHOR_ORIGIN:
                cond = !dimp || imp;
                break;
            case StyleMap.OVERRIDE_ORIGIN:
                cond = false;
                break;
            default:
                cond = true;
            }
        }
        if (cond) {
            dest.putValue(idx, sval);
            dest.putImportant(idx, imp);
            dest.putOrigin(idx, origin);
        }
    }
    protected void addMatchingRules(List rules,
                                    StyleSheet ss,
                                    Element elt,
                                    String pseudo) {
        int len = ss.getSize();
        for (int i = 0; i < len; i++) {
            Rule r = ss.getRule(i);
            switch (r.getType()) {
            case StyleRule.TYPE:
                StyleRule style = (StyleRule)r;
                SelectorList sl = style.getSelectorList();
                int slen = sl.getLength();
                for (int j = 0; j < slen; j++) {
                    ExtendedSelector s = (ExtendedSelector)sl.item(j);
                    if (s.match(elt, pseudo)) {
                        rules.add(style);
                    }
                }
                break;
            case MediaRule.TYPE:
            case ImportRule.TYPE:
                MediaRule mr = (MediaRule)r;
                if (mediaMatch(mr.getMediaList())) {
                    addMatchingRules(rules, mr, elt, pseudo);
                }
                break;
            }
        }
    }
    protected void addRules(Element elt,
                            String pseudo,
                            StyleMap sm,
                            ArrayList rules,
                            short origin) {
        sortRules(rules, elt, pseudo);
        int rlen = rules.size();
        if (origin == StyleMap.AUTHOR_ORIGIN) {
            for (int r = 0; r < rlen; r++) {
                StyleRule sr = (StyleRule)rules.get(r);
                StyleDeclaration sd = sr.getStyleDeclaration();
                int len = sd.size();
                for (int i = 0; i < len; i++) {
                    putAuthorProperty(sm,
                                      sd.getIndex(i),
                                      sd.getValue(i),
                                      sd.getPriority(i),
                                      origin);
                }
            }
        } else {
            for (int r = 0; r < rlen; r++) {
                StyleRule sr = (StyleRule)rules.get(r);
                StyleDeclaration sd = sr.getStyleDeclaration();
                int len = sd.size();
                for (int i = 0; i < len; i++) {
                    int idx = sd.getIndex(i);
                    sm.putValue(idx, sd.getValue(i));
                    sm.putImportant(idx, sd.getPriority(i));
                    sm.putOrigin(idx, origin);
                }
            }
        }
    }
    protected void sortRules(ArrayList rules, Element elt, String pseudo) {
        int len = rules.size();
        int[] specificities = new int[len];
        for (int i = 0; i < len; i++) {
            StyleRule r = (StyleRule) rules.get(i);
            SelectorList sl = r.getSelectorList();
            int spec = 0;
            int slen = sl.getLength();
            for (int k = 0; k < slen; k++) {
                ExtendedSelector s = (ExtendedSelector) sl.item(k);
                if (s.match(elt, pseudo)) {
                    int sp = s.getSpecificity();
                    if (sp > spec) {
                        spec = sp;
                    }
                }
            }
            specificities[i] = spec;
        }
        for (int i = 1; i < len; i++) {
            Object rule = rules.get(i);
            int spec = specificities[i];
            int j = i - 1;
            while (j >= 0 && specificities[j] > spec) {
                rules.set(j + 1, rules.get(j));
                specificities[j + 1] = specificities[j];
                j--;
            }
            rules.set(j + 1, rule);
            specificities[j + 1] = spec;
        }
    }
    protected boolean mediaMatch(SACMediaList ml) {
    if (media == null ||
            ml == null ||
            media.getLength() == 0 ||
            ml.getLength() == 0) {
        return true;
    }
    for (int i = 0; i < ml.getLength(); i++) {
            if (ml.item(i).equalsIgnoreCase("all"))
                return true;
        for (int j = 0; j < media.getLength(); j++) {
        if (media.item(j).equalsIgnoreCase("all") ||
                    ml.item(i).equalsIgnoreCase(media.item(j))) {
            return true;
        }
        }
    }
    return false;
    }
    protected class StyleDeclarationDocumentHandler
        extends DocumentAdapter
        implements ShorthandManager.PropertyHandler {
        public StyleMap styleMap;
        public void property(String name, LexicalUnit value, boolean important)
            throws CSSException {
            int i = getPropertyIndex(name);
            if (i == -1) {
                i = getShorthandIndex(name);
                if (i == -1) {
                    return;
                }
                shorthandManagers[i].setValues(CSSEngine.this,
                                               this,
                                               value,
                                               important);
            } else {
                Value v = valueManagers[i].createValue(value, CSSEngine.this);
                putAuthorProperty(styleMap, i, v, important,
                                  StyleMap.INLINE_AUTHOR_ORIGIN);
            }
        }
    }
    protected class StyleDeclarationBuilder
        extends DocumentAdapter
        implements ShorthandManager.PropertyHandler {
        public StyleDeclaration styleDeclaration;
        public void property(String name, LexicalUnit value, boolean important)
            throws CSSException {
            int i = getPropertyIndex(name);
            if (i == -1) {
                i = getShorthandIndex(name);
                if (i == -1) {
                    return;
                }
                shorthandManagers[i].setValues(CSSEngine.this,
                                               this,
                                               value,
                                               important);
            } else {
                Value v = valueManagers[i].createValue(value, CSSEngine.this);
                styleDeclaration.append(v, i, important);
            }
        }
    }
    protected class StyleSheetDocumentHandler
        extends DocumentAdapter
        implements ShorthandManager.PropertyHandler {
        public StyleSheet styleSheet;
        protected StyleRule styleRule;
        protected StyleDeclaration styleDeclaration;
        public void startDocument(InputSource source)
            throws CSSException {
        }
        public void endDocument(InputSource source) throws CSSException {
        }
        public void ignorableAtRule(String atRule) throws CSSException {
        }
        public void importStyle(String       uri,
                                SACMediaList media,
                                String       defaultNamespaceURI)
            throws CSSException {
            ImportRule ir = new ImportRule();
            ir.setMediaList(media);
            ir.setParent(styleSheet);
            ParsedURL base = getCSSBaseURI();
            ParsedURL url;
            if (base == null) {
                url = new ParsedURL(uri);
            } else {
                url = new ParsedURL(base, uri);
            }
            ir.setURI(url);
            styleSheet.append(ir);
        }
        public void startMedia(SACMediaList media) throws CSSException {
            MediaRule mr = new MediaRule();
            mr.setMediaList(media);
            mr.setParent(styleSheet);
            styleSheet.append(mr);
            styleSheet = mr;
        }
        public void endMedia(SACMediaList media) throws CSSException {
            styleSheet = styleSheet.getParent();
        }
        public void startPage(String name, String pseudo_page)
            throws CSSException {
        }
        public void endPage(String name, String pseudo_page)
            throws CSSException {
        }
        public void startFontFace() throws CSSException {
            styleDeclaration = new StyleDeclaration();
        }
        public void endFontFace() throws CSSException {
            StyleMap sm = new StyleMap(getNumberOfProperties());
            int len = styleDeclaration.size();
            for (int i=0; i<len; i++) {
                int idx = styleDeclaration.getIndex(i);
                sm.putValue(idx, styleDeclaration.getValue(i));
                sm.putImportant(idx, styleDeclaration.getPriority(i));
                sm.putOrigin(idx, StyleMap.AUTHOR_ORIGIN);
            }
            styleDeclaration = null;
            int pidx = getPropertyIndex(CSSConstants.CSS_FONT_FAMILY_PROPERTY);
            Value fontFamily = sm.getValue(pidx);
            if (fontFamily == null) return;
            ParsedURL base = getCSSBaseURI();
            fontFaces.add(new FontFaceRule(sm, base));
        }
        public void startSelector(SelectorList selectors) throws CSSException {
            styleRule = new StyleRule();
            styleRule.setSelectorList(selectors);
            styleDeclaration = new StyleDeclaration();
            styleRule.setStyleDeclaration(styleDeclaration);
            styleSheet.append(styleRule);
        }
        public void endSelector(SelectorList selectors) throws CSSException {
            styleRule = null;
            styleDeclaration = null;
        }
        public void property(String name, LexicalUnit value, boolean important)
            throws CSSException {
            int i = getPropertyIndex(name);
            if (i == -1) {
                i = getShorthandIndex(name);
                if (i == -1) {
                    return;
                }
                shorthandManagers[i].setValues(CSSEngine.this,
                                               this,
                                               value,
                                               important);
            } else {
                Value v = valueManagers[i].createValue(value, CSSEngine.this);
                styleDeclaration.append(v, i, important);
            }
        }
    }
    protected static class DocumentAdapter implements DocumentHandler {
        public void startDocument(InputSource source){
            throwUnsupportedEx();
        }
        public void endDocument(InputSource source) {
            throwUnsupportedEx();
        }
        public void comment(String text) {
        }
        public void ignorableAtRule(String atRule) {
            throwUnsupportedEx();
        }
        public void namespaceDeclaration(String prefix, String uri) {
            throwUnsupportedEx();
        }
        public void importStyle(String       uri,
                                SACMediaList media,
                                String       defaultNamespaceURI) {
            throwUnsupportedEx();
        }
        public void startMedia(SACMediaList media) {
            throwUnsupportedEx();
        }
        public void endMedia(SACMediaList media) {
            throwUnsupportedEx();
        }
        public void startPage(String name, String pseudo_page) {
            throwUnsupportedEx();
        }
        public void endPage(String name, String pseudo_page) {
            throwUnsupportedEx();
        }
        public void startFontFace() {
            throwUnsupportedEx();
        }
        public void endFontFace() {
            throwUnsupportedEx();
        }
        public void startSelector(SelectorList selectors) {
            throwUnsupportedEx();
        }
        public void endSelector(SelectorList selectors) {
            throwUnsupportedEx();
        }
        public void property(String name, LexicalUnit value, boolean important) {
            throwUnsupportedEx();
        }
        private void throwUnsupportedEx(){
            throw new UnsupportedOperationException("you try to use an empty method in Adapter-class" );
        }
    }
    protected static final CSSEngineListener[] LISTENER_ARRAY =
        new CSSEngineListener[0];
    public void addCSSEngineListener(CSSEngineListener l) {
        listeners.add(l);
    }
    public void removeCSSEngineListener(CSSEngineListener l) {
        listeners.remove(l);
    }
    protected void firePropertiesChangedEvent(Element target, int[] props) {
        CSSEngineListener[] ll =
            (CSSEngineListener[])listeners.toArray(LISTENER_ARRAY);
        int len = ll.length;
        if (len > 0) {
            CSSEngineEvent evt = new CSSEngineEvent(this, target, props);
            for (int i = 0; i < len; i++) {
                ll[i].propertiesChanged(evt);
            }
        }
    }
    protected void inlineStyleAttributeUpdated(CSSStylableElement elt,
                                               StyleMap style,
                                               short attrChange,
                                               String prevValue,
                                               String newValue) {
        boolean[] updated = styleDeclarationUpdateHandler.updatedProperties;
        for (int i = getNumberOfProperties() - 1; i >= 0; --i) {
            updated[i] = false;
        }
        switch (attrChange) {
        case MutationEvent.ADDITION:            
        case MutationEvent.MODIFICATION:
            if (newValue.length() > 0) {
                element = elt;
                try {
                    parser.setSelectorFactory(CSSSelectorFactory.INSTANCE);
                    parser.setConditionFactory(cssConditionFactory);
                    styleDeclarationUpdateHandler.styleMap = style;
                    parser.setDocumentHandler(styleDeclarationUpdateHandler);
                    parser.parseStyleDeclaration(newValue);
                    styleDeclarationUpdateHandler.styleMap = null;
                } catch (Exception e) {
                    String m = e.getMessage();
                    if (m == null) m = "";
                    String u = ((documentURI == null)?"<unknown>":
                                documentURI.toString());
                    String s = Messages.formatMessage
                        ("style.syntax.error.at",
                         new Object[] { u, styleLocalName, newValue, m });
                    DOMException de = new DOMException(DOMException.SYNTAX_ERR, s);
                    if (userAgent == null) throw de;
                    userAgent.displayError(de);
                } finally {
                    element = null;
                    cssBaseURI = null;
                }
            }
        case MutationEvent.REMOVAL:
            boolean removed = false;
            if (prevValue != null && prevValue.length() > 0) {
                for (int i = getNumberOfProperties() - 1; i >= 0; --i) {
                    if (style.isComputed(i) && !updated[i]) {
                        short origin = style.getOrigin(i);
                        if (origin >= StyleMap.INLINE_AUTHOR_ORIGIN) {     
                            removed = true;
                            updated[i] = true;
                        }
                    }
                }
            }
            if (removed) {
                invalidateProperties(elt, null, updated, true);
            } else {
                int count = 0;
                boolean fs = (fontSizeIndex == -1)
                    ? false
                    : updated[fontSizeIndex];
                boolean lh = (lineHeightIndex == -1)
                    ? false
                    : updated[lineHeightIndex];
                boolean cl = (colorIndex == -1)
                    ? false
                    : updated[colorIndex];
                for (int i = getNumberOfProperties() - 1; i >= 0; --i) {
                    if (updated[i]) {
                        count++;
                    }
                    else if ((fs && style.isFontSizeRelative(i)) ||
                             (lh && style.isLineHeightRelative(i)) ||
                             (cl && style.isColorRelative(i))) {
                        updated[i] = true;
                        clearComputedValue(style, i);
                        count++;
                    }
                }
                if (count > 0) {
                    int[] props = new int[count];
                    count = 0;
                    for (int i = getNumberOfProperties() - 1; i >= 0; --i) {
                        if (updated[i]) {
                            props[count++] = i;
                        }
                    }
                    invalidateProperties(elt, props, null, true);
                }
            }
            break;
        default:
            throw new IllegalStateException("Invalid attrChangeType");
        }
    }
    private static void clearComputedValue(StyleMap style, int n) {
        if (style.isNullCascaded(n)) {
            style.putValue(n, null);
        } else {
            Value v = style.getValue(n);
            if (v instanceof ComputedValue) {
                ComputedValue cv = (ComputedValue)v;
                v = cv.getCascadedValue();
                style.putValue(n, v);
            }
        }
        style.putComputed(n, false);
    }
    protected void invalidateProperties(Node node,
                                        int [] properties,
                                        boolean [] updated,
                                        boolean recascade) {
        if (!(node instanceof CSSStylableElement))
            return;  
        CSSStylableElement elt = (CSSStylableElement)node;
        StyleMap style = elt.getComputedStyleMap(null);
        if (style == null)
            return;  
        boolean [] diffs = new boolean[getNumberOfProperties()];
        if (updated != null) {
            System.arraycopy( updated, 0, diffs, 0, updated.length );
        }
        if (properties != null) {
            for (int i=0; i<properties.length; i++) {
                diffs[properties[i]] = true;
            }
        }
        int count =0;
        if (!recascade) {
            for (int i=0; i<diffs.length; i++) {
                if (diffs[i]) {
                    count++;
                }
            }
        } else {
            StyleMap newStyle = getCascadedStyleMap(elt, null);
            elt.setComputedStyleMap(null, newStyle);
            for (int i=0; i<diffs.length; i++) {
                if (diffs[i]) {
                    count++;
                    continue; 
                }
                Value nv = newStyle.getValue(i);
                Value ov = null;
                if (!style.isNullCascaded(i)) {
                    ov = style.getValue(i);
                    if (ov instanceof ComputedValue) {
                        ov = ((ComputedValue)ov).getCascadedValue();
                    }
                }
                if (nv == ov) continue;
                if ((nv != null) && (ov != null)) {
                    if (nv.equals(ov)) continue;
                    String ovCssText = ov.getCssText();
                    String nvCssText = nv.getCssText();
                    if ((nvCssText == ovCssText) ||
                        ((nvCssText != null) && nvCssText.equals(ovCssText)))
                        continue;
                }
                count++;
                diffs[i] = true;
            }
        }
        int []props = null;
        if (count != 0) {
            props = new int[count];
            count = 0;
            for (int i=0; i<diffs.length; i++) {
                if (diffs[i])
                    props[count++] = i;
            }
        }
        propagateChanges(elt, props, recascade);
    }
    protected void propagateChanges(Node node, int[] props,
                                    boolean recascade) {
        if (!(node instanceof CSSStylableElement))
            return;
        CSSStylableElement elt = (CSSStylableElement)node;
        StyleMap style = elt.getComputedStyleMap(null);
        if (style != null) {
            boolean[] updated =
                styleDeclarationUpdateHandler.updatedProperties;
            for (int i = getNumberOfProperties() - 1; i >= 0; --i) {
                updated[i] = false;
            }
            if (props != null) {
                for (int i = props.length - 1; i >= 0; --i) {
                    int idx = props[i];
                    updated[idx] = true;
                }
            }
            boolean fs = (fontSizeIndex == -1)
                ? false
                : updated[fontSizeIndex];
            boolean lh = (lineHeightIndex == -1)
                ? false
                : updated[lineHeightIndex];
            boolean cl = (colorIndex == -1)
                ? false
                : updated[colorIndex];
            int count = 0;
            for (int i = getNumberOfProperties() - 1; i >= 0; --i) {
                if (updated[i]) {
                    count++;
                }
                else if ((fs && style.isFontSizeRelative(i)) ||
                         (lh && style.isLineHeightRelative(i)) ||
                         (cl && style.isColorRelative(i))) {
                    updated[i] = true;
                    clearComputedValue(style, i);
                    count++;
                }
            }
            if (count == 0) {
                props = null;
            } else {
                props = new int[count];
                count = 0;
                for (int i = getNumberOfProperties() - 1; i >= 0; --i) {
                    if (updated[i]) {
                        props[count++] = i;
                    }
                }
                firePropertiesChangedEvent(elt, props);
            }
        }
        int [] inherited = props;
        if (props != null) {
            int count = 0;
            for (int i=0; i<props.length; i++) {
                ValueManager vm = valueManagers[props[i]];
                if (vm.isInheritedProperty()) count++;
                else props[i] = -1;
            }
            if (count == 0) {
                inherited = null;
            } else {
                inherited = new int[count];
                count=0;
                for (int i=0; i<props.length; i++)
                    if (props[i] != -1)
                        inherited[count++] = props[i];
            }
        }
        for (Node n = getCSSFirstChild(node);
             n != null;
             n = getCSSNextSibling(n)) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                invalidateProperties(n, inherited, null, recascade);
            }
        }
    }
    protected class StyleDeclarationUpdateHandler
        extends DocumentAdapter
        implements ShorthandManager.PropertyHandler {
        public StyleMap styleMap;
        public boolean[] updatedProperties =
            new boolean[getNumberOfProperties()];
        public void property(String name, LexicalUnit value, boolean important)
            throws CSSException {
            int i = getPropertyIndex(name);
            if (i == -1) {
                i = getShorthandIndex(name);
                if (i == -1) {
                    return;
                }
                shorthandManagers[i].setValues(CSSEngine.this,
                                               this,
                                               value,
                                               important);
            } else {
                if (styleMap.isImportant(i)) {
                    return;
                }
                updatedProperties[i] = true;
                Value v = valueManagers[i].createValue(value, CSSEngine.this);
                styleMap.putMask(i, (short)0);
                styleMap.putValue(i, v);
                styleMap.putOrigin(i, StyleMap.INLINE_AUTHOR_ORIGIN);
            }
        }
    }
    protected void nonCSSPresentationalHintUpdated(CSSStylableElement elt,
                                                   StyleMap style,
                                                   String property,
                                                   short attrChange,
                                                   String newValue) {
        int idx = getPropertyIndex(property);
        if (style.isImportant(idx)) {
            return;
        }
        if (style.getOrigin(idx) >= StyleMap.AUTHOR_ORIGIN) {
            return;
        }
        switch (attrChange) {
        case MutationEvent.ADDITION:   
        case MutationEvent.MODIFICATION:
            element = elt;
            try {
                LexicalUnit lu;
                lu = parser.parsePropertyValue(newValue);
                ValueManager vm = valueManagers[idx];
                Value v = vm.createValue(lu, CSSEngine.this);
                style.putMask(idx, (short)0);
                style.putValue(idx, v);
                style.putOrigin(idx, StyleMap.NON_CSS_ORIGIN);
            } catch (Exception e) {
                String m = e.getMessage();
                if (m == null) m = "";
                String u = ((documentURI == null)?"<unknown>":
                            documentURI.toString());
                String s = Messages.formatMessage
                    ("property.syntax.error.at",
                     new Object[] { u, property, newValue, m });
                DOMException de = new DOMException(DOMException.SYNTAX_ERR, s);
                if (userAgent == null) throw de;
                userAgent.displayError(de);
            } finally {
                element = null;
                cssBaseURI = null;
            }
            break;
        case MutationEvent.REMOVAL:
            {
                int [] invalid = { idx };
                invalidateProperties(elt, invalid, null, true);
                return;
            }
        }
        boolean[] updated = styleDeclarationUpdateHandler.updatedProperties;
        for (int i = getNumberOfProperties() - 1; i >= 0; --i) {
            updated[i] = false;
        }
        updated[idx] = true;
        boolean fs = idx == fontSizeIndex;
        boolean lh = idx == lineHeightIndex;
        boolean cl = idx == colorIndex;
        int count = 0;
        for (int i = getNumberOfProperties() - 1; i >= 0; --i) {
            if (updated[i]) {
                count++;
            }
            else if ((fs && style.isFontSizeRelative(i)) ||
                     (lh && style.isLineHeightRelative(i)) ||
                     (cl && style.isColorRelative(i))) {
                updated[i] = true;
                clearComputedValue(style, i);
                count++;
            }
        }
        int[] props = new int[count];
        count = 0;
        for (int i = getNumberOfProperties() - 1; i >= 0; --i) {
            if (updated[i]) {
                props[count++] = i;
            }
        }
        invalidateProperties(elt, props, null, true);
    }
    protected boolean hasStyleSheetNode(Node n) {
        if (n instanceof CSSStyleSheetNode) {
            return true;
        }
        n = getCSSFirstChild(n);
        while (n != null) {
            if (hasStyleSheetNode(n)) {
                return true;
            }
            n = getCSSNextSibling(n);
        }
        return false;
    }
    protected void handleAttrModified(Element e,
                                      Attr attr,
                                      short attrChange,
                                      String prevValue,
                                      String newValue) {
        if (!(e instanceof CSSStylableElement)) {
            return;
        }
        if (newValue.equals(prevValue)) {
            return;  
        }
        String attrNS = attr.getNamespaceURI();
        String name = attrNS == null ? attr.getNodeName() : attr.getLocalName();
        CSSStylableElement elt = (CSSStylableElement) e;
        StyleMap style = elt.getComputedStyleMap(null);
        if (style != null) {
            if (attrNS == styleNamespaceURI
                    || attrNS != null && attrNS.equals(styleNamespaceURI)) {
                if (name.equals(styleLocalName)) {
                    inlineStyleAttributeUpdated
                        (elt, style, attrChange, prevValue, newValue);
                    return;
                }
            }
            if (nonCSSPresentationalHints != null) {
                if (attrNS == nonCSSPresentationalHintsNamespaceURI ||
                        attrNS != null &&
                        attrNS.equals(nonCSSPresentationalHintsNamespaceURI)) {
                    if (nonCSSPresentationalHints.contains(name)) {
                        nonCSSPresentationalHintUpdated
                            (elt, style, name, attrChange, newValue);
                        return;
                    }
                }
            }
        }
        if (selectorAttributes != null &&
            selectorAttributes.contains(name)) {
            invalidateProperties(elt, null, null, true);
            for (Node n = getCSSNextSibling(elt);
                 n != null;
                 n = getCSSNextSibling(n)) {
                invalidateProperties(n, null, null, true);
            }
        }
    }
    protected void handleNodeInserted(Node n) {
        if (hasStyleSheetNode(n)) {
            styleSheetNodes = null;
            invalidateProperties(document.getDocumentElement(),
                                 null, null, true);
        } else if (n instanceof CSSStylableElement) {
            n = getCSSNextSibling(n);
            while (n != null) {
                invalidateProperties(n, null, null, true);
                n = getCSSNextSibling(n);
            }
        }
    }
    protected void handleNodeRemoved(Node n) {
        if (hasStyleSheetNode(n)) {
            styleSheetRemoved = true;
        } else if (n instanceof CSSStylableElement) {
            removedStylableElementSibling = getCSSNextSibling(n);
        }
        disposeStyleMaps(n);
    }
    protected void handleSubtreeModified(Node ignored) {
        if (styleSheetRemoved) {
            styleSheetRemoved = false;
            styleSheetNodes = null;
            invalidateProperties(document.getDocumentElement(),
                                 null, null, true);
        } else if (removedStylableElementSibling != null) {
            Node n = removedStylableElementSibling;
            while (n != null) {
                invalidateProperties(n, null, null, true);
                n = getCSSNextSibling(n);
            }
            removedStylableElementSibling = null;
        }
    }
    protected void handleCharacterDataModified(Node n) {
        if (getCSSParentNode(n) instanceof CSSStyleSheetNode) {
            styleSheetNodes = null;
            invalidateProperties(document.getDocumentElement(),
                                 null, null, true);
        }
    }
    protected class CSSNavigableDocumentHandler
            implements CSSNavigableDocumentListener,
                       MainPropertyReceiver {
        protected boolean[] mainPropertiesChanged;
        protected StyleDeclaration declaration;
        public void nodeInserted(Node newNode) {
            handleNodeInserted(newNode);
        }
        public void nodeToBeRemoved(Node oldNode) {
            handleNodeRemoved(oldNode);
        }
        public void subtreeModified(Node rootOfModifications) {
            handleSubtreeModified(rootOfModifications);
        }
        public void characterDataModified(Node text) {
            handleCharacterDataModified(text);
        }
        public void attrModified(Element e,
                                 Attr attr,
                                 short attrChange,
                                 String prevValue,
                                 String newValue) {
            handleAttrModified(e, attr, attrChange, prevValue, newValue);
        }
        public void overrideStyleTextChanged(CSSStylableElement elt,
                                             String text) {
            StyleDeclarationProvider p =
                elt.getOverrideStyleDeclarationProvider();
            StyleDeclaration declaration = p.getStyleDeclaration();
            int ds = declaration.size();
            boolean[] updated = new boolean[getNumberOfProperties()];
            for (int i = 0; i < ds; i++) {
                updated[declaration.getIndex(i)] = true;
            }
            declaration = parseStyleDeclaration(elt, text);
            p.setStyleDeclaration(declaration);
            ds = declaration.size();
            for (int i = 0; i < ds; i++) {
                updated[declaration.getIndex(i)] = true;
            }
            invalidateProperties(elt, null, updated, true);
        }
        public void overrideStylePropertyRemoved(CSSStylableElement elt,
                                                 String name) {
            StyleDeclarationProvider p =
                elt.getOverrideStyleDeclarationProvider();
            StyleDeclaration declaration = p.getStyleDeclaration();
            int idx = getPropertyIndex(name);
            int ds = declaration.size();
            for (int i = 0; i < ds; i++) {
                if (idx == declaration.getIndex(i)) {
                    declaration.remove(i);
                    StyleMap style = elt.getComputedStyleMap(null);
                    if (style != null
                            && style.getOrigin(idx) == StyleMap.OVERRIDE_ORIGIN
                            ) {
                        invalidateProperties
                            (elt, new int[] { idx }, null, true);
                    }
                    break;
                }
            }
        }
        public void overrideStylePropertyChanged(CSSStylableElement elt,
                                                 String name, String val,
                                                 String prio) {
            boolean important = prio != null && prio.length() != 0;
            StyleDeclarationProvider p =
                elt.getOverrideStyleDeclarationProvider();
            declaration = p.getStyleDeclaration();
            setMainProperties(elt, this, name, val, important);
            declaration = null;
            invalidateProperties(elt, null, mainPropertiesChanged, true);
        }
        public void setMainProperty(String name, Value v, boolean important) {
            int idx = getPropertyIndex(name);
            if (idx == -1) {
                return;   
            }
            int i;
            for (i = 0; i < declaration.size(); i++) {
                if (idx == declaration.getIndex(i)) {
                    break;
                }
            }
            if (i < declaration.size()) {
                declaration.put(i, v, idx, important);
            } else {
                declaration.append(v, idx, important);
            }
        }
    }
    protected class DOMNodeInsertedListener implements EventListener {
        public void handleEvent(Event evt) {
            handleNodeInserted((Node) evt.getTarget());
        }
    }
    protected class DOMNodeRemovedListener implements EventListener {
        public void handleEvent(Event evt) {
            handleNodeRemoved((Node) evt.getTarget());
        }
    }
    protected class DOMSubtreeModifiedListener implements EventListener {
        public void handleEvent(Event evt) {
            handleSubtreeModified((Node) evt.getTarget());
        }
    }
    protected class DOMCharacterDataModifiedListener implements EventListener {
        public void handleEvent(Event evt) {
            handleCharacterDataModified((Node) evt.getTarget());
        }
    }
    protected class DOMAttrModifiedListener implements EventListener {
        public void handleEvent(Event evt) {
            MutationEvent mevt = (MutationEvent) evt;
            handleAttrModified((Element) evt.getTarget(),
                               (Attr) mevt.getRelatedNode(),
                               mevt.getAttrChange(),
                               mevt.getPrevValue(),
                               mevt.getNewValue());
        }
    }
}
