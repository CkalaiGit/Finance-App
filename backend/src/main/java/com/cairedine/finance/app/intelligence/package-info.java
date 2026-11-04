@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "shared::exceptions",
        "shared::web",
        "financialanalysis::port",
        "financialanalysis::model",
        "watchlist"
    }
)
package com.cairedine.finance.app.intelligence;
