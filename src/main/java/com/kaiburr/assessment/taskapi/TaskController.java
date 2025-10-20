package com.kaiburr.assessment.taskapi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    @Autowired
    private TaskRepository repository;

    // GET /tasks or /tasks?id=xyz
    @GetMapping
    public ResponseEntity<?> getTasks(@RequestParam(name = "id", required = false) String id) {
        if (id != null) {
            Optional<Task> task = repository.findById(id);
            if (task.isPresent()) return ResponseEntity.ok(task.get());
            else return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } else return ResponseEntity.ok(repository.findAll());
    }

    // PUT /tasks (create new task)
    @PutMapping
    public ResponseEntity<?> createTask(@Valid @RequestBody Task task) {
        if (!isSafeCommand(task.getCommand()))
            return ResponseEntity.badRequest().body("Unsafe command detected.");
        Task saved = repository.save(task);
        return ResponseEntity.ok(saved);
    }

    // DELETE /tasks/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable("id") String id) {
        if (!repository.existsById(id))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        repository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }

    // GET /tasks/search?name=substring
    @GetMapping("/search")
    public ResponseEntity<?> searchTasksByName(@RequestParam(name = "name") String name) {
        List<Task> list = repository.findByNameContainingIgnoreCase(name);
        if (list.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        return ResponseEntity.ok(list);
    }

    // PUT /tasks/{id}/execute
    @PutMapping("/{id}/execute")
    public ResponseEntity<?> executeTask(@PathVariable("id") String id) {
        Optional<Task> optTask = repository.findById(id);
        if (!optTask.isPresent()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        Task task = optTask.get();
        // Command validation
        if (!isSafeCommand(task.getCommand()))
            return ResponseEntity.badRequest().body("Unsafe command execution blocked.");
        // Execute shell command
        Date start = new Date();
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(task.getCommand());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
        } catch (Exception e) {
            output.append("Error: ").append(e.getMessage());
        }
        Date end = new Date();
        TaskExecution exec = new TaskExecution(start, end, output.toString().trim());
        task.getTaskExecutions().add(exec);
        repository.save(task);
        return ResponseEntity.ok(exec);
    }

    // Basic unsafe shell command detection
    private boolean isSafeCommand(String command) {
        String lc = command.toLowerCase();
        String[] unsafe = { "rm", "del", "shutdown", "reboot", "mkfs", "/dev/", "format", ":(){", "kill", "dd", "init", "halt", "poweroff" };
        for (String w : unsafe) {
            if (lc.contains(w)) return false;
        }
        return true;
    }
}
