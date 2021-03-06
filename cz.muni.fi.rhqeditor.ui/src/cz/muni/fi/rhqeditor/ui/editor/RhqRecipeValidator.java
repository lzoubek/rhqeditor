package cz.muni.fi.rhqeditor.ui.editor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

import cz.muni.fi.rhqeditor.core.rhqmodel.RhqAttribute;
import cz.muni.fi.rhqeditor.core.rhqmodel.RhqModelReader;
import cz.muni.fi.rhqeditor.core.rhqmodel.RhqTask;
import cz.muni.fi.rhqeditor.core.utils.DocumentProvider;
import cz.muni.fi.rhqeditor.core.utils.ExtractorProvider;
import cz.muni.fi.rhqeditor.core.utils.InputPropertiesManager;
import cz.muni.fi.rhqeditor.core.utils.RecipeReader;
import cz.muni.fi.rhqeditor.core.utils.RhqConstants;
import cz.muni.fi.rhqeditor.core.utils.RhqPathExtractor;
import cz.muni.fi.rhqeditor.ui.UiActivator;


/**
 * Class extends uses SAX parser to parse recipe and uses RhqAnnotation model to manage markers.
 * @author syche
 *
 */
public class RhqRecipeValidator extends DefaultHandler2 {
	
	
	private RhqPathExtractor	fRhqPathExtractor	= null;
	private RhqAnnotationModel	fRhqAnnotationModel = null;
	private SAXParserFactory 	fParserFactory 		= null;
	private SAXParser		 	fParser				= null;

	private IDocument			fDocument 			= null;
	private String				fOpenArchiveName 	= null;
	private HashMap<String, Integer > fExistingTargets	= null;
	private HashMap<String,	Integer > fRequiredTargets  = null;
	
	
	private RhqModelReader fRhqModelReader = null;
	private InputPropertiesManager fInputPropertiesManager;
	
	//contains all rhq tasks and required atts
	private static final String EMPTY_STRING = "";
	private Stack<String> openElements;
	private Locator locator;
	
	
	
	/**
	 * 
	 * @param recipe	- IResource corresponding to recipe ("deploy.xml")
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public RhqRecipeValidator(IProject project) {
		fRhqPathExtractor = ExtractorProvider.INSTANCE.getExtractor(project);
		fDocument = DocumentProvider.INSTANCE.getDocument(project);
		fInputPropertiesManager = new InputPropertiesManager(project.getName());
		openElements = new Stack<>();
		fRequiredTargets = new HashMap<String, Integer>();
		fExistingTargets = new HashMap<String, Integer>();
		try {			
			fParserFactory = SAXParserFactory.newInstance();
			fParser = fParserFactory.newSAXParser();
		} catch (SAXException e) {
			e.printStackTrace();	
		} catch(ParserConfigurationException e){
			e.printStackTrace();
		}
	}
	
	public void setAnnotationModel(RhqAnnotationModel model){
		fRhqAnnotationModel = model;
	}
	
	
	private RhqModelReader getModelReader(){
		if(fRhqModelReader == null)
			fRhqModelReader = new RhqModelReader(0);
    	return fRhqModelReader;
	}



	public void validateRecipe(){
		
		try {
			String text = fDocument.get();
			ByteArrayInputStream bs = new ByteArrayInputStream(text.getBytes());
			InputSource input = new InputSource(bs);
			
			if(input != null){
				fRhqAnnotationModel.removeMarkers();
				fParser.parse(input, this);
			}
			
		} catch (SAXException e) {
			//do nothing if document isn't well formed
		} catch (IOException e) {
			UiActivator.getLogger().log(new Status(IStatus.WARNING,RhqConstants.PLUGIN_UI_ID, e.getMessage()));
		}
	}
	
	@Override
	public void setDocumentLocator(Locator loc){
		locator = loc;
	}
	
	@Override
	public void startDocument() throws SAXException {
			fRhqAnnotationModel.removeMarkers();
			fOpenArchiveName = null;
			fExistingTargets.clear();
			fRequiredTargets.clear();
			//init property map on each validation
			fInputPropertiesManager.initalizeAntPropertyMap();
	}
	
	@Override
	public void endDocument() throws SAXException {
		manageTargets();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		
		//if task isn't rhq task do nothing, except include
		RhqTask currentTask = getModelReader().getTask(qName);
		if(currentTask == null && !qName.equals("include")){
			openElements.push(qName);
			super.startElement(uri, localName, qName, attributes);
			return;
		}
			
		//managing targets
		String attrValue;
		IPath attrPath ;
		checkAttributesAndSetMarkers(qName, attributes);
		checkParentAndSetMarkers(currentTask,openElements.peek());
		String currentTaskName = (currentTask == null ? qName : currentTask.getName());
		
		switch(currentTaskName){
		case  RhqConstants.RHQ_TYPE_ARCHIVE:
			
			attrValue = attributes.getValue(RhqConstants.RHQ_ATTRIBUTE_NAME);
			if(attrValue == null || attrValue.equals(EMPTY_STRING)){
				fOpenArchiveName = EMPTY_STRING;
				break;
			}
			
			//try to fix properties in filename
			attrValue = unpropertizePath(attrValue);
			
			attrPath = new Path(attrValue);
			
			if(attrPath.toString().startsWith(RhqConstants.RHQ_DEFAULT_BUILD_DIR) ||
					attrPath.toString().startsWith(RhqConstants.RHQ_DEFAULT_DEPLOY_DIR)) {
				fRhqAnnotationModel.addMarker(locator.getLineNumber(), "Forbidden destination", IMarker.SEVERITY_ERROR);
				break;
			}
			
			
			
			//check whether archive name consist property variable
			if(!isPropertized(attrValue)) {
				//ignore check if name contains property variable
				if (!fRhqPathExtractor.isPathToArchiveValid(attrPath))
					fRhqAnnotationModel.addMarker(locator.getLineNumber(), "Archive not found", IMarker.SEVERITY_WARNING);
			}
			
			
			fOpenArchiveName = attrValue;
			break;
			
		case  RhqConstants.RHQ_TYPE_FILE:
			
			attrValue = attributes.getValue("name");
			if (attrValue == null || attrValue.equals(EMPTY_STRING)) {
				break;
			}
			//try to fix properties in filename
			attrValue = unpropertizePath(attrValue);
			
			attrPath = new Path(attrValue);
			
			//ignore rhq:file name=file.${property}
			if( isPropertized(attrPath.toString()))
				break;
			
			//file can have only one only destinationDir or destinationFile, not both
			if(attributes.getIndex("destinationFile") > -1 && attributes.getIndex("destinationDir") > -1){
				fRhqAnnotationModel.addMarker(locator.getLineNumber(), 
						"File can't have specified destinationFile and destinationDir at the same time", IMarker.SEVERITY_WARNING);
			}
				
			if(attrPath.toString().startsWith(RhqConstants.RHQ_DEFAULT_BUILD_DIR) ||
					attrPath.toString().startsWith(RhqConstants.RHQ_DEFAULT_DEPLOY_DIR)) {
				fRhqAnnotationModel.addMarker(locator.getLineNumber(), "Forbidden destination", IMarker.SEVERITY_ERROR);
				break;
			}
			if(!fRhqPathExtractor.isPathToFileValid(attrPath)) {
				fRhqAnnotationModel.addMarker(locator.getLineNumber(), "File '"+ attrValue+"' not found", IMarker.SEVERITY_WARNING);	
				
			}

			break;
		
		case RhqConstants.RHQ_TYPE_FILESET:
			//fileset in rhq:ignore is not validated
			attrValue = attributes.getValue(RhqConstants.RHQ_ATTRIBUTE_INCLUDES);
			//includes not found or no parent archive open
			if(attrValue == null || attrValue.equals(EMPTY_STRING)|| fOpenArchiveName == null || isPropertized(fOpenArchiveName) ) {
				break;
			}
			//try to fix properties in filename
			attrValue = unpropertizePath(attrValue);
			attrPath = new Path(attrValue);
			
			if(fOpenArchiveName.equals(EMPTY_STRING)){
				fRhqAnnotationModel.addMarker(locator.getLineNumber(), "Unrecognized path to archive", IMarker.SEVERITY_WARNING);
			} else if ( !isPropertized(attrPath.toString())
					&& !fRhqPathExtractor.isPathToArchiveFileValid(attrPath, fOpenArchiveName)) {
						//validate filenames if archive name contains no property
						fRhqAnnotationModel.addMarker(locator.getLineNumber(), "There's no file '"+ attrValue +"' in archive " + fOpenArchiveName	, IMarker.SEVERITY_WARNING);
			}
			
			break;
		
		case "include":
			attrValue = attributes.getValue(RhqConstants.RHQ_ATTRIBUTE_NAME);
			//only include with parent archive matters
			if(attrValue == null || attrValue.equals(EMPTY_STRING)|| fOpenArchiveName == null || isPropertized(fOpenArchiveName) ) {
				break;
			}
			//try to fix properties in filename
			attrValue = unpropertizePath(attrValue);
		    attrPath = new Path(attrValue);
		    if(fOpenArchiveName.equals(EMPTY_STRING)){
				fRhqAnnotationModel.addMarker(locator.getLineNumber(), "Unrecognized path to archive", IMarker.SEVERITY_WARNING);
		    } else if ( !isPropertized(attrPath.toString())
					&& !fRhqPathExtractor.isPathToArchiveFileValid(attrPath, fOpenArchiveName)) {
						fRhqAnnotationModel.addMarker(locator.getLineNumber(), "There's no file '"+ attrValue +"' in archive " + fOpenArchiveName, IMarker.SEVERITY_WARNING);
			}
			
		    break;
			
		}
		
		
		openElements.push(qName);
		super.startElement(uri, localName, qName, attributes);
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) {
		if(openElements.peek() != qName){
			//this means not well formed document which will cause SAX exception anyway
			return;
		}
		openElements.pop();
		if(qName.equals(RecipeReader.getRhqNamespacePrefix(fDocument.get()) + RhqConstants.RHQ_TYPE_ARCHIVE)){
			fOpenArchiveName = null;
		}
	}
	
	/**
	 * makes intersect of existing and required targets and puts marker on unpaired ones
	 */
	private void manageTargets(){
		for(String target: fRequiredTargets.keySet()){
			if(!fExistingTargets.containsKey(target)){
				fRhqAnnotationModel.addMarker(fRequiredTargets.get(target).intValue(), "Target\""+target+"not found", IMarker.SEVERITY_WARNING);
			}
		}
	}
	
		
	/**
	 * checcs whether element has all required attributes
	 * @param elementName
	 * @param atts
	 */
	private void checkAttributesAndSetMarkers(String elementName, Attributes atts){
		RhqTask task = getModelReader().getTask(elementName);
		if(task == null || atts == null)
			return;
		
		for(RhqAttribute attr: task.getAttributes()){
			String attrValue =  atts.getValue(attr.getName());
			if(attr.isRequired()){
				if(attrValue == null || attrValue.equals(EMPTY_STRING))
					fRhqAnnotationModel.addMarker(locator.getLineNumber(), "Attribute is mandatory: "+attr.getName(), IMarker.SEVERITY_WARNING);
			} else {
				if(attrValue != null && attrValue.equals(EMPTY_STRING))
					fRhqAnnotationModel.addMarker(locator.getLineNumber(), "Empty attribute: "+attr.getName(), IMarker.SEVERITY_WARNING);
				}
			}
	}
	
	/**
	 * checks whether element parent is one of required parents
	 */
	private void checkParentAndSetMarkers(RhqTask child, String parentName){
		if(child == null || parentName == null)
			return;
		if(child.canBePlacedInAnyTask() == true)
			return;
		
		for(String parent: child.getAllParentNames()){
			if(parent.equals(RhqModelReader.removeNamespacePrefix(parentName)))
				return;
		}
		fRhqAnnotationModel.addMarker(locator.getLineNumber(), "Misplaced element "+child.getName(), IMarker.SEVERITY_WARNING);
	}
	
	/**
	 * checks whether given filename contains some property (sytax ${....} )
	 */
	private boolean isPropertized(String filename) {
		return filename.matches(".*\\$\\{.*\\}.*");
	}
	
	/**
	 * returns given string with replaced values of known properties
	 * @param path
	 * @return
	 */
	private String unpropertizePath( String path ) {
		if(isPropertized(path)) {
			return fInputPropertiesManager.resolveNameWithProperty(path);
		}
		return path;
	}
	
	
	
	

}
