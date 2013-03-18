package cz.muni.fi.rhqeditor.core.launch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;


import cz.muni.fi.rhqeditor.core.Activator;
import cz.muni.fi.rhqeditor.core.utils.InputPropertiesManager;
import cz.muni.fi.rhqeditor.core.utils.InputProperty;
import cz.muni.fi.rhqeditor.core.utils.RhqConstants;


public class StandaloneDeployer {

	//console stream to write output
	protected MessageConsoleStream fConsoleStream;
	protected MessageConsole fConsole;
	//project directory
	private IProject fProject;
	protected IPath fRunningDir;
	
	private final String EMPTY_VALUE = "";
	
	public StandaloneDeployer(){
	}
	
	public void setMessageConsoleStream(MessageConsoleStream mcs){
		fConsoleStream = mcs;
	}
	
	public void setProject(IProject proj){
		fProject = proj;
	}
	
	
	
	public void deploy(ILaunchConfiguration configuration){

		StringBuilder deployCommand = new StringBuilder();
		try{
			
			
			String projectName = configuration.getAttribute(RhqConstants.RHQ_LAUNCH_ATTR_PROJECT, RhqConstants.NOT_FOUND);
			if(projectName.equals(RhqConstants.NOT_FOUND))
				return;
			
			fProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			
			if(fProject == null)
				return;
			
			initializeDeployment();
			
			
			
			boolean useDefaultDeployer = configuration.getAttribute(
					RhqConstants.RHQ_LAUNCH_ATTR_USE_DEFAULT_DEPLOYER, true);
			
			String pathToDeployer = null;
			//initialize standalone deployer
			if(useDefaultDeployer){
				pathToDeployer = initializeLocalDeployer();
			}else{
				pathToDeployer=configuration.getAttribute(RhqConstants.RHQ_LAUNCH_ATTR_LOCAL_DEPLOYER, RhqConstants.NOT_FOUND);
			}
			
			if(pathToDeployer.equals(RhqConstants.NOT_FOUND))
				return;
			
			fConsole = findConsole(fProject.getName()+"[RHQ Standalone deployment]"+ pathToDeployer);
			fConsole.clearConsole();
			deployCommand.append(pathToDeployer+" ");
			
			//deploy dir
			String dir;
			if(configuration.getAttribute(RhqConstants.RHQ_LAUNCH_ATTR_USE_DEFAULT_DIRECTORY, true) == true) {
				dir = getDefaltDeployDirectory();

			}else{
				dir = configuration.getAttribute(RhqConstants.RHQ_LAUNCH_ATTR_LOCAL_DIRECTORY, RhqConstants.NOT_FOUND);
				if(dir.equals(RhqConstants.NOT_FOUND)){
					fConsoleStream.println("Select deploy directory or use project default.");
				return;
				}		
			}
			deployCommand.append("-Drhq.deploy.dir="+dir+" ");
			
			InputPropertiesManager propManager = new InputPropertiesManager(fProject.getName());
			
			
			String inputPropertyValue;
			for(InputProperty property: propManager.getInputPropertiesFromRecipe(false)){
				inputPropertyValue = configuration.getAttribute(
						RhqConstants.RHQ_LAUNCH_ATTR_INPUT_PROPERTY+"."+property.getName(), EMPTY_VALUE);
				if(inputPropertyValue.equals(EMPTY_VALUE)){
					if(property.isRequired()){
						System.out.println("handle error");
						return;
					}else{
						continue;
					}
				}
				deployCommand.append("-D" + property.getName() + "=" +inputPropertyValue + " ");
				
			}
			System.out.println(deployCommand);
			
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("standalone deployment");

	
	    final String cmd =deployCommand.toString();
	    final File dir = new File(fProject.getFolder(RhqConstants.RHQ_DEFAULT_BUILD_DIR).getLocation().toString());
	    fConsoleStream = fConsole.newMessageStream();
//	    initializeStandaloneDeployment();
	       
	    Job deployment = new Job("deploy"){
	    	   
	    	@Override
			protected IStatus run(IProgressMonitor monitor) {
			      Process p;
			      fConsoleStream.println("running deployer with command: " + cmd);
			      try {
			    	  p = Runtime.getRuntime().exec(cmd,null,dir);
			    	  BufferedReader stdInput = new BufferedReader(new 
			                 InputStreamReader(p.getInputStream()));
			    	  String line;
			    	  while ((line = stdInput.readLine()) != null) {
						  fConsoleStream.println(line);
			    	  }
					} catch (IOException e) {
						e.printStackTrace();
						try {
							fConsoleStream.println(e.toString());
							fProject.refreshLocal(IResource.DEPTH_INFINITE, null);
						} catch (CoreException e1) {
							fConsoleStream.println("Refresh of resources failed");
						}
						return Status.CANCEL_STATUS;
					}
			      try {
						fProject.refreshLocal(IResource.DEPTH_INFINITE, null);
					} catch (CoreException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			      return Status.OK_STATUS;
			}
	       };
	
		deployment.schedule();  
	}
	
	
	/**
	 * initialize local deployer and returns path to it or NOT_FOUND
	 * @return
	 */
	private String initializeLocalDeployer(){
		DeployerProider provider = DeployerProider.getInstance();
		provider.initializeDeployer(Activator.getFileURL("/cz/muni/fi/rhqeditor/core/launch/rhq-bundle-deployer-4.6.0.zip"));
		Path path = provider.getDeployerPath();
		if(path == null || !provider.isExexutable())
		{
			return RhqConstants.NOT_FOUND;
		}
		return path.toString();
	}
	
	
	
	private void initializeDeployment(){
		try {		
			IFolder folder = fProject.getFolder(RhqConstants.RHQ_DEFAULT_BUILD_DIR);
			//delete content of previous deployment
			if(folder.exists()){
				folder.delete(true, null);
			folder = fProject.getFolder(RhqConstants.RHQ_DEFAULT_BUILD_DIR);
		    folder.create(true, true, null);
			}
			for(IResource res:fProject.members())
			{
				if(res.getName().toString().startsWith("."))
					continue;
				res.copy(new org.eclipse.core.runtime.Path(RhqConstants.RHQ_DEFAULT_BUILD_DIR+System.getProperty("file.separator")+res.getName()), true , null);
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private MessageConsole findConsole(String name) {
	      ConsolePlugin plugin = ConsolePlugin.getDefault();
	      IConsoleManager conMan = plugin.getConsoleManager();
	      IConsole[] existing = conMan.getConsoles();
	      for (int i = 0; i < existing.length; i++)
	         if (name.equals(existing[i].getName()))
	            return (MessageConsole) existing[i];
	      //no console found, so create a new one
	      MessageConsole myConsole = new MessageConsole(name, null);
	      
	      conMan.addConsoles(new IConsole[]{myConsole});
	      return myConsole;
	   }
	
	/**
	 * Return path to default deploy directory for project (project/build). Creates this this in case it doesn't exist.
	 * @return
	 * @throws CoreException 
	 */
	private String getDefaltDeployDirectory() throws CoreException{
		IFolder folder = fProject.getFolder(RhqConstants.RHQ_DEFAULT_DEPLOY_DIR);
		if(!folder.exists())
			folder.create(true,true,null);
		
		return folder.getLocation().toString();
	}

}
