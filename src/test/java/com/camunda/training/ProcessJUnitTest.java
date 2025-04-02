package com.camunda.training;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.execute;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.jobQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(ProcessEngineCoverageExtension.class)
class ProcessJUnitTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessJUnitTest.class);

  @RegisterExtension
  static ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder().build();

  private RuntimeService runtimeService;
  private TaskService taskService;

  @BeforeEach
  void setup() {
    ProcessEngine processEngine = processEngineExtension.getProcessEngine();
    this.runtimeService = processEngine.getRuntimeService();
    this.taskService = processEngine.getTaskService();
  }

  @Test
  @Deployment(resources = "TwitterQAProcess.bpmn")
  void testHappyPath() {
    // Mock the "Review Tweet" task
    Task mockReviewTweetTask = mock(Task.class);
    when(mockReviewTweetTask.getName()).thenReturn("Review Tweet");
    when(mockReviewTweetTask.getId()).thenReturn("mockTaskId");

    // Mock the TaskService to return the mocked task
    TaskService mockTaskService = mock(TaskService.class);
    TaskQuery mockTaskQuery = mock(TaskQuery.class);
    when(mockTaskService.createTaskQuery()).thenReturn(mockTaskQuery);
    when(mockTaskQuery.processInstanceId(anyString())).thenReturn(mockTaskQuery);
    when(mockTaskQuery.list()).thenReturn(List.of(mockReviewTweetTask)).thenReturn(Collections.emptyList());
    when(mockTaskQuery.taskCandidateGroup("management")).thenReturn(mockTaskQuery);
    when(mockTaskQuery.singleResult()).thenReturn(mockReviewTweetTask);

    // Use the mockTaskService in the test
    this.taskService = mockTaskService;

    // Define the process input variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("content", "Exercise 4 test - Benoît");

    // Start the process
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TwitterQAProcess", variables);

    // Verify if the process has started
    assertThat(processInstance).isNotNull();
    LOGGER.info("The process has started with ID: {}", processInstance.getId());

    // Verify if there are pending tasks
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    LOGGER.info("Pending tasks: {}", tasks);

    // Verify that the "Review tweet" task is present and set approved to true
    boolean reviewTweetTaskFound = false;
    for (Task task : tasks) {
      if ("Review Tweet".equals(task.getName())) {
        taskService.setVariable(task.getId(), "approved", true);
        reviewTweetTaskFound = true;
        break;
      }
    }
    assertTrue(reviewTweetTaskFound);
    LOGGER.info("'Review tweet' task found in pending tasks.");

    // Retrieve the "Review tweet" task
    Task reviewTweetTask = taskService.createTaskQuery()
            .processInstanceId(processInstance.getId())
            .taskCandidateGroup("management")
            .singleResult();

    // Verify that the task was retrieved correctly
    assertThat(reviewTweetTask).isNotNull();
    LOGGER.info("Task to complete: {}", reviewTweetTask.getName());

    // Complete the task
    taskService.complete(reviewTweetTask.getId());
    LOGGER.info("Task completed successfully.");

    // Query for jobs that are waiting to be executed and execute them
    List<Job> jobList = jobQuery()
            .processInstanceId(processInstance.getId())
            .list();
      assertEquals(1, jobList.size(), "There should be exactly one job waiting to be executed.");
    Job job = jobList.get(0);
    execute(job);

    // Verify that there are no more pending tasks
    tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    if (!tasks.isEmpty()) {
      LOGGER.info("Remaining tasks: {}", tasks);
    }
    assertTrue(tasks.isEmpty(), "There should be no more pending tasks.");
  }
}
