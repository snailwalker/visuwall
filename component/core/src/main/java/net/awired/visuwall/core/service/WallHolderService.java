/**
 *     Copyright (C) 2010 Julien SMADJA <julien dot smadja at gmail dot com> - Arnaud LEMAIRE <alemaire at norad dot fr>
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package net.awired.visuwall.core.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import net.awired.visuwall.api.domain.ProjectId;
import net.awired.visuwall.api.domain.ProjectStatus;
import net.awired.visuwall.api.exception.NotImplementedOperationException;
import net.awired.visuwall.api.exception.ProjectNotFoundException;
import net.awired.visuwall.api.plugin.ConnectionPlugin;
import net.awired.visuwall.api.plugin.VisuwallPlugin;
import net.awired.visuwall.core.domain.ConnectedProject;
import net.awired.visuwall.core.domain.SoftwareAccess;
import net.awired.visuwall.core.domain.Wall;
import net.awired.visuwall.core.exception.NotCreatedException;
import net.awired.visuwall.core.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

@Service
public class WallHolderService implements WallService {

    private static final Logger LOG = LoggerFactory.getLogger(WallHolderService.class);

    private final static int EVERY_FIVE_MINUTES = 5 * 60 * 1000;
    private final static int THIRTY_SECONDS = 30 * 1000;

    @Autowired
    ProjectService projectService;

    @Autowired
    PluginService pluginService;

    @Autowired
    ProjectEnhancerService projectEnhancerService;

    @Autowired
    WallService wallService;

    static Map<String, Wall> WALLS;

    @PostConstruct
    void init() throws NotCreatedException {
        if (WALLS == null) {
            WALLS = new HashMap<String, Wall>();
            List<Wall> walls = wallService.getWalls();
            for (Wall wall : walls) {
                try {
                    reconstructWall(wall);
                    WALLS.put(wall.getName(), wall);
                } catch (Exception e) {
                    LOG.error("can not reconstruct wall with name " + wall.getName(), e);
                }
            }
        }
    }

    @Override
    // ////////////////////////////////////////////////////////////////////////////////
    public List<Wall> getWalls() {
        return new ArrayList<Wall>(WALLS.values());
    }

    @Override
    public Wall find(String wallName) throws NotFoundException {
        Preconditions.checkNotNull(wallName, "wallName");

        Wall wall = WALLS.get(wallName);
        if (wall == null) {
            throw new NotFoundException("Wall with name : " + wallName + " not found in database");
        }
        return wall;
    }

    @Override
    public void persist(Wall wall) throws NotCreatedException {
        Preconditions.checkNotNull(wall, "wall parameter is mandatory");
        Preconditions.checkNotNull(wall.getName(), "wall must have a name");

        try {
            wallService.persist(wall);
        } catch (Throwable e) {
            String message = "Can't create wall " + wall + " in database";
            LOG.error(message, e);
            throw new NotCreatedException(message, e);
        }
        reconstructWall(wall);
        WALLS.put(wall.getName(), wall);
    }

    @Override
    public Wall update(Wall wall) {
        Wall newWall = wallService.update(wall);
        reconstructWall(newWall);
        WALLS.put(newWall.getName(), newWall);
        return newWall;
    }

    @Override
    public Set<String> getWallNames() {
        return WALLS.keySet();
    }

    @Scheduled(fixedDelay = THIRTY_SECONDS)
    public void refreshWalls() {
        if (LOG.isInfoEnabled()) {
            LOG.info("It's time to refresh all walls");
        }
        for (Wall wall : WALLS.values()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Refreshing wall : " + wall + " and its " + wall.getProjects().size() + " projects");
            }
            reconstructWall(wall);
        }
    }

    private void updateWallProjects(List<ConnectionPlugin> connectionPlugins, Wall wall) {
        Preconditions.checkNotNull(wall, "wall is a mandatory parameter");

        for (ConnectionPlugin connectionPlugin : connectionPlugins) {
            try {
                List<ProjectId> discoveredProjects = connectionPlugin.findAllProjects();
                for (ProjectId discoveredProjectId : discoveredProjects) {
                    ConnectedProject project;
                    try {
                        project = wall.getProjectByProjectId(discoveredProjectId);
                    } catch (ProjectNotFoundException e) {
                        project = new ConnectedProject(discoveredProjectId);
                        project.setConnectionPlugins(connectionPlugins);
                        wall.getProjects().add(project);
                    }
                    updateProject(project);
                }
            } catch (NotImplementedOperationException e) {
            }
        }
    }

    private void reconstructWallTransientInfo(Wall wall) {
        Preconditions.checkNotNull(wall, "Wall is a mandatory parameter");
        // rebuild ConnectionPlugin for software access
        for (SoftwareAccess softwareAccess : wall.getSoftwareAccesses()) {
            try {
                VisuwallPlugin plugin = pluginService.getPluginFromUrl(new URL(softwareAccess.getUrl()));
                ConnectionPlugin connectionPlugin = plugin.getConnection(softwareAccess.getUrl(), null);
                softwareAccess.setConnectionPlugin(connectionPlugin);
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // find projects
        for (SoftwareAccess softwareAccess : wall.getSoftwareAccesses()) {
            if (softwareAccess.isAllProject()) {
                List<ProjectId> projectIds = softwareAccess.getConnectionPlugin().findAllProjects();
                for (ProjectId projectId : projectIds) {
                    ConnectedProject connectedProject = new ConnectedProject(projectId);
                    projectEnhancerService.enhanceWithBuildInformations(connectedProject,
                            softwareAccess.getConnectionPlugin());
                    connectedProject.getConnectionPlugins().add(softwareAccess.getConnectionPlugin());
                    wall.getProjects().add(connectedProject);
                }

            } else {
                // look in projectsNames
                // lokk in views
            }

            // loop with contains
        }

        //updateWallProjects(connectionPlugins, wall);
        if (LOG.isInfoEnabled()) {
            LOG.info("Done refreshing wall : " + wall + " and its " + wall.getProjects().size() + " projects");
        }
    }

    private boolean isProjectListUpdateNeeded(SoftwareAccess softwareAccess) {
        if (softwareAccess.getViewNames().size() > 0 || softwareAccess.isAllProject()) {
            return true;
        }
        return false;
    }

    public List<ProjectStatus> getStatus(String wallName) {
        Preconditions.checkNotNull(wallName, "wallName can not be null");

        Wall wall = WALLS.get(wallName);
        Preconditions.checkNotNull(wall, "wall not found for name", wallName);

        List<ProjectStatus> statusList = new ArrayList<ProjectStatus>();
        Iterator<ConnectedProject> iter = wall.getProjects().iterator();
        while (iter.hasNext()) {
            ConnectedProject project = iter.next();
            ProjectStatus status = new ProjectStatus();
            List<ConnectionPlugin> connectionPlugin = project.getConnectionPlugins();
            ProjectId projectId = project.getProjectId();

            status.setBuilding(projectService.isBuilding(connectionPlugin, projectId));
            status.setLastBuildId(projectService.getLastBuildNumber(connectionPlugin, projectId));
            status.setName(projectId.getName());
            try {
                status.setState(projectService.getState(connectionPlugin, projectId));
            } catch (Exception e) {
                LOG.debug("state of project not found", e);
                iter.remove();
                continue;
            }
            statusList.add(status);
        }
        return statusList;
    }

    // //////////////////////////////////////////////////////////////////////

    @VisibleForTesting
    public void setProjectService(ProjectService projectService) {
        this.projectService = projectService;
    }

    @VisibleForTesting
    public void setPluginService(PluginService pluginService) {
        this.pluginService = pluginService;
    }

}
