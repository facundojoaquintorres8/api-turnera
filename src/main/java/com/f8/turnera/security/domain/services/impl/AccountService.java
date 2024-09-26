package com.f8.turnera.security.domain.services.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.f8.turnera.domain.entities.Organization;
import com.f8.turnera.domain.repositories.IOrganizationRepository;
import com.f8.turnera.domain.services.IEmailService;
import com.f8.turnera.exception.BadRequestException;
import com.f8.turnera.exception.NoContentException;
import com.f8.turnera.security.domain.dtos.ActivateDTO;
import com.f8.turnera.security.domain.dtos.PasswordChangeDTO;
import com.f8.turnera.security.domain.dtos.PasswordResetDTO;
import com.f8.turnera.security.domain.dtos.PasswordResetRequestDTO;
import com.f8.turnera.security.domain.dtos.RegisterDTO;
import com.f8.turnera.security.domain.dtos.ResponseDTO;
import com.f8.turnera.security.domain.dtos.UserDTO;
import com.f8.turnera.security.domain.entities.Permission;
import com.f8.turnera.security.domain.entities.Profile;
import com.f8.turnera.security.domain.entities.User;
import com.f8.turnera.security.domain.repositories.IPermissionRepository;
import com.f8.turnera.security.domain.repositories.IProfileRepository;
import com.f8.turnera.security.domain.repositories.IUserRepository;
import com.f8.turnera.security.domain.services.IAccountService;
import com.f8.turnera.util.EmailValidation;
import com.f8.turnera.util.MapperHelper;
import com.f8.turnera.util.OrganizationHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import net.bytebuddy.utility.RandomString;

@Service
public class AccountService implements IAccountService {

    @Autowired
    private IOrganizationRepository organizationRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IProfileRepository profileRepository;

    @Autowired
    private IPermissionRepository permissionRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private IEmailService emailService;

    @Override
    public ResponseDTO register(RegisterDTO registerDTO) throws Exception {
        // TODO: hacer transactional por si falla
        Optional<User> existingUser = userRepository.findByUsername(registerDTO.getUsername());
        if (existingUser.isPresent()) {
            throw new BadRequestException("El Correo Electrónico ingresado ya está registrado. Por favor ingrese otro.");
        }

        EmailValidation.validateEmail(registerDTO.getUsername());

        // create organization
        Organization organization = new Organization();
        organization.setCreatedDate(LocalDateTime.now());
        organization.setActive(false);
        organization.setBusinessName(registerDTO.getBusinessName());
        organization.setDefaultEmail(registerDTO.getUsername());

        // create user
        User user = new User();
        user.setCreatedDate(LocalDateTime.now());
        user.setActive(false);
        user.setOrganization(organization);
        user.setLastName(registerDTO.getLastName());
        user.setFirstName(registerDTO.getFirstName());
        user.setUsername(registerDTO.getUsername());
        user.setAdmin(true);
        user.setActivationKey(RandomString.make(20));
        user.setPassword(RandomString.make(40));

        // save all
        // TODO: hacer transacción
        organizationRepository.save(organization);
        userRepository.save(user);

        emailService.sendOrganizationActivationEmail(user);

        return new ResponseDTO(MapperHelper.modelMapper().map(user, UserDTO.class));
    }

    @Override
    public ResponseDTO activate(ActivateDTO activateDTO) throws Exception {
        Optional<User> user = userRepository.findByActivationKey(activateDTO.getActivationKey());
        if (!user.isPresent()) {
            throw new BadRequestException("La clave de activación no está asociada a ningún Usuario.");
        }

        user.ifPresent(u -> {
            if (u.getAdmin()) {
                u.getOrganization().setActive(true);
                addDefaultProfiles(u);
            }
            u.setActivationKey(null);
            u.setPassword(bCryptPasswordEncoder.encode(activateDTO.getPassword()));
            u.setActive(true);
        });

        // TODO: hacer transaccion
        profileRepository.saveAll(user.get().getProfiles());
        userRepository.save(user.get());
        organizationRepository.save(user.get().getOrganization());
        
        return new ResponseDTO(MapperHelper.modelMapper().map(user.get(), UserDTO.class));
    }

    private void addDefaultProfiles(User user) {
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAll());
        Profile profile = new Profile(user.getOrganization(), "Administrador", permissions);
        Set<Profile> profiles = new HashSet<>();
        profiles.add(profile);
        user.setProfiles(profiles);
    }

    @Override
    public ResponseDTO passwordResetRequest(PasswordResetRequestDTO passwordResetRequestDTO) throws Exception {
        Optional<User> user = userRepository.findByUsername(passwordResetRequestDTO.getUsername());
        if (!user.isPresent()) {
            throw new NoContentException("Usuario no encontrado.");
        }

        EmailValidation.validateEmail(passwordResetRequestDTO.getUsername());

        user.ifPresent(u -> {
            u.setResetDate(LocalDateTime.now());
            u.setResetKey(RandomString.make(20));
        });

        userRepository.save(user.get());

        emailService.sendPasswordResetRequestEmail(user.get());

        return new ResponseDTO(MapperHelper.modelMapper().map(user.get(), UserDTO.class));
    }

    @Override
    public ResponseDTO passwordReset(PasswordResetDTO passwordResetDTO) throws Exception {
        Optional<User> user = userRepository.findByResetKey(passwordResetDTO.getResetKey());
        if (!user.isPresent()) {
            throw new BadRequestException("La Clave de Reseteo no está asociada a ningún Usuario.");
        }

        if (user.get().getResetDate().isAfter(LocalDateTime.now().plusHours(3))) {
            throw new BadRequestException("Su Clave de Reseteo ha expirado.");
        }

        user.ifPresent(u -> {
            u.setResetDate(null);
            u.setResetKey(null);
            u.setPassword(bCryptPasswordEncoder.encode(passwordResetDTO.getPassword()));
        });

        userRepository.save(user.get());
        
        return new ResponseDTO(MapperHelper.modelMapper().map(user.get(), UserDTO.class));
    }

    @Override
    public ResponseDTO passwordChange(String token, PasswordChangeDTO passwordChangeDTO) throws Exception {
        Optional<User> user = userRepository.findByUsernameAndOrganizationId(passwordChangeDTO.getUsername(), OrganizationHelper.getOrganizationId(token));
        if (!user.isPresent()) {
            throw new NoContentException("Usuario no encontrado.");
        }

        if (!bCryptPasswordEncoder.matches(passwordChangeDTO.getCurrentPassword(), user.get().getPassword())) {
            throw new BadRequestException("Su Contraseña actual es inválida.");
        }

        user.get().setPassword(bCryptPasswordEncoder.encode(passwordChangeDTO.getPassword()));

        userRepository.save(user.get());
        
        return new ResponseDTO(MapperHelper.modelMapper().map(user.get(), UserDTO.class));
    }

}
