package com.cairedine.finance.app.user.domain.service;

import com.cairedine.finance.app.user.UserContext;
import org.springframework.security.oauth2.jwt.Jwt;


public interface IUserSyncService {

    UserContext syncAndBuildContext(Jwt jwt);
}