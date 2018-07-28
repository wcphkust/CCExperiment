package org.apache.log4j.chainsaw;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
class LoadXMLAction
    extends AbstractAction
{
    private static final Logger LOG = Logger.getLogger(LoadXMLAction.class);
    private final JFrame mParent;
    private final JFileChooser mChooser = new JFileChooser();
    {
        mChooser.setMultiSelectionEnabled(false);
        mChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    }
    private final XMLReader mParser;
    private final XMLFileHandler mHandler;
    LoadXMLAction(JFrame aParent, MyTableModel aModel)
        throws SAXException, ParserConfigurationException
    {
        mParent = aParent;
        mHandler = new XMLFileHandler(aModel);
        mParser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        mParser.setContentHandler(mHandler);
    }
    public void actionPerformed(ActionEvent aIgnore) {
        LOG.info("load file called");
        if (mChooser.showOpenDialog(mParent) == JFileChooser.APPROVE_OPTION) {
            LOG.info("Need to load a file");
            final File chosen = mChooser.getSelectedFile();
            LOG.info("loading the contents of " + chosen.getAbsolutePath());
            try {
                final int num = loadFile(chosen.getAbsolutePath());
                JOptionPane.showMessageDialog(
                    mParent,
                    "Loaded " + num + " events.",
                    "CHAINSAW",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                LOG.warn("caught an exception loading the file", e);
                JOptionPane.showMessageDialog(
                    mParent,
                    "Error parsing file - " + e.getMessage(),
                    "CHAINSAW",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private int loadFile(String aFile)
        throws SAXException, IOException
    {
        synchronized (mParser) {
            final StringBuffer buf = new StringBuffer();
            buf.append("<?xml version=\"1.0\" standalone=\"yes\"?>\n");
            buf.append("<!DOCTYPE log4j:eventSet ");
            buf.append("[<!ENTITY data SYSTEM \"file:///");
            buf.append(aFile);
            buf.append("\">]>\n");
            buf.append("<log4j:eventSet xmlns:log4j=\"Claira\">\n");
            buf.append("&data;\n");
            buf.append("</log4j:eventSet>\n");
            final InputSource is =
                new InputSource(new StringReader(buf.toString()));
            mParser.parse(is);
            return mHandler.getNumEvents();
        }
    }
}