package cz.muni.fi.rhqeditor.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import cz.muni.fi.rhqeditor.core.listeners.RecipeChangeListener;
import cz.muni.fi.rhqeditor.core.utils.ExtractorProvider;
import cz.muni.fi.rhqeditor.core.utils.RhqConstants;
import cz.muni.fi.rhqeditor.core.utils.RhqPathExtractor;

public class ProjectScanner {
	
	public final String NATURE_ID = "cz.muni.fi.rhqeditor.natures.rhqeditornature";
	
	
	/**
	 * scans all projects in workspace, finds ones having RHQ nature, add listener to them and ...
	 */
	
	public ProjectScanner(){
	}
	
	/**
	 * calls init on all projects existing in workspace
	 * Set's up listener - this method should be called on workspace start only
	 * TODO consider sigleton to prevent duplicit listeners
	 */
	public void initAllProjects(){
		RecipeChangeListener listener = new RecipeChangeListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener);
			
		for(IProject proj: ResourcesPlugin.getWorkspace().getRoot().getProjects()){
			initProject(proj);
		}
	}
	
	/**
	 * setup for RHQ project after opening. 
	 * @param project
	 */
	public void initProject(IProject project){
		
		ExtractorProvider extProvider = ExtractorProvider.getInstance();
		
		//terminate if project is already initialized
		if(extProvider.getMap().keySet().contains(project))
			return;
		
		try {
			if(project.isOpen() && project.hasNature(RhqConstants.RHQ_NATURE_ID)){
				RhqPathExtractor extractor = new RhqPathExtractor(project);
				extProvider.attachExtractorToProject(project, extractor);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}
