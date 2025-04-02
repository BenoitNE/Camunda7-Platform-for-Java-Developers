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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;

@ExtendWith(ProcessEngineCoverageExtension.class)
class ProcessJUnitTest {

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
    variables.put("approved", true); // Assurez-vous que cette variable est bien utilisée dans le BPMN

    // 2️⃣ Démarrer le processus
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TwitterQAProcess", variables);

    // 3️⃣ Vérifier si le processus a bien démarré
    assertThat(processInstance).isNotNull();
    System.out.println("Le processus a démarré avec l'ID : " + processInstance.getId());

    // 4️⃣ Vérifier si des tâches sont en attente
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    System.out.println("Tâches en cours : " + tasks);

    // 5️⃣ Compléter toutes les tâches utilisateur
    for (Task task : tasks) {
      System.out.println("Complétion de la tâche : " + task.getName());
      taskService.complete(task.getId());
    }

    // 6️⃣ Vérifier que le processus est bien terminé
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult())
            .isNull();  // L'instance doit être supprimée si le processus s'est bien terminé
  }
}