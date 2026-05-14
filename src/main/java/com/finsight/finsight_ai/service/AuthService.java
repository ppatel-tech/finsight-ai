package com.finsight.finsight_ai.service;

import com.finsight.finsight_ai.dto.request.LoginRequest;
import com.finsight.finsight_ai.dto.request.RegisterRequest;
import com.finsight.finsight_ai.dto.response.AuthResponse;
import com.finsight.finsight_ai.entity.RefreshToken;
import com.finsight.finsight_ai.entity.User;
import com.finsight.finsight_ai.exception.DuplicateResourceException;
import com.finsight.finsight_ai.exception.ResourceNotFoundException;
import com.finsight.finsight_ai.repository.RefreshTokenRepository;
import com.finsight.finsight_ai.repository.UserRepository;
import com.finsight.finsight_ai.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${application.jwt.expiration}")
    private long refreshExpiration;


    @Transactional
    public AuthResponse register(RegisterRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
            throw new DuplicateResourceException("Email already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();
        userRepository.save(user);



    String accessToken = jwtService.generateToken(user.getEmail());
    String refreshToken = createRefreshToken(user);

    return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .email(user.getEmail())
            .name(user.getName())
            .build();
    }




    @Transactional
    public AuthResponse login(LoginRequest request){

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));


        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .name(user.getName())
                .build();

    }


    private String createRefreshToken(User user){

        refreshTokenRepository.deleteByUser(user);

       RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                .user(user)
                .build();


       refreshTokenRepository.save(refreshToken);
       return refreshToken.getToken();


    }



}
