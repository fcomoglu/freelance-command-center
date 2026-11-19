/**
 * Shared kernel used by every module: identifiers, money, time and the error model. Kept deliberately
 * small.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Shared kernel",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package dev.furkancomoglu.fcc.shared;
