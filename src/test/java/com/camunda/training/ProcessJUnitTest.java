package com.camunda.training;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ProcessEngineCoverageExtension.class)
class ProcessJUnitTest {

  // Définition du logger
  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessJUnitTest.class);

  // Extension JUnit pour gérer le moteur Camunda
  @RegisterExtension
  static ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder().build();

  private RuntimeService runtimeService;
  private TaskService taskService;

  @BeforeEach
  void setup() {
    // Initialisation des services à partir du moteur Camunda
    ProcessEngine processEngine = processEngineExtension.getProcessEngine();
    this.runtimeService = processEngine.getRuntimeService();
    this.taskService = processEngine.getTaskService();
  }

  @Test
  @Deployment(resources = "TwitterQAProcess.bpmn")
  void testHappyPath() {

    // 1️⃣ Définir les variables d'entrée du processus
    Map<String, Object> variables = new HashMap<>();
    variables.put("content", "Exercise 4 test - Benoît");
    // Nous n'ajoutons plus la variable "approved" ici, car elle sera ajoutée lors de la complétion de la tâche

    // 2️⃣ Démarrer le processus
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TwitterQAProcess", variables);

    // 3️⃣ Vérifier si le processus a bien démarré
    assertThat(processInstance).isNotNull();
    LOGGER.info("Le processus a démarré avec l'ID : {}", processInstance.getId());

    // 4️⃣ Vérifier si des tâches sont en attente
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    LOGGER.info("Tâches en cours : {}", tasks);

    // 5️⃣ Vérifier que la tâche "Review tweet" est bien présente et setter approved à true
    boolean reviewTweetTaskFound = false;
    for (Task task : tasks) {
      if ("Review Tweet".equals(task.getName())) {
        taskService.setVariable(task.getId(), "approved", true);
        reviewTweetTaskFound = true;
        break;
      }
    }
    assertTrue(reviewTweetTaskFound);
    LOGGER.info("Tâche 'Review tweet' trouvée dans les tâches en attente.");

    // 6️⃣ Récupérer la tâche "Review tweet"
    Task reviewTweetTask = taskService.createTaskQuery()
            .processInstanceId(processInstance.getId())
            .taskCandidateGroup("management")
            .singleResult(); // Nous récupérons la tâche de groupe "management"

    // 7️⃣ Vérifier que la tâche a été récupérée correctement
    assertThat(reviewTweetTask).isNotNull();
    LOGGER.info("Tâche à compléter : {}", reviewTweetTask.getName());

    // 8️⃣ Compléter la tâche
    taskService.complete(reviewTweetTask.getId()); // Compléter la tâche sans la variable "approved"
    LOGGER.info("Tâche complétée avec succès.");

    // 9️⃣ Vérifier que le processus est bien terminé
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult())
            .isNull();  // L'instance doit être supprimée si le processus s'est bien terminé
    LOGGER.info("Le processus est terminé.");
  }
}
