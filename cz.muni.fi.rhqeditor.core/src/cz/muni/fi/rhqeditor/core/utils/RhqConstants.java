package cz.muni.fi.rhqeditor.core.utils;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;


public class RhqConstants {
	
	private RhqConstants() {}

	public static final int 	RHQ_VERSION_DEFAULT 	= 0;
	public static final int 	RHQ_VERSION_4_6_0 		= 0;
	
									
	public static final String PLUGIN_UI_ID = "cz.muni.fi.rhqeditor.ui";
	public static final String PLUGIN_CORE_ID = "cz.muni.fi.rhqeditor.core";
	public static final String 	RHQ_NATURE_ID =  "cz.muni.fi.rhqeditor.natures.rhqeditornature";
	public static final String	RHQ_PREFIX = "rhq:";
	
	public static final String 	RHQ_ARCHIVE_ZIP_SUFFIX 	= ".zip";
	public static final String	RHQ_ARCHIVE_JAR_SUFFIX 	= ".jar";
	
	public static final String 	RHQ_RECIPE_FILE 			= "deploy.xml";
	
	public static final String	RHQ_TYPE_FILE 				= "file";
	public static final String 	RHQ_TYPE_ARCHIVE 			= "archive";
	public static final String 	RHQ_TYPE_DEPLOYMENT_UNIT 	= "deployment-unit";
	public static final String	RHQ_TYPE_INPUT_PROPERTY 	= "input-property";
	public static final String 	RHQ_TYPE_URL_ARCHIVE 		= "url_archive";
	public static final String 	RHQ_TYPE_REPLACE 			= "replace";	
	public static final String	RHQ_TYPE_IGNORE 			= "ignore";
	public static final String 	RHQ_TYPE_URL_FILE 			= "url_file";
	public static final String 	RHQ_TYPE_AUDIT			 	= "audit";	
	public static final String 	RHQ_TYPE_FILESET			= "fileset";
	public static final String  RHQ_TYPE_BUNDLE				= "bundle";
	
	
	
	public static final String RHQ_ATTRIBUTE_NAME		= "name";
	public static final String RHQ_ATTRIBUTE_INCLUDES   = "includes";
	public static final String RHQ_ATTRIBUTE_PREINSTALL_TARGET = "preinstallTarget";
	public static final String RHQ_ATTRIBUTE_POSTINSTALL_TARGET = "postinstallTarget";	
	
	public static final String RHQ_ELEMENT_TARGET 		= "target";
	
		
	public static final String RHQ_NAMESPACE_URL = "\"antlib:org.rhq.bundle\"";
	
	public static final String RHQ_MARKER_TYPE = "cz.muni.fi.rhqeditor.ui.rhqproblemmarker";
	
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	
	
	public static final String RHQ_LAUNCH_CONFIGURATION_ID 			= "cz.muni.fi.rhqeditor.core.launchconfigurationtype";
	public static final String RHQ_LAUNCH_ATTR_PROJECT 				= "cz.muni.fi.rhqditor.launchattr.project";
	public static final String RHQ_LAUNCH_ATTR_USE_DEFAULT_DEPLOYER = "cz.muni.fi.rhqditor.launchattr.usedefaultdeployer";
	public static final String RHQ_LAUNCH_ATTR_LOCAL_DEPLOYER 		= "cz.muni.fi.rhqditor.launchattr.localdeployer";
	public static final String RHQ_LAUNCH_ATTR_LOCAL_DIRECTORY 		= "cz.muni.fi.rhqditor.launchattr.localdirectory";
	public static final String RHQ_LAUNCH_ATTR_USE_DEFAULT_DIRECTORY 	= "cz.muni.fi.rhqditor.launchattr.usedefaultdirectory";
	public static final String RHQ_LAUNCH_ATTR_INPUT_PROPERTY		= "cz.muni.fi.rhqditor.launchattr.inputproperty";
	
	
	//used for handling persistent proterties
	public static final String RHQ_PROPERTY_NODE = "rhq";
	public static final String RHQ_PROPERTY_INPUT = "input-property:";
	public static final String RHQ_DEPLOY_DIR = "rhq.deploy.dir";
	public static final String RHQ_DEPLOY_NAME = "rhq.deploy.name";
	public static final String RHQ_DEPLOY_ID = "rhq.deploy.id";
	public static final String RHQ_DEPLOYER_PATH = "rhq.deployer.path";
	public static final String RHQ_USE_DEFAULT_DEPLOYER = "rhq.default.deployer";
	public static final String RHQ_LAST_USED_CONFIGURATION ="rhq.last.used.configuration";
	
	public static final String NOT_FOUND = "not found";
	
	public static final String RHQ_STANDALONE_DEPLOYER_URL = "cz/muni/fi/rhqeditor/core/launch/rhq-bundle-deployer-4.6.0.zip";
	public static final String RHQ_MODEL_SOURCEFILE = "cz/muni/fi/rhqeditor/core/rhqmodel/rhq_tasks-4.6.0.xml";
	public static final String RHQ_STANDALONE_DEPLOYER = "rhq-bundle-deployer-4.6.0.zip";
	
	public static final String RHQ_DEFAULT_BUILD_DIR = ".bin";
	public static final String RHQ_DEFAULT_DEPLOY_DIR = "build";
	
	public static final String RHQ_DIALOG_SETTINGS = ResourcesPlugin.getWorkspace().getRoot().getLocation()
			.append(new Path(".metadata/.rhq-dialog-settings.xml")).toString();
	
	public static final String RHQ_ALL_POSSIBLE_PARENTS = "#all_posible_parents";

	
	/**
	 * lowercase representation of all possible archive types used in <rhq:archive name=""
	 */
	public static final HashSet<String> RHQ_SUPPORTED_ARCHIVE_TYPES = new HashSet<>(Arrays.asList(new String[]{"zip","jar","war","ear"}));
	
	
	/**
	 * finds whetehter is filename valid archive type
	 * @param filename
	 * @return
	 */
	public static boolean isSupportedArchive(final String filename) {
		if(filename == null || filename.length() < 4) {
			return false;
		}
		String extension = filename.substring(filename.length() -3, filename.length());
		return RHQ_SUPPORTED_ARCHIVE_TYPES.contains(extension.toLowerCase());
	}
	
	public static boolean isSupportedArchive( IPath path) {
		return isSupportedArchive(path.toString());
	}
	

}
