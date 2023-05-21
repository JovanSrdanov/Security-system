package jass.security.controller;

import jass.security.dto.employee.EmployeeProfileInfoDto;
import jass.security.exception.NotFoundException;
import jass.security.model.Account;
import jass.security.model.Administrator;
import jass.security.model.SoftwareEngineer;
import jass.security.service.interfaces.IAccountService;
import jass.security.service.interfaces.IAdministratorService;
import jass.security.utils.ObjectMapperUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("administrator")
public class AdministratorController {
    private final IAccountService _accountService;
    private final IAdministratorService _administratorService;

    @Autowired
    public AdministratorController(IAccountService accountService, IAdministratorService administratorService) {
        _accountService = accountService;
        _administratorService = administratorService;
    }

    @GetMapping("/logged-in-info")
    @PreAuthorize("hasAuthority('getLoggedInInfo')")
    public ResponseEntity<?> getLoggedInInfo(Principal principal) {
        String employeeEmail = principal.getName();
        Account employeeCredentials = _accountService.findByEmail(employeeEmail);

        try {
            Administrator employeeInfo = _administratorService.findById(employeeCredentials.getEmployeeId());
            return new ResponseEntity<>(
                    ObjectMapperUtils.map(employeeInfo, EmployeeProfileInfoDto.class),
                    HttpStatus.OK
            );
        } catch (NotFoundException e) {
            return new ResponseEntity<>(
                    "administrator not found",
                    HttpStatus.NOT_FOUND
            );
        }
    }

    @PutMapping("/logged-in-info")
    @PreAuthorize("hasAuthority('updateLoggedInInfo')")
    public ResponseEntity<?> updateLoggedInInfo(@RequestBody EmployeeProfileInfoDto dto, Principal principal) {
        String employeeEmail = principal.getName();
        Account employeeCredentials = _accountService.findByEmail(employeeEmail);

        try {
            var updatedInfo = _administratorService.update(employeeCredentials.getEmployeeId(), dto);
            return new ResponseEntity<>(
                    ObjectMapperUtils.map(updatedInfo, EmployeeProfileInfoDto.class),
                    HttpStatus.OK
            );
        } catch (NotFoundException e) {
            return new ResponseEntity<>(
                    "cannot find employee info",
                    HttpStatus.NOT_FOUND
            );
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                    "failed to update info, argument is null",
                    HttpStatus.BAD_REQUEST
            );
        } catch (OptimisticLockingFailureException e) {
            return new ResponseEntity<>(
                    "failed to update info due to optimistic locking",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}