package com.cairedine.finance.app.user.domain.service;

import com.cairedine.finance.app.user.UserContext;
import com.cairedine.finance.app.user.domain.model.UserClaims;

public interface IUserSyncService {

    UserContext syncAndBuildContext(UserClaims claims);
}