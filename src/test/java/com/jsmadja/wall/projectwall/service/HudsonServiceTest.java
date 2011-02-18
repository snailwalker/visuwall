package com.jsmadja.wall.projectwall.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Test;

import com.jsmadja.wall.projectwall.FileClientHandlerBuilder;
import com.jsmadja.wall.projectwall.HudsonUrlBuilder;
import com.jsmadja.wall.projectwall.Integration;
import com.jsmadja.wall.projectwall.domain.HudsonJob;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandler;
import com.sun.jersey.api.client.config.ClientConfig;

public class HudsonServiceTest {

    private static final int FLUXX_BUILT_WITH_COMMITERS = 273;

    HudsonUrlBuilder hudsonUrlBuilder = new HudsonUrlBuilder(Integration.HUDSON_URL);

    private HudsonService hudsonService = new HudsonService(Integration.HUDSON_URL) {
        @Override
        Client buildJerseyClient(ClientConfig clientConfig) {
            ClientHandler clientHandler = FileClientHandlerBuilder.newFileClientHandler()
            .withFile(hudsonUrlBuilder.getAllJobsUrl(), "hudson/all_jobs.xml")
            .withFile(hudsonUrlBuilder.getJobUrl("fluxx"), "hudson/fluxx.xml")
            .withFile(hudsonUrlBuilder.getJobUrl("fluxx", 101), "hudson/fluxx_101.xml")
            .withFile(hudsonUrlBuilder.getJobUrl("fluxx", 102), "hudson/fluxx_102.xml")
            .withFile(hudsonUrlBuilder.getJobUrl("dev-radar"), "hudson/dev-radar.xml")
            .withFile(hudsonUrlBuilder.getJobUrl("dev-radar", 107), "hudson/dev-radar_107.xml")
            .withFile(hudsonUrlBuilder.getJobUrl("dev-radar", 108), "hudson/dev-radar_108.xml")
            .withFile(hudsonUrlBuilder.getJobUrl("dev-radar"), "hudson/dev-radar.xml")
            .withFile(hudsonUrlBuilder.getJobUrl("fluxx", FLUXX_BUILT_WITH_COMMITERS), "hudson/fluxx_built_with_commiters.xml")
            .withHeader("Content-Type", "application/xml; charset=utf-8")
            .create();
            return new Client(clientHandler, clientConfig);
        }
    };

    @Test
    public void should_retrieve_jobs_from_hudson() throws JAXBException {
        List<HudsonJob> jobs = hudsonService.findAllJobs();
        assertFalse(jobs.isEmpty());
        assertEquals("dev-radar", jobs.get(0).getName());
        assertEquals("fluxx", jobs.get(1).getName());
    }

    @Test
    public void should_retrieve_jobs_with_building_status() {
        List<HudsonJob> jobs = hudsonService.findAllJobs();
        assertFalse(jobs.get(0).isBuilding());
        assertTrue(jobs.get(1).isBuilding());
    }

    @Test
    public void should_retrieve_job_with_last_commiters() {
        HudsonJob job = hudsonService.findJob("fluxx", FLUXX_BUILT_WITH_COMMITERS);
        assertEquals("Julien Smadja", job.getCommiters()[0]);
        assertEquals("Arnaud Lemaire", job.getCommiters()[1]);
    }

    @Test
    public void should_retrieve_jobs_with_status() {
        List<HudsonJob> jobs = hudsonService.findAllJobs();
        for (HudsonJob job:jobs) {
            assertNotNull(job.isSuccessful());
        }
    }

    @Test
    public void should_retrieve_build_start_time() {
        HudsonJob job = hudsonService.findJob("fluxx", FLUXX_BUILT_WITH_COMMITERS);
        assertEquals(1298022037803L ,job.getStartTime().getTime());
    }

    @Test
    public void should_retrieve_artifact_id() {
        String artifactId = hudsonService.findJob("fluxx").getArtifactId();
        assertEquals("fr.fluxx:fluxx", artifactId);
    }

    @Test
    public void should_retrieve_jobs_with_description() {
        List<HudsonJob> jobs = hudsonService.findAllJobs();
        assertEquals("Dev Radar, un mur d'informations", jobs.get(0).getDescription());
        assertEquals("Fluxx, aggrégez vos flux RSS!", jobs.get(1).getDescription());
    }

    @Test
    public void should_retrieve_average_build_duration_time() {
        long duration108 = 31953;
        long duration107 = 29261;

        float sumDuration = duration107 + duration108;
        long averageBuildDurationTime = (long)(sumDuration / 2);

        long duration = hudsonService.getAverageBuildDurationTime("dev-radar");
        assertEquals(averageBuildDurationTime, duration);
    }

    @Test
    public void should_return_successful_build_numbers() {
        HudsonJob hudsonJob = hudsonService.findJob("fluxx");
        int[] successfullBuildNumbers = hudsonService.getSuccessfulBuildNumbers(hudsonJob);
        assertEquals(102, successfullBuildNumbers[0]);
        assertEquals(101, successfullBuildNumbers[1]);
    }

    @Test
    public void should_retrieve_estimated_remaining_time() {
        Date estimatedFinishTime = hudsonService.getEstimatedFinishTime("fluxx");
        assertNotNull(estimatedFinishTime);
    }
}