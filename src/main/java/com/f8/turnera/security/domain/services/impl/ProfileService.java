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

import com.f8.turnera.domain.dtos.OrganizationDTO;
import com.f8.turnera.domain.entities.Organization;
import com.f8.turnera.domain.services.IOrganizationService;
import com.f8.turnera.exception.BadRequestException;
import com.f8.turnera.exception.NoContentException;
import com.f8.turnera.security.domain.dtos.PermissionDTO;
import com.f8.turnera.security.domain.dtos.ProfileDTO;
import com.f8.turnera.security.domain.dtos.ProfileFilterDTO;
import com.f8.turnera.security.domain.dtos.ResponseDTO;
import com.f8.turnera.security.domain.entities.Permission;
import com.f8.turnera.security.domain.entities.Profile;
import com.f8.turnera.security.domain.entities.User;
import com.f8.turnera.security.domain.repositories.IProfileRepository;
import com.f8.turnera.security.domain.services.IPermissionService;
import com.f8.turnera.security.domain.services.IProfileService;
import com.f8.turnera.util.Constants;
import com.f8.turnera.util.MapperHelper;
import com.f8.turnera.util.OrganizationHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProfileService implements IProfileService {

    @Autowired
    private IProfileRepository profileRepository;

    @Autowired
    private IOrganizationService organizationService;

    @Autowired
    private IPermissionService permissionService;

    @Autowired
    private EntityManager em;

    @Override
    public ResponseDTO findAllByFilter(String token, ProfileFilterDTO filter) throws Exception {
        filter.setOrganizationId(OrganizationHelper.getOrganizationId(token));

        Page<Profile> profiles = findByCriteria(filter);
        return new ResponseDTO(HttpStatus.OK.value(), profiles.map(profile -> MapperHelper.modelMapper().map(profile, ProfileDTO.class)));
    }

    private Page<Profile> findByCriteria(ProfileFilterDTO filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Profile> cq = cb.createQuery(Profile.class);

        List<Predicate> predicates = new ArrayList<>();

        Root<Profile> root = cq.from(Profile.class);
        predicates.add(cb.equal(root.join("organization", JoinType.LEFT), filter.getOrganizationId()));
        if (filter.getDescription() != null) {
            Predicate predicate = cb.like(cb.lower(root.get("description")),
                    "%" + filter.getDescription().toLowerCase() + "%");
            predicates.add(predicate);
        }
        if (filter.getActive() != null) {
            Predicate predicate = cb.equal(root.get("active"), filter.getActive());
            predicates.add(predicate);
        }

        cq.where(predicates.toArray(new Predicate[0]));

        List<Profile> result = em.createQuery(cq).getResultList();
        if (filter.getSort() != null) {
            if (filter.getSort().get(0).equals("ASC")) {
                switch (filter.getSort().get(1)) {
                case "description":
                    result.sort(Comparator.comparing(Profile::getDescription, String::compareToIgnoreCase));
                    break;
                case "active":
                    result.sort(Comparator.comparing(Profile::getActive));
                    break;
                default:
                    break;
                }
            } else if (filter.getSort().get(0).equals("DESC")) {
                switch (filter.getSort().get(1)) {
                case "description":
                    result.sort(Comparator.comparing(Profile::getDescription, String::compareToIgnoreCase).reversed());
                    break;
                case "active":
                    result.sort(Comparator.comparing(Profile::getActive).reversed());
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
        return new PageImpl<Profile>(result.subList(fromIndex, toIndex), pageable, count);
    }

    @Override
    public ResponseDTO findById(String token, Long id) throws Exception {
        Optional<Profile> profile = profileRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!profile.isPresent()) {
            throw new NoContentException("Perfil no encontrado - " + id);
        }

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(profile.get(), ProfileDTO.class));
    }

    @Override
    public ResponseDTO create(String token, ProfileDTO profileDTO) throws Exception {
        OrganizationDTO organization = (OrganizationDTO) organizationService.findById(token).getData();

        Profile profile = MapperHelper.modelMapper().map(profileDTO, Profile.class);
        profile.setCreatedDate(LocalDateTime.now());
        profile.setActive(true);
        profile.setOrganization(MapperHelper.modelMapper().map(organization, Organization.class));
        profile.setPermissions(addPermissions(profileDTO));

        profileRepository.save(profile);

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(profile, ProfileDTO.class));
    }

    @Override
    public ResponseDTO update(String token, ProfileDTO profileDTO) throws Exception {
        Optional<Profile> profile = profileRepository.findByIdAndOrganizationId(profileDTO.getId(), OrganizationHelper.getOrganizationId(token));
        if (!profile.isPresent()) {
            throw new NoContentException("Perfil no encontrado - " + profileDTO.getId());
        }

        if (profile.get().getActive() && !profileDTO.getActive()
                && profile.get().getUsers().stream().filter(User::getActive).count() > 0) {
            throw new BadRequestException(
                    "Existen Usuarios activos con este Perfil asociado. Primero debe modificar los Usuarios para continuar.");
        }

        Set<Permission> newPermissions = addPermissions(profileDTO);

        profile.ifPresent(p -> {
            p.setActive(profileDTO.getActive());
            p.setDescription(profileDTO.getDescription());
            p.setPermissions(newPermissions);

            profileRepository.save(p);
        });

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(profile.get(), ProfileDTO.class));
    }

    private Set<Permission> addPermissions(ProfileDTO profileDTO) throws Exception {
        if (profileDTO.getPermissions().stream().filter(x -> x.getCode().equals("home.index")).count() == 0) {
            PermissionDTO permissionHomeIndex = permissionService.findByCode("home.index");
            profileDTO.getPermissions().add(permissionHomeIndex);
        }

        Set<Permission> newPermissions = new HashSet<>();
        for (PermissionDTO permission : profileDTO.getPermissions()) {
            newPermissions.add(MapperHelper.modelMapper().map(permission, Permission.class));
        }
        return newPermissions;
    }

    @Override
    public ResponseDTO deleteById(String token, Long id) throws Exception {
        Optional<Profile> profile = profileRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!profile.isPresent()) {
            throw new NoContentException("Perfil no encontrado - " + id);
        }

        profileRepository.delete(profile.get());

        return new ResponseDTO(HttpStatus.OK.value(), "Borrado exitoso!");
    }
}
