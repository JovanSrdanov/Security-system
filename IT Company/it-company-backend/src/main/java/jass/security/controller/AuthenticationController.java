package jass.security.controller;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletResponse;
import jass.security.dto.JwtAuthenticationRequest;
import jass.security.dto.RefreshRequest;
import jass.security.dto.RegisterAccountDto;
import jass.security.dto.UserTokenState;
import jass.security.exception.EmailActivationExpiredException;
import jass.security.exception.EmailTakenException;
import jass.security.exception.NotFoundException;
import jass.security.exception.TokenExpiredException;
import jass.security.model.Account;
import jass.security.model.RegistrationRequestStatus;
import jass.security.model.Role;
import jass.security.service.interfaces.IAccountActivationService;
import jass.security.service.interfaces.IAccountService;
import jass.security.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.UUID;

//Kontroler zaduzen za autentifikaciju korisnika
@RestController
@RequestMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthenticationController {

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private IAccountService accountService;

    @Autowired
    private IAccountActivationService accountActivationService;

    // Prvi endpoint koji pogadja korisnik kada se loguje.
    // Tada zna samo svoje korisnicko ime i lozinku i to prosledjuje na backend.
    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(
            @RequestBody JwtAuthenticationRequest authenticationRequest, HttpServletResponse response) {
        // Ukoliko kredencijali nisu ispravni, logovanje nece biti uspesno, desice se
        // AuthenticationException
        Account acc = accountService.findByEmail(authenticationRequest.getEmail());
        if (acc == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User not found");
        }

        if (acc.getStatus() != RegistrationRequestStatus.APPROVED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Registration is not accepted by admin");
        }

        if (!acc.getIsActivated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account not activated");
        }

        Authentication authentication = null;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    authenticationRequest.getEmail(), authenticationRequest.getPassword() + acc.getSalt()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Auth failed");
        }


        // Ukoliko je autentifikacija uspesna, ubaci korisnika u trenutni security
        // kontekst
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Kreiraj token za tog korisnika
        //TODO Strahinja: Zasto ovde baca error?
        //Account user = (Account) authentication.getPrincipal();
        Account account = accountService.findByEmail(authenticationRequest.getEmail());
        var roles = new ArrayList<Role>(account.getRoles());

        String jwt = tokenUtils.generateToken(authenticationRequest.getEmail(), roles.get(0).getName());
        String resfresh = tokenUtils.generateRefreshToken(authenticationRequest.getEmail());
        int expiresIn = tokenUtils.getExpiredIn();

        // Vrati token kao odgovor na uspesnu autentifikaciju
        return ResponseEntity.ok(new UserTokenState(jwt, resfresh, expiresIn));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshRequest token) {
        try {
            if (tokenUtils.validateRefreshToken(token.getToken())) {
                String email = tokenUtils.getUsernameFromToken(token.getToken());
                Account account = accountService.findByEmail(email);
                var roles = new ArrayList<>(account.getRoles());
                String jwt = tokenUtils.generateToken(email, roles.get(0).getName());
                int expiresIn = tokenUtils.getExpiredIn();
                return ResponseEntity.ok(new UserTokenState(jwt, token.getToken(), expiresIn));
            }
        } catch (ExpiredJwtException | TokenExpiredException ex) {
            return ResponseEntity.status(HttpStatus.GONE).body("Token expired");
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token not valid");

    }

    @PostMapping("/register")
    public ResponseEntity<?> registerNewAccount(@RequestBody RegisterAccountDto dto) {
        try {
            //Namesti acc u bazi
            accountService.registerAccount(dto);

            //Admin treba da odobri
            //TODO Strahinja: admin da odobri


            return ResponseEntity.ok("Account created, waiting admin approval");
        } catch (EmailTakenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This e-mail is taken");
        }
    }

    @PreAuthorize("hasAuthority('chagneAccStatus')")
    @GetMapping("/accept-registration/{mail}")
    public ResponseEntity<?> acceptRegistration(@PathVariable String mail) {
        accountService.approveAccount(mail, true);

        //Posalji mail
        String link = accountActivationService.createAcctivationLink(mail);

        //TODO Strhinja: poslati na mail link umesto u response
        return ResponseEntity.ok("Registration approved \n Activation link: " + link);
    }

    @PreAuthorize("hasAuthority('chagneAccStatus')")
    @GetMapping("/reject-registration/{mail}")
    public ResponseEntity<?> rejectRegistration(@PathVariable String mail) {
        accountService.approveAccount(mail, false);

        //TODO Strhinja: poslati na mail da je nalog odbijen
        return ResponseEntity.ok("Registration rejected");
    }

    @GetMapping("/activate/{id}")
    public ResponseEntity<?> activateAccount(@PathVariable UUID id) {
        try {
            accountActivationService.activateAccount(id);
        } catch (EmailActivationExpiredException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Expired link");
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Corrupted link");
        }

        return ResponseEntity.ok("Account activated");
    }
}
