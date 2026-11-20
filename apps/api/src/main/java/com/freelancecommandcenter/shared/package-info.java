/**
 * Shared kernel used by every module: identifiers, platform codes, money, pagination, the error model and HTTP client
 * settings. Kept deliberately small.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Shared kernel",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.freelancecommandcenter.shared;
