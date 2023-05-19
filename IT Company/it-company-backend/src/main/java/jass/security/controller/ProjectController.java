package jass.security.controller;

import jass.security.dto.project.AddProjectMangerToProjectDto;
import jass.security.dto.project.AddSwEngineerToProjectDto;
import jass.security.dto.project.CreateProjectDto;
import jass.security.dto.project.DismissWorkerFromProjectDto;
import jass.security.exception.NotFoundException;
import jass.security.model.Project;
import jass.security.service.interfaces.IProjectService;
import jass.security.utils.ObjectMapperUtils;
import org.hibernate.cfg.NotYetImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("project")
public class ProjectController {

    private final IProjectService _projectService;

    @Autowired
    public ProjectController(IProjectService projectService) {
        _projectService = projectService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('createProject')")
    public ResponseEntity<Project> Create(@RequestBody CreateProjectDto dto){
        var result = _projectService.save(ObjectMapperUtils.map(dto, Project.class));
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }
    @GetMapping
    @PreAuthorize("hasAuthority('getAllProject')")
    public ResponseEntity<List<Project>> GetAll(){
        var result = _projectService.findAll();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }



    @PatchMapping("{id}/add-sw-engineer")
    @PreAuthorize("hasAuthority('addSwEngineerToProject')")
    public ResponseEntity<?> AddSwEngineerToProject(@RequestBody AddSwEngineerToProjectDto dto, @PathVariable("id") UUID projectId){
        try {
            _projectService.AddSwEngineerToProject(dto, projectId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("{id}/add-pr-manager")
    @PreAuthorize("hasAuthority('addPrManagerToProject')")
    public ResponseEntity<?> AddPrManagerToProject(@RequestBody AddProjectMangerToProjectDto dto, @PathVariable("id") UUID projectId){
        try {
            _projectService.AddPrManagerToProject(dto, projectId);
            return new ResponseEntity<>( HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("{id}/dismiss-sw-engineer")
    @PreAuthorize("hasAuthority('dismissSwEngineerFromProject')")
    public ResponseEntity<?> DismissSwEngineerFromProject(@RequestBody DismissWorkerFromProjectDto dto, @PathVariable("id") UUID projectId){
        try {
            _projectService.DismissSwEngineerFromProject(dto.getWorkerId(), projectId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("{id}/dismiss-pr-manager")
    @PreAuthorize("hasAuthority('dismissPrManagerFromProject')")
    public ResponseEntity<?> DismissPrManagerFromProject(@RequestBody DismissWorkerFromProjectDto dto, @PathVariable("id") UUID projectId){
        try {
            _projectService.DismissPrManagerFromProject(dto.getWorkerId(), projectId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("{id}/sw-engineers")
    @PreAuthorize("hasAuthority('getSwEngineersOnProject')")
    public ResponseEntity<?> GetSwEngineersOnProject(@PathVariable("id") UUID projectId){
        var result = _projectService.GetSwEngineersOnProject(projectId);
        return new ResponseEntity<>(result ,HttpStatus.OK);
    }

    @GetMapping("{id}/pr-managers")
    @PreAuthorize("hasAuthority('getPrManagersOnProject')")
    public ResponseEntity<?> GetPrManagersOnProject(@PathVariable("id") UUID projectId){
        var result = _projectService.GetPrManagersOnProject(projectId);
        return new ResponseEntity<>(result ,HttpStatus.OK);
    }

    public ResponseEntity<?> GetPrManagerProjects(@PathVariable("id") UUID projectId){
        throw new NotYetImplementedException();
    }
}
