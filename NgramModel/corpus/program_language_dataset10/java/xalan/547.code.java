package org.apache.xalan.xsltc.trax;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Properties;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.xml.XMLConstants;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.URIResolver;
import org.apache.xalan.xsltc.DOM;
import org.apache.xalan.xsltc.Translet;
import org.apache.xalan.xsltc.compiler.util.ErrorMsg;
import org.apache.xalan.xsltc.runtime.AbstractTranslet;
import org.apache.xalan.xsltc.runtime.Hashtable;
public final class TemplatesImpl implements Templates, Serializable {
    static final long serialVersionUID = 673094361519270707L;
    private static String ABSTRACT_TRANSLET 
	= "org.apache.xalan.xsltc.runtime.AbstractTranslet";
    private String _name = null;
    private byte[][] _bytecodes = null;
    private Class[] _class = null;
    private int _transletIndex = -1;
    private Hashtable _auxClasses = null;
    private Properties _outputProperties; 
    private int _indentNumber;
    private transient URIResolver _uriResolver = null;
    private transient ThreadLocal _sdom = new ThreadLocal();
    private transient TransformerFactoryImpl _tfactory = null;
    static final class TransletClassLoader extends ClassLoader {
	TransletClassLoader(ClassLoader parent) {
	    super(parent);
	}
	Class defineClass(final byte[] b) {
            return defineClass(null, b, 0, b.length);
	}
    }
    protected TemplatesImpl(byte[][] bytecodes, String transletName,
	Properties outputProperties, int indentNumber,
	TransformerFactoryImpl tfactory) 
    {
	_bytecodes = bytecodes;
	_name      = transletName;
	_outputProperties = outputProperties;
	_indentNumber = indentNumber;
	_tfactory = tfactory;
    }
    protected TemplatesImpl(Class[] transletClasses, String transletName,
	Properties outputProperties, int indentNumber,
	TransformerFactoryImpl tfactory) 
    {
	_class     = transletClasses;
	_name      = transletName;
	_transletIndex = 0;
	_outputProperties = outputProperties;
	_indentNumber = indentNumber;
	_tfactory = tfactory;
    }
    public TemplatesImpl() { }
    private void  readObject(ObjectInputStream is) 
      throws IOException, ClassNotFoundException 
    {
	is.defaultReadObject();
        if (is.readBoolean()) {
            _uriResolver = (URIResolver) is.readObject();
        }
	_tfactory = new TransformerFactoryImpl();
    } 
    private void writeObject(ObjectOutputStream os)
        throws IOException, ClassNotFoundException {
        os.defaultWriteObject();
        if (_uriResolver instanceof Serializable) {
            os.writeBoolean(true);
            os.writeObject((Serializable) _uriResolver);
        }
        else {
            os.writeBoolean(false);
        }
    }
    public synchronized void setURIResolver(URIResolver resolver) {
	_uriResolver = resolver;
    }
    protected synchronized void setTransletBytecodes(byte[][] bytecodes) {
	_bytecodes = bytecodes;
    }
    public synchronized byte[][] getTransletBytecodes() {
	return _bytecodes;
    }
    public synchronized Class[] getTransletClasses() {
	try {
	    if (_class == null) defineTransletClasses();
	}
	catch (TransformerConfigurationException e) {
	}
	return _class;
    }
    public synchronized int getTransletIndex() {
	try {
	    if (_class == null) defineTransletClasses();
	}
	catch (TransformerConfigurationException e) {
	}
	return _transletIndex;
    }
    protected synchronized void setTransletName(String name) {
	_name = name;
    }
    protected synchronized String getTransletName() {
	return _name;
    }
    private void defineTransletClasses()
	throws TransformerConfigurationException {
	if (_bytecodes == null) {
	    ErrorMsg err = new ErrorMsg(ErrorMsg.NO_TRANSLET_CLASS_ERR);
	    throw new TransformerConfigurationException(err.toString());
	}
        TransletClassLoader loader = (TransletClassLoader)
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    return new TransletClassLoader(ObjectFactory.findClassLoader());
                }
            });
	try {
	    final int classCount = _bytecodes.length;
	    _class = new Class[classCount];
	    if (classCount > 1) {
	        _auxClasses = new Hashtable();
	    }
	    for (int i = 0; i < classCount; i++) {
		_class[i] = loader.defineClass(_bytecodes[i]);
		final Class superClass = _class[i].getSuperclass();
		if (superClass.getName().equals(ABSTRACT_TRANSLET)) {
		    _transletIndex = i;
		}
		else {
		    _auxClasses.put(_class[i].getName(), _class[i]);
		}
	    }
	    if (_transletIndex < 0) {
		ErrorMsg err= new ErrorMsg(ErrorMsg.NO_MAIN_TRANSLET_ERR, _name);
		throw new TransformerConfigurationException(err.toString());
	    }
	}
	catch (ClassFormatError e) {
	    ErrorMsg err = new ErrorMsg(ErrorMsg.TRANSLET_CLASS_ERR, _name);
	    throw new TransformerConfigurationException(err.toString());
	}
	catch (LinkageError e) {
	    ErrorMsg err = new ErrorMsg(ErrorMsg.TRANSLET_OBJECT_ERR, _name);
	    throw new TransformerConfigurationException(err.toString());
	}
    }
    private Translet getTransletInstance()
	throws TransformerConfigurationException {
	try {
	    if (_name == null) return null;
	    if (_class == null) defineTransletClasses();
	    AbstractTranslet translet = (AbstractTranslet) _class[_transletIndex].newInstance();
            translet.postInitialization();
	    translet.setTemplates(this);
	    if (_auxClasses != null) {
	        translet.setAuxiliaryClasses(_auxClasses);
	    }
	    return translet;
	}
	catch (InstantiationException e) {
	    ErrorMsg err = new ErrorMsg(ErrorMsg.TRANSLET_OBJECT_ERR, _name);
	    throw new TransformerConfigurationException(err.toString());
	}
	catch (IllegalAccessException e) {
	    ErrorMsg err = new ErrorMsg(ErrorMsg.TRANSLET_OBJECT_ERR, _name);
	    throw new TransformerConfigurationException(err.toString());
	}
    }
    public synchronized Transformer newTransformer()
	throws TransformerConfigurationException 
    {
	TransformerImpl transformer;
	transformer = new TransformerImpl(getTransletInstance(), _outputProperties,
	    _indentNumber, _tfactory);
	if (_uriResolver != null) {
	    transformer.setURIResolver(_uriResolver);
	}
	if (_tfactory.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING)) {
	    transformer.setSecureProcessing(true);
	}
	return transformer;
    }
    public synchronized Properties getOutputProperties() { 
	try {
	    return newTransformer().getOutputProperties();
	}
	catch (TransformerConfigurationException e) {
	    return null;
	}
    }
    public DOM getStylesheetDOM() {
    	return (DOM)_sdom.get();
    }
    public void setStylesheetDOM(DOM sdom) {
    	_sdom.set(sdom);
    }
}
