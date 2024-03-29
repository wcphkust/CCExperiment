package org.apache.xerces.parsers;
import java.util.Vector;
import org.apache.xerces.dom.ASModelImpl;
import org.apache.xerces.dom3.as.ASModel;
import org.apache.xerces.dom3.as.DOMASBuilder;
import org.apache.xerces.dom3.as.DOMASException;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.xs.SchemaGrammar;
import org.apache.xerces.impl.xs.XSGrammarBucket;
import org.apache.xerces.util.SymbolTable;
import org.apache.xerces.util.XMLGrammarPoolImpl;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarPool;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.w3c.dom.ls.LSInput;
public class DOMASBuilderImpl
    extends DOMParserImpl implements DOMASBuilder {
    protected static final String SCHEMA_FULL_CHECKING =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING;
    protected static final String ERROR_REPORTER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;
    protected static final String SYMBOL_TABLE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;
    protected static final String ENTITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_MANAGER_PROPERTY;
    protected XSGrammarBucket fGrammarBucket;
    protected ASModelImpl fAbstractSchema;
    public DOMASBuilderImpl() {
        super(new XMLGrammarCachingConfiguration());
    } 
    public DOMASBuilderImpl(XMLGrammarCachingConfiguration config) {
        super(config);
    } 
    public DOMASBuilderImpl(SymbolTable symbolTable) {
        super(new XMLGrammarCachingConfiguration(symbolTable));
    } 
    public DOMASBuilderImpl(SymbolTable symbolTable, XMLGrammarPool grammarPool) {
        super(new XMLGrammarCachingConfiguration(symbolTable, grammarPool));
    }
    public ASModel getAbstractSchema() {
        return fAbstractSchema;
    }
    public void setAbstractSchema(ASModel abstractSchema) {
        fAbstractSchema = (ASModelImpl)abstractSchema;
        XMLGrammarPool grammarPool = (XMLGrammarPool)fConfiguration.getProperty(StandardParserConfiguration.XMLGRAMMAR_POOL);
        if (grammarPool == null) {
            grammarPool = new XMLGrammarPoolImpl();
            fConfiguration.setProperty(StandardParserConfiguration.XMLGRAMMAR_POOL,
                                       grammarPool);
        }
        if (fAbstractSchema != null) {
            initGrammarPool(fAbstractSchema, grammarPool);
        }
    }
    public ASModel parseASURI(String uri)
                              throws DOMASException, Exception {
        XMLInputSource source = new XMLInputSource(null, uri, null);
        return parseASInputSource(source);
    }
    public ASModel parseASInputSource(LSInput is)
                                      throws DOMASException, Exception {
        XMLInputSource xis = this.dom2xmlInputSource(is);
        try {
            return parseASInputSource(xis);
        }
        catch (XNIException e) {
            Exception ex = e.getException();
            throw ex;
        }
    }
    ASModel parseASInputSource(XMLInputSource is) throws Exception {
        if (fGrammarBucket == null) {
            fGrammarBucket = new XSGrammarBucket();
        }
        initGrammarBucket();
        XMLGrammarCachingConfiguration gramConfig = (XMLGrammarCachingConfiguration)fConfiguration;
        gramConfig.lockGrammarPool();
        SchemaGrammar grammar = gramConfig.parseXMLSchema(is);
        gramConfig.unlockGrammarPool();
        ASModelImpl newAsModel = null;
        if (grammar != null) {
            newAsModel = new ASModelImpl();
            fGrammarBucket.putGrammar (grammar, true);
            addGrammars(newAsModel, fGrammarBucket);
        }
        return newAsModel;
    }
    private void initGrammarBucket() {
        fGrammarBucket.reset();
        if (fAbstractSchema != null)
            initGrammarBucketRecurse(fAbstractSchema);
    }
    private void initGrammarBucketRecurse(ASModelImpl currModel) {
        if(currModel.getGrammar() != null) {
            fGrammarBucket.putGrammar(currModel.getGrammar());
        }
        for(int i = 0; i < currModel.getInternalASModels().size(); i++) {
            ASModelImpl nextModel = (ASModelImpl)(currModel.getInternalASModels().elementAt(i));
            initGrammarBucketRecurse(nextModel);
        }
    }
    private void addGrammars(ASModelImpl model, XSGrammarBucket grammarBucket) {
        SchemaGrammar [] grammarList = grammarBucket.getGrammars();
        for(int i=0; i<grammarList.length; i++) {
            ASModelImpl newModel = new ASModelImpl();
            newModel.setGrammar(grammarList[i]);
            model.addASModel(newModel);
        }
    } 
    private void initGrammarPool(ASModelImpl currModel, XMLGrammarPool grammarPool) {
        Grammar[] grammars = new Grammar[1];
        if ((grammars[0] = (Grammar)currModel.getGrammar()) != null) {
            grammarPool.cacheGrammars(grammars[0].getGrammarDescription().getGrammarType(), grammars);
        }
        Vector modelStore = currModel.getInternalASModels();
        for (int i = 0; i < modelStore.size(); i++) {
            initGrammarPool((ASModelImpl)modelStore.elementAt(i), grammarPool);
        }
    }
} 
