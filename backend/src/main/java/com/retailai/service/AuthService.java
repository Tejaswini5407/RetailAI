package com.retailai.service;

import com.retailai.domain.Organization;
import com.retailai.domain.User;
import com.retailai.dto.auth.AuthResponse;
import com.retailai.dto.auth.LoginRequest;
import com.retailai.dto.auth.RegisterRequest;
import com.retailai.dto.auth.UserSummaryDto;
import com.retailai.exception.EmailAlreadyExistsException;
import com.retailai.exception.InvalidCredentialsException;
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

        Organization organization = organizationRepository.findByCode(request.getOrganizationCode())
                .orElseGet(() -> {
                    String orgName = (request.getOrganizationName() != null && !request.getOrganizationName().isBlank())
                            ? request.getOrganizationName()
                            : request.getOrganizationCode() + " Organization";
                    return organizationRepository.save(new Organization(request.getOrganizationCode(), orgName));
                });

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(organization, request.getEmail(), request.getName(), hashedPassword, request.getRole());
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
