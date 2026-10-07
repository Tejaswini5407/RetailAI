package com.retailai.service;

import com.retailai.domain.Organization;
import com.retailai.domain.User;
import com.retailai.dto.auth.AuthResponse;
import com.retailai.dto.auth.LoginRequest;
import com.retailai.dto.auth.RegisterRequest;
import com.retailai.dto.auth.UserSummaryDto;
import com.retailai.domain.UserRole;
import com.retailai.exception.EmailAlreadyExistsException;
import com.retailai.exception.InvalidCredentialsException;
import com.retailai.exception.OrganizationAlreadyExistsException;
import com.retailai.exception.OrganizationNotFoundException;
import com.retailai.repository.OrganizationRepository;
import com.retailai.repository.UserRepository;
import com.retailai.security.CustomUserDetails;
import com.retailai.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        UserRepository userRepository,
        OrganizationRepository organizationRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email '" + request.getEmail() + "' is already registered");
        }

        if (organizationRepository.existsByCode(request.getOrganizationCode())) {
            throw new OrganizationAlreadyExistsException(
                "Organization with code '" + request.getOrganizationCode() + "' already exists. Public registration cannot join an existing organization."
            );
        }

        String orgName = (request.getOrganizationName() != null && !request.getOrganizationName().isBlank())
                ? request.getOrganizationName()
                : request.getOrganizationCode() + " Organization";

        Organization organization = organizationRepository.save(new Organization(request.getOrganizationCode(), orgName));

        // Public registration always assigns ADMIN role to the founder of a new organization.
        // Client request role is ignored to prevent privilege escalation.
        UserRole assignedRole = UserRole.ADMIN;

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(organization, request.getEmail(), request.getName(), hashedPassword, assignedRole);
        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = jwtService.generateToken(userDetails);

        UserSummaryDto summary = new UserSummaryDto(
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getName(),
            organization.getId(),
            organization.getCode(),
            savedUser.getRole()
        );

        return new AuthResponse(token, jwtService.getExpirationMs(), summary);
    }

    @Transactional
    public UserSummaryDto createUserInTenant(RegisterRequest request, Long tenantId) {
        Organization organization = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found for ID: " + tenantId));

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email '" + request.getEmail() + "' is already registered");
        }

        UserRole roleToAssign = (request.getRole() != null) ? request.getRole() : UserRole.STORE_MANAGER;

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(organization, request.getEmail(), request.getName(), hashedPassword, roleToAssign);
        User savedUser = userRepository.save(user);

        return new UserSummaryDto(
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getName(),
            organization.getId(),
            organization.getCode(),
            savedUser.getRole()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        UserSummaryDto summary = new UserSummaryDto(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getOrganization().getId(),
            user.getOrganization().getCode(),
            user.getRole()
        );

        return new AuthResponse(token, jwtService.getExpirationMs(), summary);
    }
}
