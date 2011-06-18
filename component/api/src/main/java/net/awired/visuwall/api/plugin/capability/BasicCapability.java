package net.awired.visuwall.api.plugin.capability;

import java.util.List;
import net.awired.visuwall.api.domain.Project;
import net.awired.visuwall.api.domain.ProjectId;
import net.awired.visuwall.api.exception.ProjectNotFoundException;

public interface BasicCapability {
    /**
     * @param projectId
     * @throws NotImplementedOperationException
     * @returntrue if the project is in the Quality Software
     */
    boolean contains(ProjectId projectId);

    /**
     * Return the full list of project id contained in the software
     * 
     * @return
     */
    List<ProjectId> findAllProjects();

    /**
     * Return a list of project id contained in the software by a list of names
     * 
     * @return
     */
    List<ProjectId> findProjectsByNames(List<String> names);

    /**
     * Plugin should be able to retrieve projects by theirs projectId. ProjectId are filled when you call
     * findAllProjects for example
     * 
     * @param projectId
     * @return
     * @throws ProjectNotFoundException
     */
    Project findProject(ProjectId projectId) throws ProjectNotFoundException;

    /**
     * Find all project names of projects handle by the software
     * 
     * @return
     */
    List<String> findProjectNames();

}