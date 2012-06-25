package com.javaetmoi.core.batch.tasklet;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit test of the {@link RemoveSpringBatchHistoryTasklet}
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional("repositoryTransactionManager")
public class TestRemoveSpringBatchHistoryTasklet {

    @Autowired
    private RemoveSpringBatchHistoryTasklet removeSpringBatchHistoryTasklet;

    @Autowired
    protected JobExplorer                   jobExplorer;

    @Autowired
    protected DataSource                    dataSource;

    @Test
    public void execute() throws Exception {
        // 1. Prepare test dataset
        Resource sqlScript = new ClassPathResource(
                "com/javaetmoi/core/batch/tasklet/TestRemoveSpringBatchHistoryTasklet.sql");
        // The SimpleJdbcTestUtils is using the deprecated SimpleJdbcTemplate, so we don't have the
        // choice
        SimpleJdbcTemplate simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
        SimpleJdbcTestUtils.executeSqlScript(simpleJdbcTemplate, sqlScript, false);

        // 2. Check the dataset before removing history
        List<JobInstance> jobInstances = jobExplorer.getJobInstances("jobTest", 0, 5);
        assertEquals("2 job instances before the purge", 2, jobInstances.size());

        // 3. Execute the tested method
        final ChunkContext chunkContext = new ChunkContext(null);
        StepExecution stepExecution = new StepExecution("step1", null);
        StepContribution stepContribution = new StepContribution(stepExecution);
        removeSpringBatchHistoryTasklet.execute(stepContribution, chunkContext);

        // 4. Assertions
        assertEquals("6 lines should be deleted from the history", 6,
                stepContribution.getWriteCount());
        jobInstances = jobExplorer.getJobInstances("jobTest", 0, 5);
        assertEquals("Just a single job instance after the delete", 1, jobInstances.size());
        JobInstance jobInstance = jobInstances.get(0);
        assertEquals("Only the job instance number 2 should remain into the history",
                Long.valueOf(102), jobInstance.getId());
    }
}
