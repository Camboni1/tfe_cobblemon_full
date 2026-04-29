package be.loic.tfe_cobblemon.spawn.controller;

import be.loic.tfe_cobblemon.spawn.dto.SpawnRuleResponse;
import be.loic.tfe_cobblemon.spawn.entity.SpawnRule;
import be.loic.tfe_cobblemon.spawn.service.SpawnRuleService;
import be.loic.tfe_cobblemon.spawn.service.command.CreateSpawnRuleCommand;
import be.loic.tfe_cobblemon.spawn.service.command.UpdateSpawnRuleCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/spawns")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class SpawnController {

    private final SpawnRuleService spawnRuleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpawnRuleResponse create(@RequestBody @Validated CreateSpawnRuleCommand command) {
        SpawnRule created = spawnRuleService.create(command);
        return spawnRuleService.getResponseById(created.getId());
    }

    @PutMapping("/{id}")
    public SpawnRuleResponse update(
            @PathVariable Long id,
            @RequestBody @Validated UpdateSpawnRuleCommand command
    ) {
        SpawnRule updated = spawnRuleService.update(id, command);
        return spawnRuleService.getResponseById(updated.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        spawnRuleService.delete(id);
    }
}