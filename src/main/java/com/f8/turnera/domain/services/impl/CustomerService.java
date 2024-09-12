package com.f8.turnera.domain.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.f8.turnera.domain.dtos.CustomerDTO;
import com.f8.turnera.domain.dtos.CustomerFilterDTO;
import com.f8.turnera.domain.dtos.OrganizationDTO;
import com.f8.turnera.domain.dtos.ResponseDTO;
import com.f8.turnera.domain.entities.Customer;
import com.f8.turnera.domain.entities.Organization;
import com.f8.turnera.domain.repositories.ICustomerRepository;
import com.f8.turnera.domain.services.ICustomerService;
import com.f8.turnera.domain.services.IOrganizationService;
import com.f8.turnera.exception.NoContentException;
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

@Service
public class CustomerService implements ICustomerService {

    @Autowired
    private ICustomerRepository customerRepository;

    @Autowired
    private IOrganizationService organizationService;

    @Autowired
    private EntityManager em;

    @Override
    public ResponseDTO findAllByFilter(String token, CustomerFilterDTO filter) throws Exception {
        filter.setOrganizationId(OrganizationHelper.getOrganizationId(token));

        Page<Customer> customers = findByCriteria(filter);

        return new ResponseDTO(HttpStatus.OK.value(),
                customers.map(customer -> MapperHelper.modelMapper().map(customer, CustomerDTO.class)));
    }

    private Page<Customer> findByCriteria(CustomerFilterDTO filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Customer> cq = cb.createQuery(Customer.class);

        List<Predicate> predicates = new ArrayList<>();

        Root<Customer> root = cq.from(Customer.class);
        predicates.add(cb.equal(root.join("organization", JoinType.LEFT), filter.getOrganizationId()));
        if (filter.getAllProperties() != null) {
            Predicate predicate1 = cb.like(cb.lower(root.get("businessName")), "%" + filter.getAllProperties().toLowerCase() + "%");
            Predicate predicate2 = cb.like(cb.lower(root.get("email")), "%" + filter.getAllProperties().toLowerCase() + "%");
            Predicate predicate3 = cb.like(cb.lower(root.get("phone1")), "%" + filter.getAllProperties().toLowerCase() + "%");
            Predicate predicate4 = cb.like(cb.lower(root.get("cuit")), "%" + filter.getAllProperties().toLowerCase() + "%");
            Predicate finalPredicate = cb.or(predicate1, predicate2, predicate3, predicate4);
            predicates.add(finalPredicate);
        } else {
            if (filter.getBusinessName() != null) {
                Predicate predicate = cb.like(cb.lower(root.get("businessName")),
                        "%" + filter.getBusinessName().toLowerCase() + "%");
                predicates.add(predicate);
            }
            if (filter.getBrandName() != null) {
                Predicate predicate = cb.like(cb.lower(root.get("brandName")),
                        "%" + filter.getBrandName().toLowerCase() + "%");
                predicates.add(predicate);
            }
            if (filter.getEmail() != null) {
                Predicate predicate = cb.like(cb.lower(root.get("email")), "%" + filter.getEmail().toLowerCase() + "%");
                predicates.add(predicate);
            }
            if (filter.getPhone1() != null) {
                Predicate predicate = cb.like(cb.lower(root.get("phone1")), "%" + filter.getPhone1().toLowerCase() + "%");
                predicates.add(predicate);
            }
            if (filter.getCuit() != null) {
                Predicate predicate = cb.like(cb.lower(root.get("cuit")), "%" + filter.getCuit().toLowerCase() + "%");
                predicates.add(predicate);
            }
        }
        if (filter.getActive() != null) {
            Predicate predicate = cb.equal(root.get("active"), filter.getActive());
            predicates.add(predicate);
        }

        cq.where(predicates.toArray(new Predicate[0]));

        List<Customer> result = em.createQuery(cq).getResultList();
        if (filter.getSort() != null) {
            if (filter.getSort().get(0).equals("ASC")) {
                switch (filter.getSort().get(1)) {
                    case "businessName":
                        result.sort(Comparator.comparing(Customer::getBusinessName, String::compareToIgnoreCase));
                        break;
                    case "brandName":
                        result.sort(Comparator.comparing(Customer::getBrandName,
                                Comparator.nullsFirst(String::compareToIgnoreCase)));
                        break;
                    case "email":
                        result.sort(Comparator.comparing(Customer::getEmail, String::compareToIgnoreCase));
                        break;
                    case "phone1":
                        result.sort(Comparator.comparing(Customer::getPhone1, String::compareToIgnoreCase));
                        break;
                    default:
                        break;
                }
            } else if (filter.getSort().get(0).equals("DESC")) {
                switch (filter.getSort().get(1)) {
                    case "businessName":
                        result.sort(
                                Comparator.comparing(Customer::getBusinessName, String::compareToIgnoreCase)
                                        .reversed());
                        break;
                    case "brandName":
                        result.sort(Comparator
                                .comparing(Customer::getBrandName, Comparator.nullsFirst(String::compareToIgnoreCase))
                                .reversed());
                        break;
                    case "email":
                        result.sort(Comparator.comparing(Customer::getEmail, String::compareToIgnoreCase).reversed());
                        break;
                    case "phone1":
                        result.sort(Comparator.comparing(Customer::getPhone1, String::compareToIgnoreCase).reversed());
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
        return new PageImpl<Customer>(result.subList(fromIndex, toIndex), pageable, count);
    }

    @Override
    public ResponseDTO findById(String token, Long id) throws Exception {
        Optional<Customer> customer = customerRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!customer.isPresent()) {
            throw new NoContentException("Cliente no encontrado - " + id);
        }

        return new ResponseDTO(HttpStatus.OK.value(),
                MapperHelper.modelMapper().map(customer.get(), CustomerDTO.class));
    }

    @Override
    public ResponseDTO create(String token, CustomerDTO customerDTO) throws Exception {
        OrganizationDTO organization = (OrganizationDTO) organizationService.findById(token).getData();

        EmailValidation.validateEmail(customerDTO.getEmail());

        Customer customer = MapperHelper.modelMapper().map(customerDTO, Customer.class);
        customer.setCreatedDate(LocalDateTime.now());
        customer.setActive(true);
        customer.setOrganization(MapperHelper.modelMapper().map(organization, Organization.class));

        customerRepository.save(customer);

        return new ResponseDTO(HttpStatus.OK.value(), MapperHelper.modelMapper().map(customer, CustomerDTO.class));
    }

    @Override
    public Customer createQuick(CustomerDTO customerDTO, Organization organization) throws Exception {
        EmailValidation.validateEmail(customerDTO.getEmail());

        Customer customer = MapperHelper.modelMapper().map(customerDTO, Customer.class);
        customer.setCreatedDate(LocalDateTime.now());
        customer.setActive(true);
        customer.setOrganization(organization);

        customerRepository.save(customer);

        return customer;
    }

    @Override
    public ResponseDTO update(String token, CustomerDTO customerDTO) throws Exception {
        Optional<Customer> customer = customerRepository.findByIdAndOrganizationId(customerDTO.getId(), OrganizationHelper.getOrganizationId(token));
        if (!customer.isPresent()) {
            throw new NoContentException("Cliente no encontrado - " + customerDTO.getId());
        }

        EmailValidation.validateEmail(customerDTO.getEmail());

        customer.ifPresent(c -> {
            c.setActive(customerDTO.getActive());
            c.setBusinessName(customerDTO.getBusinessName());
            c.setBrandName(customerDTO.getBrandName());
            c.setCuit(customerDTO.getCuit());
            c.setAddress(customerDTO.getAddress());
            c.setPhone1(customerDTO.getPhone1());
            c.setPhone2(customerDTO.getPhone2());
            c.setEmail(customerDTO.getEmail());

            customerRepository.save(c);
        });

        return new ResponseDTO(HttpStatus.OK.value(),
                MapperHelper.modelMapper().map(customer.get(), CustomerDTO.class));
    }

    @Override
    public ResponseDTO deleteById(String token, Long id) throws Exception {
        Optional<Customer> customer = customerRepository.findByIdAndOrganizationId(id, OrganizationHelper.getOrganizationId(token));
        if (!customer.isPresent()) {
            throw new NoContentException("Cliente no encontrado - " + id);
        }

        customerRepository.delete(customer.get());

        return new ResponseDTO(HttpStatus.OK.value(), "Borrado exitoso!");
    }
}
