package com.f8.turnera.security.domain.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.f8.turnera.config.TokenUtil;
import com.f8.turnera.domain.dtos.OrganizationDTO;
import com.f8.turnera.domain.entities.Organization;
import com.f8.turnera.domain.services.IEmailService;
import com.f8.turnera.domain.services.IOrganizationService;
import com.f8.turnera.exception.BadRequestException;
import com.f8.turnera.exception.NoContentException;
import com.f8.turnera.security.domain.dtos.ProfileDTO;
import com.f8.turnera.security.domain.dtos.ResponseDTO;
import com.f8.turnera.security.domain.dtos.UserDTO;
import com.f8.turnera.security.domain.dtos.UserFilterDTO;
import com.f8.turnera.security.domain.entities.Profile;
import com.f8.turnera.security.domain.entities.User;
import com.f8.turnera.security.domain.repositories.IUserRepository;
import com.f8.turnera.security.domain.services.IUserService;
import com.f8.turnera.util.Constants;
import com.f8.turnera.util.EmailValidation;
import com.f8.turnera.util.MapperHelper;
import com.f8.turnera.util.OrganizationHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import net.bytebuddy.utility.RandomString;

@Service
public class UserService implements IUserService {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IOrganizationService organizationService;

    @Autowired
    private IEmailService emailService;

    @Autowired
    private EntityManager em;

    @Override
    public ResponseDTO findAllByFilter(String token, UserFilterDTO filter) throws Exception {
        filter.setOrganizationId(
                OrganizationHelper.getOrganizationId(token));

        Page<User> users = findByCriteria(filter);
        return new ResponseDTO(HttpStatus.OK.value(), users.map(user -> MapperHelper.modelMapper().map(user, UserDTO.class)));
    }

    private Page<User> findByCriteria(UserFilterDTO filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> cq = cb.createQuery(User.class);

        List<Predicate> predicates = new ArrayList<>();

        Root<User> root = cq.from(User.class);
        predicates.add(cb.equal(root.join("organization", JoinType.LEFT), filter.getOrganizationId()));
        if (filter.getFirstName() != null) {
            Predicate predicate = cb.like(cb.lower(root.get("firstName")),
                    "%" + filter.getFirstName().toLowerCase() + "%");
            predicates.add(predicate);
        }
        if (filter.getLastName() != null) {
            Predicate predicate = cb.like(cb.lower(root.get("lastName")),
                    "%" + filter.getLastName().toLowerCase() + "%");
            predicates.add(predicate);
        }
        if (filter.getUsername() != null) {
            Predicate predicate = cb.like(cb.lower(root.get("username")),
                    "%" + filter.getUsername().toLowerCase() + "%");
            predicates.add(predicate);
        }
        if (filter.getActive() != null) {
            Predicate predicate = cb.equal(root.get("active"), filter.getActive());
            predicates.add(predicate);
        }

        cq.where(predicates.toArray(new Predicate[0]));

        List<User> result = em.createQuery(cq).getResultList();
        if (filter.getSort() != null) {
            if (filter.getSort().get(0).equals("ASC")) {
                switch (filter.getSort().get(1)) {
                    case "firstName":
                        result.sort(Comparator.comparing(User::getFirstName, String::compareToIgnoreCase));
                        break;
                    case "lastName":
                        result.sort(Comparator.comparing(User::getLastName, String::compareToIgnoreCase));
                        break;
                    case "username":
                        result.sort(Comparator.comparing(User::getUsername, String::compareToIgnoreCase));
                        break;
                    case "active":
                        result.sort(Comparator.comparing(User::getActive));
                        break;
                    default:
                        break;
                }
            } else if (filter.getSort().get(0).equals("DESC")) {
                switch (filter.getSort().get(1)) {
                    case "firstName":
                        result.sort(Comparator.comparing(User::getFirstName, String::compareToIgnoreCase).reversed());
                        break;
                    case "lastName":
                        result.sort(Comparator.comparing(User::getLastName, String::compareToIgnoreCase).reversed());
                        break;
                    case "username":
                        result.sort(Comparator.comparing(User::getUsername, String::compareToIgnoreCase).reversed());
                        break;
                    case "active":
                        result.sort(Comparator.comparing(User::getActive).reversed());
                        break;
                    default:
                        break;
                }
            }
        }
        int count = result.size();
        int fromIndex = Constants.ITEMS_PER_PAGE * (filter.getPage());
        int toIndex = fromIndex + Constants.ITEMS_PER_PAGE > count ? count : fromIndex + Constants.ITEMS_PER_PAGE;
        Pageable pageable = PageRequest.of(filter.getPage(), Constants.ITEMS_PER_PAGE);
        return new PageImpl<User>(result.subList(fromIndex, toIndex), pageable, count);
    }

    @Override
    public ResponseDTO findById(String token, Long id) throws Exception {
        Optional<User> user = userRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!user.isPresent()) {
            throw new NoContentException("Usuario no encontrado - " + id);
        }

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(user.get(), UserDTO.class));
    }

    @Override
    public ResponseDTO create(String token, UserDTO userDTO) throws Exception {
        OrganizationDTO organization = (OrganizationDTO) organizationService.findById(token).getData();

        Optional<User> existingUser = userRepository.findByUsername(userDTO.getUsername());
        if (existingUser.isPresent()) {
            throw new BadRequestException(
                    "El Correo Electrónico ingresado ya está registrado. Por favor ingrese otro.");
        }

        EmailValidation.validateEmail(userDTO.getUsername());

        User user = MapperHelper.modelMapper().map(userDTO, User.class);
        user.setCreatedDate(LocalDateTime.now());
        user.setOrganization(MapperHelper.modelMapper().map(organization, Organization.class));
        user.setActivationKey(RandomString.make(20));
        user.setProfiles(addPermissions(userDTO));
        user.setPassword("");
        user.setAdmin(false);

        emailService.sendAccountActivationEmail(user);

        userRepository.save(user);

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(user, UserDTO.class));
    }

    @Override
    public ResponseDTO update(String token, UserDTO userDTO) throws Exception {
        Optional<User> user = userRepository.findByIdAndOrganizationId(userDTO.getId(), OrganizationHelper.getOrganizationId(token));
        if (!user.isPresent()) {
            throw new NoContentException("Usuario no encontrado - " + userDTO.getId());
        }

        Optional<User> existingUser = userRepository.findByUsername(userDTO.getUsername());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userDTO.getId())) {
            throw new BadRequestException(
                    "El Correo Electrónico ingresado ya está registrado. Por favor ingrese otro.");
        }

        EmailValidation.validateEmail(userDTO.getUsername());

        user.ifPresent(u -> {
            u.setUsername(userDTO.getUsername());
            u.setActive(userDTO.getActive());
            u.setFirstName(userDTO.getFirstName());
            u.setLastName(userDTO.getLastName());
            u.setProfiles(addPermissions(userDTO));

            userRepository.save(u);
        });

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(user.get(), UserDTO.class));
    }

    private Set<Profile> addPermissions(UserDTO userDTO) {
        Set<Profile> newProfiles = new HashSet<>();
        for (ProfileDTO profile : userDTO.getProfiles()) {
            newProfiles.add(MapperHelper.modelMapper().map(profile, Profile.class));
        }
        return newProfiles;
    }

    @Override
    public ResponseDTO deleteById(String token, Long id) throws Exception {
        Optional<User> user = userRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!user.isPresent()) {
            throw new NoContentException("Usuario no encontrado - " + id);
        }

        if (TokenUtil.getUsernameWithoutToken().equals(user.get().getUsername())) {
            throw new BadRequestException("No puede eliminar su propio Usuario.");
        }

        userRepository.delete(user.get());

        return new ResponseDTO(HttpStatus.OK.value(), "Borrado exitoso!");
    }
}
