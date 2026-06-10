package com.lumiroom.feature.ar.engine

import com.google.ar.core.Config
import com.google.ar.core.Session

object LumiroomArSessionManager {

    /**
     * Configures the ARCore Session for horizontal plane detection and optimal lighting.
     */
    fun configureSession(session: Session, config: Config) {
        // Enable horizontal planes for floor detection
        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        
        // Optimize for depth if available
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.depthMode = Config.DepthMode.AUTOMATIC
        }

        // Enable light estimation for realistic shadows
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

        // Adjust focus mode
        config.focusMode = Config.FocusMode.AUTO
    }
}
