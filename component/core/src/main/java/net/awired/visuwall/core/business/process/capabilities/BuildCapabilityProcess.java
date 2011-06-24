package net.awired.visuwall.core.business.process.capabilities;

import java.util.Arrays;
import java.util.Date;
import net.awired.visuwall.api.domain.SoftwareProjectId;
import net.awired.visuwall.api.domain.State;
import net.awired.visuwall.api.exception.BuildNotFoundException;
import net.awired.visuwall.api.exception.BuildNumberNotFoundException;
import net.awired.visuwall.api.exception.ProjectNotFoundException;
import net.awired.visuwall.core.business.domain.Build;
import net.awired.visuwall.core.business.domain.ConnectedProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import com.google.common.base.Preconditions;

@Component
public class BuildCapabilityProcess {

    private static final Logger LOG = LoggerFactory.getLogger(BuildCapabilityProcess.class);

    @Autowired
    TaskScheduler scheduler;

    private static final int SKIP_FIRST_BUILD_POS = 1;

    public void updatePreviousCompletedBuild(ConnectedProject project) throws ProjectNotFoundException {
        Integer[] projectNumbers = project.getBuildNumbers();
        for (int i = SKIP_FIRST_BUILD_POS; i < projectNumbers.length; i++) {
            int buildNumber = projectNumbers[i];
            Build build = project.getBuilds().get(buildNumber);
            if (build == null) {
                updateBuild(project, buildNumber);
                build = project.getBuilds().get(buildNumber);
                if (build == null) {
                    LOG.warn("Build " + buildNumber + " not found after update for project " + project);
                    continue;
                }
            }

            State state = build.getState();
            if (state == State.UNKNOWN || state == State.ABORTED || state == State.NOTBUILT) {
                continue;
            }

            project.setLastCompletedBuildNumber(build.getBuildNumber());
            break;
        }
    }

    public void updateBuild(ConnectedProject project, int buildNumber) throws ProjectNotFoundException {
        Preconditions.checkState(Arrays.binarySearch(project.getBuildNumbers(), buildNumber) < 0,
                "buildNumber '%s' not found in builds Number to update build in project %s ", buildNumber, project);
        try {
            SoftwareProjectId projectId = project.getBuildProjectId();

            State state = project.getBuildConnection().getBuildState(projectId, buildNumber);
            boolean building = project.getBuildConnection().isBuilding(projectId, buildNumber);
            // buildTime
            //TODO it

            Build lastBuild = project.findCreatedBuild(buildNumber);
            lastBuild.setState(state);
            lastBuild.setBuilding(building);

            if (building == true) {
                Date estimatedFinishTime = project.getBuildConnection()
                        .getEstimatedFinishTime(projectId, buildNumber);
                lastBuild.setEstimatedFinishTime(estimatedFinishTime);
            }

            project.findCreatedBuild(buildNumber);
        } catch (BuildNotFoundException e) {
            LOG.warn("BuildNumber " + buildNumber + " not found in software to update project " + project, e);
            //TODO remove buildNumber from buildNumbers as its removed from software
        }
    }

    public boolean updateStatusAndReturnFullUpdateNeeded(ConnectedProject project) throws ProjectNotFoundException,
            BuildNotFoundException {
        try {
            int lastBuildNumber = project.getBuildConnection().getLastBuildNumber(project.getBuildProjectId());
            int previousLastBuildNumber = project.getLastBuildNumber();
            boolean building = project.getBuildConnection().isBuilding(project.getBuildProjectId(), lastBuildNumber);
            boolean previousBuilding = false;
            //TODO
            //            try {
            previousBuilding = project.getLastBuild().isBuilding();
            //            } catch (BuildNotFoundException e) {
            //                LOG.info("No currentBuild found to say the project was building before refresh " + project);
            //            }

            Build lastBuild = project.findCreatedBuild(lastBuildNumber);
            lastBuild.setBuilding(building);
            project.setLastBuildNumber(lastBuildNumber);
            if (previousBuilding == false && building == true) {
                // currently building
                Runnable finishTimeRunner = getEstimatedFinishTimeRunner(project, lastBuild);
                scheduler.schedule(finishTimeRunner, new Date());
            }
            if (previousBuilding == true && building == false) {
                // build is over
                lastBuild.setEstimatedFinishTime(null);
            }

            if (previousLastBuildNumber != lastBuildNumber && !building) {
                return true;
            }
        } catch (BuildNumberNotFoundException e) {
            LOG.info("No last build number found to update project " + project);
        }
        return false;
    }

    ////////////////////////////////////////////////////////////////////////

    /**
     * @return null if no date could be estimated
     * @throws ProjectNotFoundException
     */
    Runnable getEstimatedFinishTimeRunner(final ConnectedProject project, final Build build)
            throws ProjectNotFoundException {
        Preconditions.checkNotNull(project, "project is a mandatory parameter");
        return new Runnable() {
            @Override
            public void run() {
                LOG.info("Running getEstimatedFinishTime for project " + project);
                try {
                    Date estimatedFinishTime = project.getBuildConnection().getEstimatedFinishTime(
                            project.getBuildProjectId(), build.getBuildNumber());
                    if (estimatedFinishTime != null) {
                        build.setEstimatedFinishTime(estimatedFinishTime);
                    }
                } catch (ProjectNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (BuildNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
    }

}
