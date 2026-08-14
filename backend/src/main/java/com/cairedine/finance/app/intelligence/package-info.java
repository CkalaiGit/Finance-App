@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "shared::exceptions",
        "shared::web",
        "webclient",
        "financialanalysis"
    }
)
package com.cairedine.finance.app.intelligence;
