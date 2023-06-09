package jass.security.service.implementations;

import com.google.zxing.WriterException;
import jass.security.dto.*;
import jass.security.exception.*;
import jass.security.model.*;
import jass.security.repository.*;
import jass.security.service.interfaces.IAccountService;
import jass.security.service.interfaces.IRejectedMailService;
import jass.security.service.interfaces.ITOTPService;
import jass.security.utils.DateUtils;
import jass.security.utils.ObjectMapperUtils;
import jass.security.utils.RandomPasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Primary
public class AccountService implements IAccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final IAccountRepository _accountRepository;

    private final IRoleRepository _roleRespository;

    private final IHrManagerRepository hrManagerRepository;

    private final IProjectManagerRepository projectManagerRepository;

    private final ISoftwareEngineerRepository softwareEngineerRepository;

    private final IAddressRepository addressRepository;

    private final IRejectedMailService rejectedMailService;
    private final IAdministratorRepository administratorRepository;
    private final MailSenderService mailService;
    private final IPasswordlessLoginTokenRepository passwordlessLoginTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final ITOTPService totpService;

    @Autowired
    public AccountService(IAccountRepository accountRepository, IRoleRepository roleRespository, IHrManagerRepository hrManagerRepository, IProjectManagerRepository projectManagerRepository, ISoftwareEngineerRepository softwareEngineerRepository, IAddressRepository addressRepository, IRejectedMailService rejectedMailService, IAdministratorRepository administratorRepository, MailSenderService mailService, IPasswordlessLoginTokenRepository passwordlessLoginTokenRepository, PasswordEncoder passwordEncoder, ITOTPService totpService) {
        this._accountRepository = accountRepository;
        _roleRespository = roleRespository;
        this.hrManagerRepository = hrManagerRepository;
        this.projectManagerRepository = projectManagerRepository;
        this.softwareEngineerRepository = softwareEngineerRepository;
        this.addressRepository = addressRepository;
        this.rejectedMailService = rejectedMailService;
        this.administratorRepository = administratorRepository;
        this.mailService = mailService;
        this.passwordlessLoginTokenRepository = passwordlessLoginTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
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
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        logger.info("Account with an ID: " + entity.getId() + ", successfully created");
        return _accountRepository.save(entity);
    }

    @Override
    public void delete(UUID id) {

    }

    @Override
    public Account findByEmail(String email) throws NotFoundException {
        var acc = _accountRepository.findByEmail(email);
        if (acc == null) {
            throw new NotFoundException("Account with this mail does not exist!");
        }
        return acc;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void registerAccount(RegisterAccountDto dto) throws EmailTakenException, NotFoundException, EmailRejectedException, IOException, WriterException {
        //Check if mail is not rejected
        if (rejectedMailService.isMailRejected(dto.getEmail())) {
            throw new EmailRejectedException();
        }


        //make address
        Address address = makeAddress(dto.getAddress());
        //make employee
        UUID employeeId;
        Role role;


        addressRepository.save(address);

        if (dto.getRole().equals("hrManager")) {

            var employee = makeHrManager(dto, address);
            employeeId = employee.getId();
            role = _roleRespository.findByName("ROLE_HR_MANAGER");
            hrManagerRepository.save(employee);

        } else if (dto.getRole().equals("projectManager")) {

            var employee = makeProjectManager(dto, address);
            employeeId = employee.getId();
            role = _roleRespository.findByName("ROLE_PROJECT_MANAGER");
            projectManagerRepository.save(employee);

        } else if (dto.getRole().equals("softwareEngineer")) {

            var employee = makeSoftwareEngineer(dto, address);
            employeeId = employee.getId();
            role = _roleRespository.findByName("ROLE_ENGINEER");
            softwareEngineerRepository.save(employee);

        } else {
            throw new NotFoundException("Nepostojeca rola");
        }

        // **** 2FA ****
        String totpSecretKey = totpService.generateSecretKey();
        // **** 2FA ****
        //make acc
        Account newAcc = makeAccount(dto, employeeId, totpSecretKey);


        //TODO Strahinja: Da li ovo ovako ili nekako bolje da se salju ove role sa fronta?
        var roles = new ArrayList<Role>();
        roles.add(role);
        newAcc.setRoles(roles);
        role.getUsers().add(newAcc);
        newAcc.setIsBlocked(false);

        save(newAcc);
        _roleRespository.save(role);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public UUID registerAdminAccount(RegisterAdminAccountDto dto) throws EmailTakenException, IOException, WriterException {
        Address address = makeAddress(dto.getAddress());
        UUID adminId = UUID.randomUUID();

        Administrator admin = new Administrator(adminId, dto.getName(), dto.getSurname(), address, dto.getPhoneNumber(), dto.getProfession());
        administratorRepository.save(admin);

        var role = _roleRespository.findByName("ROLE_ADMIN_PASSWORD_CHANGE");

        Account newAcc = makeAdminAccount(dto, adminId);


        var roles = new ArrayList<Role>();
        roles.add(role);
        newAcc.setIsBlocked(false);
        newAcc.setRoles(roles);
        role.getUsers().add(newAcc);

        save(newAcc);
        _roleRespository.save(role);
        logger.info("Admin successfully registered, from user ID: " + adminId);
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

    private Account makeAccount(RegisterAccountDto dto, UUID employeeId, String totpSecretKey) throws EmailTakenException, NotFoundException {
        try {
            findByEmail(dto.getEmail());
            throw new EmailTakenException();
        } catch (NotFoundException ignored) {

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
        newAcc.setTotpSecretKey(totpSecretKey);

        return newAcc;
    }

    private Account makeAdminAccount(RegisterAdminAccountDto dto, UUID adminId) throws EmailTakenException, IOException, WriterException {
        try {
            findByEmail(dto.getEmail());
            throw new EmailTakenException();
        } catch (NotFoundException ignored) {

        }

        // **** 2FA ****
        String totpSecretKey = totpService.generateSecretKey();
        String qrCodeString = totpService.getGoogleAuthenticatorBarCode(totpSecretKey,dto.getEmail(), "JSSA");
        byte[] qrCode = totpService.createQRCode(qrCodeString);
        // *** 2FA ***

        Account newAcc = new Account();

        String salt = genereteSalt();

        newAcc.setEmail(dto.getEmail());
        String password = RandomPasswordGenerator.generatePassword(15);
        newAcc.setPassword(passwordEncoder.encode(password + salt));
        newAcc.setSalt(salt);
        newAcc.setId(UUID.randomUUID());
        newAcc.setEmployeeId(adminId);
        newAcc.setStatus(RegistrationRequestStatus.APPROVED);
        newAcc.setIsActivated(true);
        newAcc.setTotpSecretKey(totpSecretKey);

        String mailBody = "Your password is: " + password + "\n You will need to change it after first login.\n" +
                "Also, qr code for 2 factor authentication is attached in this email. Scan it with google authenticator" +
                " app on your phone to be able to login.";
        mailService.sendHtmlMailWithImage(dto.getEmail(), "New registration password", mailBody, qrCode);

        return newAcc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveAccount(String email, Boolean approve) throws NotFoundException {
        Account account = findByEmail(email);
        if (account == null) {
            throw new NotFoundException("Account not found");
        }
        if (approve) {
            account.setStatus(RegistrationRequestStatus.APPROVED);

            var softwareEngineer = softwareEngineerRepository.findById(account.getEmployeeId());
            if (softwareEngineer.isPresent()) {
                softwareEngineer.get().setDateOfEmployment(new Date());
                softwareEngineerRepository.save(softwareEngineer.get());
            }

            save(account);
            logger.info("Account with ID: " + account.getId() + " successfully approved");
        } else {
            //account.setStatus(RegistrationRequestStatus.REJECTED);

            //Del acc
            Account acc = _accountRepository.findByEmail(email);
            _accountRepository.deleteById(acc.getId());
            //Del employee
            UUID addressId = UUID.randomUUID();
            if (hrManagerRepository.findById(acc.getEmployeeId()).isPresent()) {
                var employee = hrManagerRepository.findById(acc.getEmployeeId()).get();
                addressId = employee.getAddress().getId();
                hrManagerRepository.deleteById(employee.getId());
            } else if (projectManagerRepository.findById(acc.getEmployeeId()).isPresent()) {
                var employee = projectManagerRepository.findById(acc.getEmployeeId()).get();
                addressId = employee.getAddress().getId();
                projectManagerRepository.deleteById(employee.getId());
            } else if (softwareEngineerRepository.findById(acc.getEmployeeId()).isPresent()) {
                var employee = softwareEngineerRepository.findById(acc.getEmployeeId()).get();
                addressId = employee.getAddress().getId();
                softwareEngineerRepository.deleteById(employee.getId());
            }
            //Del adr
            addressRepository.deleteById(addressId);

            _roleRespository.deleteAllByUserId(acc.getId().toString());

            //Add to rejected table
            Date date = DateUtils.addHoursToDate(new Date(), 72);
            RejectedMail rejectedMail = new RejectedMail(UUID.randomUUID(), email, date);
            rejectedMailService.save(rejectedMail);

            logger.info("Account with ID: " + acc.getId() + " successfully rejected");
        }
    }

    @Override
    public ArrayList<Account> findAllByStatus(RegistrationRequestStatus status) {
        var accs = _accountRepository.findAllByStatus(status);
        return accs;
    }

    @Override
    public ArrayList<AccountApprovalDto> findAllByStatusInfo(RegistrationRequestStatus status) {
        var accs = findAllByStatus(status);
        ArrayList<AccountApprovalDto> infos = new ArrayList<>();

        for (var acc : accs) {
            AccountApprovalDto info = new AccountApprovalDto();

            if (hrManagerRepository.findById(acc.getEmployeeId()).isPresent()) {
                var employee = hrManagerRepository.findById(acc.getEmployeeId()).get();

                info.setEmail(acc.getEmail());
                info.setName(employee.getName());
                info.setSurname(employee.getSurname());
                info.setAddress(ObjectMapperUtils.map(employee.getAddress(), AddressDto.class));
                info.setPhoneNumber(employee.getPhoneNumber());
                info.setProfession(employee.getProfession());
                var roles = new ArrayList<Role>(acc.getRoles());
                info.setRole(roles.get(0).getName());

            } else if (projectManagerRepository.findById(acc.getEmployeeId()).isPresent()) {
                var employee = projectManagerRepository.findById(acc.getEmployeeId()).get();

                info.setEmail(acc.getEmail());
                info.setName(employee.getName());
                info.setSurname(employee.getSurname());
                info.setAddress(ObjectMapperUtils.map(employee.getAddress(), AddressDto.class));
                info.setPhoneNumber(employee.getPhoneNumber());
                info.setProfession(employee.getProfession());
                var roles = new ArrayList<Role>(acc.getRoles());
                info.setRole(roles.get(0).getName());
            } else if (softwareEngineerRepository.findById(acc.getEmployeeId()).isPresent()) {
                var employee = softwareEngineerRepository.findById(acc.getEmployeeId()).get();

                info.setEmail(acc.getEmail());
                info.setName(employee.getName());
                info.setSurname(employee.getSurname());
                info.setAddress(ObjectMapperUtils.map(employee.getAddress(), AddressDto.class));
                info.setPhoneNumber(employee.getPhoneNumber());
                info.setProfession(employee.getProfession());
                var roles = new ArrayList<Role>(acc.getRoles());
                info.setRole(roles.get(0).getName());
            }

            infos.add(info);
        }
        return infos;
    }

    private boolean passwordValid(String inputPassword, String dbPassword, String dbSalt) {
        return passwordEncoder.matches(inputPassword + dbSalt, dbPassword);
    }

    @Override
    public void changeAdminPassword(String email, ChangeAdminPasswordDto dto) throws IncorrectPasswordException, NotFoundException {
        var account = _accountRepository.findByEmail(email);

        if (account == null) {
            throw new NotFoundException("Account with given email not found");
        }

        if (!passwordValid(dto.getOldPassword(), account.getPassword(), account.getSalt())) {
            throw new IncorrectPasswordException("Old password is incorrect");
        }


        String newSalt = genereteSalt();
        String newPassword = passwordEncoder.encode(dto.getNewPassword() + newSalt);

        account.setSalt(newSalt);
        account.setPassword(newPassword);


        //Role change

        var oldRole = _roleRespository.findByName("ROLE_ADMIN_PASSWORD_CHANGE");
        var newRole = _roleRespository.findByName("ROLE_ADMIN");

        //TODO Strahinja: Da li ovo ovako ili nekako bolje da se salju ove role sa fronta?
        var roles = new ArrayList<Role>();
        roles.add(newRole);
        account.setRoles(roles);

        oldRole.getUsers().remove(account);
        newRole.getUsers().add(account);

        _accountRepository.save(account);
        _roleRespository.save(newRole);
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

    public void generatePasswordlessLoginToken(String email) throws NotFoundException {

        //Check if account with email exists
        var result = _accountRepository.findByEmail(email);
        if (result == null) {
            throw new NotFoundException("Username with given email not found");
        }


        String token = RandomPasswordGenerator.generatePassword(32);
        int tokenDuration = 10; //minutes
        PasswordlessLoginToken passwordlessLoginToken = new PasswordlessLoginToken(UUID.randomUUID(), email, token, LocalDateTime.now(), tokenDuration, false);

        passwordlessLoginTokenRepository.save(passwordlessLoginToken);
        String link = "https://localhost:4444/passwordless-login?hash=" + token;
        String mailBody = "<html>Click on this <a href=\"" + link + "\">link</a> to login.</html>";
        mailService.sendHtmlMail(email, "Passwordless login", mailBody);
    }


    @Override
    public PasswordlessLoginToken usePLToken(String token) throws NotFoundException, PlTokenUsedException, TokenExpiredException {
        var result = passwordlessLoginTokenRepository.findPasswordlessLoginTokenByToken(token);

        if (result.isEmpty()) {
            throw new NotFoundException("Passwordless login token not found");
        }

        PasswordlessLoginToken plToken = result.get();

        if (plToken.isUsed()) {
            throw new PlTokenUsedException("Passwordless login token already used");
        }

        if (plToken.isExpired()) {
            throw new TokenExpiredException("Passwordless login token expired");
        }

        plToken.setUsed(true);
        passwordlessLoginTokenRepository.save(plToken);
        return plToken;
    }

    @Override
    public List<Account> findAllAccountsByRole(String role) {
        return _accountRepository.findByRolesName(role);
    }

    @Override
    public void blockUnblockAccount(String email) throws NotFoundException {
        Account account = findByEmail(email);
        if (account == null) {
            throw new NotFoundException("Account with this email does not exist");
        }
        account.setIsBlocked(!account.getIsBlocked());
        save(account);
    }

    @Override
    public void changePassword(ChangePasswordDto dto) throws NotFoundException, PasswordsDontMatchException {
        Account account = findByEmail(dto.getEmail());

        String dbPassword = account.getPassword();
        if (passwordEncoder.matches(dto.getOldPassword() + account.getSalt(), dbPassword)) {
            account.setPassword(passwordEncoder.encode(dto.getNewPassword() + account.getSalt()));
            save(account);
        } else throw new PasswordsDontMatchException("Passwords don`t match");
    }

    @Override
    public byte[] getTwoFactorAuthQr(String email) throws IOException, WriterException {
        String totpSecretKey = _accountRepository.findByEmail(email).getTotpSecretKey();
        var barCode = totpService.getGoogleAuthenticatorBarCode(totpSecretKey, email, "JSSA");
        return totpService.createQRCode(barCode);
    }


}
