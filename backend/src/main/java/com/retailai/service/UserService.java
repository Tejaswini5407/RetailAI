package com.retailai.service;

import com.retailai.domain.Organization;
import com.retailai.domain.User;
import com.retailai.domain.UserRole;
import com.retailai.dto.user.CreateUserRequestDto;
import com.retailai.dto.user.UpdateUserRequestDto;
import com.retailai.dto.user.UserResponseDto;
import com.retailai.exception.EmailAlreadyExistsException;
import com.retailai.exception.OrganizationNotFoundException;
import com.retailai.exception.ResourceNotFoundException;
import com.retailai.repository.OrganizationRepository;
import com.retailai.repository.UserRepository;
import com.retailai.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
        UserRepository userRepository,
        OrganizationRepository organizationRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponseDto createUser(CreateUserRequestDto request) {
        Long tenantId = TenantContext.getTenantId();
        Organization organization = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found for ID: " + tenantId));

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email '" + request.getEmail() + "' is already registered");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(organization, request.getEmail(), request.getName(), hashedPassword, request.getRole());
        User savedUser = userRepository.save(user);

        return mapToDto(savedUser);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUsers(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        return userRepository.findByOrganizationId(tenantId, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        User user = userRepository.findByOrganizationIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return mapToDto(user);
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UpdateUserRequestDto request) {
        Long tenantId = TenantContext.getTenantId();
        User user = userRepository.findByOrganizationIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return mapToDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        Long tenantId = TenantContext.getTenantId();
        User user = userRepository.findByOrganizationIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        userRepository.delete(user);
    }

    public UserResponseDto mapToDto(User user) {
        return new UserResponseDto(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getOrganization().getId(),
            user.getOrganization().getCode(),
            user.getRole(),
            user.getCreatedAt()
        );
    }
}
