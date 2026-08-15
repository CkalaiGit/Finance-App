@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "shared::exceptions",
        "shared::web",
        "webclient",
        "financialanalysis",
        "watchlist"
    }
)
package com.cairedine.finance.app.intelligence;
