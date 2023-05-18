package jass.security.service.implementations;

import jass.security.dto.AddressDto;
import jass.security.dto.RegisterAccountDto;
import jass.security.exception.EmailTakenException;
import jass.security.exception.NotFoundException;
import jass.security.model.*;
import jass.security.repository.*;
import jass.security.service.interfaces.IAccountService;
import jass.security.utils.ObjectMapperUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;

@Service
@Primary
public class AccountService implements IAccountService {
    private final IAccountRepository _accountRepository;

    private final IRoleRepository _roleRespository;

    private final IHrManagerRepository hrManagerRepository;

    private final IProjectManagerRepository projectManagerRepository;

    private final ISoftwareEngineerRepository softwareEngineerRepository;

    private final IAddressRepository addressRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    public AccountService(IAccountRepository accountRepository, IRoleRepository roleRespository, IHrManagerRepository hrManagerRepository, IProjectManagerRepository projectManagerRepository, ISoftwareEngineerRepository softwareEngineerRepository, IAddressRepository addressRepository) {
        this._accountRepository = accountRepository;
        _roleRespository = roleRespository;
        this.hrManagerRepository = hrManagerRepository;
        this.projectManagerRepository = projectManagerRepository;
        this.softwareEngineerRepository = softwareEngineerRepository;
        this.addressRepository = addressRepository;
    }

    @Override
    public List<Account> findAll() {
        return _accountRepository.findAll();
    }

    @Override
    public Account findById(UUID id) {
        return null;
    }

    @Override
    public Account save(Account entity) {
        if( entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        return _accountRepository.save(entity);
    }

    @Override
    public void delete(UUID id) {

    }

    @Override
    public Account findByEmail(String email) {
        return _accountRepository.findByEmail(email);
    }

    @Override
    @Transactional(rollbackFor = { Exception.class })
    public UUID registerAccount(RegisterAccountDto dto) throws EmailTakenException, NotFoundException {
        //make adres
        Address address = makeAddress(dto.getAddress());
        //make employye
        UUID employeeId;
        Role role;

        addressRepository.save(address);

        if(dto.getRole().equals("hrManager")) {

            var employee = makeHrManager(dto, address);
            employeeId = employee.getId();
            role = _roleRespository.findByName("ROLE_HR_MANAGER");
            hrManagerRepository.save(employee);

        } else if(dto.getRole().equals("projectManager")) {

            var employee = makeProjectManager(dto, address);
            employeeId = employee.getId();
            role = _roleRespository.findByName("ROLE_PROJECT_MANAGER");
            projectManagerRepository.save(employee);

        } else if(dto.getRole().equals("softwareEngineer")) {

            var employee = makeSoftwareEngineer(dto, address);
            employeeId = employee.getId();
            role = _roleRespository.findByName("ROLE_ENGINEER");
            softwareEngineerRepository.save(employee);

        } else {
            throw new NotFoundException("Nepostojeca rola");
        }
        //make acc
        Account newAcc = makeAccount(dto, employeeId);


        //TODO Strahinja: Da li ovo ovako ili nekako bolje da se salju ove role sa fronta?
        var roles = new ArrayList<Role>();
        roles.add(role);
        newAcc.setRoles(roles);
        role.getUsers().add(newAcc);

        save(newAcc);
        _roleRespository.save(role);

        return newAcc.getId();
    }

    private SoftwareEngineer makeSoftwareEngineer(RegisterAccountDto dto, Address address) {
        SoftwareEngineer softwareEngineer = new SoftwareEngineer();
        softwareEngineer.setName(dto.getName());
        softwareEngineer.setSurname(dto.getSurname());
        softwareEngineer.setPhoneNumber(dto.getPhoneNumber());
        softwareEngineer.setProfession(dto.getProfession());
        softwareEngineer.setId(UUID.randomUUID());
        softwareEngineer.setAddress(address);
        softwareEngineer.setProfession(dto.getProfession());

        //softwareEngineer.setSkills(new ArrayList<>());
        //softwareEngineer.setProjectStats(new ArrayList<>());

        softwareEngineerRepository.save(softwareEngineer);
        return softwareEngineer;
    }

    private ProjectManager makeProjectManager(RegisterAccountDto dto, Address address) {
        ProjectManager projectManager = new ProjectManager();
        projectManager.setName(dto.getName());
        projectManager.setSurname(dto.getSurname());
        projectManager.setPhoneNumber(dto.getPhoneNumber());
        projectManager.setProfession(dto.getProfession());
        projectManager.setId(UUID.randomUUID());
        projectManager.setAddress(address);
        projectManager.setProfession(dto.getProfession());

        //projectManager.setProjects(new ArrayList<>());

        projectManagerRepository.save(projectManager);
        return projectManager;
    }

    private HrManager makeHrManager(RegisterAccountDto dto, Address address) {
        HrManager hrManager = new HrManager();
        hrManager.setName(dto.getName());
        hrManager.setSurname(dto.getSurname());
        hrManager.setPhoneNumber(dto.getPhoneNumber());
        hrManager.setProfession(dto.getProfession());
        hrManager.setId(UUID.randomUUID());
        hrManager.setAddress(address);
        hrManager.setProfession(dto.getProfession());

        hrManagerRepository.save(hrManager);
        return hrManager;
    }

    private Address makeAddress(AddressDto addressDto) {
        Address address = ObjectMapperUtils.map(addressDto, Address.class);
        address.setId(UUID.randomUUID());
        return address;
    }

    private Account makeAccount(RegisterAccountDto dto, UUID employeeId) throws EmailTakenException {
        if(findByEmail(dto.getEmail()) != null) {
            throw new EmailTakenException();
        }

        Account newAcc = new Account();

        String salt = genereteSalt();

        newAcc.setEmail(dto.getEmail());
        newAcc.setPassword(passwordEncoder.encode(dto.getPassword() + salt));
        newAcc.setSalt(salt);
        newAcc.setId(UUID.randomUUID());
        newAcc.setEmployeeId(employeeId);
        newAcc.setStatus(RegistrationRequestStatus.PENDING);
        newAcc.setIsActivated(false);

        return newAcc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveAccount(String email, Boolean approve) throws NotFoundException {
        Account account = findByEmail(email);
        if(account == null) {
            throw new NotFoundException("Account not found");
        }
        if(approve) {
            account.setStatus(RegistrationRequestStatus.APPROVED);
        } else
            account.setStatus(RegistrationRequestStatus.REJECTED);

        var softwareEngineer = softwareEngineerRepository.findById(account.getEmployeeId());
        if(softwareEngineer.isPresent()) {
            softwareEngineer.get().setDateOfEmployment(new Date());
            softwareEngineerRepository.save(softwareEngineer.get());
        }

        save(account);
    }

    @Override
    public ArrayList<Account> findAllByStatus(RegistrationRequestStatus status) {
        var accs = _accountRepository.findAllByStatus(status);
        return accs;
    }

    private String genereteSalt() {
        int length = 8; // Desired length of the random string
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();

        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            char randomChar = characters.charAt(index);
            sb.append(randomChar);
        }

        return sb.toString();
    }


}
